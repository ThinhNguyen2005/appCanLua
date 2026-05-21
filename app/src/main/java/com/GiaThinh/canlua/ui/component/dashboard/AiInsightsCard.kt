package com.GiaThinh.canlua.ui.component.dashboard

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.GiaThinh.canlua.R
import com.GiaThinh.canlua.ui.theme.AppColors
import com.GiaThinh.canlua.ui.util.parseInlineMarkdown
import com.GiaThinh.canlua.ui.viewmodel.AiAnalysisState

/**
 * Card "AI Insights" cho Dashboard.
 *
 * 4 states:
 *  - Idle: hiển thị button + tagline.
 *  - Loading: shimmer gradient + spinner.
 *  - Success: render markdown headings + paragraphs.
 *  - Error: thông báo + nút thử lại.
 */
@Composable
fun AiInsightsCard(
    state: AiAnalysisState,
    onAnalyze: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    val purple = Color(0xFF6750A4)
    val indigo = Color(0xFF3F51B5)

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            HeaderRow(
                state = state,
                accentColor = purple,
                onReset = onReset
            )

            Spacer(Modifier.height(12.dp))

            AnimatedContent(
                targetState = state::class.simpleName,
                label = "ai_state",
                transitionSpec = {
                    (fadeIn(tween(220)) + expandVertically(tween(260))) togetherWith
                            (fadeOut(tween(120)) + shrinkVertically(tween(180)))
                }
            ) { _ ->
                when (state) {
                    is AiAnalysisState.Idle -> IdleContent(
                        accentColor = purple,
                        accentColor2 = indigo,
                        onAnalyze = onAnalyze
                    )
                    is AiAnalysisState.Loading -> LoadingContent(accentColor = purple)
                    is AiAnalysisState.Success -> SuccessContent(markdown = state.markdown)
                    is AiAnalysisState.Error -> ErrorContent(
                        message = state.message,
                        onRetry = onAnalyze
                    )
                }
            }
        }
    }
}

@Composable
private fun HeaderRow(
    state: AiAnalysisState,
    accentColor: Color,
    onReset: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(accentColor, Color(0xFFEC407A))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.ai_insights_title),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary
            )
            Text(
                text = when (state) {
                    is AiAnalysisState.Loading -> stringResource(R.string.ai_insights_loading)
                    is AiAnalysisState.Success -> stringResource(R.string.ai_insights_success_subtitle)
                    is AiAnalysisState.Error -> stringResource(R.string.ai_insights_error_subtitle)
                    else -> stringResource(R.string.ai_insights_idle_subtitle)
                },
                fontSize = 11.sp,
                color = AppColors.TextSecondary
            )
        }

        // Nút reset chỉ hiện khi có result
        AnimatedVisibility(
            visible = state is AiAnalysisState.Success,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            OutlinedButton(
                onClick = onReset,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 10.dp, vertical = 4.dp
                )
            ) {
                Text(stringResource(R.string.ai_insights_close), fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun IdleContent(
    accentColor: Color,
    accentColor2: Color,
    onAnalyze: () -> Unit
) {
    Column {
        Text(
            text = stringResource(R.string.ai_insights_idle_body),
            fontSize = 12.sp,
            color = AppColors.TextSecondary,
            lineHeight = 18.sp
        )
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onAnalyze,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = accentColor,
                contentColor = Color.White
            )
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.ai_insights_analyze_action),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun LoadingContent(accentColor: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ShimmerLine(width = 1f, color = accentColor)
        ShimmerLine(width = 0.85f, color = accentColor)
        ShimmerLine(width = 0.6f, color = accentColor)
    }
}

@Composable
private fun ShimmerLine(width: Float, color: Color) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer_alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth(width)
            .height(12.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        color.copy(alpha = 0.08f),
                        color.copy(alpha = alpha),
                        color.copy(alpha = 0.08f)
                    )
                )
            )
    )
}

@Composable
private fun SuccessContent(markdown: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MarkdownBlocks(markdown)
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Column {
        Text(
            text = message,
            fontSize = 12.sp,
            color = AppColors.Error,
            lineHeight = 18.sp
        )
        Spacer(Modifier.height(10.dp))
        OutlinedButton(
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.ai_insights_retry), fontSize = 13.sp)
        }
    }
}

/**
 * Mini block-level markdown renderer.
 * Hỗ trợ:
 *  - `## Heading 2` → bold + accent color + spacing
 *  - `- bullet` → bullet với indent
 *  - paragraph thường → body text
 *
 * Inline (bold/italic/code) delegate cho `parseInlineMarkdown`.
 */
@Composable
private fun MarkdownBlocks(markdown: String) {
    markdown.lines().forEach { line ->
        when {
            line.isBlank() -> Unit
            line.startsWith("## ") -> {
                Text(
                    text = parseInlineMarkdown(line.removePrefix("## ").trim()),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.GreenPrimary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            line.startsWith("# ") -> {
                Text(
                    text = parseInlineMarkdown(line.removePrefix("# ").trim()),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = AppColors.GreenPrimary
                )
            }
            line.startsWith("- ") || line.startsWith("• ") -> {
                val text = line.removePrefix("- ").removePrefix("• ").trim()
                Row(
                    modifier = Modifier.padding(start = 4.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = "•",
                        fontSize = 13.sp,
                        color = AppColors.GreenPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = parseInlineMarkdown(text),
                        fontSize = 12.sp,
                        color = AppColors.TextPrimary,
                        lineHeight = 18.sp
                    )
                }
            }
            else -> {
                Text(
                    text = parseInlineMarkdown(line.trim()),
                    fontSize = 12.sp,
                    color = AppColors.TextPrimary,
                    lineHeight = 18.sp
                )
            }
        }
    }
}
