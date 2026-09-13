package com.giathinh.canlua.ui.component.weight

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.giathinh.canlua.ui.component.ShimmerBox
import com.giathinh.canlua.ui.component.rememberShimmerProgress
import com.giathinh.canlua.ui.component.skeletonColors

import androidx.compose.runtime.CompositionLocalProvider
import com.giathinh.canlua.ui.component.LocalShimmerProgress

/**
 * Skeleton loading cho màn hình nhập liệu cân lúa (WeightInputScreen).
 */
@Composable
fun WeightInputSkeleton(
    modifier: Modifier = Modifier,
    progress: Float? = null
) {
    val colors = skeletonColors()
    val sharedProgress = progress ?: rememberShimmerProgress()

    CompositionLocalProvider(LocalShimmerProgress provides sharedProgress) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Numpad display placeholder
            ShimmerBox(
                modifier = Modifier.fillMaxWidth().height(130.dp),
                cornerRadius = 16.dp,
                colors = colors,
                progress = sharedProgress
            )
            // Entries list placeholder
            ShimmerBox(
                modifier = Modifier.fillMaxWidth().weight(1f),
                cornerRadius = 16.dp,
                colors = colors,
                progress = sharedProgress
            )
            // Keypad grid placeholder
            ShimmerBox(
                modifier = Modifier.fillMaxWidth().height(220.dp),
                cornerRadius = 16.dp,
                colors = colors,
                progress = sharedProgress
            )
        }
    }
}
