package com.example.data

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.model.ReceiptItem
import com.example.model.ReceiptResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

class NoReceiptItemsDetectedException(message: String) : Exception(message)

class GeminiReceiptParser {

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val primaryModel = "gemini-2.5-flash"
    private val fallbackModel = "gemini-3.5-flash"

    suspend fun parseReceiptImage(bitmap: Bitmap): ReceiptResult = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            throw IllegalStateException(
                "Gemini API key is not configured. Please add your key in the AI Studio Secrets panel."
            )
        }

        // Scale bitmap down if too large to optimize transfer and latency while maintaining readability
        val scaledBitmap = scaleBitmapDown(bitmap, maxDimension = 1800)
        val base64Image = scaledBitmap.toBase64()

        val prompt = """
            You are an expert AI receipt reader and OCR scanner for bill splitting.
            Analyze the provided receipt or invoice image and extract all purchased items, prices, quantities, and optional service charges/tips.

            Extract every line item (food, drink, retail item, service) along with its individual price and quantity.
            If the image shows a receipt with abbreviated names (e.g. "CHKN BRGR"), clean/expand them to readable names where obvious.
            If an item has a quantity greater than 1, specify the single unit price in "price" and the count in "quantity".
            If prices are shown as total line prices, calculate unit price = total line price / quantity.

            Return ONLY a valid JSON object matching this exact schema:
            {
              "currency": "$",
              "serviceFee": 10.0,
              "items": [
                {
                  "name": "Margherita Pizza",
                  "price": 14.50,
                  "quantity": 1
                }
              ]
            }

            Extraction guidelines:
            1. "currency": Symbol detected on the bill (e.g. $, EUR, GBP, JPY). Default to "$" if none visible.
            2. "serviceFee": Service charge, gratuity, or tip percentage as a number (e.g. 10.0 for 10% or (tip / subtotal) * 100). If none, return 0.0.
            3. "items": List of all purchased items. Do NOT include subtotal, tax, VAT, gratuity, tip, change, total, discount, or payment method as an item.
            4. If the image does not contain any readable receipt items or products, return an empty items list: "items": [].
            5. Output ONLY pure valid JSON without markdown wrapping.
        """.trimIndent()

        // Build Gemini REST payload
        val requestJson = JSONObject().apply {
            val contentsArray = JSONArray().apply {
                val contentObj = JSONObject().apply {
                    val partsArray = JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                        put(JSONObject().apply {
                            put("inlineData", JSONObject().apply {
                                put("mimeType", "image/jpeg")
                                put("data", base64Image)
                            })
                        })
                    }
                    put("parts", partsArray)
                }
                put(contentObj)
            }
            put("contents", contentsArray)

            val generationConfig = JSONObject().apply {
                put("temperature", 0.1)
                put("responseMimeType", "application/json")
            }
            put("generationConfig", generationConfig)
        }

        val requestBody = requestJson.toString().toRequestBody("application/json".toMediaType())

        // Try primary model first, fallback to secondary model if primary fails
        try {
            executeGeminiRequest(primaryModel, apiKey, requestBody)
        } catch (e: Exception) {
            Log.w("GeminiReceiptParser", "Primary model ($primaryModel) attempt failed: ${e.message}, trying fallback $fallbackModel", e)
            executeGeminiRequest(fallbackModel, apiKey, requestBody)
        }
    }

    private fun executeGeminiRequest(
        model: String,
        apiKey: String,
        requestBody: okhttp3.RequestBody
    ): ReceiptResult {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        val response = okHttpClient.newCall(request).execute()
        val responseBody = response.body?.string()

        if (!response.isSuccessful || responseBody.isNullOrBlank()) {
            val errorMsg = try {
                val errorObj = JSONObject(responseBody ?: "")
                errorObj.optJSONObject("error")?.optString("message") ?: response.message
            } catch (_: Exception) {
                "HTTP ${response.code}: ${response.message}"
            }
            throw IllegalStateException("AI Service Error: $errorMsg")
        }

        return parseGeminiResponse(responseBody)
    }

    private fun parseGeminiResponse(responseBody: String): ReceiptResult {
        val root: JSONObject
        val rawText: String
        try {
            root = JSONObject(responseBody)
            val candidates = root.optJSONArray("candidates")
            if (candidates == null || candidates.length() == 0) {
                throw IllegalStateException("No content generated from receipt image.")
            }

            val firstCandidate = candidates.getJSONObject(0)
            val content = firstCandidate.getJSONObject("content")
            val parts = content.getJSONArray("parts")
            rawText = parts.getJSONObject(0).getString("text").trim()
        } catch (e: Exception) {
            Log.e("GeminiReceiptParser", "Error parsing Gemini response envelope: $responseBody", e)
            throw IllegalStateException("AI response format error: ${e.localizedMessage ?: "Invalid response"}")
        }

        // Clean markdown codeblocks if present
        val cleanJson = rawText
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        val json = try {
            JSONObject(cleanJson)
        } catch (e: Exception) {
            Log.e("GeminiReceiptParser", "Error parsing JSON from candidate text: $cleanJson", e)
            throw IllegalStateException("Failed to parse receipt data: ${e.localizedMessage}")
        }

        val currency = json.optString("currency", "$").ifBlank { "$" }
        val serviceFee = json.optDouble("serviceFee", 0.0)

        val itemsArray = json.optJSONArray("items") ?: JSONArray()
        val itemsList = mutableListOf<ReceiptItem>()

        for (i in 0 until itemsArray.length()) {
            val itemObj = itemsArray.optJSONObject(i) ?: continue
            val name = itemObj.optString("name", "Item ${i + 1}").trim()
            val price = itemObj.optDouble("price", 0.0)
            val quantity = itemObj.optInt("quantity", 1).coerceAtLeast(1)

            if (name.isNotBlank() && price > 0) {
                itemsList.add(
                    ReceiptItem(
                        name = name,
                        price = price,
                        quantity = quantity,
                        isSelected = false
                    )
                )
            }
        }

        if (itemsList.isEmpty()) {
            throw NoReceiptItemsDetectedException(
                "Could not detect any receipt line items in this image. Please ensure the receipt is clear, well-lit, and flat in the camera frame, or try our sample receipt."
            )
        }

        return ReceiptResult(
            currency = currency,
            serviceFee = if (serviceFee.isNaN()) 0.0 else serviceFee,
            items = itemsList
        )
    }

    private fun scaleBitmapDown(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxDimension && height <= maxDimension) return bitmap

        val ratio = width.toFloat() / height.toFloat()
        val targetWidth: Int
        val targetHeight: Int
        if (width > height) {
            targetWidth = maxDimension
            targetHeight = (maxDimension / ratio).toInt()
        } else {
            targetHeight = maxDimension
            targetWidth = (maxDimension * ratio).toInt()
        }
        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
    }

    private fun Bitmap.toBase64(): String {
        val outputStream = ByteArrayOutputStream()
        compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }
}
