package com.giathinh.canlua.ui.component

import android.Manifest
import android.content.pm.PackageManager
import android.provider.ContactsContract
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.byValue
import androidx.compose.foundation.text.input.insert
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.giathinh.canlua.R
import com.giathinh.canlua.ui.theme.AppColors
import com.giathinh.canlua.ui.util.shake
import com.giathinh.canlua.util.HapticUtil

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
            Toast.makeText(
                context,
                context.getString(R.string.card_detail_location_denied),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // Lưu chuỗi số thô, hiển thị được format qua OutputTransformation
    var moistureRaw        by remember { mutableStateOf("") }   // "18.2" -> 18.2%
    val priceState         = rememberTextFieldState()           // "8200" -> 8.200 đ
    val depositState       = rememberTextFieldState()           // "500000" -> 500.000 đ
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
    val focusManager = LocalFocusManager.current
    var showErrors by remember { mutableStateOf(false) }
    var shakeTrigger by remember { mutableIntStateOf(0) }
    val isValid = counterpartyName.isNotBlank() &&
                  riceVariety.isNotBlank() &&
                  (cccd.isEmpty() || cccd.length == 12)
    val isVarietyError = showErrors && riceVariety.isBlank()
    val isNameError = showErrors && counterpartyName.isBlank()
    val isCccdError = showErrors && cccd.isNotEmpty() && cccd.length != 12

    val nextAction = remember(focusManager) {
        KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
    }

    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AppColors.CardBg
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
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
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 8.dp)
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
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    // Section: Thông tin lô hàng
                    SectionLabel(stringResource(R.string.create_card_section_lot))

                    // Giống lúa ▼
                    RiceVarietyDropdown(
                        selected = riceVariety,
                        onSelect = { riceVariety = it },
                        suggestions = combinedSuggestions,
                        isError = isVarietyError,
                        supportingText = if (isVarietyError) {
                            { Text(stringResource(R.string.validation_variety_required)) }
                        } else null,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = nextAction,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .shake(if (isVarietyError) shakeTrigger else 0)
                    )

                    // Vụ mùa ▼
                    SeasonDropdown(
                        selected = seasonLabel,
                        onSelect = { seasonLabel = it },
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = nextAction,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Row 2: Tên counterparty
                    FormTextField(
                        value = counterpartyName,
                        onValueChange = { counterpartyName = it },
                        label = stringResource(R.string.create_card_counterparty_name_label, counterpartyLabel),
                        placeholder = stringResource(R.string.create_card_counterparty_name_placeholder, counterpartyLabel),
                        isError = isNameError,
                        supportingText = if (isNameError) {
                            { Text(stringResource(R.string.validation_counterparty_required, counterpartyLabel)) }
                        } else null,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = nextAction,
                        modifier = Modifier
                            .fillMaxWidth()
                            .shake(if (isNameError) shakeTrigger else 0)
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
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                        keyboardActions = nextAction,
                        singleLine = true,
                        trailingIcon = {
                            val hasPermission = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.READ_CONTACTS
                            ) == PackageManager.PERMISSION_GRANTED
                            
                            IconButton(
                                onClick = {
                                    if (hasPermission) {
                                        contactPickerLauncher.launch(null)
                                    } else {
                                        permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
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
                        shape = RoundedCornerShape(12.dp),
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
                        isError = isCccdError,
                        supportingText = if (isCccdError) {
                            { Text(stringResource(R.string.validation_cccd_length)) }
                        } else null,
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    HapticUtil.tick(context)
                                    showCccdHelp = !showCccdHelp
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Info,
                                    contentDescription = stringResource(R.string.create_card_cccd_help_content_desc),
                                    tint = AppColors.GreenPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        keyboardActions = nextAction,
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 60.dp)
                            .shake(if (isCccdError) shakeTrigger else 0),
                        shape = RoundedCornerShape(12.dp),
                        colors = dialogTextFieldColors()
                    )

                    InlineFieldHelpHint(
                        visible = showCccdHelp,
                        title = stringResource(R.string.create_card_cccd_help_title),
                        description = stringResource(R.string.create_card_cccd_help_description),
                        onDismiss = { showCccdHelp = false }
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
                                    showImpurityHelp = !showImpurityHelp
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Info,
                                    contentDescription = stringResource(R.string.create_card_impurity_help_content_desc),
                                    tint = AppColors.GreenPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                        keyboardActions = nextAction,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = dialogTextFieldColors()
                    )

                    InlineFieldHelpHint(
                        visible = showImpurityHelp,
                        title = stringResource(R.string.create_card_impurity_help_title),
                        description = stringResource(R.string.create_card_impurity_help_description),
                        onDismiss = { showImpurityHelp = false }
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
                                    showMoistureHelp = !showMoistureHelp
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Info,
                                    contentDescription = stringResource(R.string.create_card_moisture_help_content_desc),
                                    tint = AppColors.GreenPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                        keyboardActions = nextAction,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = dialogTextFieldColors()
                    )

                    InlineFieldHelpHint(
                        visible = showMoistureHelp,
                        title = stringResource(R.string.create_card_moisture_help_title),
                        description = stringResource(R.string.create_card_moisture_help_description),
                        onDismiss = { showMoistureHelp = false }
                    )

                    // Đơn giá đ/kg (1 dòng)
                    OutlinedTextField(
                        state = priceState,
                        label = { Text(stringResource(R.string.create_card_price_label)) },
                        placeholder = { Text(stringResource(R.string.create_card_price_placeholder), style = MaterialTheme.typography.bodyMedium) },
                        inputTransformation = digitsOnlyInputTransformation(7),
                        outputTransformation = ThousandSeparatorOutputTransformation,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        onKeyboardAction = { focusManager.moveFocus(FocusDirection.Down) },
                        lineLimits = TextFieldLineLimits.SingleLine,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = dialogTextFieldColors()
                    )

                    // Row 5: Tiền cọc — full width
                    OutlinedTextField(
                        state = depositState,
                        label = { Text(stringResource(R.string.create_card_deposit_label)) },
                        placeholder = { Text(stringResource(R.string.create_card_deposit_placeholder), style = MaterialTheme.typography.bodyMedium) },
                        inputTransformation = digitsOnlyInputTransformation(10),
                        outputTransformation = ThousandSeparatorOutputTransformation,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                        onKeyboardAction = { focusManager.clearFocus() },
                        lineLimits = TextFieldLineLimits.SingleLine,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp),
                        shape = RoundedCornerShape(12.dp),
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
                            onCheckedChange = { checked ->
                                if (checked) {
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
                            },
                            colors = CheckboxDefaults.colors(
                                checkedColor = AppColors.GreenPrimary,
                                uncheckedColor = AppColors.TextSecondary
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.create_card_gps_title),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = AppColors.TextPrimary
                            )
                            Text(
                                text = stringResource(R.string.create_card_gps_subtitle),
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
                            if (!isValid) {
                                showErrors = true
                                shakeTrigger++
                                HapticUtil.error(context)
                            } else {
                                HapticUtil.confirm(context)
                                onCreate(
                                    counterpartyName.trim(),
                                    counterpartyPhone.trim(),
                                    riceVariety,
                                    seasonLabel.trim(),
                                    moistureRaw.toDoubleOrNull() ?: 0.0,
                                    priceState.text.toString().toDoubleOrNull() ?: 0.0,
                                    depositState.text.toString().toDoubleOrNull() ?: 0.0,
                                    cccd.trim().takeIf { it.isNotEmpty() },
                                    impurityWeightRaw.toDoubleOrNull() ?: 0.0,
                                    recordLocation
                                )
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppColors.GreenPrimary
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
}

@Composable
private fun InlineFieldHelpHint(
    visible: Boolean,
    title: String,
    description: String,
    onDismiss: () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = AppColors.GreenSurface,
            border = BorderStroke(1.dp, AppColors.GreenPrimary.copy(alpha = 0.25f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.GreenPrimary
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextSecondary
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.popover_close),
                        tint = AppColors.TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// OutputTransformation: Tự động chèn dấu chấm phân cách phần ngàn
// ─────────────────────────────────────────────────────────────────────────────
object ThousandSeparatorOutputTransformation : OutputTransformation {
    private const val THOUSAND_GROUP_SIZE = 3

    override fun TextFieldBuffer.transformOutput() {
        val len = length
        if (len <= THOUSAND_GROUP_SIZE || !asCharSequence().all { it.isDigit() }) return
        var pos = len - THOUSAND_GROUP_SIZE
        while (pos > 0) {
            insert(pos, ".")
            pos -= THOUSAND_GROUP_SIZE
        }
    }
}

fun digitsOnlyInputTransformation(maxLength: Int? = null): InputTransformation =
    InputTransformation.byValue { current, proposed ->
        val digits = proposed.filter { it.isDigit() }
        if (maxLength != null && digits.length > maxLength) current else digits
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
    isError: Boolean = false,
    supportingText: (@Composable () -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder, style = MaterialTheme.typography.bodyMedium) },
        isError = isError,
        supportingText = supportingText,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = true,
        modifier = modifier.heightIn(min = 60.dp),
        shape = RoundedCornerShape(12.dp),
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
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED ||
    ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
}
