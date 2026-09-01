package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.ReceiptItem
import com.example.model.ReceiptResult
import com.example.model.SplitCalculation
import com.example.model.UserSettings
import com.example.ui.SettingsScreen
import com.example.ui.SplitSnapUiState
import com.example.ui.SplitSnapViewModel
import com.example.ui.theme.AiBannerBg
import com.example.ui.theme.BorderGreen
import com.example.ui.theme.BorderUnchecked
import com.example.ui.theme.CanvasBackground
import com.example.ui.theme.FooterBg
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.SageContainer
import com.example.ui.theme.SageContainerText
import com.example.ui.theme.SelectedCardBg
import com.example.ui.theme.SplitSnapTheme
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.io.File
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val viewModel: SplitSnapViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SplitSnapTheme {
                SplitSnapApp(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplitSnapApp(
    viewModel: SplitSnapViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val userSettings by viewModel.userSettings.collectAsStateWithLifecycle()

    var showSourceSheet by remember { mutableStateOf(false) }
    var isSettingsOpen by remember { mutableStateOf(false) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    // ActivityResultLauncher for modern Android Photo Picker
    val pickMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.processReceiptUri(context, uri)
        }
    }

    // ActivityResultLauncher for System Camera app capture via FileProvider
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success && pendingCameraUri != null) {
            viewModel.processReceiptUri(context, pendingCameraUri!!)
        }
    }

    val launchPicker = {
        pickMediaLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    val launchCamera = {
        try {
            val cacheFile = File.createTempFile("receipt_snap_", ".jpg", context.cacheDir)
            val photoUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                cacheFile
            )
            pendingCameraUri = photoUri
            takePictureLauncher.launch(photoUri)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    val shareBreakdown = {
        val shareText = viewModel.generateShareBreakdownText()
        if (shareText.isNotBlank()) {
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, shareText)
                type = "text/plain"
            }
            val shareIntent = Intent.createChooser(sendIntent, "Share Receipt Breakdown")
            context.startActivity(shareIntent)
        }
    }

    if (isSettingsOpen) {
        SettingsScreen(
            userSettings = userSettings,
            onCurrencySelected = { viewModel.updateSelectedCurrency(it) },
            onTipChanged = { viewModel.updateDefaultTip(it) },
            onResetDefaults = { viewModel.resetSettingsToDefaults() },
            onBackClick = { isSettingsOpen = false }
        )
        return
    }

    val currentSuccessState = uiState as? SplitSnapUiState.Success
    val calculation = currentSuccessState?.calculation ?: SplitCalculation(currency = userSettings.currency.symbol)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            HighDensityHeader(
                isLoading = uiState is SplitSnapUiState.Loading,
                hasReceipt = currentSuccessState != null,
                onUploadClick = { showSourceSheet = true },
                onSettingsClick = { isSettingsOpen = true },
                onClearClick = { viewModel.resetState() }
            )
        },
        bottomBar = {
            if (currentSuccessState != null) {
                HighDensityStickyFooter(
                    calculation = calculation,
                    serviceFee = currentSuccessState.receipt.serviceFee,
                    onShareClick = shareBreakdown,
                    onUploadClick = { showSourceSheet = true },
                    onServiceChange = { viewModel.updateServiceFee(it) }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val state = uiState) {
                is SplitSnapUiState.Idle -> {
                    HighDensityEmptyState(
                        userSettings = userSettings,
                        onCameraClick = launchCamera,
                        onGalleryClick = launchPicker,
                        onSampleClick = { viewModel.loadSampleReceipt() },
                        onSettingsClick = { isSettingsOpen = true }
                    )
                }

                is SplitSnapUiState.Loading -> {
                    HighDensityLoadingView(message = state.message)
                }

                is SplitSnapUiState.Error -> {
                    HighDensityErrorView(
                        errorMessage = state.errorMessage,
                        onCameraClick = launchCamera,
                        onGalleryClick = launchPicker,
                        onSampleClick = { viewModel.loadSampleReceipt() }
                    )
                }

                is SplitSnapUiState.Success -> {
                    HighDensityReceiptContent(
                        receipt = state.receipt,
                        calculation = state.calculation,
                        imageUri = state.imageUri,
                        onToggleItem = { index -> viewModel.toggleItemSelection(index) },
                        onUpdateQuantity = { index, qty -> viewModel.updateClaimedQuantity(index, qty) },
                        onSelectAll = { select -> viewModel.selectAll(select) }
                    )
                }
            }
        }
    }

    // Photo Source Selection Bottom Sheet
    if (showSourceSheet) {
        PhotoSourceBottomSheet(
            onDismiss = { showSourceSheet = false },
            onCameraSelect = {
                showSourceSheet = false
                launchCamera()
            },
            onGallerySelect = {
                showSourceSheet = false
                launchPicker()
            },
            onSampleSelect = {
                showSourceSheet = false
                viewModel.loadSampleReceipt()
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoSourceBottomSheet(
    onDismiss: () -> Unit,
    onCameraSelect: () -> Unit,
    onGallerySelect: () -> Unit,
    onSampleSelect: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = CanvasBackground,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp)
                .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
                .testTag("photo_source_bottom_sheet")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Add Receipt",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Choose how you'd like to scan your bill",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(AiBannerBg)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = ForestGreen,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Option 1: Take Photo with Camera
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White)
                    .border(1.dp, BorderGreen, RoundedCornerShape(14.dp))
                    .clickable(onClick = onCameraSelect)
                    .padding(14.dp)
                    .testTag("source_camera_option")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(SageContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Camera",
                            tint = ForestGreen,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Take Photo with Camera",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Snap a picture of your physical receipt",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Option 2: Choose from Photo Gallery
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White)
                    .border(1.dp, BorderGreen, RoundedCornerShape(14.dp))
                    .clickable(onClick = onGallerySelect)
                    .padding(14.dp)
                    .testTag("source_gallery_option")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(AiBannerBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = "Gallery",
                            tint = ForestGreen,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Choose from Gallery",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Select an existing photo from your device",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Option 3: Sample Receipt
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White)
                    .border(1.dp, BorderGreen, RoundedCornerShape(14.dp))
                    .clickable(onClick = onSampleSelect)
                    .padding(14.dp)
                    .testTag("source_sample_option")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(SageContainer.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Sample Receipt",
                            tint = ForestGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Try Sample Dinner Receipt",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Test instant bill splitting with demo data",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HighDensityHeader(
    isLoading: Boolean,
    hasReceipt: Boolean,
    onUploadClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onClearClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "SplitSnap",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.5).sp,
                    color = ForestGreen
                )
                Text(
                    text = "RECEIPT SCANNER",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.sp,
                    color = TextSecondary
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Settings button
                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(AiBannerBg)
                        .testTag("open_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = ForestGreen,
                        modifier = Modifier.size(18.dp)
                    )
                }

                if (hasReceipt) {
                    IconButton(
                        onClick = onClearClick,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(AiBannerBg)
                            .testTag("clear_receipt_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Clear Receipt",
                            tint = ForestGreen,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // High-Density Pill Upload Button
                Button(
                    onClick = onUploadClick,
                    enabled = !isLoading,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SageContainer,
                        contentColor = SageContainerText
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                    modifier = Modifier.testTag("upload_receipt_button")
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = ForestGreen
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Scanning",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SageContainerText
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = "Upload",
                            modifier = Modifier.size(18.dp),
                            tint = SageContainerText
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (hasReceipt) "New Photo" else "Upload",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SageContainerText
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HighDensityReceiptContent(
    receipt: ReceiptResult,
    calculation: SplitCalculation,
    imageUri: Uri?,
    onToggleItem: (Int) -> Unit,
    onUpdateQuantity: (Int, Int) -> Unit,
    onSelectAll: (Boolean) -> Unit
) {
    val allSelected = receipt.items.isNotEmpty() && receipt.items.all { it.isSelected }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("receipt_items_list"),
        contentPadding = PaddingValues(top = 4.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // AI Scanning Insight Banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(AiBannerBg)
                    .border(1.dp, ForestGreen.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                    .padding(12.dp)
                    .testTag("ai_status_banner")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(ForestGreen),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "AI",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontStyle = FontStyle.Italic
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Extracted ${receipt.items.size} line items (${calculation.currency} ${String.format(Locale.US, "%.2f", calculation.totalSubtotal)} total)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        )
                        Text(
                            text = "Tap items to claim your quantity",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            fontStyle = FontStyle.Italic
                        )
                    }

                    OutlinedButton(
                        onClick = { onSelectAll(!allSelected) },
                        modifier = Modifier.testTag("select_all_button"),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, ForestGreen.copy(alpha = 0.3f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ForestGreen),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(if (allSelected) "None" else "All", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Table Header Columns (High Density Theme Pattern)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ITEM DETAILS & PORTIONS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    color = TextSecondary
                )
                Text(
                    text = "MY PORTION",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    color = TextSecondary
                )
            }
        }

        // High Density Line Items
        itemsIndexed(
            items = receipt.items,
            key = { index, item -> "${item.name}_${index}" }
        ) { index, item ->
            HighDensityItemRow(
                item = item,
                currency = receipt.currency,
                index = index,
                onToggle = { onToggleItem(index) },
                onUpdateQuantity = { newQty -> onUpdateQuantity(index, newQty) }
            )
        }

        // Bottom space for scrolling above sticky footer
        item {
            Spacer(modifier = Modifier.height(140.dp))
        }
    }
}

@Composable
fun HighDensityItemRow(
    item: ReceiptItem,
    currency: String,
    index: Int,
    onToggle: () -> Unit,
    onUpdateQuantity: (Int) -> Unit
) {
    val isSelected = item.isSelected
    val hasMultipleUnits = item.quantity > 1

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isSelected) SelectedCardBg else Color.White)
            .border(
                1.dp,
                if (isSelected) ForestGreen.copy(alpha = 0.45f) else BorderGreen,
                RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onToggle)
            .padding(horizontal = 12.dp, vertical = 12.dp)
            .testTag("item_card_$index")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Custom High Density Checkbox
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) ForestGreen else Color.Transparent)
                        .border(
                            2.dp,
                            if (isSelected) ForestGreen else BorderUnchecked,
                            RoundedCornerShape(6.dp)
                        )
                        .testTag("checkbox_$index"),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (hasMultipleUnits) {
                            "Total on bill: ${item.quantity}x @ $currency ${String.format(Locale.US, "%.2f", item.price)} ea"
                        } else {
                            "Qty: 1 • Unit: $currency ${String.format(Locale.US, "%.2f", item.price)}"
                        },
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(horizontalAlignment = Alignment.End) {
                    val displayPrice = if (isSelected) item.claimedTotal else item.itemTotal
                    Text(
                        text = "$currency ${String.format(Locale.US, "%.2f", displayPrice)}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) TextPrimary else TextSecondary
                    )
                    if (hasMultipleUnits) {
                        Text(
                            text = if (isSelected) {
                                "${item.effectiveClaimedQuantity} of ${item.quantity} claimed"
                            } else {
                                "Tap to claim"
                            },
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) ForestGreen else TextSecondary
                        )
                    }
                }
            }

            // Interactive Quantity Portion Stepper for Multiple Items
            if (hasMultipleUnits && isSelected) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = BorderGreen.copy(alpha = 0.6f),
                    modifier = Modifier.padding(start = 36.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 36.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "My portion:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary
                        )

                        // Stepper Container
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White)
                                .border(1.dp, BorderGreen, RoundedCornerShape(8.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    val currentPortion = item.effectiveClaimedQuantity
                                    onUpdateQuantity(currentPortion - 1)
                                },
                                modifier = Modifier
                                    .size(28.dp)
                                    .testTag("item_qty_minus_$index")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Remove,
                                    contentDescription = "Decrease portion",
                                    tint = ForestGreen,
                                    modifier = Modifier.size(14.dp)
                                )
                            }

                            Text(
                                text = "${item.effectiveClaimedQuantity} of ${item.quantity}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = ForestGreen,
                                modifier = Modifier
                                    .padding(horizontal = 6.dp)
                                    .testTag("item_qty_text_$index")
                            )

                            IconButton(
                                onClick = {
                                    val currentPortion = item.effectiveClaimedQuantity
                                    onUpdateQuantity(currentPortion + 1)
                                },
                                enabled = item.effectiveClaimedQuantity < item.quantity,
                                modifier = Modifier
                                    .size(28.dp)
                                    .testTag("item_qty_plus_$index")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Increase portion",
                                    tint = if (item.effectiveClaimedQuantity < item.quantity) ForestGreen else TextSecondary.copy(alpha = 0.3f),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }

                    // Quick Action: Claim All or Reset to 1
                    if (item.effectiveClaimedQuantity < item.quantity) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(AiBannerBg)
                                .border(1.dp, BorderGreen, RoundedCornerShape(6.dp))
                                .clickable { onUpdateQuantity(item.quantity) }
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                                .testTag("item_claim_all_$index")
                        ) {
                            Text(
                                text = "All (${item.quantity})",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = ForestGreen
                            )
                        }
                    } else if (item.effectiveClaimedQuantity > 1) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.White)
                                .border(1.dp, BorderGreen, RoundedCornerShape(6.dp))
                                .clickable { onUpdateQuantity(1) }
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                                .testTag("item_claim_one_$index")
                        ) {
                            Text(
                                text = "1 only",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HighDensityStickyFooter(
    calculation: SplitCalculation,
    serviceFee: Double,
    onShareClick: () -> Unit,
    onUploadClick: () -> Unit,
    onServiceChange: (Double) -> Unit
) {
    var showSliders by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("sticky_bottom_bar"),
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        shadowElevation = 12.dp,
        color = FooterBg,
        border = BorderStroke(1.dp, BorderGreen)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp)
                .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
        ) {
            // Quick Adjust Tip toggler
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { showSliders = !showSliders }
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Adjust Tip",
                        tint = ForestGreen,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Tip / Service: ${String.format(Locale.US, "%.1f", serviceFee)}%",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = ForestGreen
                    )
                }

                Text(
                    text = if (showSliders) "DONE" else "ADJUST",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    color = ForestGreen
                )
            }

            AnimatedVisibility(visible = showSliders) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Text(
                        text = "Service Fee / Tip: ${String.format(Locale.US, "%.1f", serviceFee)}%",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                    Slider(
                        value = serviceFee.toFloat(),
                        onValueChange = { onServiceChange(it.toDouble()) },
                        valueRange = 0f..35f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("service_slider"),
                        colors = SliderDefaults.colors(
                            thumbColor = ForestGreen,
                            activeTrackColor = ForestGreen
                        )
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = BorderGreen.copy(alpha = 0.5f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Subtotal Line
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Selected Subtotal (${calculation.checkedCount} items)",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Text(
                    text = "${calculation.currency} ${String.format(Locale.US, "%.2f", calculation.checkedSubtotal)}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
            }

            // Tip / Service Line
            if (serviceFee > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Tip / Service (${String.format(Locale.US, "%.1f", serviceFee)}%)",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    Text(
                        text = "${calculation.currency} ${String.format(Locale.US, "%.2f", calculation.proportionalServiceFee)}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = BorderGreen.copy(alpha = 0.5f)
            )

            // Final Total Line
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Your Total",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "${calculation.currency} ${String.format(Locale.US, "%.2f", calculation.totalDue)}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = ForestGreen,
                    modifier = Modifier.testTag("total_amount_text")
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons Row: [Share Breakdown] + [Add / New Photo]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onShareClick,
                    enabled = calculation.checkedCount > 0,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("share_breakdown_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ForestGreen,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "Share Breakdown",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        modifier = Modifier.size(16.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .border(1.dp, BorderGreen, RoundedCornerShape(16.dp))
                        .clickable(onClick = onUploadClick)
                        .testTag("footer_new_photo_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Upload another",
                        tint = ForestGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun HighDensityEmptyState(
    userSettings: UserSettings,
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit,
    onSampleClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .testTag("empty_state_view"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(AiBannerBg)
                .border(1.dp, BorderGreen, RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                contentDescription = "Receipt Scanner",
                modifier = Modifier.size(36.dp),
                tint = ForestGreen
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "Snap, Split & Settle Bills",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Snap a photo of your receipt or upload from your gallery. Gemini AI will extract all line items for seamless bill splitting.",
            fontSize = 13.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Currency and Tax Defaults Badge (Clickable to open settings)
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(SageContainer)
                .clickable(onClick = onSettingsClick)
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .testTag("active_currency_badge")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Currency: ${userSettings.currency.code} (${userSettings.currency.symbol}) • Tip: ${String.format(Locale.US, "%.1f", userSettings.tipPercentage)}%",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SageContainerText
                )
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "Configure Defaults",
                    modifier = Modifier.size(12.dp),
                    tint = SageContainerText
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Primary: Camera Snap Button
        Button(
            onClick = onCameraClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("empty_camera_button"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ForestGreen,
                contentColor = Color.White
            )
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Take Photo with Camera", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Secondary: Gallery Button
        OutlinedButton(
            onClick = onGalleryClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .testTag("empty_upload_button"),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, BorderGreen),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color.White,
                contentColor = ForestGreen
            )
        ) {
            Icon(
                imageVector = Icons.Default.Image,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = ForestGreen
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Choose from Gallery", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Tertiary: Sample Demo
        OutlinedButton(
            onClick = onSampleClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .testTag("sample_receipt_button"),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, BorderGreen.copy(alpha = 0.6f)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(15.dp),
                tint = ForestGreen
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Try Sample Dinner Receipt", fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun HighDensityLoadingView(message: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .testTag("loading_view"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(AiBannerBg)
                .border(1.dp, BorderGreen, RoundedCornerShape(20.dp))
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(ForestGreen),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "AI",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontStyle = FontStyle.Italic
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Scanning receipt...",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        text = message,
                        fontSize = 12.sp,
                        color = TextSecondary,
                        fontStyle = FontStyle.Italic
                    )
                }

                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.5.dp,
                    color = ForestGreen
                )
            }
        }
    }
}

@Composable
fun HighDensityErrorView(
    errorMessage: String,
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit,
    onSampleClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .testTag("error_view"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(AiBannerBg)
                .border(1.dp, BorderGreen, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = "Error",
                modifier = Modifier.size(36.dp),
                tint = ForestGreen
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Receipt Scan Failed",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = errorMessage,
            fontSize = 12.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = onCameraClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .testTag("error_camera_button"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ForestGreen,
                contentColor = Color.White
            )
        ) {
            Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Take New Photo with Camera", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = onGalleryClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .testTag("error_gallery_button"),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, BorderGreen),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = ForestGreen)
        ) {
            Icon(imageVector = Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Choose from Gallery", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = onSampleClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .testTag("error_sample_button"),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, BorderGreen.copy(alpha = 0.5f)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
        ) {
            Text("Load Sample Receipt Instead", fontSize = 13.sp)
        }
    }
}
