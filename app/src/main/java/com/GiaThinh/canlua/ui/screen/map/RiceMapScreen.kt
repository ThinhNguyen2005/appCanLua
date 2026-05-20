package com.GiaThinh.canlua.ui.screen.map

import android.Manifest
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.GiaThinh.canlua.BuildConfig
import com.GiaThinh.canlua.data.location.LocationProvider
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
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Bản đồ vệ tinh hiển thị các thẻ đã quét/cân lúa thực địa.
 *
 * v2.9 (Trader UX restructure):
 * - Default camera = vị trí GPS user (không còn cố định Cần Thơ).
 * - Search bar: lọc theo tên nông dân + giống lúa (đối sánh không dấu).
 * - Filter chip: ngưỡng giá (`>0`, `≥7k`, `≥8k`, `≥9k đ/kg`) + giống phổ biến.
 * - Toggle bộ lọc → ẩn/hiện hàng filter để giải phóng không gian map.
 *
 * @param navController điều hướng đến `weight_input/{cardId}`.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun RiceMapScreen(
    navController: NavController,
    viewModel: CardViewModel = hiltViewModel()
) {
    if (BuildConfig.MAPS_API_KEY.isBlank() ||
        BuildConfig.MAPS_API_KEY == "YOUR_GOOGLE_MAPS_API_KEY_HERE"
    ) {
        MapComingSoonPlaceholder()
        return
    }

    val cards by viewModel.cards.collectAsState()
    val context = LocalContext.current
    // Hilt entry-point để inject LocationProvider trong @Composable mà không
    // cần thay đổi viewmodel signature.
    val locationProvider = remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            LocationEntryPoint::class.java
        ).locationProvider()
    }

    // Mặc định: ĐBSCL (fallback nếu chưa có permission/GPS).
    val fallbackLatLng = remember { LatLng(10.0452, 105.7469) }
    var userLatLng by remember { mutableStateOf<LatLng?>(null) }
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(fallbackLatLng, 10f)
    }

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

    // Khi có permission → lấy GPS thật, animate camera tới vị trí user.
    LaunchedEffect(permissionState.allPermissionsGranted) {
        if (permissionState.allPermissionsGranted && userLatLng == null) {
            locationProvider.getCurrentLocation()?.let { gp ->
                val ll = LatLng(gp.lat, gp.lon)
                userLatLng = ll
                cameraPositionState.animate(
                    update = CameraUpdateFactory.newLatLngZoom(ll, 13f),
                    durationMs = 700
                )
            }
        }
    }

    // === Filter state ===
    var query by remember { mutableStateOf("") }
    var showFilters by remember { mutableStateOf(false) }
    var priceFilter by remember { mutableStateOf(PriceFilter.ALL) }
    var varietyFilter by remember { mutableStateOf<String?>(null) }

    val mappable = remember(cards) {
        cards.filter { it.latitude != null && it.longitude != null }
    }

    val filtered = remember(mappable, query, priceFilter, varietyFilter) {
        val nq = query.normalizeForSearch()
        mappable.filter { c ->
            val matchesQuery = nq.isBlank() ||
                c.name.normalizeForSearch().contains(nq) ||
                c.riceVariety.normalizeForSearch().contains(nq) ||
                c.fieldAddress.normalizeForSearch().contains(nq)
            val matchesPrice = when (priceFilter) {
                PriceFilter.ALL -> true
                PriceFilter.HAS_PRICE -> c.pricePerKg > 0
                PriceFilter.MIN_7K -> c.pricePerKg >= 7000
                PriceFilter.MIN_8K -> c.pricePerKg >= 8000
                PriceFilter.MIN_9K -> c.pricePerKg >= 9000
            }
            val matchesVariety = varietyFilter == null ||
                c.riceVariety.equals(varietyFilter, ignoreCase = true)
            matchesQuery && matchesPrice && matchesVariety
        }
    }

    val varieties = remember(mappable) {
        mappable.mapNotNull { it.riceVariety.takeIf { v -> v.isNotBlank() } }
            .distinct()
            .sorted()
    }

    var selectedCard by remember { mutableStateOf<Card?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    val target = userLatLng ?: fallbackLatLng
                    scope.launch {
                        cameraPositionState.animate(
                            update = CameraUpdateFactory.newLatLngZoom(target, 13f),
                            durationMs = 600
                        )
                    }
                },
                containerColor = AppColors.GreenPrimary,
                contentColor = Color.White,
                modifier = Modifier.padding(bottom = 88.dp)
            ) {
                Icon(Icons.Filled.MyLocation, contentDescription = "Vị trí của tôi")
            }
        },
        containerColor = AppColors.Surface
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // === Top control: search + filter (đặt NGOÀI map, không overlay) ===
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppColors.Surface)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                SearchAndFilterBar(
                    query = query,
                    onQueryChange = { query = it },
                    onClear = { query = "" },
                    showFilters = showFilters,
                    onToggleFilters = { showFilters = !showFilters },
                    activeFilters = (priceFilter != PriceFilter.ALL) || varietyFilter != null,
                    matchCount = filtered.size,
                    totalCount = mappable.size
                )

                AnimatedVisibility(
                    visible = showFilters,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    FilterChipsRow(
                        priceFilter = priceFilter,
                        onPriceChange = { priceFilter = it },
                        varieties = varieties,
                        selectedVariety = varietyFilter,
                        onVarietyChange = { varietyFilter = it }
                    )
                }
            }

            // === Map area: chiếm phần còn lại, có viền bo và padding ===
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(20.dp))
            ) {
                MapContent(
                    cards = filtered,
                    cameraPositionState = cameraPositionState,
                    hasLocationPermission = permissionState.allPermissionsGranted,
                    onCardSelected = { selectedCard = it },
                    onClusterTap = { items ->
                        if (items.size == 1) {
                            selectedCard = items.first().card
                        } else {
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

                if (filtered.isEmpty()) {
                    EmptyMapHint(
                        permissionGranted = permissionState.allPermissionsGranted,
                        hasFilters = query.isNotBlank() ||
                            priceFilter != PriceFilter.ALL ||
                            varietyFilter != null,
                        modifier = Modifier
                            .padding(16.dp)
                            .align(Alignment.BottomCenter)
                    )
                }
            }
        }
    }

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

// ────────────────────── Search + Filter ──────────────────────

private enum class PriceFilter { ALL, HAS_PRICE, MIN_7K, MIN_8K, MIN_9K }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchAndFilterBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    showFilters: Boolean,
    onToggleFilters: () -> Unit,
    activeFilters: Boolean,
    matchCount: Int,
    totalCount: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AppColors.CardBg.copy(alpha = 0.96f))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = {
                Text(
                    "Tìm tên / giống lúa / địa chỉ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.TextHint
                )
            },
            leadingIcon = {
                Icon(Icons.Filled.Search, contentDescription = null, tint = AppColors.TextHint)
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = onClear) {
                        Icon(Icons.Filled.Close, contentDescription = "Xoá")
                    }
                }
            },
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent
            ),
            modifier = Modifier
                .weight(1f)
                .height(54.dp)
        )

        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(
                    if (activeFilters || showFilters)
                        AppColors.GreenPrimary.copy(alpha = 0.18f)
                    else
                        Color.Transparent
                ),
            contentAlignment = Alignment.Center
        ) {
            IconButton(onClick = onToggleFilters) {
                Icon(
                    Icons.Filled.FilterList,
                    contentDescription = "Bộ lọc",
                    tint = if (activeFilters) AppColors.GreenPrimary else AppColors.TextSecondary
                )
            }
        }
    }

    if (totalCount > 0 && (query.isNotBlank() || activeFilters)) {
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black.copy(alpha = 0.55f))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                "Hiện $matchCount / $totalCount điểm",
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterChipsRow(
    priceFilter: PriceFilter,
    onPriceChange: (PriceFilter) -> Unit,
    varieties: List<String>,
    selectedVariety: String?,
    onVarietyChange: (String?) -> Unit
) {
    Column(modifier = Modifier.padding(top = 8.dp)) {
        Text(
            "Giá lúa",
            style = MaterialTheme.typography.labelSmall,
            color = AppColors.TextHint,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(PriceFilter.entries.size) { idx ->
                val pf = PriceFilter.entries[idx]
                FilterChip(
                    selected = priceFilter == pf,
                    onClick = { onPriceChange(pf) },
                    label = { Text(pf.label()) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AppColors.GreenPrimary,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        if (varieties.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Giống lúa",
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.TextHint,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(varieties.size + 1) { i ->
                    if (i == 0) {
                        FilterChip(
                            selected = selectedVariety == null,
                            onClick = { onVarietyChange(null) },
                            label = { Text("Tất cả") }
                        )
                    } else {
                        val v = varieties[i - 1]
                        FilterChip(
                            selected = selectedVariety == v,
                            onClick = {
                                onVarietyChange(if (selectedVariety == v) null else v)
                            },
                            label = { Text(v) }
                        )
                    }
                }
            }
        }
    }
}

private fun PriceFilter.label(): String = when (this) {
    PriceFilter.ALL -> "Tất cả"
    PriceFilter.HAS_PRICE -> "Đã có giá"
    PriceFilter.MIN_7K -> "≥ 7,000 đ"
    PriceFilter.MIN_8K -> "≥ 8,000 đ"
    PriceFilter.MIN_9K -> "≥ 9,000 đ"
}

private fun String.normalizeForSearch(): String {
    val lowered = lowercase()
    val noDiacritics = java.text.Normalizer.normalize(lowered, java.text.Normalizer.Form.NFD)
        .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
        .replace("đ", "d")
    return noDiacritics
}

// LazyRow needs items() — import shim
private inline fun androidx.compose.foundation.lazy.LazyListScope.items(
    count: Int,
    crossinline itemContent: @Composable (Int) -> Unit
) {
    items(count = count) { idx -> itemContent(idx) }
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
            myLocationButtonEnabled = false,
            mapToolbarEnabled = false,
            compassEnabled = true
        )
    ) {
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
            clusterContent = { cluster -> ClusterBubble(count = cluster.size) }
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

        if (card.pricePerKg > 0 || card.riceVariety.isNotBlank()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
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
                if (card.pricePerKg > 0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(AppColors.GreenPrimary.copy(alpha = 0.14f))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            "Giá: ${formatNumber(card.pricePerKg)} đ/kg",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = AppColors.GreenPrimary
                        )
                    }
                }
            }
        }

        if (card.fieldAddress.isNotBlank()) {
            ContactInfoRow(
                icon = Icons.Filled.LocationOn,
                label = "Địa chỉ ruộng",
                value = card.fieldAddress,
                tint = AppColors.GreenPrimary
            )
        }

        if (card.traderPhone.isNotBlank()) {
            ContactInfoRow(
                icon = Icons.Filled.Phone,
                label = "SĐT thương lái",
                value = card.traderPhone,
                tint = AppColors.Info
            )
        }

        Spacer(Modifier.height(4.dp))

        Button(
            onClick = onWeighClick,
            modifier = Modifier.fillMaxWidth().height(54.dp),
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
            Icon(icon, null, tint = AppColors.TextHint, modifier = Modifier.size(16.dp))
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

@Composable
private fun ContactInfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    tint: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AppColors.SurfaceContainer)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.size(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = AppColors.TextHint)
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun EmptyMapHint(
    permissionGranted: Boolean,
    hasFilters: Boolean,
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
                    text = when {
                        !permissionGranted -> "Cần quyền vị trí để hiển thị bản đồ"
                        hasFilters -> "Không có điểm nào khớp bộ lọc"
                        else -> "Chưa có thẻ cân nào có toạ độ GPS"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = when {
                        !permissionGranted -> "Cấp quyền vị trí trong Cài đặt rồi quay lại."
                        hasFilters -> "Thử bỏ bớt bộ lọc hoặc đổi từ khoá tìm kiếm."
                        else -> "Tạo thẻ cân mới ngoài đồng — toạ độ sẽ tự ghi nhận."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextSecondary,
                    textAlign = TextAlign.Start
                )
            }
        }
    }
}

// ────────────────────── Helpers ──────────────────────

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

// === Hilt entry-point để inject LocationProvider trong @Composable ===
@EntryPoint
@InstallIn(SingletonComponent::class)
private interface LocationEntryPoint {
    fun locationProvider(): LocationProvider
}

// ────────────────────── Placeholder khi thiếu Maps API key ──────────────────────

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
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(AppColors.GreenPrimary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Map, null, tint = AppColors.GreenPrimary, modifier = Modifier.size(48.dp))
            }
            Text(
                "Bản đồ thu mua",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary,
                textAlign = TextAlign.Center
            )
            Text(
                "Tính năng đang trong giai đoạn phát triển.\nSẽ sớm ra mắt trong bản cập nhật tiếp theo.",
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(AppColors.GreenPrimary.copy(alpha = 0.15f))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    "🚧  Sắp ra mắt",
                    style = MaterialTheme.typography.labelMedium,
                    color = AppColors.GreenPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
