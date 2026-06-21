@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package com.giathinh.canlua.ui.screen.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.giathinh.canlua.data.model.Card
import com.giathinh.canlua.ui.theme.AppColors
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.clustering.ClusterItem
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings

/**
 * Render bản đồ Google Maps với clustering — toàn bộ logic Maps SDK gói trong file này.
 *
 * @param cards đã được parent screen filter sẵn theo search/price/variety.
 * @param onCardSelected user tap 1 marker đơn lẻ (cluster size = 1).
 * @param onClusterTap user tap cluster nhiều card — caller tự quyết định
 *                    zoom in hay show list bottom sheet.
 */
@Composable
internal fun MapContent(
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
internal fun ClusterBubble(count: Int) {
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

/**
 * Adapter Card → ClusterItem cho Google Maps clustering library.
 * `getSnippet()` hiện trên info window mặc định nếu user dùng default marker;
 * trong app này custom info qua bottom sheet nên snippet chỉ là backup.
 */
data class RiceClusterItem(
    val card: Card,
    private val latLng: LatLng
) : ClusterItem {
    override fun getPosition(): LatLng = latLng
    override fun getTitle(): String = card.name.ifBlank { "Không tên" }
    override fun getSnippet(): String = "${MapFormatters.number(card.netWeight)} kg · ${card.bagCount} bao"
    override fun getZIndex(): Float = 0f
}
