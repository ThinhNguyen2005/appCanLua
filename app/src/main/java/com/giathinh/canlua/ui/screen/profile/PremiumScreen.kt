package com.giathinh.canlua.ui.screen.profile

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
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.giathinh.canlua.R
import com.giathinh.canlua.ui.theme.AppColors
import com.giathinh.canlua.util.HapticUtil
import com.giathinh.canlua.util.PremiumState
import com.giathinh.canlua.util.TrackScreenRender
import kotlinx.coroutines.delay

enum class PremiumPackage(
    val titleRes: Int,
    val price: String,
    val originalPrice: String?,
    val durationRes: Int,
    val isBestValue: Boolean = false
) {
    MONTHLY(R.string.premium_plan_monthly_title, "49.000 đ", null, R.string.premium_plan_monthly_dur),
    YEARLY(R.string.premium_plan_yearly_title, "299.000 đ", "588.000 đ", R.string.premium_plan_yearly_dur, isBestValue = true),
    LIFETIME(R.string.premium_plan_lifetime_title, "599.000 đ", "999.000 đ", R.string.premium_plan_lifetime_dur)
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
                        stringResource(R.string.premium_screen_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            HapticUtil.tick(context)
                            navController.popBackStack()
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.premium_action_back),
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
                            text = stringResource(R.string.premium_banner_title),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = stringResource(R.string.premium_banner_subtitle),
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
                            text = stringResource(R.string.premium_benefits_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.GreenPrimary
                        )

                        PremiumFeatureRow(
                            title = stringResource(R.string.premium_feat_slips_title),
                            description = stringResource(R.string.premium_feat_slips_desc)
                        )
                        PremiumFeatureRow(
                            title = stringResource(R.string.premium_feat_ai_title),
                            description = stringResource(R.string.premium_feat_ai_desc)
                        )
                        PremiumFeatureRow(
                            title = stringResource(R.string.premium_feat_map_title),
                            description = stringResource(R.string.premium_feat_map_desc)
                        )
                        PremiumFeatureRow(
                            title = stringResource(R.string.premium_feat_sync_title),
                            description = stringResource(R.string.premium_feat_sync_desc)
                        )
                        PremiumFeatureRow(
                            title = stringResource(R.string.premium_feat_ads_title),
                            description = stringResource(R.string.premium_feat_ads_desc)
                        )
                    }
                }

                Text(
                    text = stringResource(R.string.premium_select_plan),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextSecondary,
                    modifier = Modifier.align(Alignment.Start)
                )

                // Danh Sách Gói Cước
                PremiumPackage.entries.forEach { pack ->
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
                                HapticUtil.tick(context)
                                selectedPackage = pack
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
                                        text = stringResource(pack.titleRes),
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
                                                text = stringResource(R.string.premium_badge_best_value),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White,
                                                fontWeight = FontWeight.ExtraBold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = stringResource(R.string.premium_plan_duration_format, stringResource(pack.durationRes)),
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
                                        style = MaterialTheme.typography.bodySmall.copy(
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
                            text = stringResource(R.string.premium_btn_subscribe, selectedPackage.price),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                        )
                    }
                }

                Text(
                    text = stringResource(R.string.premium_disclaimer),
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.TextHint,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }

    // Giả lập tiến trình thanh toán Premium
    val selectedPackageTitle = stringResource(selectedPackage.titleRes)
    LaunchedEffect(isPurchasing) {
        if (isPurchasing) {
            delay(1800L) // Giả lập mạng và thanh toán Google Play
            isPurchasing = false
            // Flip cờ Premium reactive — quảng cáo trên MarketScreen sẽ fade out ngay.
            // Lưu kèm tên gói để hiển thị trên huy hiệu Premium ở Profile + Settings.
            PremiumState.setPremium(context, true, plan = selectedPackageTitle)
            com.giathinh.canlua.util.AnalyticsHelper.premiumPurchased(selectedPackageTitle)
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
                        text = stringResource(R.string.premium_success_title),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                }
            },
            text = {
                Text(
                    text = stringResource(R.string.premium_success_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.TextPrimary,
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        HapticUtil.tick(context)
                        showSuccessDialog = false
                        navController.popBackStack()
                    },
                    modifier = Modifier.heightIn(min = 48.dp),
                    colors = ButtonDefaults.textButtonColors(contentColor = AppColors.GreenPrimary)
                ) {
                    Text(stringResource(R.string.premium_success_action), fontWeight = FontWeight.Bold)
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
