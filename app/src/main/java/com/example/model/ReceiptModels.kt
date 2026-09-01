package com.example.model

data class CurrencyOption(
    val code: String,
    val name: String,
    val symbol: String
)

val ALL_MAJOR_CURRENCIES = listOf(
    CurrencyOption(code = "ZAR", name = "South African Rand", symbol = "R"),
    CurrencyOption(code = "USD", name = "US Dollar", symbol = "$"),
    CurrencyOption(code = "EUR", name = "Euro", symbol = "€"),
    CurrencyOption(code = "GBP", name = "British Pound", symbol = "£"),
    CurrencyOption(code = "CAD", name = "Canadian Dollar", symbol = "CA$"),
    CurrencyOption(code = "AUD", name = "Australian Dollar", symbol = "A$"),
    CurrencyOption(code = "JPY", name = "Japanese Yen", symbol = "¥"),
    CurrencyOption(code = "CHF", name = "Swiss Franc", symbol = "CHF"),
    CurrencyOption(code = "CNY", name = "Chinese Yuan", symbol = "¥"),
    CurrencyOption(code = "INR", name = "Indian Rupee", symbol = "₹"),
    CurrencyOption(code = "SGD", name = "Singapore Dollar", symbol = "S$"),
    CurrencyOption(code = "NZD", name = "New Zealand Dollar", symbol = "NZ$"),
    CurrencyOption(code = "BRL", name = "Brazilian Real", symbol = "R$"),
    CurrencyOption(code = "MXN", name = "Mexican Peso", symbol = "Mex$"),
    CurrencyOption(code = "AED", name = "UAE Dirham", symbol = "AED"),
    CurrencyOption(code = "SEK", name = "Swedish Krona", symbol = "kr"),
    CurrencyOption(code = "NOK", name = "Norwegian Krone", symbol = "kr"),
    CurrencyOption(code = "DKK", name = "Danish Kunde", symbol = "kr"),
    CurrencyOption(code = "PLN", name = "Polish Złoty", symbol = "zł"),
    CurrencyOption(code = "NGN", name = "Nigerian Naira", symbol = "₦"),
    CurrencyOption(code = "KES", name = "Kenyan Shilling", symbol = "KSh")
)

val DEFAULT_CURRENCY = ALL_MAJOR_CURRENCIES[0] // ZAR (South African Rand)

data class UserSettings(
    val currency: CurrencyOption = DEFAULT_CURRENCY,
    val tipPercentage: Double = 10.0
)

data class ReceiptItem(
    val name: String,
    val price: Double,
    val quantity: Int = 1,
    val claimedQuantity: Int = 1,
    val isSelected: Boolean = false
) {
    val effectiveClaimedQuantity: Int
        get() = if (isSelected) claimedQuantity.coerceIn(1, quantity.coerceAtLeast(1)) else 0

    val claimedTotal: Double
        get() = price * effectiveClaimedQuantity

    val itemTotal: Double
        get() = price * quantity
}

data class ReceiptResult(
    val currency: String = DEFAULT_CURRENCY.symbol,
    val serviceFee: Double = 10.0,
    val items: List<ReceiptItem> = emptyList()
)

data class SplitCalculation(
    val checkedCount: Int = 0,
    val totalCount: Int = 0,
    val checkedSubtotal: Double = 0.0,
    val totalSubtotal: Double = 0.0,
    val proportionalServiceFee: Double = 0.0,
    val totalDue: Double = 0.0,
    val currency: String = DEFAULT_CURRENCY.symbol
)
