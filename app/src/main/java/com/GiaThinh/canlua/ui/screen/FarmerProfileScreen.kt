package com.GiaThinh.canlua.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.GiaThinh.canlua.data.model.TraderHistoryItem
import com.GiaThinh.canlua.ui.screen.profile.PersonalInfoCard
import com.GiaThinh.canlua.ui.screen.profile.ProfileHeader
import com.GiaThinh.canlua.ui.screen.profile.RoleSwitcher
import com.GiaThinh.canlua.ui.theme.AppColors
import com.GiaThinh.canlua.ui.viewmodel.AuthViewModel
import com.GiaThinh.canlua.ui.viewmodel.ProfileViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Tab "Tài khoản" cho FARMER (và STAFF).
 *
 * Cấu trúc:
 *  1. Header avatar + tên + role badge
 *  2. Inline-edit thông tin: tên, SĐT, khu vực, CCCD
 *  3. Đổi vai trò → FARMER ↔ TRADER (có confirm dialog vì sẽ thay đổi cả nav graph)
 *  4. Lịch sử thương lái đã mua ruộng — group theo (tên, SĐT)
 *  5. Đăng xuất
 *
 * KHÔNG dùng cho TRADER vì đã có TraderProfileScreen riêng (UX khác).
 */
@Composable
fun FarmerProfileScreen(
    profileViewModel: ProfileViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val profile by profileViewModel.profile.collectAsState(initial = null)
    val traderHistory by profileViewModel.traderHistory.collectAsState()

    var editing by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var region by remember { mutableStateOf("") }
    var cccd by remember { mutableStateOf("") }
    var pendingRole by remember { mutableStateOf<String?>(null) }

    // Sync state khi profile load lần đầu — chạy mỗi lần profile id đổi
    LaunchedEffect(profile?.id) {
        profile?.let {
            name = it.name
            phone = it.phone
            region = it.region
            cccd = it.cccd
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Surface)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Header ──
            item {
                ProfileHeader(
                    name = profile?.name?.takeIf { it.isNotBlank() } ?: "Nông dân",
                    role = profile?.role ?: "FARMER",
                    email = profile?.email.orEmpty()
                )
            }

            // ── Personal info card with inline edit ──
            item {
                PersonalInfoCard(
                    editing = editing,
                    name = name, onName = { name = it },
                    phone = phone, onPhone = { phone = it },
                    region = region, onRegion = { region = it },
                    cccd = cccd, onCccd = { cccd = it },
                    onToggleEdit = {
                        if (editing) {
                            // Save
                            profile?.let { current ->
                                profileViewModel.updateProfile(
                                    current = current,
                                    name = name,
                                    phone = phone,
                                    region = region,
                                    cccd = cccd
                                )
                            }
                        }
                        editing = !editing
                    }
                )
            }

            // ── Role switcher ──
            item {
                RoleSwitcher(
                    currentRole = profile?.role ?: "FARMER",
                    onRequestChange = { newRole -> pendingRole = newRole }
                )
            }

            // ── Trader history ──
            item {
                TraderHistorySection(history = traderHistory)
            }

            items(traderHistory, key = { "${it.traderName}-${it.traderPhone}" }) { item ->
                TraderHistoryRow(item)
            }

            if (traderHistory.isEmpty()) {
                item {
                    EmptyHistoryHint()
                }
            }

            // ── Sign out ──
            item {
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { authViewModel.signOut() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.Error.copy(alpha = 0.1f),
                        contentColor = AppColors.Error
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Đăng xuất", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Confirm dialog đổi role
    pendingRole?.let { newRole ->
        AlertDialog(
            onDismissRequest = { pendingRole = null },
            title = { Text("Đổi vai trò?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    when (newRole) {
                        "TRADER" -> "Bạn sẽ chuyển sang giao diện THƯƠNG LÁI với các chức năng đăng giá, sổ giao dịch, bản đồ nguồn cung. Có thể đổi lại bất cứ lúc nào."
                        else -> "Bạn sẽ quay lại giao diện NÔNG DÂN với các chức năng cân lúa, mùa vụ, AI khuyến nông."
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    profile?.let { current ->
                        profileViewModel.updateProfile(current = current, role = newRole)
                    }
                    pendingRole = null
                }) { Text("Đổi vai trò", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { pendingRole = null }) { Text("Hủy") }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}


// ─────────────────────────────────────────────────────────────
// Trader history
// ─────────────────────────────────────────────────────────────

@Composable
private fun TraderHistorySection(history: List<TraderHistoryItem>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.CardBg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(AppColors.GoldLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.History, null, tint = AppColors.GoldDark, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Lịch sử thương lái",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary
                )
                Text(
                    if (history.isEmpty()) "Chưa có giao dịch nào"
                    else "${history.size} thương lái đã từng mua",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextHint
                )
            }
        }
    }
}

@Composable
private fun TraderHistoryRow(item: TraderHistoryItem) {
    val moneyFmt = remember { NumberFormat.getInstance(Locale("vi", "VN")) }
    val dateFmt = remember { SimpleDateFormat("dd/MM/yyyy", Locale("vi", "VN")) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.CardBg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(AppColors.GreenSurface),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = item.traderName.firstOrNull()?.uppercase() ?: "?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.GreenPrimary
                )
            }
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.traderName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.TextPrimary
                )
                if (item.traderPhone.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Phone,
                            null,
                            tint = AppColors.TextHint,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(Modifier.size(4.dp))
                        Text(
                            item.traderPhone,
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.TextHint
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.MonetizationOn,
                        null,
                        tint = AppColors.GoldDark,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(Modifier.size(4.dp))
                    Text(
                        "${moneyFmt.format(item.totalRevenue.toLong())} đ",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.size(8.dp))
                    Text("•", color = AppColors.TextHint)
                    Spacer(Modifier.size(8.dp))
                    Text(
                        "${item.deals} phiên",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextSecondary
                    )
                }
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.CalendarToday,
                        null,
                        tint = AppColors.TextHint,
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(Modifier.size(4.dp))
                    Text(
                        "Lần cuối ${dateFmt.format(Date(item.lastDealDate))}",
                        style = MaterialTheme.typography.labelSmall,
                        color = AppColors.TextHint
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyHistoryHint() {
    AnimatedVisibility(
        visible = true,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = AppColors.SurfaceContainer)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Filled.Storefront,
                    null,
                    tint = AppColors.TextHint,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Chưa có thương lái nào",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.TextSecondary
                )
                Text(
                    "Mỗi khi tạo phiếu cân, tên thương lái sẽ được lưu lại đây.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextHint
                )
            }
        }
    }
}
