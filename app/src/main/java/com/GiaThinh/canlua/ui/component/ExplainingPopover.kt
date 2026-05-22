package com.GiaThinh.canlua.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.GiaThinh.canlua.R
import com.GiaThinh.canlua.ui.theme.AppColors

/**
 * Bong bóng popover giải thích thông tin các chỉ số / icon trong app.
 * Hiển thị ở tọa độ của anchor, tự động ẩn khi tap ra ngoài hoặc tap trực tiếp vào popup.
 */
@Composable
fun ExplainingPopover(
    visible: Boolean,
    title: String,
    description: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (visible) {
        Popup(
            alignment = Alignment.TopCenter,
            onDismissRequest = onDismiss,
            properties = PopupProperties(focusable = false)
        ) {
            Box(
                modifier = modifier
                    .padding(horizontal = 24.dp, vertical = 8.dp)
                    .shadow(12.dp, shape = RoundedCornerShape(14.dp))
                    .background(AppColors.GreenSurface, shape = RoundedCornerShape(14.dp))
                    .border(1.dp, AppColors.GreenPrimary.copy(alpha = 0.5f), shape = RoundedCornerShape(14.dp))
                    .padding(16.dp)
                    .clickable { onDismiss() } // Nhấn để đóng nhanh
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        color = AppColors.GreenDark,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.TextPrimary
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.popover_tap_to_close),
                        style = MaterialTheme.typography.labelSmall,
                        color = AppColors.TextHint,
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
        }
    }
}
