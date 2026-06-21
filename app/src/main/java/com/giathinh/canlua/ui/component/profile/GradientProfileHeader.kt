package com.giathinh.canlua.ui.component.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import coil.compose.AsyncImage
import com.giathinh.canlua.R
import com.giathinh.canlua.ui.theme.AppColors

/**
 * Hero header tối giản — phiên bản flat, không có background màu.
 *
 * Triết lý "less is more":
 *  - KHÔNG nền gradient/solid → blend với Surface chung của app, dark mode tự khớp.
 *  - Role badge nhỏ với GreenSurface tint → không ồn ào.
 *  - Tên dùng TextPrimary đậm, email TextHint nhẹ.
 *
 * Style tham chiếu: Linear / Notion / Apple Settings — clean, không màu mè.
 */
@Composable
fun GradientProfileHeader(
    name: String,
    role: String,
    email: String,
    isGoogleLoggedIn: Boolean = false,
    googleAvatarUrl: String? = null,
    modifier: Modifier = Modifier
) {
    val isTrader = role.equals("TRADER", ignoreCase = true)
    val roleIcon: ImageVector = if (isTrader) Icons.Filled.Storefront else Icons.Filled.Agriculture
    val roleLabel = stringResource(if (isTrader) R.string.trader else R.string.farmer)
    val defaultName = stringResource(if (isTrader) R.string.profile_trader_default_name else R.string.profile_farmer_default_name)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar tròn 56.dp ở góc trái
            if (isGoogleLoggedIn && googleAvatarUrl != null) {
                AsyncImage(
                    model = googleAvatarUrl,
                    contentDescription = "Google Avatar",
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(AppColors.SurfaceContainer),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    error = rememberVectorPainter(Icons.Default.Person)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(AppColors.SurfaceContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Default Avatar",
                        tint = AppColors.TextSecondary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(Modifier.width(16.dp))

            // Cụm chữ thông tin khách hàng và email ở bên phải
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = name.ifBlank { defaultName },
                        color = AppColors.TextPrimary,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold
                        ),
                        modifier = Modifier.weight(1f, fill = false),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.size(8.dp))
                    // Role badge — nhỏ gọn, tint xanh lá nông nghiệp
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(AppColors.GreenSurface)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = roleIcon,
                            contentDescription = null,
                            tint = AppColors.GreenPrimary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(Modifier.size(4.dp))
                        Text(
                            text = roleLabel,
                            color = AppColors.GreenPrimary,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }

                if (email.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = email,
                        color = AppColors.TextHint,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }
    }
}

