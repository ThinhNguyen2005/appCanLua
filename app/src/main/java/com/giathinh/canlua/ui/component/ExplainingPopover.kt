package com.giathinh.canlua.ui.component

import com.giathinh.canlua.R

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.giathinh.canlua.ui.theme.AppColors

/**
 * Bong bóng popover giải thích thông tin các chỉ số / icon trong app.
 *
 * Trước đây dùng `alignment = TopCenter` → popup neo vào top của parent layout
 * (LazyColumn item), khi user scroll xuống mới tap icon thì popup hiện ngoài
 * vùng nhìn hiện tại. Đổi sang custom PopupPositionProvider để CENTER trên
 * cửa sổ, đảm bảo luôn xuất hiện đúng giữa màn hình bất kể vị trí icon.
 */
@Composable
fun ExplainingPopover(
    visible: Boolean,
    title: String,
    description: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!visible) return

    val positionProvider = remember {
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize
            ): IntOffset = IntOffset(
                x = ((windowSize.width - popupContentSize.width) / 2).coerceAtLeast(0),
                y = ((windowSize.height - popupContentSize.height) / 2).coerceAtLeast(0)
            )
        }
    }

    Popup(
        popupPositionProvider = positionProvider,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true, dismissOnClickOutside = true)
    ) {
        // Scrim mờ phía sau để nhấn mạnh popup + bắt sự kiện tap-outside
        Box(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .shadow(16.dp, shape = RoundedCornerShape(14.dp))
                .background(AppColors.CardBg, shape = RoundedCornerShape(14.dp))
                .border(1.5.dp, AppColors.GreenPrimary, shape = RoundedCornerShape(14.dp))
                .clickable { onDismiss() }
                .padding(20.dp)
                .then(modifier)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = AppColors.GreenPrimary,
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
