@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.GiaThinh.canlua.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.GiaThinh.canlua.data.model.Card
import com.GiaThinh.canlua.ui.component.CardItem
import com.GiaThinh.canlua.ui.component.CreateCardDialog
import com.GiaThinh.canlua.ui.theme.CanLuaTheme
import com.GiaThinh.canlua.ui.theme.YellowHighlight
import com.GiaThinh.canlua.ui.viewmodel.CardViewModel
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardListScreen(
    navController: NavController,
    viewModel: CardViewModel = hiltViewModel()
) {
    val cards by viewModel.cards.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }

    CardListContent(
        cards = cards,
        onCardClick = { navController.navigate("card_detail/${it.id}") },
        onCreateClick = { showCreateDialog = true },
        onSettingsClick = { navController.navigate("settings") }
    )

    if (showCreateDialog) {
        CreateCardDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { name, cccd, traderName, pricePerKg, depositAmount ->
                viewModel.createNewCard(name, cccd, traderName, pricePerKg, depositAmount)
                showCreateDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CardListContent(
    cards: List<Card>,
    onCardClick: (Card) -> Unit,
    onCreateClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Scaffold(
        topBar = {
            CardListTopBar(onSettingsClick = onSettingsClick)
        },
        floatingActionButton = {
            AddCardFab(onClick = onCreateClick)
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (cards.isEmpty()) {
                EmptyStateCardList(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 32.dp, vertical = 24.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    contentPadding = PaddingValues(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(cards, key = { it.id }) { card ->
                        CardItem(
                            card = card,
                            onClick = { onCardClick(card) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CardListTopBar(onSettingsClick: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(androidx.compose.foundation.layout.WindowInsets.statusBars)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "Cân Lúa",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Quản lý phiếu cân",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                )
            }

            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "Cài đặt",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Composable
private fun AddCardFab(onClick: () -> Unit) {
    FloatingActionButton(
        onClick = onClick,
        containerColor = YellowHighlight,
        contentColor = Color.Black,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.shadow(10.dp, RoundedCornerShape(18.dp))
    ) {
        Icon(
            Icons.Default.Add,
            contentDescription = "Thêm card mới",
            tint = MaterialTheme.colorScheme.onPrimary
        )
    }
}

@Composable
private fun EmptyStateCardList(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(132.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            tonalElevation = 2.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Chưa có phiếu cân",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Nhấn nút + để tạo phiếu mới và bắt đầu cân",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun CardListScreenPreview() {
    val sampleCards = listOf(
        Card(
            id = 1L,
            name = "Nguyễn Văn A",
            cccd = "123456789012",
            date = Date(),
            totalWeight = 1250.0,
            netWeight = 1220.0,
            pricePerKg = 6800.0,
            totalAmount = 8296000.0,
            paidAmount = 3000000.0,
            remainingAmount = 5296000.0,
            bagCount = 24,
            isLocked = false
        ),
        Card(
            id = 2L,
            name = "Trần Thị B",
            cccd = "098765432109",
            date = Date(),
            totalWeight = 980.0,
            netWeight = 950.0,
            pricePerKg = 7000.0,
            totalAmount = 6650000.0,
            paidAmount = 6650000.0,
            remainingAmount = 0.0,
            bagCount = 20,
            isLocked = true
        )
    )

    CanLuaTheme {
        CardListContent(
            cards = sampleCards,
            onCardClick = {},
            onCreateClick = {},
            onSettingsClick = {}
        )
    }
}
