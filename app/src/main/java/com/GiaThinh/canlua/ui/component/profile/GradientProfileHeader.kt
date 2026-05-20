package com.GiaThinh.canlua.ui.component.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.GiaThinh.canlua.ui.theme.AppColors

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
    modifier: Modifier = Modifier
) {
    val isTrader = role.equals("TRADER", ignoreCase = true)
    val roleIcon: ImageVector = if (isTrader) Icons.Filled.Storefront else Icons.Filled.Agriculture
    val roleLabel = if (isTrader) "Thương lái" else "Nông dân"

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        // Role badge subtle với tint xanh lá nhẹ — đồng bộ với theme nông nghiệp
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(AppColors.GreenSurface)
                .padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = roleIcon,
                contentDescription = null,
                tint = AppColors.GreenPrimary,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.size(6.dp))
            Text(
                text = roleLabel,
                color = AppColors.GreenPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(Modifier.height(10.dp))

        Text(
            text = name.ifBlank { if (isTrader) "Thương lái" else "Nông dân" },
            color = AppColors.TextPrimary,
            fontSize = 26.sp,
            fontWeight = FontWeight.ExtraBold,
            style = MaterialTheme.typography.headlineMedium
        )

        if (email.isNotBlank()) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = email,
                color = AppColors.TextHint,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
