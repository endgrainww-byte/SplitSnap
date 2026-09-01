package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.ALL_MAJOR_CURRENCIES
import com.example.model.DEFAULT_CURRENCY
import com.example.model.UserSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        "splitsnap_user_settings",
        Context.MODE_PRIVATE
    )

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<UserSettings> = _settings.asStateFlow()

    private fun loadSettings(): UserSettings {
        val currencyCode = prefs.getString("currency_code", DEFAULT_CURRENCY.code) ?: DEFAULT_CURRENCY.code
        val currency = ALL_MAJOR_CURRENCIES.find { it.code.equals(currencyCode, ignoreCase = true) }
            ?: DEFAULT_CURRENCY

        val tip = prefs.getFloat("tip_percentage", 10.0f).toDouble()

        return UserSettings(
            currency = currency,
            tipPercentage = tip
        )
    }

    fun updateCurrency(currencyCode: String) {
        val currency = ALL_MAJOR_CURRENCIES.find { it.code.equals(currencyCode, ignoreCase = true) }
            ?: DEFAULT_CURRENCY
        prefs.edit().putString("currency_code", currency.code).apply()
        _settings.value = _settings.value.copy(currency = currency)
    }

    fun updateTipPercentage(tipPercentage: Double) {
        val clampedTip = tipPercentage.coerceIn(0.0, 100.0)
        prefs.edit().putFloat("tip_percentage", clampedTip.toFloat()).apply()
        _settings.value = _settings.value.copy(tipPercentage = clampedTip)
    }

    fun resetToDefaults() {
        prefs.edit()
            .putString("currency_code", DEFAULT_CURRENCY.code)
            .remove("vat_percentage")
            .putFloat("tip_percentage", 10.0f)
            .apply()

        _settings.value = UserSettings(
            currency = DEFAULT_CURRENCY,
            tipPercentage = 10.0
        )
    }
}
