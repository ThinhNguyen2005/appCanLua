package com.GiaThinh.canlua.ui.component.detail

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Grass
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.GiaThinh.canlua.R
import com.GiaThinh.canlua.ui.theme.AppColors

/**
 * Card 0/N (DetailScreen): Thông tin phiếu cân.
 *
 * Design parity với WeightSummaryCard / FinancialSummaryCard:
 *  - Surface CardBg + 2dp elevation
 *  - Header có icon + title
 *  - Mỗi row dùng FluentStatRow style nhưng có trailing action button
 *
 * Tương tác:
 *  - Tap số điện thoại → ACTION_DIAL
 *  - Tap địa chỉ ruộng → geo:lat,lon (Google Maps / Maps app mặc định)
 *  - Nút refresh location → callback refresh GPS
 */
@Composable
fun CardInfoCard(
    traderName: String,
    traderPhone: String,
    riceVariety: String,
    seasonLabel: String,
    createdDateLabel: String,
    fieldAddress: String,
    hasCoordinates: Boolean,
    onCallTrader: () -> Unit,
    onOpenMap: () -> Unit,
    onRefreshLocation: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.CardBg),
        border = BorderStroke(0.8.dp, AppColors.Divider),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // ── Header ─────────────────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Description,
                    contentDescription = null,
                    tint = AppColors.GreenPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(R.string.detail_info_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary,
                    modifier = Modifier.weight(1f)
                )
            }

            // ── Thương lái ─────────────────────────────────────────────
            InfoRow(
                icon = Icons.Outlined.Person,
                label = stringResource(R.string.detail_info_trader),
                value = traderName.ifBlank { stringResource(R.string.detail_info_empty_value) }
            )

            // ── SĐT thương lái (tap-to-call) ──────────────────────────
            ActionRow(
                icon = Icons.Outlined.Phone,
                label = stringResource(R.string.detail_info_phone),
                value = traderPhone.ifBlank { stringResource(R.string.detail_info_missing_phone) },
                actionEnabled = traderPhone.isNotBlank(),
                actionTint = AppColors.GreenPrimary,
                onClick = onCallTrader
            )

            HorizontalDivider(color = AppColors.Divider, thickness = 0.6.dp)

            // ── Giống lúa ──────────────────────────────────────────────
            InfoRow(
                icon = Icons.Outlined.Grass,
                label = stringResource(R.string.detail_info_rice_variety),
                value = listOfNotNull(
                    riceVariety.takeIf { it.isNotBlank() },
                    seasonLabel.takeIf { it.isNotBlank() }
                ).joinToString(" · ").ifBlank { stringResource(R.string.detail_info_empty_value) }
            )

            // ── Ngày tạo ───────────────────────────────────────────────
            InfoRow(
                icon = Icons.Outlined.CalendarMonth,
                label = stringResource(R.string.detail_info_created_date),
                value = createdDateLabel
            )

            HorizontalDivider(color = AppColors.Divider, thickness = 0.6.dp)

            // ── Địa chỉ ruộng (tap-to-map + refresh) ──────────────────
            FieldAddressRow(
                address = fieldAddress,
                hasCoordinates = hasCoordinates,
                onOpenMap = onOpenMap,
                onRefreshLocation = onRefreshLocation
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────
// Static info row (no action)
// ─────────────────────────────────────────────────────────────────────
@Composable
private fun InfoRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(0.55f)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = AppColors.TextSecondary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextSecondary
            )
        }
        Text(
            value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(0.45f),
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
    }
}

// ─────────────────────────────────────────────────────────────────────
// Interactive row with leading icon + tap action (ripple + scale)
// ─────────────────────────────────────────────────────────────────────
@Composable
private fun ActionRow(
    icon: ImageVector,
    label: String,
    value: String,
    actionEnabled: Boolean,
    actionTint: Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && actionEnabled) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "actionRowScale"
    )

    val baseModifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(10.dp))
        .scale(scale)

    val rowModifier = if (actionEnabled) {
        baseModifier.clickable(
            interactionSource = interactionSource,
            indication = ripple(bounded = true, color = actionTint),
            onClick = onClick
        )
    } else baseModifier

    Row(
        modifier = rowModifier
            .heightIn(min = 48.dp)
            .padding(vertical = 4.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(0.55f)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (actionEnabled) actionTint else AppColors.TextSecondary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextSecondary
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(0.45f),
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (actionEnabled) actionTint else AppColors.TextHint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (actionEnabled) {
                Spacer(Modifier.width(4.dp))
                Icon(
                    Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = actionTint,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────
// Field address row: dài → multi-line, có refresh button
// ─────────────────────────────────────────────────────────────────────
@Composable
private fun FieldAddressRow(
    address: String,
    hasCoordinates: Boolean,
    onOpenMap: () -> Unit,
    onRefreshLocation: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && hasCoordinates) 0.98f else 1f,
        animationSpec = tween(durationMillis = 180),
        label = "fieldAddressScale"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Tap-to-map area
        val baseAddrModifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(10.dp))
            .scale(scale)

        val addrModifier = if (hasCoordinates) {
            baseAddrModifier.clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = AppColors.GreenPrimary),
                onClick = onOpenMap
            )
        } else baseAddrModifier

        Row(
            modifier = addrModifier
                .heightIn(min = 48.dp)
                .padding(vertical = 6.dp, horizontal = 2.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                Icons.Outlined.LocationOn,
                contentDescription = null,
                tint = if (hasCoordinates) AppColors.GreenPrimary else AppColors.TextSecondary,
                modifier = Modifier.size(18.dp).padding(top = 2.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.detail_info_field_address),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.TextSecondary
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    address.ifBlank { stringResource(R.string.detail_info_unknown_location) },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (hasCoordinates) AppColors.GreenPrimary
                            else if (address.isBlank()) AppColors.TextHint
                            else AppColors.TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (hasCoordinates) {
                Icon(
                    Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = AppColors.GreenPrimary,
                    modifier = Modifier.size(18.dp).padding(top = 2.dp)
                )
            }
        }

        // Refresh button — luôn visible để cập nhật khi GPS chưa lấy được
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(AppColors.GreenSurface)
        ) {
            IconButton(
                onClick = onRefreshLocation,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    Icons.Outlined.MyLocation,
                    contentDescription = stringResource(R.string.detail_info_refresh_location),
                    tint = AppColors.GreenPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
