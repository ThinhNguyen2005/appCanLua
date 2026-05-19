package com.GiaThinh.canlua.ui.screen.trader

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.GiaThinh.canlua.ui.screen.profile.PersonalInfoCard
import com.GiaThinh.canlua.ui.screen.profile.ProfileHeader
import com.GiaThinh.canlua.ui.screen.profile.RoleSwitcher
import com.GiaThinh.canlua.ui.theme.AppColors
import com.GiaThinh.canlua.ui.viewmodel.AuthViewModel
import com.GiaThinh.canlua.ui.viewmodel.ProfileViewModel
import com.GiaThinh.canlua.ui.viewmodel.TraderBidsViewModel

/**
 * Tab "Tài khoản" cho TRADER.
 *
 * Cấu trúc:
 *  1. Header (avatar + tên + role badge)
 *  2. Inline-edit thông tin cá nhân (tên, SĐT, khu vực, CCCD)
 *  3. Stats: số bảng giá đã đăng, số đang hoạt động
 *  4. Role switcher → có thể chuyển về FARMER bất cứ lúc nào
 *  5. Đăng xuất
 *
 * Dùng `ProfileViewModel` cho update profile (giống FarmerProfileScreen),
 * `TraderBidsViewModel` chỉ để lấy số liệu bids của chính trader.
 */
@Composable
fun TraderProfileScreen(
    bidsViewModel: TraderBidsViewModel = hiltViewModel(),
    profileViewModel: ProfileViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val profile by profileViewModel.profile.collectAsState(initial = null)
    val bids by bidsViewModel.myBids.collectAsState()

    var editing by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var region by remember { mutableStateOf("") }
    var cccd by remember { mutableStateOf("") }
    var pendingRole by remember { mutableStateOf<String?>(null) }

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
            item {
                ProfileHeader(
                    name = profile?.name?.takeIf { it.isNotBlank() } ?: "Thương lái",
                    role = profile?.role ?: "TRADER",
                    email = profile?.email.orEmpty()
                )
            }

            // Stats bid
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatBox(
                        label = "Bảng giá",
                        value = bids.size.toString(),
                        color = AppColors.GreenPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    StatBox(
                        label = "Đang hoạt động",
                        value = bids.count { it.active }.toString(),
                        color = AppColors.Success,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                PersonalInfoCard(
                    editing = editing,
                    name = name, onName = { name = it },
                    phone = phone, onPhone = { phone = it },
                    region = region, onRegion = { region = it },
                    cccd = cccd, onCccd = { cccd = it },
                    onToggleEdit = {
                        if (editing) {
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

            item {
                RoleSwitcher(
                    currentRole = profile?.role ?: "TRADER",
                    onRequestChange = { newRole -> pendingRole = newRole }
                )
            }

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
                    Spacer(Modifier.padding(4.dp))
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
                        "TRADER" -> "Bạn sẽ chuyển sang giao diện THƯƠNG LÁI với các chức năng đăng giá, sổ giao dịch, bản đồ nguồn cung."
                        else -> "Bạn sẽ quay lại giao diện NÔNG DÂN với các chức năng cân lúa, mùa vụ, AI khuyến nông. Các bảng giá đã đăng vẫn được giữ."
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

@Composable
private fun StatBox(label: String, value: String, color: Color, modifier: Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.CardBg)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextHint
            )
        }
    }
}
