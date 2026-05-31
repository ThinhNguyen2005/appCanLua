package com.GiaThinh.canlua.ui.screen.qr

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.GiaThinh.canlua.R
import com.GiaThinh.canlua.ui.theme.AppColors
import com.GiaThinh.canlua.ui.viewmodel.CardDetailViewModel
import com.GiaThinh.canlua.util.HapticUtil
import com.GiaThinh.canlua.util.QrBitmapGenerator
import com.GiaThinh.canlua.util.TrackScreenRender
import java.text.NumberFormat
import java.util.Locale

/**
 * Màn hình Nông dân tạo mã QR cho giao dịch.
 * QR chứa token SHA-256 để thương lái xác thực.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrGenerateScreen(
    cardId: Long,
    navController: NavController,
    viewModel: CardDetailViewModel = hiltViewModel()
) {
    TrackScreenRender("qr_generate")
    val currentCard by viewModel.currentCard.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val fmt = remember { NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN")) }

    LaunchedEffect(cardId) { viewModel.loadCardById(cardId) }

    val card = currentCard ?: return

    // Generate QR token and bitmap
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var currentToken by remember { mutableStateOf(card.qrToken) }

    // Auto-generate if no token exists
    LaunchedEffect(card.id) {
        if (card.qrToken.isNullOrBlank()) {
            viewModel.generateQrToken(cardId)
        }
    }

    // Update bitmap when token changes
    LaunchedEffect(card.qrToken) {
        card.qrToken?.let { token ->
            currentToken = token
            // QR content: JSON-like payload
            val qrContent = "CANLUA|${card.id}|${card.name}|${"%.1f".format(card.totalWeight)}|${"%.0f".format(card.totalAmount)}|$token"
            qrBitmap = QrBitmapGenerator.generate(qrContent, 600)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.qr_generate_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.content_back))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(16.dp))

            // Card info summary
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = AppColors.GreenSurface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(card.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(stringResource(R.string.weight_label_weight), style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
                            Text("${"%.1f".format(card.totalWeight)} kg", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(stringResource(R.string.weight_metrics_total_amount), style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
                            Text("${fmt.format(card.totalAmount)} đ", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = AppColors.GreenPrimary)
                        }
                    }
                    if (card.riceVariety.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(stringResource(R.string.qr_generate_variety, card.riceVariety), style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // QR Code display
            val bitmap = qrBitmap
            if (bitmap != null) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(
                        Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = stringResource(R.string.qr_generate_content_description),
                            modifier = Modifier
                                .size(240.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            stringResource(R.string.qr_generate_instruction),
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppColors.TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                CircularProgressIndicator(color = AppColors.GreenPrimary)
            }

            Spacer(Modifier.height(20.dp))

            // Refresh token button
            OutlinedButton(
                onClick = {
                    viewModel.generateQrToken(cardId)
                    HapticUtil.tick(context)
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Outlined.Refresh, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.qr_generate_refresh))
            }

            Spacer(Modifier.height(12.dp))

            // Lock status
            if (card.isLocked) {
                Surface(
                    color = AppColors.GreenSurface,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("✓ ", style = MaterialTheme.typography.titleMedium, color = AppColors.Success)
                        Text(
                            stringResource(R.string.qr_generate_verified),
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppColors.Success,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            // Token info (small)
            currentToken?.let { token ->
                Text(
                    stringResource(R.string.qr_generate_token, token.take(12)),
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.TextHint
                )
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
