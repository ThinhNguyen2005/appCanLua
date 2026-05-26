package com.GiaThinh.canlua.ui.screen.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.GiaThinh.canlua.R
import com.GiaThinh.canlua.ui.theme.AppColors

// Singleton SimpleDateFormat — dùng chung cho mọi PremiumStatusCard.
private val PROFILE_DATE_FMT: java.text.SimpleDateFormat =
    java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())

/**
 * Components dùng chung giữa FarmerProfileScreen và TraderProfileScreen.
 * Tách ra để giữ DRY và đảm bảo UX nhất quán giữa 2 role.
 */

private data class RoleVisual(
    val icon: ImageVector,
    val label: String,
    val tint: Color,
    val bg: Color
)

@Composable
fun ProfileHeader(name: String, role: String, email: String) {
    val visual = when (role) {
        "TRADER" -> RoleVisual(Icons.Filled.Storefront, stringResource(R.string.profile_trader_account), AppColors.GoldDark, AppColors.GoldLight)
        else -> RoleVisual(Icons.Filled.Agriculture, stringResource(R.string.profile_farmer_account), AppColors.GreenDark, AppColors.GreenSurface)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(visual.bg),
                contentAlignment = Alignment.Center
            ) {
                Icon(visual.icon, null, tint = visual.tint, modifier = Modifier.size(44.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary
            )
            if (email.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(email, style = MaterialTheme.typography.bodySmall, color = AppColors.TextHint)
            }
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(visual.bg)
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    visual.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = visual.tint,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun PersonalInfoCard(
    editing: Boolean,
    name: String, onName: (String) -> Unit,
    phone: String, onPhone: (String) -> Unit,
    region: String, onRegion: (String) -> Unit,
    cccd: String, onCccd: (String) -> Unit,
    onToggleEdit: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.CardBg)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.personal_information),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onToggleEdit) {
                    Icon(
                        imageVector = if (editing) Icons.Filled.Save else Icons.Filled.Edit,
                        contentDescription = stringResource(if (editing) R.string.save else R.string.edit),
                        tint = AppColors.GreenPrimary
                    )
                }
            }
            Spacer(Modifier.height(8.dp))

            EditableRow(
                editing = editing,
                icon = Icons.Filled.Person,
                label = stringResource(R.string.full_name),
                value = name,
                onValueChange = onName
            )
            EditableRow(
                editing = editing,
                icon = Icons.Filled.Phone,
                label = stringResource(R.string.phone_number),
                value = phone,
                onValueChange = onPhone,
                keyboardType = KeyboardType.Phone
            )
            EditableRow(
                editing = editing,
                icon = Icons.Filled.LocationOn,
                label = stringResource(R.string.region),
                value = region,
                onValueChange = onRegion
            )
            EditableRow(
                editing = editing,
                icon = Icons.Filled.Badge,
                label = "CCCD",
                value = cccd,
                onValueChange = onCccd,
                keyboardType = KeyboardType.Number
            )
        }
    }
}

@Composable
private fun EditableRow(
    editing: Boolean,
    icon: ImageVector,
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(AppColors.GreenSurface),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = AppColors.GreenPrimary, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = AppColors.TextHint)
            if (editing) {
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppColors.GreenPrimary,
                        unfocusedBorderColor = AppColors.Divider
                    )
                )
            } else {
                Text(
                    text = value.ifBlank { stringResource(R.string.not_updated) },
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (value.isBlank()) AppColors.TextHint else AppColors.TextPrimary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun RoleSwitcher(currentRole: String, onRequestChange: (String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.CardBg)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                stringResource(R.string.profile_user_role),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary
            )
            Text(
                text = stringResource(R.string.profile_user_role_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextHint
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(AppColors.SurfaceContainer)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                RoleChip(
                    icon = Icons.Filled.Agriculture,
                    label = stringResource(R.string.farmer),
                    selected = currentRole != "TRADER",
                    onClick = { if (currentRole != "FARMER") onRequestChange("FARMER") },
                    modifier = Modifier.weight(1f)
                )
                RoleChip(
                    icon = Icons.Filled.Storefront,
                    label = stringResource(R.string.trader),
                    selected = currentRole == "TRADER",
                    onClick = { if (currentRole != "TRADER") onRequestChange("TRADER") },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun RoleChip(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(11.dp))
            .background(if (selected) AppColors.GreenPrimary else Color.Transparent)
            .clickable(enabled = !selected, onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon,
                null,
                tint = if (selected) Color.White else AppColors.TextSecondary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.size(6.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (selected) Color.White else AppColors.TextSecondary
            )
        }
    }
}

/**
 * Card upsell Premium — gradient gold lúa chín, dùng chung Farmer & Trader.
 * Tap → navigate("premium").
 */
@Composable
fun PremiumUpsellCard(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(AppColors.GoldLight, AppColors.GoldAccent)
                )
            )
            .clickable(onClick = onClick, role = Role.Button)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Star,
                    contentDescription = null,
                    tint = AppColors.GoldDark,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.size(14.dp))
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.upgrade_to_premium),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = AppColors.GoldDark
                )
                Text(
                    text = stringResource(R.string.profile_premium_upsell_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextSecondary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * Card huy hiệu Premium đang active — hiển thị thay UpsellCard khi user đã mua.
 * Gradient xanh primary + viền gold + ngôi sao + tên gói + thời điểm kích hoạt.
 * Nếu là Early Adopter → badge đặc biệt màu vàng gold lúa chín.
 */
@Composable
fun PremiumStatusCard(
    plan: String?,
    sinceMs: Long,
    isEarlyAdopter: Boolean = false,
    onClick: () -> Unit
) {
    val activatedLabel = stringResource(R.string.activated)
    val activatedSinceLabel = stringResource(R.string.activated_since)
    val sinceLabel = remember(sinceMs, activatedLabel, activatedSinceLabel) {
        if (sinceMs <= 0L) activatedLabel
        else {
            "$activatedSinceLabel ${PROFILE_DATE_FMT.format(java.util.Date(sinceMs))}"
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.linearGradient(
                    colors = if (isEarlyAdopter) {
                        listOf(AppColors.GoldAccent, AppColors.GoldDark)
                    } else {
                        listOf(AppColors.GreenPrimary, AppColors.GreenDark)
                    }
                )
            )
            .clickable(onClick = onClick, role = Role.Button)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Star,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(Modifier.size(14.dp))
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Premium",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Spacer(Modifier.size(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (isEarlyAdopter) Color.White
                                else AppColors.GoldAccent
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = stringResource(
                                if (isEarlyAdopter) R.string.settings_premium_early_adopter
                                else R.string.settings_premium_active
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isEarlyAdopter) AppColors.GoldDark else Color.White,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
                Text(
                    text = plan?.takeIf { it.isNotBlank() } ?: stringResource(R.string.profile_premium_active_plan),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.95f),
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = sinceLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.75f)
                )
                if (isEarlyAdopter) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.profile_premium_early_adopter_thanks),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.65f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
