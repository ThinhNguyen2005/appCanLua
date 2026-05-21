package com.GiaThinh.canlua.ui.component.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.GiaThinh.canlua.ui.theme.AppColors

@Composable
fun ProfileSectionTitle(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            fontWeight = FontWeight.ExtraBold,
            color = AppColors.TextPrimary,
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = subtitle,
            color = AppColors.TextHint,
            style = MaterialTheme.typography.bodySmall
        )
    }
}
