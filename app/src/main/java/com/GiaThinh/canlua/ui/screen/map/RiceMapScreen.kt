package com.GiaThinh.canlua.ui.screen.map

import android.Manifest
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.GiaThinh.canlua.BuildConfig
import com.GiaThinh.canlua.data.model.Card
import com.GiaThinh.canlua.ui.theme.AppColors
import com.GiaThinh.canlua.ui.viewmodel.CardViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.clustering.ClusterItem
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Bản đồ vệ tinh hiển thị các thẻ đã quét/cân lúa thực địa.
 *
 * Tính năng:
 * - Hybrid map (vệ tinh + đường) để nhìn rõ cánh đồng.
 * - Marker xanh cho mỗi card có toạ độ; tự động cluster khi zoom out.
 * - Bottom sheet hiển thị tên + KL + bao + nút đi đến nhập cân.
 * - Permission flow: yêu cầu FINE/COARSE location nếu chưa có.
 *
 * @param navController điều hướng đến `weight_input/{cardId}`.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun RiceMapScreen(
    navController: NavController,
    viewModel: CardViewModel = hiltViewModel()
) {
    // Guard: chưa cấu hình Maps API key → hiển thị placeholder, không khởi tạo GoogleMap
    // để tránh crash "AuthFailure" + RuntimeException khi SDK chưa được cấp quyền.
    if (BuildConfig.MAPS_API_KEY.isBlank() ||
        BuildConfig.MAPS_API_KEY == "YOUR_GOOGLE_MAPS_API_KEY_HERE"
    ) {
        MapComingSoonPlaceholder()
        return
    }
    val cards by viewModel.cards.collectAsState()
    // Mặc định: ĐBSCL (Cần Thơ) — zoom 10 đủ thấy hầu hết tỉnh.
    val defaultLatLng = remember { LatLng(10.0452, 105.7469) }
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLatLng, 10f)
    }

    // ── Permission ──
    val permissionState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )
    LaunchedEffect(Unit) {
        if (!permissionState.allPermissionsGranted) {
            permissionState.launchMultiplePermissionRequest()
        }
    }

    // Lọc các card có GPS hợp lệ
    val mappable = remember(cards) {
        cards.filter { it.latitude != null && it.longitude != null }
    }

    // ── Bottom sheet state ──
    var selectedCard by remember { mutableStateOf<Card?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    // Quay về vị trí mặc định ĐBSCL
                    scope.launch {
                        cameraPositionState.animate(
                            update = CameraUpdateFactory.newLatLngZoom(defaultLatLng, 10f),
                            durationMs = 600
                        )
                    }
                },
                containerColor = AppColors.GreenPrimary,
                contentColor = Color.White
            ) {
                Icon(Icons.Filled.MyLocation, contentDescription = "Vị trí mặc định")
            }
        },
        containerColor = AppColors.Surface
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            MapContent(
                cards = mappable,
                cameraPositionState = cameraPositionState,
                hasLocationPermission = permissionState.allPermissionsGranted,
                onCardSelected = { selectedCard = it },
                onClusterTap = { items ->
                    // Cluster có nhiều item → mở sheet cho item đầu (hoặc mở danh sách)
                    if (items.size == 1) {
                        selectedCard = items.first().card
                    } else {
                        // Zoom in tự nhiên — Maps utility tự handle, nhưng nếu cần force:
                        scope.launch {
                            val current = cameraPositionState.position.zoom
                            cameraPositionState.animate(
                                CameraUpdateFactory.zoomTo((current + 2f).coerceAtMost(18f)),
                                durationMs = 350
                            )
                        }
                    }
                }
            )

            // Header overlay (legend)
            HeaderLegend(
                total = mappable.size,
                modifier = Modifier
                    .padding(12.dp)
                    .align(Alignment.TopCenter)
            )

            // Empty hint khi chưa có card nào có GPS
            if (mappable.isEmpty()) {
                EmptyMapHint(
                    permissionGranted = permissionState.allPermissionsGranted,
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.BottomCenter)
                )
            }
        }
    }

    // ── Bottom Sheet ──
    selectedCard?.let { card ->
        ModalBottomSheet(
            onDismissRequest = { selectedCard = null },
            sheetState = sheetState,
            containerColor = AppColors.CardBg
        ) {
            CardSummaryBottomSheet(
                card = card,
                onWeighClick = {
                    selectedCard = null
                    navController.navigate("weight_input/${card.id}")
                }
            )
        }
    }
}

// ────────────────────── Map Content ──────────────────────

@Composable
private fun MapContent(
    cards: List<Card>,
    cameraPositionState: CameraPositionState,
    hasLocationPermission: Boolean,
    onCardSelected: (Card) -> Unit,
    onClusterTap: (List<RiceClusterItem>) -> Unit
) {
    val items = remember(cards) {
        cards.mapNotNull { card ->
            val lat = card.latitude ?: return@mapNotNull null
            val lon = card.longitude ?: return@mapNotNull null
            RiceClusterItem(card = card, latLng = LatLng(lat, lon))
        }
    }

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        properties = MapProperties(
            mapType = MapType.HYBRID,
            isMyLocationEnabled = hasLocationPermission
        ),
        uiSettings = MapUiSettings(
            zoomControlsEnabled = false,
            myLocationButtonEnabled = true,
            mapToolbarEnabled = false,
            compassEnabled = true
        )
    ) {
        // Clustering — built-in của maps-compose-utils
        com.google.maps.android.compose.clustering.Clustering(
            items = items,
            onClusterClick = { cluster ->
                onClusterTap(cluster.items.toList())
                true
            },
            onClusterItemClick = { item ->
                onCardSelected(item.card)
                true
            },
            clusterContent = { cluster ->
                ClusterBubble(count = cluster.size)
            }
        )
    }
}

