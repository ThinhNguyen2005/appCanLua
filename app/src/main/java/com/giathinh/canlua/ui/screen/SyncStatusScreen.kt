package com.giathinh.canlua.ui.screen

import com.giathinh.canlua.R
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.giathinh.canlua.repository.SyncStatus
import com.giathinh.canlua.ui.theme.AppColors
import com.giathinh.canlua.ui.viewmodel.SyncViewModel
import com.giathinh.canlua.util.HapticUtil
import com.giathinh.canlua.util.TrackScreenRender
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncStatusScreen(
    navController: NavController,
    viewModel: SyncViewModel = hiltViewModel()
) {
    TrackScreenRender("sync_status")
    val syncStatus by viewModel.syncStatus.collectAsStateWithLifecycle()
    val lastSyncTime by viewModel.lastSyncTime.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.sync_screen_title),
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
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.Surface)
                .padding(paddingValues)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Status Card
            val (cardBg, iconBg, contentColor) = when (syncStatus) {
                is SyncStatus.Success -> Triple(AppColors.GreenSurface, AppColors.GreenPrimary.copy(alpha = 0.15f), AppColors.GreenPrimary)
                is SyncStatus.Error -> Triple(AppColors.Error.copy(alpha = 0.1f), AppColors.Error.copy(alpha = 0.2f), AppColors.Error)
                is SyncStatus.Syncing -> Triple(AppColors.BlueSurface, AppColors.Blue.copy(alpha = 0.2f), AppColors.Blue)
                else -> Triple(AppColors.SurfaceContainer, AppColors.Divider, AppColors.TextSecondary)
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(iconBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (syncStatus) {
                                is SyncStatus.Success -> Icons.Default.CloudDone
                                is SyncStatus.Error -> Icons.Default.Error
                                is SyncStatus.Syncing -> Icons.Default.CloudSync
                                else -> Icons.Default.CloudOff
                            },
                            contentDescription = null,
                            modifier = Modifier.size(44.dp),
                            tint = contentColor
                        )
                    }

                    Text(
                        text = when (syncStatus) {
                            is SyncStatus.Success -> stringResource(R.string.sync_screen_success)
                            is SyncStatus.Error -> stringResource(R.string.sync_screen_error)
                            is SyncStatus.Syncing -> stringResource(R.string.sync_screen_syncing)
                            else -> stringResource(R.string.sync_screen_idle)
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary,
                        textAlign = TextAlign.Center
                    )

                    if (syncStatus is SyncStatus.Error) {
                        Text(
                            text = (syncStatus as SyncStatus.Error).message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppColors.Error,
                            textAlign = TextAlign.Center
                        )
                    }

                    val time = lastSyncTime
                    if (time != null) {
                        val dateFormat = remember { SimpleDateFormat("HH:mm:ss dd/MM/yyyy", Locale.getDefault()) }
                        Text(
                            text = stringResource(R.string.sync_screen_last_time, dateFormat.format(Date(time))),
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.TextHint
                        )
                    }
                }
            }

            // Sync Button
            Button(
                onClick = {
                    HapticUtil.tick(context)
                    viewModel.syncAll()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = syncStatus !is SyncStatus.Syncing,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.GreenPrimary,
                    contentColor = Color.White,
                    disabledContainerColor = AppColors.Divider
                )
            ) {
                if (syncStatus is SyncStatus.Syncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                }
                Text(
                    text = stringResource(R.string.sync_screen_btn_action),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = AppColors.CardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.Info,
                            contentDescription = null,
                            tint = AppColors.GreenPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.sync_screen_info_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = AppColors.TextPrimary
                        )
                    }
                    Text(
                        text = stringResource(R.string.sync_screen_info_line1),
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.TextSecondary
                    )
                    Text(
                        text = stringResource(R.string.sync_screen_info_line2),
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.TextSecondary
                    )
                    Text(
                        text = stringResource(R.string.sync_screen_info_line3),
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.TextSecondary
                    )
                }
            }
        }
    }
}
