package com.giathinh.canlua.ui.screen

import com.giathinh.canlua.R
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card as M3Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.giathinh.canlua.data.model.DeletedCard
import com.giathinh.canlua.ui.theme.AppColors
import com.giathinh.canlua.ui.viewmodel.DeletedCardsViewModel
import com.giathinh.canlua.util.HapticUtil
import com.giathinh.canlua.util.TrackScreenRender
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeletedCardsScreen(
    navController: NavController
) {
    TrackScreenRender("deleted_cards")
    val viewModel: DeletedCardsViewModel = hiltViewModel()
    DeletedCardsScreenContent(
        navController = navController,
        viewModel = viewModel
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeletedCardsScreenContent(
    navController: NavController,
    viewModel: DeletedCardsViewModel
) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    val restored by viewModel.restored.collectAsStateWithLifecycle()
    var pendingPurge by remember { mutableStateOf<DeletedCard?>(null) }
    val context = LocalContext.current

    // Sau khi restore thành công → navigate vào card detail.
    LaunchedEffect(restored) {
        restored?.let { newId ->
            viewModel.clearRestoredEvent()
            navController.navigate("card_detail/$newId") {
                popUpTo("deleted_cards") { inclusive = true }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_deleted_cards_title),
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
            if (items.isEmpty()) {
                EmptyState()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            text = stringResource(R.string.deleted_cards_header_desc, items.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.TextHint
                        )
                    }
                    items(items, key = { it.id }) { tomb ->
                        DeletedCardRow(
                            tomb = tomb,
                            onRestore = {
                                HapticUtil.tick(context)
                                viewModel.restore(tomb.id)
                            },
                            onPurge = {
                                HapticUtil.tick(context)
                                pendingPurge = tomb
                            }
                        )
                    }
                }
            }
        }
    }

    pendingPurge?.let { tomb ->
        AlertDialog(
            onDismissRequest = { pendingPurge = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.DeleteForever,
                        contentDescription = null,
                        tint = AppColors.Error,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(
                        text = stringResource(R.string.deleted_cards_purge_title),
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Text(
                    text = stringResource(
                        R.string.deleted_cards_purge_msg,
                        tomb.name.ifBlank { stringResource(R.string.deleted_cards_unnamed) }
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        HapticUtil.tick(context)
                        viewModel.purge(tomb.id)
                        pendingPurge = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = AppColors.Error)
                ) {
                    Text(
                        text = stringResource(R.string.deleted_cards_purge_action),
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    HapticUtil.tick(context)
                    pendingPurge = null
                }) {
                    Text(
                        text = stringResource(R.string.action_cancel),
                        color = AppColors.TextSecondary
                    )
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = AppColors.Surface
        )
    }
}

@Composable
private fun DeletedCardRow(
    tomb: DeletedCard,
    onRestore: () -> Unit,
    onPurge: () -> Unit
) {
    val moneyFmt = remember { NumberFormat.getInstance(Locale.forLanguageTag("vi-VN")) }
    val dateFmt = remember { SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("vi-VN")) }

    M3Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(AppColors.Error.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = null,
                        tint = AppColors.Error,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.size(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tomb.name.ifBlank { stringResource(R.string.deleted_cards_unnamed) },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.TextPrimary
                    )
                    Text(
                        text = stringResource(R.string.deleted_cards_date_prefix, dateFmt.format(Date(tomb.cardDate))) +
                            if (tomb.seasonLabel.isNotBlank()) " · ${tomb.seasonLabel}" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextHint
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${moneyFmt.format(tomb.totalWeight.toLong())} kg",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.TextSecondary
                )
                Text(
                    text = "${moneyFmt.format(tomb.totalAmount.toLong())} đ",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.GreenPrimary
                )
            }

            Text(
                text = stringResource(R.string.deleted_cards_deleted_at, dateFmt.format(Date(tomb.deletedAt))),
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.TextHint
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onPurge,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.Error),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Outlined.DeleteForever, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(6.dp))
                    Text(stringResource(R.string.deleted_cards_purge_btn))
                }
                Button(
                    onClick = onRestore,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.GreenPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(6.dp))
                    Text(
                        text = stringResource(R.string.deleted_cards_restore_btn),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(AppColors.Error.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.Delete,
                contentDescription = null,
                tint = AppColors.Error.copy(alpha = 0.6f),
                modifier = Modifier.size(48.dp)
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.deleted_cards_empty_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.TextPrimary
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.deleted_cards_empty_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = AppColors.TextHint,
            textAlign = TextAlign.Center
        )
    }
}
