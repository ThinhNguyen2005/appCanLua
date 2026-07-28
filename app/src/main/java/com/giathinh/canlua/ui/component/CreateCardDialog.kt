package com.giathinh.canlua.ui.component

import android.content.pm.PackageManager
import android.Manifest
import android.provider.ContactsContract
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.giathinh.canlua.R
import com.giathinh.canlua.ui.theme.AppColors
import java.util.Locale

/**
 * Mode điều khiển lại layout của dialog cho cả nhữ vai trò:
 * - FARMER: nông dân tạo phiếu → owner = nông dân, counterparty = thương lái.
 * - TRADER: thương lái tạo phiếu đối chiếu → owner = thương lái, counterparty = nông dân.
 */
enum class CreateCardMode { FARMER, TRADER }

/**
 * Dialog tạo phiếu cân mới — v2.3
 * Hỗ trợ cả hai vai trò (farmer / trader) qua param `mode`.
 * Owner (người tạo phiếu, luôn đọc từ profile) hiển thị read-only ở header,
 * counterparty (đối tác giao dịch) được nhập tay vào form.
 *
 * @param ownerName Tên người đang đăng nhập — hiển thị read-only.
 * @param suggestedVarieties Danh sách gợi ý giống lúa động từ DB.
 * @param mode FARMER (default) hoặc TRADER — quyết định label/role swap.
 * @param onCreate Callback nhận (counterpartyName, counterpartyPhone, riceVariety, season, moisture, price, deposit, cccd, bagWeight, impurityWeight).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateCardBottomSheet(
    ownerName: String,
    suggestedVarieties: List<String>,
    onDismiss: () -> Unit,
    onCreate: (
        counterpartyName: String,
        counterpartyPhone: String,
        riceVariety: String,
        seasonLabel: String,
        moisturePercent: Double,
        pricePerKg: Double,
        depositAmount: Double,
        cccd: String?,
        impurityWeight: Double,
        recordLocation: Boolean
    ) -> Unit,
    mode: CreateCardMode = CreateCardMode.FARMER
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val contactsPermissionDeniedMessage = stringResource(R.string.create_card_contacts_permission_denied)
    // Label swap theo mode — owner header và input field counterparty.
    val ownerLabel = if (mode == CreateCardMode.FARMER) {
        stringResource(R.string.role_farmer)
    } else {
        stringResource(R.string.role_trader)
    }
    val counterpartyLabel = if (mode == CreateCardMode.FARMER) {
        stringResource(R.string.role_trader).lowercase()
    } else {
        stringResource(R.string.role_farmer).lowercase()
    }

    // ── State ───────────────────────────────────────────────────────────────────────
    var counterpartyName  by remember { mutableStateOf("") }
    var counterpartyPhone by remember { mutableStateOf("") }
    var riceVariety       by remember { mutableStateOf("") }
    var seasonLabel       by remember { mutableStateOf("") } // Vụ mùa để trống mặc định
    var cccd              by remember { mutableStateOf("") }
    var recordLocation    by remember { mutableStateOf(false) }

    val gpsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        if (granted.values.any { it }) {
            recordLocation = true
        } else {
            recordLocation = false
            Toast.makeText(context, "Cần cấp quyền vị trí để định vị ruộng", Toast.LENGTH_SHORT).show()
        }
    }

    // Lưu chuỗi số thô, hiển thị được format qua VisualTransformation
    var moistureRaw        by remember { mutableStateOf("") }   // "18.2" -> 18.2%
    var priceRaw           by remember { mutableStateOf("") }   // "8200" -> 8.200 đ
    var depositRaw         by remember { mutableStateOf("") }   // "500000" -> 500.000 đ
    var impurityWeightRaw  by remember { mutableStateOf("") }   // "5.0" -> 5.0 kg

    // Trợ giúp giải thích
    var showCccdHelp by remember { mutableStateOf(false) }
    var showImpurityHelp by remember { mutableStateOf(false) }
    var showMoistureHelp by remember { mutableStateOf(false) }

    // Gợi ý giống lúa (ưu tiên DB, sau đó là default, lấy top 5)
    val combinedSuggestions = remember(suggestedVarieties) {
        (suggestedVarieties + listOf("ST25", "OM18", "Đài Thơm 8", "Jasmine 85"))
            .distinct()
            .take(5)
    }

    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact()
    ) { uri ->
        if (uri != null) {
            val contentResolver = context.contentResolver
            var phone = ""
            var name = ""
            val cursor = contentResolver.query(uri, null, null, null, null)
            cursor?.use { c ->
                if (c.moveToFirst()) {
                    val idCol = c.getColumnIndex(ContactsContract.Contacts._ID)
                    val nameCol = c.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                    
                    val id = if (idCol >= 0) c.getString(idCol) else ""
                    val contactName = if (nameCol >= 0) c.getString(nameCol) else ""
                    if (contactName.isNotEmpty()) {
                        name = contactName
                    }
                    
                    val hasPhoneCol = c.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)
                    val hasPhone = if (hasPhoneCol >= 0) c.getInt(hasPhoneCol) else 0
                    if (hasPhone > 0 && id.isNotEmpty()) {
                        val phoneCursor = contentResolver.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                            arrayOf(id),
                            null
                        )
                        phoneCursor?.use { pCursor ->
                            if (pCursor.moveToFirst()) {
                                val numberCol = pCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                                if (numberCol >= 0) {
                                    phone = pCursor.getString(numberCol)
                                }
                            }
                        }
                    }
                }
            }
            val cleanedPhone = phone.replace(Regex("[^\\d+]"), "")
            if (cleanedPhone.isNotEmpty()) {
                counterpartyPhone = cleanedPhone
            }
            if (name.isNotEmpty() && counterpartyName.isEmpty()) {
                counterpartyName = name
            }
        }
    }

    // Launchers cho quyền và lấy contact từ danh bạ
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            contactPickerLauncher.launch(null)
        } else {
            Toast.makeText(
                context,
                contactsPermissionDeniedMessage,
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // Validation: Yêu cầu tối thiểu tên counterparty, giống lúa. CCCD phải trống hoặc đúng 12 chữ số.
    val isValid = counterpartyName.isNotBlank() && 
                  riceVariety.isNotBlank() && 
                  (cccd.isEmpty() || cccd.length == 12)

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
        containerColor = AppColors.CardBg
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
        ) {

                // ── Header ────────────────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AppColors.GreenSurface)
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.create_card_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.GreenPrimary
                        )
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(top = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = AppColors.TextSecondary
                            )
                            Text(
                                text = stringResource(R.string.create_card_owner_line, ownerLabel, ownerName),
                                style = MaterialTheme.typography.bodyMedium,
                                color = AppColors.TextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // ── Form (Scrollable) ─────────────────────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Card 1: Thông tin phiếu cân (Xanh lá)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column {
                            // Accent top border
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .background(AppColors.GreenPrimary)
                            )
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Text(
                                    text = "Thông tin phiếu cân",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = AppColors.GreenPrimary
                                )

                                // Giống lúa ▼
                                RiceVarietyDropdown(
                                    selected = riceVariety,
                                    onSelect = { riceVariety = it },
                                    suggestions = combinedSuggestions,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                // Vụ mùa (Manual) + Gợi ý nhanh
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    FormTextField(
                                        value = seasonLabel,
                                        onValueChange = { seasonLabel = it },
                                        label = stringResource(R.string.card_list_filter_season),
                                        placeholder = stringResource(R.string.dropdown_season_placeholder),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    // Quick Season Chips under text field
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.padding(start = 4.dp)
                                    ) {
                                        listOf("Đông Xuân 2026", "Hè Thu 2026", "Thu Đông 2026").forEach { suggestion ->
                                            SuggestionChip(
                                                onClick = { seasonLabel = suggestion },
                                                label = { Text(suggestion, style = MaterialTheme.typography.bodySmall) }
                                            )
                                        }
                                    }
                                }

                                // Tên thương lái/nông dân
                                FormTextField(
                                    value = counterpartyName,
                                    onValueChange = { counterpartyName = it },
                                    label = stringResource(R.string.create_card_counterparty_name_label, counterpartyLabel),
                                    placeholder = stringResource(R.string.create_card_counterparty_name_placeholder, counterpartyLabel),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                // SĐT
                                OutlinedTextField(
                                    value = counterpartyPhone,
                                    onValueChange = { input ->
                                        val filtered = input.filter { it.isDigit() || it == '+' || it == ' ' || it == '-' }
                                        if (filtered.length <= 15) counterpartyPhone = filtered
                                    },
                                    label = { Text(stringResource(R.string.create_card_counterparty_phone_label, counterpartyLabel)) },
                                    placeholder = { Text(stringResource(R.string.create_card_counterparty_phone_placeholder), style = MaterialTheme.typography.bodyMedium) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    singleLine = true,
                                    trailingIcon = {
                                        val hasPermission = ContextCompat.checkSelfPermission(
                                            context,
                                            android.Manifest.permission.READ_CONTACTS
                                        ) == PackageManager.PERMISSION_GRANTED
                                        
                                        IconButton(
                                            onClick = {
                                                if (hasPermission) {
                                                    contactPickerLauncher.launch(null)
                                                } else {
                                                    permissionLauncher.launch(android.Manifest.permission.READ_CONTACTS)
                                                }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ContactPhone,
                                                contentDescription = stringResource(R.string.create_card_contacts_select),
                                                tint = AppColors.GreenPrimary
                                            )
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = dialogTextFieldColors()
                                )

                                // CCCD
                                OutlinedTextField(
                                    value = cccd,
                                    onValueChange = { input ->
                                        val digits = input.filter { it.isDigit() }
                                        if (digits.length <= 12) cccd = digits
                                    },
                                    label = { Text(stringResource(R.string.create_card_cccd_label)) },
                                    placeholder = { Text(stringResource(R.string.create_card_cccd_placeholder), style = MaterialTheme.typography.bodyMedium) },
                                    trailingIcon = {
                                        IconButton(
                                            onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                                showCccdHelp = true
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Info,
                                                contentDescription = "Giải thích CCCD",
                                                tint = AppColors.GreenPrimary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = dialogTextFieldColors()
                                )
                            }
                        }
                    }

                    // Card 2: Giá & Thanh toán (Cam)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column {
                            // Accent top border (Orange)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .background(AppColors.Orange)
                            )
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Text(
                                    text = "Đơn giá & Khấu trừ",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = AppColors.Orange
                                )

                                // Tạp chất (%) & Độ ẩm (%) xếp trên 1 hàng (2 cột)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    OutlinedTextField(
                                        value = impurityWeightRaw,
                                        onValueChange = { input ->
                                            val filtered = input.filter { it.isDigit() || it == '.' }
                                            if (filtered.length <= 5) impurityWeightRaw = filtered
                                        },
                                        label = { Text(stringResource(R.string.create_card_impurity_weight_label)) },
                                        placeholder = { Text(stringResource(R.string.create_card_impurity_weight_placeholder), style = MaterialTheme.typography.bodyMedium) },
                                        trailingIcon = {
                                            IconButton(
                                                onClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                                    showImpurityHelp = true
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Info,
                                                    contentDescription = "Giải thích tạp chất",
                                                    tint = AppColors.GreenPrimary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f).heightIn(min = 60.dp),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = dialogTextFieldColors()
                                    )

                                    OutlinedTextField(
                                        value = moistureRaw,
                                        onValueChange = { input ->
                                            val filtered = input.filter { it.isDigit() || it == '.' }
                                            if (filtered.length <= 4) moistureRaw = filtered
                                        },
                                        label = { Text(stringResource(R.string.create_card_moisture_label)) },
                                        placeholder = { Text(stringResource(R.string.create_card_moisture_placeholder), style = MaterialTheme.typography.bodyMedium) },
                                        trailingIcon = {
                                            IconButton(
                                                onClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                                    showMoistureHelp = true
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Info,
                                                    contentDescription = "Giải thích độ ẩm",
                                                    tint = AppColors.GreenPrimary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f).heightIn(min = 60.dp),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = dialogTextFieldColors()
                                    )
                                }

                                // Đơn giá đ/kg (Chữ to, Bold màu GreenPrimary)
                                OutlinedTextField(
                                    value = priceRaw,
                                    onValueChange = { input ->
                                        val digits = input.filter { it.isDigit() }
                                        if (digits.length <= 7) priceRaw = digits
                                    },
                                    label = { Text(stringResource(R.string.create_card_price_label)) },
                                    placeholder = { Text(stringResource(R.string.create_card_price_placeholder), style = MaterialTheme.typography.bodyMedium) },
                                    visualTransformation = ThousandSeparatorTransformation(),
                                    textStyle = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = AppColors.GreenPrimary
                                    ),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = dialogTextFieldColors()
                                )

                                // Tiền cọc (đ)
                                OutlinedTextField(
                                    value = depositRaw,
                                    onValueChange = { input ->
                                        val digits = input.filter { it.isDigit() }
                                        if (digits.length <= 10) depositRaw = digits
                                    },
                                    label = { Text(stringResource(R.string.create_card_deposit_label)) },
                                    placeholder = { Text(stringResource(R.string.create_card_deposit_placeholder), style = MaterialTheme.typography.bodyMedium) },
                                    visualTransformation = ThousandSeparatorTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = dialogTextFieldColors()
                                )
                            }
                        }
                    }

                    // Card 3: Tính năng mở rộng (GPS)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .background(AppColors.TextSecondary)
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val nextChecked = !recordLocation
                                        if (nextChecked) {
                                            if (hasLocationPermission(context)) {
                                                recordLocation = true
                                            } else {
                                                gpsPermissionLauncher.launch(
                                                    arrayOf(
                                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                                    )
                                                )
                                            }
                                        } else {
                                            recordLocation = false
                                        }
                                    }
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = recordLocation,
                                    onCheckedChange = null,
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = AppColors.GreenPrimary,
                                        uncheckedColor = AppColors.TextSecondary
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Lưu vị trí GPS ruộng",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = AppColors.TextPrimary
                                    )
                                    Text(
                                        text = "Ghi lại tọa độ để hiển thị trên bản đồ lúa",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = AppColors.TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                // ── Footer Buttons ──
                HorizontalDivider(color = AppColors.Divider)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Cancel button
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1.5f).height(56.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.action_cancel).uppercase(),
                            color = AppColors.TextSecondary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Create button
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                            onCreate(
                                counterpartyName.trim(),
                                counterpartyPhone.trim(),
                                riceVariety,
                                seasonLabel.trim(),
                                moistureRaw.toDoubleOrNull() ?: 0.0,
                                priceRaw.toDoubleOrNull() ?: 0.0,
                                depositRaw.toDoubleOrNull() ?: 0.0,
                                cccd.trim().takeIf { it.isNotEmpty() },
                                impurityWeightRaw.toDoubleOrNull() ?: 0.0,
                                recordLocation
                            )
                            onDismiss()
                        },
                        enabled = isValid,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppColors.GreenPrimary,
                            disabledContainerColor = AppColors.GreenPrimary.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.weight(2f).height(56.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.create_card_submit).uppercase(),
                            fontWeight = FontWeight.Bold,
                            color = AppColors.CardBg
                        )
                    }
                }
            }

        // Help Popovers (rendered inside Dialog window so they show on top of it)
        ExplainingPopover(
            visible = showCccdHelp,
            title = stringResource(R.string.create_card_cccd_help_title),
            description = stringResource(R.string.create_card_cccd_help_description),
            onDismiss = { showCccdHelp = false }
        )

        ExplainingPopover(
            visible = showImpurityHelp,
            title = stringResource(R.string.create_card_impurity_help_title),
            description = stringResource(R.string.create_card_impurity_help_description),
            onDismiss = { showImpurityHelp = false }
        )


        ExplainingPopover(
            visible = showMoistureHelp,
            title = stringResource(R.string.create_card_moisture_help_title),
            description = stringResource(R.string.create_card_moisture_help_description),
            onDismiss = { showMoistureHelp = false }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// VisualTransformation: Tự động chèn dấu chấm phân cách phần ngàn
// ─────────────────────────────────────────────────────────────────────────────
class ThousandSeparatorTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val original = text.text
        if (original.isEmpty()) {
            return TransformedText(text, OffsetMapping.Identity)
        }

        val formatted = try {
            val number = original.toLong()
            String.format(Locale.forLanguageTag("vi-VN"), "%,d", number)
        } catch (e: Exception) {
            original
        }

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 0) return 0
                var transformedOffset = 0
                var originalOffset = 0
                while (transformedOffset < formatted.length && originalOffset < offset) {
                    if (formatted[transformedOffset].isDigit()) {
                        originalOffset++
                    }
                    transformedOffset++
                }
                while (transformedOffset < formatted.length && !formatted[transformedOffset].isDigit()) {
                    transformedOffset++
                }
                return transformedOffset.coerceAtMost(formatted.length)
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 0) return 0
                val safeOffset = offset.coerceAtMost(formatted.length)
                return formatted.take(safeOffset).count { it.isDigit() }.coerceAtMost(original.length)
            }
        }

        return TransformedText(AnnotatedString(formatted), offsetMapping)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Shared Helpers
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = AppColors.GreenPrimary,
        letterSpacing = androidx.compose.ui.unit.TextUnit(
            1f, androidx.compose.ui.unit.TextUnitType.Sp
        )
    )
}

@Composable
private fun FormTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder, style = MaterialTheme.typography.bodyMedium) },
        singleLine = true,
        modifier = modifier.heightIn(min = 60.dp),
        shape = RoundedCornerShape(14.dp),
        colors = dialogTextFieldColors()
    )
}

@Composable
private fun dialogTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = AppColors.TextPrimary,
    unfocusedTextColor = AppColors.TextPrimary,
    focusedLabelColor = AppColors.GreenPrimary,
    unfocusedLabelColor = AppColors.TextHint,
    focusedBorderColor = AppColors.GreenPrimary,
    unfocusedBorderColor = AppColors.Divider
)

private fun hasLocationPermission(context: android.content.Context): Boolean {
    return androidx.core.content.ContextCompat.checkSelfPermission(
        context,
        android.Manifest.permission.ACCESS_FINE_LOCATION
    ) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
    androidx.core.content.ContextCompat.checkSelfPermission(
        context,
        android.Manifest.permission.ACCESS_COARSE_LOCATION
    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
}
