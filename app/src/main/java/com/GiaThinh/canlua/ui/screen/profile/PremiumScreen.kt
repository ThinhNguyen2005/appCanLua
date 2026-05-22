package com.GiaThinh.canlua.ui.screen.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.OfflineBolt
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.GiaThinh.canlua.ui.theme.AppColors
import com.GiaThinh.canlua.util.HapticUtil
import com.GiaThinh.canlua.util.PremiumState
import com.GiaThinh.canlua.util.TrackScreenRender
import kotlinx.coroutines.delay

enum class PremiumPackage(
    val title: String,
    val price: String,
    val originalPrice: String?,
    val duration: String,
    val isBestValue: Boolean = false
) {
    MONTHLY("Gói Mùa Gặt", "49.000 đ", null, "1 tháng"),
    YEARLY("Gói Cả Năm", "299.000 đ", "588.000 đ", "12 tháng", isBestValue = true),
    LIFETIME("Gói Vĩnh Viễn", "599.000 đ", "999.000 đ", "Trọn đời")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumScreen(
    navController: NavController
) {
    TrackScreenRender("premium")
    val context = LocalContext.current
    var selectedPackage by remember { mutableStateOf(PremiumPackage.YEARLY) }
    var isPurchasing by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Cân Lúa Premium",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại",
                            tint = AppColors.TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppColors.Surface,
                    titleContentColor = AppColors.TextPrimary
                ),
                windowInsets = WindowInsets(0.dp)
            )
        },
        containerColor = AppColors.Surface
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(AppColors.Surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Banner Lấp Lánh Vàng Lúa Chín
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(AppColors.GreenPrimary, AppColors.GreenDark)
                            )
                        )
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(AppColors.GoldAccent.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = AppColors.GoldAccent,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Text(
                            text = "Nâng Cấp Premium",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "Giải phóng toàn bộ sức mạnh công nghệ lúa gạo cầm tay",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.85f),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Danh Sách Quyền Lợi
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = AppColors.CardBg),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Quyền lợi Premium đặc quyền:",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.GreenPrimary
                        )

                        PremiumFeatureRow(
                            title = "Không giới hạn phiếu cân lúa",
                            description = "Lưu trữ không giới hạn số lượng phiếu cân lúa, xuất file PDF báo cáo nông nghiệp chuyên nghiệp."
                        )
                        PremiumFeatureRow(
                            title = "Chat AI không giới hạn",
                            description = "Kích hoạt RAG hỗ trợ tư vấn nông nghiệp sâu cùng trợ lý AI lúa gạo thông minh 24/7."
                        )
                        PremiumFeatureRow(
                            title = "Bản đồ vệ tinh nhiệt (Heatmap)",
                            description = "Xem mật độ thu mua thực tế và lịch sử biến động giá cả nông sản 30 ngày qua."
                        )
                        PremiumFeatureRow(
                            title = "Tự động đồng bộ tức thời",
                            description = "Đồng bộ tức thời dữ liệu lên Cloud khi thiết bị kết nối mạng mà không cần chờ đợi."
                        )
                        PremiumFeatureRow(
                            title = "Hoàn toàn không có quảng cáo",
                            description = "Trải nghiệm mượt mà, sạch sẽ 100%, không bị ngắt quãng khi làm việc ngoài đồng ruộng."
                        )
                    }
                }

                Text(
                    text = "Chọn gói dịch vụ phù hợp:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextSecondary,
                    modifier = Modifier.align(Alignment.Start)
                )

                // Danh Sách Gói Cước
                PremiumPackage.values().forEach { pack ->
                    val isSelected = selectedPackage == pack
                    val borderAlpha = if (isSelected) 0.8f else 0.15f
                    val borderColor = if (isSelected) AppColors.GoldAccent else AppColors.TextHint

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (isSelected) AppColors.GoldLight.copy(alpha = 0.4f) else AppColors.CardBg)
                            .border(
                                width = if (isSelected) 2.5.dp else 1.dp,
                                color = borderColor.copy(alpha = borderAlpha),
                                shape = RoundedCornerShape(18.dp)
                            )
                            .clickable {
                                selectedPackage = pack
                                HapticUtil.tick(context)
                            }
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = pack.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) AppColors.GoldDark else AppColors.TextPrimary
                                    )
                                    if (pack.isBestValue) {
                                        Surface(
                                            color = AppColors.GoldAccent,
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.padding(horizontal = 4.dp)
                                        ) {
                                            Text(
                                                text = "HỜI NHẤT",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White,
                                                fontWeight = FontWeight.ExtraBold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = "Sử dụng trong ${pack.duration}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AppColors.TextHint
                                )
                            }

                            Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = pack.price,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = AppColors.GreenPrimary
                                )
                                pack.originalPrice?.let {
                                    Text(
                                        text = it,
                                        style = TextStyle(
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Normal,
                                            color = AppColors.TextHint,
                                            textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Nút Đăng Ký Premium
                Button(
                    onClick = {
                        isPurchasing = true
                        HapticUtil.confirm(context)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.GreenPrimary,
                        contentColor = Color.White
                    ),
                    enabled = !isPurchasing
                ) {
                    if (isPurchasing) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Text(
                            text = "Đăng ký ngay với ${selectedPackage.price}",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp
                        )
                    }
                }

                Text(
                    text = "Cam kết bảo mật giao dịch qua cổng thanh toán Google Play. Hủy bất kỳ lúc nào trong cài đặt đăng ký của bạn.",
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.TextHint,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }

    // Giả lập tiến trình thanh toán Premium cực kỳ xịn sò
    LaunchedEffect(isPurchasing) {
        if (isPurchasing) {
            delay(1800L) // Giả lập mạng và thanh toán Google Play
            isPurchasing = false
            // Flip cờ Premium reactive — quảng cáo trên MarketScreen sẽ fade out ngay.
            // Lưu kèm tên gói để hiển thị trên huy hiệu Premium ở Profile + Settings.
            PremiumState.setPremium(context, true, plan = selectedPackage.title)
            com.GiaThinh.canlua.util.AnalyticsHelper.premiumPurchased(selectedPackage.title)
            showSuccessDialog = true
            HapticUtil.confirm(context)
        }
    }

    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { 
                showSuccessDialog = false
                navController.popBackStack()
            },
            title = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = AppColors.Success,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "Thanh toán thành công!",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                }
            },
            text = {
                Text(
                    text = "Chúc mừng! Bạn đã nâng cấp thành công tài khoản Premium của Cân Lúa Mobile. Toàn bộ các đặc quyền của bạn đã được kích hoạt tức thì.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.TextPrimary,
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSuccessDialog = false
                        navController.popBackStack()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = AppColors.GreenPrimary)
                ) {
                    Text("Đóng và Trải nghiệm", fontWeight = FontWeight.Bold)
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = AppColors.Surface
        )
    }
}

@Composable
private fun PremiumFeatureRow(
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = AppColors.GreenPrimary,
            modifier = Modifier.size(20.dp)
        )

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextHint
            )
        }
    }
}
