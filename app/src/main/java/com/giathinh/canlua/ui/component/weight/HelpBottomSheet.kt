package com.giathinh.canlua.ui.component.weight

import com.giathinh.canlua.R

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.giathinh.canlua.ui.theme.AppColors
import com.giathinh.canlua.util.FirebaseRemoteConfigManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpBottomSheet(
    hasUnreadFeedback: Boolean = false,
    onFeedbackClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val tutorialUrl = FirebaseRemoteConfigManager.tutorialVideoUrl
    val websiteUrl = FirebaseRemoteConfigManager.websiteUrl

    fun openUrl(url: String) {
        runCatching {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AppColors.Surface,
        dragHandle = {
            // Drag handle tùy chỉnh — mỏng và rõ hơn default
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(AppColors.Divider)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(bottom = 32.dp)
        ) {
            // ── Hero Header ─────────────────────────────────────────────
            HelpHeader()

            Spacer(Modifier.height(4.dp))

            // ── Section: Mẹo sử dụng nhanh ────────────────────────────
            SectionLabel(
                label = "MẸO SỬ DỤNG NHANH",
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )

            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(AppColors.CardBg),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                TipAccordionRow(
                    icon = Icons.Outlined.Scale,
                    iconBg = AppColors.GreenSurface,
                    iconTint = AppColors.GreenPrimary,
                    title = stringResource(R.string.help_tip_input_title),
                    desc = stringResource(R.string.help_tip_input_desc),
                    isLast = false
                )
                TipAccordionRow(
                    icon = Icons.Outlined.Lock,
                    iconBg = AppColors.Error.copy(alpha = 0.10f),
                    iconTint = AppColors.Error,
                    title = stringResource(R.string.help_tip_lock_title),
                    desc = stringResource(R.string.help_tip_lock_desc),
                    isLast = false
                )
                TipAccordionRow(
                    icon = Icons.Outlined.Tune,
                    iconBg = AppColors.Info.copy(alpha = 0.10f),
                    iconTint = AppColors.Info,
                    title = stringResource(R.string.help_tip_options_title),
                    desc = stringResource(R.string.help_tip_options_desc),
                    isLast = false
                )
                TipAccordionRow(
                    icon = Icons.Outlined.Pinch,
                    iconBg = AppColors.GoldLight,
                    iconTint = AppColors.GoldDark,
                    title = stringResource(R.string.help_tip_swipe_title),
                    desc = stringResource(R.string.help_tip_swipe_desc),
                    isLast = false
                )
                TipAccordionRow(
                    icon = Icons.Outlined.CloudSync,
                    iconBg = AppColors.Info.copy(alpha = 0.10f),
                    iconTint = AppColors.Info,
                    title = stringResource(R.string.help_tip_offline_title),
                    desc = stringResource(R.string.help_tip_offline_desc),
                    isLast = true
                )
            }

            Spacer(Modifier.height(20.dp))

            // ── Section: Liên hệ & tài nguyên ─────────────────────────
            SectionLabel(
                label = "TÀI NGUYÊN",
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )

            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Phản hồi — nổi bật nhất (màu vàng + badge)
                ActionCard(
                    icon = Icons.AutoMirrored.Outlined.Chat,
                    iconBg = AppColors.GoldAccent.copy(alpha = 0.15f),
                    iconTint = AppColors.GoldAccent,
                    title = stringResource(R.string.help_btn_feedback),
                    subtitle = "Báo lỗi hoặc đề xuất tính năng mới",
                    badge = if (hasUnreadFeedback) "MỚI" else null,
                    onClick = {
                        onFeedbackClick()
                        onDismiss()
                    }
                )

                // Video hướng dẫn
                ActionCard(
                    icon = Icons.Outlined.PlayCircle,
                    iconBg = AppColors.GreenSurface,
                    iconTint = AppColors.GreenPrimary,
                    title = stringResource(R.string.help_btn_tutorial),
                    subtitle = "Video hướng dẫn từng bước sử dụng",
                    onClick = { openUrl(tutorialUrl) }
                )

                // Trang web
                ActionCard(
                    icon = Icons.Outlined.Public,
                    iconBg = AppColors.SurfaceContainer,
                    iconTint = AppColors.TextSecondary,
                    title = stringResource(R.string.help_btn_web),
                    subtitle = "Tài liệu, cập nhật và tin tức mới nhất",
                    onClick = { openUrl(websiteUrl) }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Hero Header
// ─────────────────────────────────────────────────────────────────

@Composable
private fun HelpHeader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        AppColors.GreenSurface,
                        AppColors.Surface
                    )
                )
            )
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Icon circle lớn
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(AppColors.GreenPrimary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Outlined.MenuBook,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            Column {
                Text(
                    text = stringResource(R.string.help_sheet_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = AppColors.TextPrimary
                )
                Text(
                    text = stringResource(R.string.help_sheet_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.TextSecondary,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Section Label
// ─────────────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(label: String, modifier: Modifier = Modifier) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = AppColors.TextHint,
        letterSpacing = 1.2.sp,
        modifier = modifier
    )
}

// ─────────────────────────────────────────────────────────────────
// Tip Accordion — tap để expand/collapse mô tả
// ─────────────────────────────────────────────────────────────────

@Composable
private fun TipAccordionRow(
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    title: String,
    desc: String,
    isLast: Boolean
) {
    var expanded by remember { mutableStateOf(false) }
    val chevronAngle by animateFloatAsState(
        targetValue = if (expanded) 90f else 0f,
        animationSpec = tween(200),
        label = "chevron"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .animateContentSize(tween(250))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Icon tile
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = iconTint, modifier = Modifier.size(20.dp))
            }

            // Title
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            // Chevron
            Icon(
                Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = AppColors.TextHint,
                modifier = Modifier
                    .size(18.dp)
                    .rotate(chevronAngle)
            )
        }

        // Expanded description
        if (expanded) {
            Text(
                text = desc,
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextSecondary,
                lineHeight = 20.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 68.dp, end = 16.dp, bottom = 14.dp)
            )
        }

        // Divider — ẩn dòng cuối
        if (!isLast) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 68.dp),
                thickness = 0.5.dp,
                color = AppColors.Divider
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Action Card — link / button dạng card ngang
// ─────────────────────────────────────────────────────────────────

@Composable
private fun ActionCard(
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
    badge: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(AppColors.CardBg)
            .border(0.5.dp, AppColors.Divider, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Icon tile
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = iconTint, modifier = Modifier.size(22.dp))
        }

        // Text column
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.TextPrimary
                )
                if (badge != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(AppColors.Error)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = badge,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextHint,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        // Arrow
        Icon(
            Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = AppColors.TextHint,
            modifier = Modifier.size(20.dp)
        )
    }
}
