package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ALL_MAJOR_CURRENCIES
import com.example.model.CurrencyOption
import com.example.model.UserSettings
import com.example.ui.theme.AiBannerBg
import com.example.ui.theme.BorderGreen
import com.example.ui.theme.CanvasBackground
import com.example.ui.theme.FooterBg
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.SageContainer
import com.example.ui.theme.SageContainerText
import com.example.ui.theme.SelectedCardBg
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    userSettings: UserSettings,
    onCurrencySelected: (String) -> Unit,
    onTipChanged: (Double) -> Unit,
    onResetDefaults: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    var currencyDropdownExpanded by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SettingsTopBar(
                onBackClick = onBackClick,
                onResetClick = onResetDefaults
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .testTag("settings_screen_list"),
            contentPadding = PaddingValues(top = 8.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: Currency Selector Dropdown
            item {
                SettingsCard(
                    icon = Icons.Default.MonetizationOn,
                    title = "Currency",
                    subtitle = "Select default currency for receipt scanning & bill calculation"
                ) {
                    ExposedDropdownMenuBox(
                        expanded = currencyDropdownExpanded,
                        onExpandedChange = { currencyDropdownExpanded = !currencyDropdownExpanded },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("currency_dropdown_box")
                    ) {
                        OutlinedTextField(
                            value = "${userSettings.currency.code} - ${userSettings.currency.name} (${userSettings.currency.symbol})",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = currencyDropdownExpanded)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                                .testTag("currency_dropdown_anchor"),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ForestGreen,
                                unfocusedBorderColor = BorderGreen,
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        )

                        ExposedDropdownMenu(
                            expanded = currencyDropdownExpanded,
                            onDismissRequest = { currencyDropdownExpanded = false },
                            modifier = Modifier
                                .background(Color.White)
                                .border(1.dp, BorderGreen, RoundedCornerShape(8.dp))
                                .testTag("currency_dropdown_menu")
                        ) {
                            ALL_MAJOR_CURRENCIES.forEach { currency ->
                                val isSelected = currency.code == userSettings.currency.code
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = currency.code,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 14.sp,
                                                        color = if (isSelected) ForestGreen else TextPrimary
                                                    )
                                                    if (currency.code == "ZAR") {
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Box(
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(4.dp))
                                                                .background(SageContainer)
                                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                                        ) {
                                                            Text(
                                                                text = "Default",
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = SageContainerText
                                                            )
                                                        }
                                                    }
                                                }
                                                Text(
                                                    text = currency.name,
                                                    fontSize = 11.sp,
                                                    color = TextSecondary
                                                )
                                            }

                                            Text(
                                                text = currency.symbol,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 15.sp,
                                                color = if (isSelected) ForestGreen else TextPrimary
                                            )
                                        }
                                    },
                                    onClick = {
                                        onCurrencySelected(currency.code)
                                        currencyDropdownExpanded = false
                                    },
                                    trailingIcon = if (isSelected) {
                                        {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Selected",
                                                tint = ForestGreen,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    } else null,
                                    modifier = Modifier.testTag("currency_option_${currency.code}")
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Quick info chip
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(AiBannerBg)
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Active symbol: ${userSettings.currency.symbol} (${userSettings.currency.code})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = ForestGreen
                        )
                    }
                }
            }

            // Section 2: Tip / Gratuity Percentage
            item {
                SettingsCard(
                    icon = Icons.Default.Tune,
                    title = "Tip / Gratuity",
                    subtitle = "Default tip percentage recommended for restaurant and service bills"
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Rate: ${String.format(Locale.US, "%.1f", userSettings.tipPercentage)}%",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = ForestGreen
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            IconButton(
                                onClick = { onTipChanged((userSettings.tipPercentage - 1.0).coerceAtLeast(0.0)) },
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                                    .border(1.dp, BorderGreen, CircleShape)
                                    .testTag("tip_minus_button")
                            ) {
                                Icon(Icons.Default.Remove, "Decrease Tip", tint = ForestGreen, modifier = Modifier.size(16.dp))
                            }

                            IconButton(
                                onClick = { onTipChanged((userSettings.tipPercentage + 1.0).coerceAtMost(50.0)) },
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                                    .border(1.dp, BorderGreen, CircleShape)
                                    .testTag("tip_plus_button")
                            ) {
                                Icon(Icons.Default.Add, "Increase Tip", tint = ForestGreen, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Slider(
                        value = userSettings.tipPercentage.toFloat(),
                        onValueChange = { onTipChanged(it.toDouble()) },
                        valueRange = 0f..35f,
                        steps = 34,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("tip_slider"),
                        colors = SliderDefaults.colors(
                            thumbColor = ForestGreen,
                            activeTrackColor = ForestGreen,
                            inactiveTrackColor = BorderGreen
                        )
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Preset Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(0.0 to "0%", 10.0 to "10%", 12.5 to "12.5%", 15.0 to "15%", 20.0 to "20%").forEach { (rate, label) ->
                            val isCurrent = Math.abs(userSettings.tipPercentage - rate) < 0.1
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isCurrent) SageContainer else Color.White)
                                    .border(1.dp, if (isCurrent) ForestGreen else BorderGreen, RoundedCornerShape(8.dp))
                                    .clickable { onTipChanged(rate) }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 10.sp,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isCurrent) SageContainerText else TextPrimary
                                )
                            }
                        }
                    }
                }
            }

            // Section 3: Live Split Preview Card
            item {
                val sampleSubtotal = 200.0
                val sampleTip = sampleSubtotal * (userSettings.tipPercentage / 100.0)
                val sampleTotal = sampleSubtotal + sampleTip
                val sym = userSettings.currency.symbol

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(FooterBg)
                        .border(1.dp, BorderGreen, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                        .testTag("live_preview_card")
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "LIVE SPLIT PREVIEW",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = TextSecondary
                            )
                            Text(
                                text = "${userSettings.currency.code} (${userSettings.currency.symbol})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ForestGreen
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Sample Items Subtotal", fontSize = 12.sp, color = TextSecondary)
                            Text("$sym${String.format(Locale.US, "%.2f", sampleSubtotal)}", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                        }

                        if (userSettings.tipPercentage > 0) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Tip / Service (${String.format(Locale.US, "%.1f", userSettings.tipPercentage)}%)", fontSize = 12.sp, color = TextSecondary)
                                Text("$sym${String.format(Locale.US, "%.2f", sampleTip)}", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = BorderGreen.copy(alpha = 0.6f)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Total Calculated", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text(
                                "$sym${String.format(Locale.US, "%.2f", sampleTotal)}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = ForestGreen
                            )
                        }
                    }
                }
            }

            // Section 4: Done / Apply Button
            item {
                Button(
                    onClick = onBackClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("save_settings_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ForestGreen,
                        contentColor = Color.White
                    )
                ) {
                    Text("Done", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun SettingsTopBar(
    onBackClick: () -> Unit,
    onResetClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding(),
        color = MaterialTheme.colorScheme.background
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(AiBannerBg)
                        .testTag("settings_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = ForestGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column {
                    Text(
                        text = "Settings",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "CURRENCY, VAT & TIP PREFERENCES",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.8.sp,
                        color = TextSecondary
                    )
                }
            }

            // Reset Button
            IconButton(
                onClick = onResetClick,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(1.dp, BorderGreen, CircleShape)
                    .testTag("settings_reset_button")
            ) {
                Icon(
                    imageVector = Icons.Default.RestartAlt,
                    contentDescription = "Reset Defaults",
                    tint = ForestGreen,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun SettingsCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, BorderGreen, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(AiBannerBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = ForestGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            content()
        }
    }
}
