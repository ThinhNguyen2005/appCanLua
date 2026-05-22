package com.GiaThinh.canlua.ui.screen.map

import android.Manifest
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
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
import com.google.maps.android.compose.rememberCameraPositionState
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import com.GiaThinh.canlua.util.TrackScreenRender
import kotlinx.coroutines.launch

/**
 * Bản đồ vệ tinh hiển thị các thẻ đã quét/cân lúa thực địa.
 *
 * v2.9 (Trader UX restructure):
 * - Default camera = vị trí GPS user (không còn cố định Cần Thơ).
 * - Search bar: lọc theo tên nông dân + giống lúa (đối sánh không dấu).
 * - Filter chip: ngưỡng giá (`>0`, `≥7k`, `≥8k`, `≥9k đ/kg`) + giống phổ biến.
 * - Toggle bộ lọc → ẩn/hiện hàng filter để giải phóng không gian map.
 *
 * v3.0 (refactor):
 * - File tách thành: MapContent (Maps SDK + clustering), MapFilters (search/chips),
 *   CardSummaryBottomSheet (sheet detail), MapEmptyStates (hint + placeholder),
 *   MapFormatters (format helpers). RiceMapScreen.kt giờ chỉ chứa state + layout.
 *
 * @param navController điều hướng đến `weight_input/{cardId}`.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun RiceMapScreen(
    navController: NavController,
    viewModel: CardViewModel = hiltViewModel()
) {
    TrackScreenRender("trader_map")
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

// === Hilt entry-point để inject LocationProvider trong @Composable ===
@EntryPoint
@InstallIn(SingletonComponent::class)
private interface LocationEntryPoint {
    fun locationProvider(): LocationProvider
}
