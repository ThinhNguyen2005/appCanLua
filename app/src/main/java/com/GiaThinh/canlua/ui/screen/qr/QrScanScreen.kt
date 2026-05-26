package com.GiaThinh.canlua.ui.screen.qr

import android.Manifest
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.GiaThinh.canlua.R
import com.GiaThinh.canlua.ui.theme.AppColors
import com.GiaThinh.canlua.ui.viewmodel.CardViewModel
import com.GiaThinh.canlua.ui.viewmodel.QrVerificationState
import com.GiaThinh.canlua.util.HapticUtil
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.firebase.auth.FirebaseAuth
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.GiaThinh.canlua.util.TrackScreenRender
import java.util.concurrent.Executors

/**
 * Màn hình Thương lái quét QR — CameraX + ML Kit Barcode.
 * Khi quét thành công → verifyAndLockTransaction.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun QrScanScreen(
    navController: NavController,
    viewModel: CardViewModel = hiltViewModel()
) {
    TrackScreenRender("qr_scan")
    val context = LocalContext.current
    val appToast = com.GiaThinh.canlua.ui.feedback.LocalAppToast.current
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)

    val qrVerificationState by viewModel.qrVerificationState.collectAsStateWithLifecycle()
    var pendingResult by remember { mutableStateOf<ScanResult?>(null) }
    var scanResult by remember { mutableStateOf<ScanResult?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    LaunchedEffect(qrVerificationState) {
        when (qrVerificationState) {
            is QrVerificationState.Success -> {
                pendingResult?.let { scanResult = it }
                pendingResult = null
                isProcessing = false
                HapticUtil.confirm(context)
                appToast.success(context.getString(R.string.qr_scan_confirmed))
                viewModel.resetQrVerificationState()
            }
            is QrVerificationState.AlreadyConfirmed -> {
                pendingResult?.let { scanResult = it }
                pendingResult = null
                isProcessing = false
                HapticUtil.confirm(context)
                appToast.info(context.getString(R.string.qr_scan_already_confirmed))
                viewModel.resetQrVerificationState()
            }
            QrVerificationState.NotFound -> {
                pendingResult = null
                isProcessing = false
                HapticUtil.error(context)
                appToast.error(context.getString(R.string.qr_scan_not_found))
                viewModel.resetQrVerificationState()
            }
            QrVerificationState.LockedByOtherTrader -> {
                pendingResult = null
                isProcessing = false
                HapticUtil.error(context)
                appToast.error(context.getString(R.string.qr_scan_locked_by_other))
                viewModel.resetQrVerificationState()
            }
            is QrVerificationState.Error -> {
                pendingResult = null
                isProcessing = false
                HapticUtil.error(context)
                appToast.error(context.getString(R.string.qr_scan_error))
                viewModel.resetQrVerificationState()
            }
            QrVerificationState.Idle,
            QrVerificationState.Loading -> Unit
        }
    }

    LaunchedEffect(Unit) {
        if (!cameraPermission.status.isGranted) {
            cameraPermission.launchPermissionRequest()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!cameraPermission.status.isGranted) {
            // Permission not granted
            Column(
                Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Outlined.CameraAlt, null, modifier = Modifier.size(64.dp), tint = AppColors.TextHint)
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.qr_scan_camera_permission_title), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.qr_scan_camera_permission_message),
                    textAlign = TextAlign.Center,
                    color = AppColors.TextSecondary
                )
                Spacer(Modifier.height(16.dp))
                Button(onClick = { cameraPermission.launchPermissionRequest() }) {
                    Text(stringResource(R.string.qr_scan_camera_permission_action))
                }
            }
        } else if (scanResult != null) {
            // Scan success
            val result = scanResult!!
            Column(
                Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Outlined.CheckCircle,
                    null,
                    modifier = Modifier.size(72.dp),
                    tint = AppColors.Success
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    stringResource(R.string.qr_scan_success_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.Success
                )
                Spacer(Modifier.height(16.dp))

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = AppColors.GreenSurface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        InfoRow(stringResource(R.string.qr_scan_farmer), result.farmerName)
                        InfoRow(stringResource(R.string.weight_label_weight), stringResource(R.string.weight_format_kg_lower, result.weight))
                        InfoRow(stringResource(R.string.weight_metrics_total_amount), stringResource(R.string.card_list_money_vnd, result.amount))
                    }
                }

                Spacer(Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = {
                            scanResult = null
                            pendingResult = null
                            isProcessing = false
                            viewModel.resetQrVerificationState()
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(stringResource(R.string.qr_scan_continue))
                    }
                    Button(
                        onClick = { navController.navigate("trader_transactions") },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.GreenPrimary)
                    ) {
                        Text(stringResource(R.string.qr_scan_view_book))
                    }
                }
            }
        } else {
            // Camera preview
            Box(modifier = Modifier.weight(1f)) {
                CameraPreview(
                    onQrScanned = { rawValue ->
                        if (!isProcessing) {
                            isProcessing = true
                            val parts = rawValue.split("|")
                            if (parts.size >= 6 && parts[0] == "CANLUA") {
                                val token = parts[5]
                                val traderId = FirebaseAuth.getInstance().currentUser?.uid
                                if (traderId.isNullOrBlank()) {
                                    appToast.warning(context.getString(R.string.qr_scan_login_required))
                                    HapticUtil.error(context)
                                    isProcessing = false
                                    return@CameraPreview
                                }
                                pendingResult = ScanResult(
                                    farmerName = parts[2],
                                    weight = parts[3],
                                    amount = parts[4]
                                )
                                viewModel.verifyAndLockTransaction(token, traderId)
                            } else {
                                appToast.error(context.getString(R.string.qr_scan_invalid_code))
                                HapticUtil.error(context)
                                isProcessing = false
                            }
                        }
                    }
                )

                // Scan overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(250.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = 0.1f))
                )
            }

            // Bottom instruction
            Surface(
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    stringResource(R.string.qr_scan_instruction),
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.TextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun CameraPreview(onQrScanned: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    val options = remember {
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
    }
    val scanner = remember { BarcodeScanning.getClient(options) }

    DisposableEffect(Unit) {
        onDispose { cameraExecutor.shutdown() }
    }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { analysis ->
                        analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                            val mediaImage = imageProxy.image
                            if (mediaImage != null) {
                                val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                                scanner.process(image)
                                    .addOnSuccessListener { barcodes ->
                                        barcodes.firstOrNull()?.rawValue?.let { value ->
                                            onQrScanned(value)
                                        }
                                    }
                                    .addOnCompleteListener { imageProxy.close() }
                            } else {
                                imageProxy.close()
                            }
                        }
                    }

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalysis)
                } catch (e: Exception) {
                    Log.e("QrScan", "Camera bind failed", e)
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = AppColors.TextSecondary)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

private data class ScanResult(
    val farmerName: String,
    val weight: String,
    val amount: String
)
