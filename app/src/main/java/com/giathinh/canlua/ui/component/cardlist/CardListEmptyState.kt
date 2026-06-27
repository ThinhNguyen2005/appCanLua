package com.giathinh.canlua.ui.component.cardlist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Scale
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.giathinh.canlua.R
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.giathinh.canlua.ui.theme.AppColors

@Composable
fun CardListEmptyState(
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Outlined.Scale,
    title: String = stringResource(R.string.card_list_empty_title),
    subtitle: String = stringResource(R.string.card_list_empty_subtitle)
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 40.dp, bottom = 24.dp, start = 32.dp, end = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(AppColors.GreenSurface),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = AppColors.GreenLight,
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = AppColors.TextSecondary
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = AppColors.TextHint,
            textAlign = TextAlign.Center
        )
        
        /*
        if (onSyncClick != null) {
            Spacer(Modifier.height(32.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(AppColors.GreenSurface)
                    .border(1.dp, AppColors.GreenPrimary.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = AppColors.GreenPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Bạn muốn tải lại dữ liệu đã lưu trữ?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.TextPrimary,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Spacer(Modifier.width(12.dp))
                
                androidx.compose.material3.TextButton(
                    onClick = onSyncClick,
                    enabled = !syncing,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = AppColors.GreenPrimary,
                        disabledContentColor = AppColors.TextHint
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier
                        .background(Color.White, RoundedCornerShape(8.dp))
                        .border(1.dp, AppColors.GreenPrimary, RoundedCornerShape(8.dp))
                ) {
                    if (syncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = AppColors.GreenPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.settings_sync_action),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
        */
    }
}
