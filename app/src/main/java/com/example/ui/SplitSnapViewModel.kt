package com.example.ui

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.GeminiReceiptParser
import com.example.data.NoReceiptItemsDetectedException
import com.example.data.SettingsRepository
import com.example.model.DEFAULT_CURRENCY
import com.example.model.ReceiptItem
import com.example.model.ReceiptResult
import com.example.model.SplitCalculation
import com.example.model.UserSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.InputStream
import java.util.Locale

sealed interface SplitSnapUiState {
    data object Idle : SplitSnapUiState
    data class Loading(val message: String = "Scanning receipt with Gemini AI...") : SplitSnapUiState
    data class Success(
        val receipt: ReceiptResult,
        val calculation: SplitCalculation,
        val imageUri: Uri? = null,
        val lastUpdated: Long = System.currentTimeMillis()
    ) : SplitSnapUiState
    data class Error(val errorMessage: String) : SplitSnapUiState
}

class SplitSnapViewModel @JvmOverloads constructor(
    application: Application,
    private val receiptParser: GeminiReceiptParser = GeminiReceiptParser(),
    private val settingsRepository: SettingsRepository = SettingsRepository(application)
) : AndroidViewModel(application) {

    val userSettings: StateFlow<UserSettings> = settingsRepository.settings

    private val _uiState = MutableStateFlow<SplitSnapUiState>(SplitSnapUiState.Idle)
    val uiState: StateFlow<SplitSnapUiState> = _uiState.asStateFlow()

    private val _currentReceipt = MutableStateFlow<ReceiptResult?>(null)
    private val _currentImageUri = MutableStateFlow<Uri?>(null)

    // Calculate math engine reactively
    val calculationState: StateFlow<SplitCalculation> = _currentReceipt.combine(_currentImageUri) { receipt, _ ->
        computeCalculation(receipt)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = SplitCalculation()
    )

    fun updateSelectedCurrency(currencyCode: String) {
        settingsRepository.updateCurrency(currencyCode)
        val current = _currentReceipt.value
        if (current != null) {
            val symbol = userSettings.value.currency.symbol
            val updated = current.copy(currency = symbol)
            _currentReceipt.value = updated
            val calc = computeCalculation(updated)
            _uiState.value = SplitSnapUiState.Success(
                receipt = updated,
                calculation = calc,
                imageUri = _currentImageUri.value
            )
        }
    }

    fun updateDefaultTip(tip: Double) {
        settingsRepository.updateTipPercentage(tip)
    }

    fun resetSettingsToDefaults() {
        settingsRepository.resetToDefaults()
    }

    fun processReceiptUri(context: Context, uri: Uri?) {
        if (uri == null) {
            _uiState.value = SplitSnapUiState.Error("No image was selected. Please choose a receipt image from your gallery or snap a photo.")
            return
        }

        _uiState.value = SplitSnapUiState.Loading("Reading and preparing receipt image...")
        _currentImageUri.value = uri

        viewModelScope.launch {
            try {
                val bitmap = loadBitmapFromUri(context, uri)
                if (bitmap == null) {
                    _uiState.value = SplitSnapUiState.Error("Could not decode image. Please ensure the image is accessible and not corrupted.")
                    return@launch
                }

                _uiState.value = SplitSnapUiState.Loading("Extracting items with Gemini AI...")
                val rawResult = receiptParser.parseReceiptImage(bitmap)

                // Apply user settings if defaults are more suitable
                val currentSettings = userSettings.value
                val currencySymbol = if (rawResult.currency.isNotBlank() && rawResult.currency != "$") {
                    rawResult.currency
                } else {
                    currentSettings.currency.symbol
                }
                val tipRate = if (rawResult.serviceFee > 0) rawResult.serviceFee else currentSettings.tipPercentage

                val finalResult = rawResult.copy(
                    currency = currencySymbol,
                    serviceFee = tipRate
                )

                _currentReceipt.value = finalResult
                val calculation = computeCalculation(finalResult)
                _uiState.value = SplitSnapUiState.Success(
                    receipt = finalResult,
                    calculation = calculation,
                    imageUri = uri
                )
            } catch (e: NoReceiptItemsDetectedException) {
                _uiState.value = SplitSnapUiState.Error(
                    e.message ?: "No items could be found on this receipt. Please ensure the photo is clear and well-lit."
                )
            } catch (e: Exception) {
                _uiState.value = SplitSnapUiState.Error(
                    e.localizedMessage ?: "Failed to scan receipt. Please check your network connection or try again."
                )
            }
        }
    }

    fun toggleItemSelection(itemIndex: Int) {
        val current = _currentReceipt.value ?: return
        if (itemIndex !in current.items.indices) return

        val updatedItems = current.items.mapIndexed { idx, item ->
            if (idx == itemIndex) {
                val newSelected = !item.isSelected
                val newClaimed = if (newSelected && item.claimedQuantity <= 0) 1 else item.claimedQuantity
                item.copy(isSelected = newSelected, claimedQuantity = newClaimed)
            } else {
                item
            }
        }
        val updatedReceipt = current.copy(items = updatedItems)
        _currentReceipt.value = updatedReceipt

        val calculation = computeCalculation(updatedReceipt)
        _uiState.value = SplitSnapUiState.Success(
            receipt = updatedReceipt,
            calculation = calculation,
            imageUri = _currentImageUri.value
        )
    }

    fun updateClaimedQuantity(itemIndex: Int, newQuantity: Int) {
        val current = _currentReceipt.value ?: return
        if (itemIndex !in current.items.indices) return

        val targetItem = current.items[itemIndex]
        val maxQty = targetItem.quantity.coerceAtLeast(1)
        val clampedQty = newQuantity.coerceIn(0, maxQty)

        val updatedItems = current.items.mapIndexed { idx, item ->
            if (idx == itemIndex) {
                if (clampedQty == 0) {
                    item.copy(isSelected = false, claimedQuantity = 1)
                } else {
                    item.copy(isSelected = true, claimedQuantity = clampedQty)
                }
            } else {
                item
            }
        }
        val updatedReceipt = current.copy(items = updatedItems)
        _currentReceipt.value = updatedReceipt

        val calculation = computeCalculation(updatedReceipt)
        _uiState.value = SplitSnapUiState.Success(
            receipt = updatedReceipt,
            calculation = calculation,
            imageUri = _currentImageUri.value
        )
    }

    fun selectAll(select: Boolean) {
        val current = _currentReceipt.value ?: return
        val updatedItems = current.items.map { item ->
            if (select) {
                item.copy(
                    isSelected = true,
                    claimedQuantity = if (item.claimedQuantity <= 0) item.quantity else item.claimedQuantity
                )
            } else {
                item.copy(isSelected = false)
            }
        }
        val updatedReceipt = current.copy(items = updatedItems)
        _currentReceipt.value = updatedReceipt

        val calculation = computeCalculation(updatedReceipt)
        _uiState.value = SplitSnapUiState.Success(
            receipt = updatedReceipt,
            calculation = calculation,
            imageUri = _currentImageUri.value
        )
    }

    fun updateServiceFee(newFee: Double) {
        val current = _currentReceipt.value ?: return
        val updatedReceipt = current.copy(serviceFee = newFee.coerceAtLeast(0.0))
        _currentReceipt.value = updatedReceipt

        val calculation = computeCalculation(updatedReceipt)
        _uiState.value = SplitSnapUiState.Success(
            receipt = updatedReceipt,
            calculation = calculation,
            imageUri = _currentImageUri.value
        )
    }

    fun loadSampleReceipt() {
        _currentImageUri.value = null
        val currentSettings = userSettings.value
        val sample = ReceiptResult(
            currency = currentSettings.currency.symbol,
            serviceFee = currentSettings.tipPercentage,
            items = listOf(
                ReceiptItem(
                    name = "Wood-Fired Margherita Pizza",
                    price = 145.00,
                    quantity = 5,
                    claimedQuantity = 2,
                    isSelected = true
                ),
                ReceiptItem(
                    name = "Craft IPA Beer (Pint)",
                    price = 65.00,
                    quantity = 4,
                    claimedQuantity = 1,
                    isSelected = true
                ),
                ReceiptItem(
                    name = "Truffle Tagliatelle",
                    price = 185.00,
                    quantity = 1,
                    claimedQuantity = 1,
                    isSelected = false
                ),
                ReceiptItem(
                    name = "Burrata & Heirloom Tomato",
                    price = 120.00,
                    quantity = 1,
                    claimedQuantity = 1,
                    isSelected = false
                ),
                ReceiptItem(
                    name = "San Pellegrino Sparkling (750ml)",
                    price = 45.00,
                    quantity = 3,
                    claimedQuantity = 1,
                    isSelected = false
                ),
                ReceiptItem(
                    name = "Tiramisu al Mascarpone",
                    price = 75.00,
                    quantity = 2,
                    claimedQuantity = 1,
                    isSelected = false
                )
            )
        )
        _currentReceipt.value = sample
        val calculation = computeCalculation(sample)
        _uiState.value = SplitSnapUiState.Success(
            receipt = sample,
            calculation = calculation,
            imageUri = null
        )
    }

    fun resetState() {
        _currentReceipt.value = null
        _currentImageUri.value = null
        _uiState.value = SplitSnapUiState.Idle
    }

    fun generateShareBreakdownText(): String {
        val receipt = _currentReceipt.value ?: return ""
        val calc = computeCalculation(receipt)
        val selectedItems = receipt.items.filter { it.isSelected && it.effectiveClaimedQuantity > 0 }

        val sb = StringBuilder()
        sb.appendLine("🧾 *SplitSnap Bill Breakdown*")
        sb.appendLine("------------------------------")

        if (selectedItems.isEmpty()) {
            sb.appendLine("No items selected.")
        } else {
            sb.appendLine("*My Selected Items:*")
            selectedItems.forEach { item ->
                val claimed = item.effectiveClaimedQuantity
                val lineTotal = item.claimedTotal
                val qtyStr = if (item.quantity > 1) {
                    " (${claimed} of ${item.quantity} @ ${calc.currency}${String.format(Locale.US, "%.2f", item.price)})"
                } else {
                    " (@ ${calc.currency}${String.format(Locale.US, "%.2f", item.price)})"
                }
                sb.appendLine("• ${item.name}$qtyStr: ${calc.currency}${String.format(Locale.US, "%.2f", lineTotal)}")
            }
        }

        sb.appendLine("------------------------------")
        sb.appendLine("Items Subtotal: ${calc.currency}${String.format(Locale.US, "%.2f", calc.checkedSubtotal)}")
        if (receipt.serviceFee > 0) {
            sb.appendLine("Tip / Service (${String.format(Locale.US, "%.1f", receipt.serviceFee)}%): ${calc.currency}${String.format(Locale.US, "%.2f", calc.proportionalServiceFee)}")
        }
        sb.appendLine("------------------------------")
        sb.appendLine("💰 *TOTAL OWED: ${calc.currency}${String.format(Locale.US, "%.2f", calc.totalDue)}*")
        sb.appendLine("\n_Calculated with SplitSnap_")

        return sb.toString()
    }

    private fun computeCalculation(receipt: ReceiptResult?): SplitCalculation {
        if (receipt == null) return SplitCalculation()

        var totalSubtotal = 0.0
        var checkedSubtotal = 0.0
        var checkedCount = 0

        for (item in receipt.items) {
            totalSubtotal += item.itemTotal
            if (item.isSelected && item.effectiveClaimedQuantity > 0) {
                checkedSubtotal += item.claimedTotal
                checkedCount += 1
            }
        }

        val proportionalServiceFee = checkedSubtotal * (receipt.serviceFee / 100.0)
        val totalDue = checkedSubtotal + proportionalServiceFee

        return SplitCalculation(
            checkedCount = checkedCount,
            totalCount = receipt.items.size,
            checkedSubtotal = checkedSubtotal,
            totalSubtotal = totalSubtotal,
            proportionalServiceFee = proportionalServiceFee,
            totalDue = totalDue,
            currency = receipt.currency
        )
    }

    private fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
        return try {
            val originalBitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            } ?: return null

            val orientation = try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val exif = ExifInterface(stream)
                    exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                } ?: ExifInterface.ORIENTATION_NORMAL
            } catch (_: Exception) {
                ExifInterface.ORIENTATION_NORMAL
            }

            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> rotateBitmap(originalBitmap, 90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> rotateBitmap(originalBitmap, 180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> rotateBitmap(originalBitmap, 270f)
                else -> originalBitmap
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
