package com.GiaThinh.canlua.ui.component

import android.content.pm.PackageManager
import android.provider.ContactsContract
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
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
import com.GiaThinh.canlua.R
import com.GiaThinh.canlua.ui.theme.AppColors
import com.GiaThinh.canlua.util.HapticUtil
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
@Composable
fun CreateCardDialog(
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
        bagWeight: Double,
        impurityWeight: Double,
        recordLocation: Boolean
    ) -> Unit,
    mode: CreateCardMode = CreateCardMode.FARMER
) {
    val context = LocalContext.current
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

    // Lưu chuỗi số thô, hiển thị được format qua VisualTransformation
    var moistureRaw        by remember { mutableStateOf("") }   // "18.2" -> 18.2%
    var priceRaw           by remember { mutableStateOf("") }   // "8200" -> 8.200 đ
    var depositRaw         by remember { mutableStateOf("") }   // "500000" -> 500.000 đ
    var bagWeightRaw       by remember { mutableStateOf("") }   // "1.0" -> 1.0 kg/bao
    var impurityWeightRaw  by remember { mutableStateOf("") }   // "5.0" -> 5.0 kg

    // Trợ giúp giải thích
    var showCccdHelp by remember { mutableStateOf(false) }
    var showImpurityHelp by remember { mutableStateOf(false) }
    var showBagHelp by remember { mutableStateOf(false) }
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

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.97f)
                .fillMaxHeight(0.9f)
                .imePadding()
                .navigationBarsPadding(),
            shape = RoundedCornerShape(24.dp),
            color = AppColors.CardBg,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {

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
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {

                    // Section: Thông tin lô hàng
                    SectionLabel(stringResource(R.string.create_card_section_lot))

                    // Giống lúa ▼
                    RiceVarietyDropdown(
                        selected = riceVariety,
                        onSelect = { riceVariety = it },
                        suggestions = combinedSuggestions,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Vụ mùa (Manual)
                    FormTextField(
                        value = seasonLabel,
                        onValueChange = { seasonLabel = it },
                        label = stringResource(R.string.card_list_filter_season),
                        placeholder = stringResource(R.string.dropdown_season_placeholder),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Row 2: Tên counterparty
                    FormTextField(
                        value = counterpartyName,
                        onValueChange = { counterpartyName = it },
                        label = stringResource(R.string.create_card_counterparty_name_label, counterpartyLabel),
                        placeholder = stringResource(R.string.create_card_counterparty_name_placeholder, counterpartyLabel),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Row 2.1: SĐT counterparty — có nút liên hệ bên phải
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

                    // Row 2.2: CCCD counterparty — 12 số
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
                                    HapticUtil.tick(context)
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

                    Spacer(Modifier.height(4.dp))

                    // Section: Giá & Thanh toán
                    SectionLabel(stringResource(R.string.create_card_section_payment))

                    Text(
                        text = stringResource(R.string.create_card_weight_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextHint,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    // Trừ bao bì mặc định (1 dòng)
                    OutlinedTextField(
                        value = bagWeightRaw,
                        onValueChange = { input ->
                            val filtered = input.filter { it.isDigit() || it == '.' }
                            if (filtered.length <= 4) bagWeightRaw = filtered
                        },
                        label = { Text(stringResource(R.string.create_card_bag_weight_label)) },
                        placeholder = { Text(stringResource(R.string.create_card_bag_weight_placeholder), style = MaterialTheme.typography.bodyMedium) },
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    HapticUtil.tick(context)
                                    showBagHelp = true
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Info,
                                    contentDescription = "Giải thích trừ bì",
                                    tint = AppColors.GreenPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = dialogTextFieldColors()
                    )

                    // Trừ tạp chất mặc định (1 dòng)
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
                                    HapticUtil.tick(context)
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
                        modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = dialogTextFieldColors()
                    )

                    // Độ ẩm % (1 dòng)
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
                                    HapticUtil.tick(context)
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
                        modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = dialogTextFieldColors()
                    )

                    // Đơn giá đ/kg (1 dòng)
                    OutlinedTextField(
                        value = priceRaw,
                        onValueChange = { input ->
                            val digits = input.filter { it.isDigit() }
                            if (digits.length <= 7) priceRaw = digits
                        },
                        label = { Text(stringResource(R.string.create_card_price_label)) },
                        placeholder = { Text(stringResource(R.string.create_card_price_placeholder), style = MaterialTheme.typography.bodyMedium) },
                        visualTransformation = ThousandSeparatorTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = dialogTextFieldColors()
                    )

                    // Row 5: Tiền cọc — full width
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

                    // Row 6: Opt-in GPS vị trí ruộng
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = recordLocation,
                            onCheckedChange = { recordLocation = it },
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

                // ── Buttons ──
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    color = AppColors.Divider
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = stringResource(R.string.action_cancel),
                            color = AppColors.TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Button(
                        onClick = {
                            HapticUtil.confirm(context)
                            onCreate(
                                counterpartyName.trim(),
                                counterpartyPhone.trim(),
                                riceVariety,
                                seasonLabel.trim(),
                                moistureRaw.toDoubleOrNull() ?: 0.0,
                                priceRaw.toDoubleOrNull() ?: 0.0,
                                depositRaw.toDoubleOrNull() ?: 0.0,
                                cccd.trim().takeIf { it.isNotEmpty() },
                                bagWeightRaw.toDoubleOrNull() ?: 0.0,
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
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.create_card_submit),
                            fontWeight = FontWeight.Bold,
                            color = AppColors.CardBg
                        )
                    }
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
            visible = showBagHelp,
            title = stringResource(R.string.create_card_bag_help_title),
            description = stringResource(R.string.create_card_bag_help_description),
            onDismiss = { showBagHelp = false }
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