@Composable
private fun ClusterBubble(count: Int) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(AppColors.GreenPrimary),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = count.toString(),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium
        )
    }
}

// ────────────────────── Bottom Sheet content ──────────────────────

@Composable
private fun CardSummaryBottomSheet(
    card: Card,
    onWeighClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header — avatar + name
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(AppColors.GreenSurface),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Map,
                    contentDescription = null,
                    tint = AppColors.GreenPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    card.name.ifBlank { "Không tên" },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    formatDate(card.date),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextHint
                )
            }
        }

        // Stats row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MapStatBox(
                icon = Icons.Filled.Scale,
                label = "Khối lượng",
                value = "${formatNumber(card.netWeight)} kg",
                modifier = Modifier.weight(1f)
            )
            MapStatBox(
                icon = Icons.Filled.ShoppingCart,
                label = "Số bao",
                value = "${card.bagCount}",
                modifier = Modifier.weight(1f)
            )
        }

        if (card.riceVariety.isNotBlank()) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(AppColors.Info.copy(alpha = 0.12f))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    "Giống: ${card.riceVariety}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.Info
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        Button(
            onClick = onWeighClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AppColors.GreenPrimary),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Filled.Scale, contentDescription = null)
            Spacer(Modifier.size(8.dp))
            Text(
                "Vào Nhập Cân",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun MapStatBox(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(AppColors.SurfaceContainer)
            .padding(vertical = 12.dp, horizontal = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon,
                contentDescription = null,
                tint = AppColors.TextHint,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.size(6.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = AppColors.TextHint)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = AppColors.TextPrimary
        )
    }
}

// ────────────────────── Overlays ──────────────────────

@Composable
private fun HeaderLegend(total: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.Black.copy(alpha = 0.55f))
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(AppColors.GreenLight)
            )
            Spacer(Modifier.size(8.dp))
            Text(
                text = "$total điểm cân lúa",
                color = Color.White,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun EmptyMapHint(
    permissionGranted: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AppColors.CardBg.copy(alpha = 0.95f))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (permissionGranted) Icons.Filled.Map else Icons.Filled.LocationOff,
                contentDescription = null,
                tint = AppColors.GoldAccent,
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (permissionGranted)
                        "Chưa có thẻ cân nào có toạ độ GPS"
                    else
                        "Cần quyền vị trí để hiển thị bản đồ",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = if (permissionGranted)
                        "Tạo thẻ cân mới ngoài đồng — toạ độ sẽ tự ghi nhận."
                    else
                        "Cấp quyền vị trí trong Cài đặt rồi quay lại.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextSecondary,
                    textAlign = TextAlign.Start
                )
            }
        }
    }
}

// ────────────────────── Helpers ──────────────────────

/**
 * Cluster item wrapper — mỗi card 1 vị trí trên map.
 * Title/snippet để Maps SDK có info nội bộ; UI chính dùng bottom sheet.
 */
data class RiceClusterItem(
    val card: Card,
    private val latLng: LatLng
) : ClusterItem {
    override fun getPosition(): LatLng = latLng
    override fun getTitle(): String = card.name.ifBlank { "Không tên" }
    override fun getSnippet(): String = "${formatNumber(card.netWeight)} kg · ${card.bagCount} bao"
    override fun getZIndex(): Float = 0f
}

private val numberFormat = NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN"))
private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("vi-VN"))

private fun formatNumber(value: Double): String =
    if (value % 1.0 == 0.0) numberFormat.format(value.toLong())
    else "%.2f".format(value)

private fun formatDate(date: java.util.Date): String = dateFormat.format(date)

// ────────────────────── Placeholder khi thiếu Maps API key ──────────────────────

/**
 * Hiển thị khi `BuildConfig.MAPS_API_KEY` rỗng — tránh khởi tạo `GoogleMap`
 * (sẽ throw `RuntimeException`/AuthFailure → văng app).
 *
 * Thiết kế premium, đồng bộ với theme: gradient xanh + icon map + hint text.
 */
@Composable
private fun MapComingSoonPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                androidx.compose.ui.graphics.Brush.verticalGradient(
                    listOf(
                        AppColors.GreenPrimary.copy(alpha = 0.08f),
                        AppColors.Surface
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            // Icon container
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(AppColors.GreenPrimary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Map,
                    contentDescription = null,
                    tint = AppColors.GreenPrimary,
                    modifier = Modifier.size(48.dp)
                )
            }

            Text(
                text = "Bản đồ thu mua",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Tính năng đang trong giai đoạn phát triển.\nSẽ sớm ra mắt trong bản cập nhật tiếp theo.",
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(Modifier.height(8.dp))

            // Badge "Coming soon"
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(AppColors.GreenPrimary.copy(alpha = 0.15f))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "🚧  Sắp ra mắt",
                    style = MaterialTheme.typography.labelMedium,
                    color = AppColors.GreenPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

