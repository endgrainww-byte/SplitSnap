package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.ALL_MAJOR_CURRENCIES
import com.example.model.DEFAULT_CURRENCY
import com.example.ui.SplitSnapUiState
import com.example.ui.SplitSnapViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("SplitSnap", appName)
  }

  @Test
  fun `default settings are ZAR with 10 percent tip`() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val viewModel = SplitSnapViewModel(app)
    val settings = viewModel.userSettings.value

    assertEquals("ZAR", settings.currency.code)
    assertEquals("R", settings.currency.symbol)
    assertEquals(10.0, settings.tipPercentage, 0.01)
  }

  @Test
  fun `all major currencies are available in settings`() {
    assertTrue(ALL_MAJOR_CURRENCIES.any { it.code == "ZAR" })
    assertTrue(ALL_MAJOR_CURRENCIES.any { it.code == "USD" })
    assertTrue(ALL_MAJOR_CURRENCIES.any { it.code == "EUR" })
    assertTrue(ALL_MAJOR_CURRENCIES.any { it.code == "GBP" })
    assertTrue(ALL_MAJOR_CURRENCIES.any { it.code == "JPY" })
    assertTrue(ALL_MAJOR_CURRENCIES.any { it.code == "CAD" })
    assertTrue(ALL_MAJOR_CURRENCIES.any { it.code == "AUD" })
    assertTrue(ALL_MAJOR_CURRENCIES.any { it.code == "INR" })
  }

  @Test
  fun `test currency and tip settings customization`() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val viewModel = SplitSnapViewModel(app)

    viewModel.updateSelectedCurrency("EUR")
    assertEquals("EUR", viewModel.userSettings.value.currency.code)
    assertEquals("€", viewModel.userSettings.value.currency.symbol)

    viewModel.updateDefaultTip(12.5)
    assertEquals(12.5, viewModel.userSettings.value.tipPercentage, 0.01)

    // Reset back to defaults
    viewModel.resetSettingsToDefaults()
    assertEquals("ZAR", viewModel.userSettings.value.currency.code)
    assertEquals(10.0, viewModel.userSettings.value.tipPercentage, 0.01)
  }

  @Test
  fun `test bill calculation with sample receipt and multiple item quantities in ZAR`() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val viewModel = SplitSnapViewModel(app)
    viewModel.resetSettingsToDefaults()
    viewModel.loadSampleReceipt()

    // Sample receipt has:
    // - Margherita Pizza: 5 on bill, claimed 2 @ 145.00 = 290.00 (selected)
    // - Craft IPA: 4 on bill, claimed 1 @ 65.00 = 65.00 (selected)
    // Total selected items = 290.00 + 65.00 = 355.00
    val successState = viewModel.uiState.value as SplitSnapUiState.Success
    val calc = successState.calculation
    assertEquals(2, calc.checkedCount)
    assertEquals(355.00, calc.checkedSubtotal, 0.01)
    assertEquals(1575.00, calc.totalSubtotal, 0.01)
    assertEquals("R", calc.currency)

    // Tip is 10.0% -> 355.00 * 0.10 = 35.50
    assertEquals(35.50, calc.proportionalServiceFee, 0.01)

    // Total due -> 355.00 + 35.50 = 390.50
    assertEquals(390.50, calc.totalDue, 0.01)

    // Increase pizza claim from 2 to 4
    viewModel.updateClaimedQuantity(itemIndex = 0, newQuantity = 4)
    val updatedCalc = (viewModel.uiState.value as SplitSnapUiState.Success).calculation
    // 4 pizzas @ 145.00 = 580.00 + 65.00 IPA = 645.00
    assertEquals(645.00, updatedCalc.checkedSubtotal, 0.01)
    assertEquals(64.50, updatedCalc.proportionalServiceFee, 0.01)
    assertEquals(709.50, updatedCalc.totalDue, 0.01)

    // Check share text includes portion details
    val shareText = viewModel.generateShareBreakdownText()
    assertTrue(shareText.contains("Wood-Fired Margherita Pizza (4 of 5 @ R145.00): R580.00"))
  }
}
