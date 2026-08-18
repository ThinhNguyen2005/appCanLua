package com.giathinh.canlua.ui.screen.market

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.giathinh.canlua.data.model.RicePrice
import com.giathinh.canlua.ui.viewmodel.LiteMarketViewModel
import java.text.NumberFormat
import java.util.Locale

@Composable
fun MarketScreen(viewModel: LiteMarketViewModel = hiltViewModel()) {
    val prices by viewModel.prices.collectAsStateWithLifecycle()
    val formatter = NumberFormat.getNumberInstance(Locale("vi", "VN"))
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Gia lua offline", style = MaterialTheme.typography.headlineSmall)
        Text("Du lieu tu bo nho Room tren thiet bi.")
        if (prices.isEmpty()) {
            Text("Chua co bang gia da cache.", modifier = Modifier.padding(top = 16.dp))
        } else {
            LazyColumn(contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(prices, key = RicePrice::id) { price ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(price.variety, style = MaterialTheme.typography.titleMedium)
                            Text("${formatter.format(price.priceMin)} - ${formatter.format(price.priceMax)} VND/kg")
                            Text("${price.region} | ${price.riceType}")
                        }
                    }
                }
            }
        }
    }
}
