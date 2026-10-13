package com.giathinh.canlua.ui.screen

import android.content.Intent
import android.net.Uri
import com.giathinh.canlua.R
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card as M3Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.giathinh.canlua.data.model.TraderHistoryItem
import com.giathinh.canlua.ui.feedback.LocalAppToast
import com.giathinh.canlua.ui.theme.AppColors
import com.giathinh.canlua.ui.viewmodel.ProfileViewModel
import com.giathinh.canlua.util.HapticUtil
import com.giathinh.canlua.util.TrackScreenRender
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TraderHistoryScreen(
    navController: NavController
) {
    TrackScreenRender("trader_history")
    val profileViewModel: ProfileViewModel = hiltViewModel()
    TraderHistoryScreenContent(
        navController = navController,
        profileViewModel = profileViewModel
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TraderHistoryScreenContent(
    navController: NavController,
    profileViewModel: ProfileViewModel
) {
    val history by profileViewModel.traderHistory.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.profile_trader_history_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        HapticUtil.tick(context)
                        navController.popBackStack()
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.content_back),
                            tint = AppColors.TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppColors.Surface,
                    titleContentColor = AppColors.TextPrimary
                ),
                windowInsets = WindowInsets(0.dp)
            )
        },
        containerColor = AppColors.Surface
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.Surface)
                .padding(padding)
        ) {
            if (history.isEmpty()) {
                EmptyHistoryFull()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        SummaryHeader(count = history.size)
                    }
                    items(history, key = { "${it.traderName}-${it.traderPhone}" }) { item ->
                        TraderHistoryRowFull(item)
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryHeader(count: Int) {
    M3Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.GreenSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(AppColors.GreenPrimary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.History,
                    contentDescription = null,
                    tint = AppColors.CardBg,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.size(12.dp))
            Column {
                Text(
                    text = stringResource(R.string.trader_hist_count, count),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary
                )
                Text(
                    text = stringResource(R.string.trader_hist_sort_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextHint
                )
            }
        }
    }
}

@Composable
private fun TraderHistoryRowFull(item: TraderHistoryItem) {
    val context = LocalContext.current
    val appToast = LocalAppToast.current
    val moneyFmt = remember { NumberFormat.getInstance(Locale.forLanguageTag("vi-VN")) }
    val dateFmt = remember { SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("vi-VN")) }

    var showContactOptions by remember { mutableStateOf(false) }

    if (showContactOptions) {
        AlertDialog(
            onDismissRequest = { showContactOptions = false },
            title = {
                Text(
                    text = stringResource(R.string.trader_hist_contact_title),
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary
                )
            },
            text = {
                Text(
                    text = stringResource(
                        R.string.trader_hist_contact_msg,
                        item.traderName,
                        item.traderPhone.ifBlank { stringResource(R.string.market_bid_unknown_variety) }
                    ),
                    color = AppColors.TextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        HapticUtil.tick(context)
                        showContactOptions = false
                        try {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${item.traderPhone}"))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            appToast.error(context.getString(R.string.trader_hist_err_call))
                        }
                    }
                ) {
                    Text(
                        text = stringResource(R.string.trader_hist_call),
                        color = AppColors.GreenPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            HapticUtil.tick(context)
                            showContactOptions = false
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://zalo.me/${item.traderPhone}"))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                appToast.error(context.getString(R.string.trader_hist_err_zalo))
                            }
                        }
                    ) {
                        Text(
                            text = stringResource(R.string.trader_hist_zalo),
                            color = AppColors.GreenPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    TextButton(
                        onClick = {
                            HapticUtil.tick(context)
                            showContactOptions = false
                        }
                    ) {
                        Text(
                            text = stringResource(R.string.action_cancel),
                            color = AppColors.TextSecondary
                        )
                    }
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = AppColors.Surface
        )
    }

    M3Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable {
                if (item.traderPhone.isNotBlank()) {
                    HapticUtil.tick(context)
                    showContactOptions = true
                }
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(AppColors.GreenSurface),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = item.traderName.firstOrNull()?.uppercase() ?: "?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.GreenPrimary
                )
            }
            Spacer(Modifier.size(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.traderName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.TextPrimary
                )
                if (item.traderPhone.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(vertical = 2.dp)
                    ) {
                        Icon(
                            Icons.Filled.Phone,
                            contentDescription = null,
                            tint = AppColors.GreenPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.size(6.dp))
                        Text(
                            text = item.traderPhone,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = AppColors.GreenPrimary,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
                if (!item.traderCccd.isNullOrBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Icon(
                            Icons.Filled.Person,
                            contentDescription = null,
                            tint = AppColors.TextHint,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(Modifier.size(6.dp))
                        Text(
                            text = stringResource(R.string.trader_hist_cccd, item.traderCccd),
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.TextHint
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.MonetizationOn,
                        contentDescription = null,
                        tint = AppColors.GoldDark,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.size(4.dp))
                    Text(
                        text = "${moneyFmt.format(item.totalRevenue.toLong())} đ",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.size(8.dp))
                    Text("•", color = AppColors.TextHint)
                    Spacer(Modifier.size(8.dp))
                    Text(
                        text = stringResource(R.string.trader_hist_deals_count, item.deals),
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextSecondary
                    )
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.CalendarToday,
                        contentDescription = null,
                        tint = AppColors.TextHint,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(Modifier.size(4.dp))
                    Text(
                        text = stringResource(R.string.trader_hist_last_deal, dateFmt.format(Date(item.lastDealDate))),
                        style = MaterialTheme.typography.labelSmall,
                        color = AppColors.TextHint
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyHistoryFull() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(AppColors.GreenSurface),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.Storefront,
                contentDescription = null,
                tint = AppColors.GreenPrimary,
                modifier = Modifier.size(48.dp)
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.trader_hist_empty_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.TextPrimary
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.trader_hist_empty_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = AppColors.TextHint,
            textAlign = TextAlign.Center
        )
    }
}
