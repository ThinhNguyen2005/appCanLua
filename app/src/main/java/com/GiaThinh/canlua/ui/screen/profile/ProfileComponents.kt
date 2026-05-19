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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.GiaThinh.canlua.ui.theme.AppColors

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
        "TRADER" -> RoleVisual(Icons.Filled.Storefront, "Tài khoản Thương lái", AppColors.GoldDark, AppColors.GoldLight)
        else -> RoleVisual(Icons.Filled.Agriculture, "Tài khoản Nông dân", AppColors.GreenDark, AppColors.GreenSurface)
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
                    "Thông tin cá nhân",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onToggleEdit) {
                    Icon(
                        imageVector = if (editing) Icons.Filled.Save else Icons.Filled.Edit,
                        contentDescription = if (editing) "Lưu" else "Chỉnh sửa",
                        tint = AppColors.GreenPrimary
                    )
                }
            }
            Spacer(Modifier.height(8.dp))

            EditableRow(
                editing = editing,
                icon = Icons.Filled.Person,
                label = "Họ và tên",
                value = name,
                onValueChange = onName
            )
            EditableRow(
                editing = editing,
                icon = Icons.Filled.Phone,
                label = "Số điện thoại",
                value = phone,
                onValueChange = onPhone,
                keyboardType = KeyboardType.Phone
            )
            EditableRow(
                editing = editing,
                icon = Icons.Filled.LocationOn,
                label = "Khu vực",
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
                    text = value.ifBlank { "Chưa cập nhật" },
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
                "Vai trò sử dụng",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary
            )
            Text(
                "Chuyển đổi giữa giao diện Nông dân và Thương lái",
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
                    label = "Nông dân",
                    selected = currentRole != "TRADER",
                    onClick = { if (currentRole != "FARMER") onRequestChange("FARMER") },
                    modifier = Modifier.weight(1f)
                )
                RoleChip(
                    icon = Icons.Filled.Storefront,
                    label = "Thương lái",
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
