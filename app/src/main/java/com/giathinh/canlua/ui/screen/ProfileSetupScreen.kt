package com.giathinh.canlua.ui.screen

import com.giathinh.canlua.R

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.giathinh.canlua.ui.theme.AppColors
import com.giathinh.canlua.ui.viewmodel.ProfileViewModel
import com.giathinh.canlua.util.HapticUtil
import com.giathinh.canlua.util.TrackScreenRender

enum class UserRole { FARMER, TRADER, STAFF }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSetupScreen(
    navController: NavController,
    viewModel: ProfileViewModel = hiltViewModel(),
    onComplete: (() -> Unit)? = null
) {
    TrackScreenRender("profile_setup")
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var region by remember { mutableStateOf("") }
    var cccd by remember { mutableStateOf("") }
    var role by remember { mutableStateOf(UserRole.FARMER) }

    Scaffold(
        containerColor = Color.Transparent
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            AppColors.GreenSurface,
                            AppColors.Surface,
                            AppColors.WeightSurface
                        )
                    )
                )
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(36.dp))
            
            Text(
                text = stringResource(R.string.profile_setup_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = stringResource(R.string.profile_setup_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextSecondary,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Role Selection Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                RoleSelectionCard(
                    modifier = Modifier.weight(1f),
                    title = stringResource(R.string.role_farmer),
                    icon = Icons.Default.Agriculture,
                    isSelected = role == UserRole.FARMER,
                    onClick = {
                        HapticUtil.tick(context)
                        role = UserRole.FARMER
                    }
                )

                RoleSelectionCard(
                    modifier = Modifier.weight(1f),
                    title = stringResource(R.string.role_trader),
                    icon = Icons.Default.Storefront,
                    isSelected = role == UserRole.TRADER,
                    onClick = {
                        HapticUtil.tick(context)
                        role = UserRole.TRADER
                    }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = stringResource(R.string.profile_setup_role_hint),
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            
            Spacer(modifier = Modifier.height(28.dp))
            
            // Form Fields
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.profile_setup_name_label)) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp),
                shape = RoundedCornerShape(14.dp),
                singleLine = true,
                colors = profileSetupTextFieldColors()
            )
            
            Spacer(modifier = Modifier.height(18.dp))
            
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text(stringResource(R.string.profile_setup_phone_label)) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp),
                shape = RoundedCornerShape(14.dp),
                singleLine = true,
                colors = profileSetupTextFieldColors()
            )
            
            Spacer(modifier = Modifier.height(18.dp))
            
            OutlinedTextField(
                value = region,
                onValueChange = { region = it },
                label = { Text(stringResource(R.string.profile_setup_region_label)) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp),
                shape = RoundedCornerShape(14.dp),
                singleLine = true,
                colors = profileSetupTextFieldColors()
            )
            
            Spacer(modifier = Modifier.height(18.dp))
            
            OutlinedTextField(
                value = cccd,
                onValueChange = { cccd = it },
                label = { Text(stringResource(R.string.profile_setup_cccd_label)) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp),
                shape = RoundedCornerShape(14.dp),
                singleLine = true,
                colors = profileSetupTextFieldColors()
            )
            
            Spacer(modifier = Modifier.height(40.dp))
            
            Button(
                onClick = {
                    HapticUtil.tick(context)
                    viewModel.saveProfile(
                        name = name,
                        phone = phone,
                        region = region,
                        note = "",
                        role = role,
                        cccd = cccd,
                        username = "",
                        onSaved = {
                            // Save xong rồi mới navigate — tránh race khi AuthVM re-read DB.
                            if (onComplete != null) {
                                onComplete()
                            } else {
                                navController.navigate("main") {
                                    popUpTo("login") { inclusive = true }
                                }
                            }
                        }
                    )
                },
                enabled = name.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.GreenPrimary,
                    contentColor = Color.White,
                    disabledContainerColor = AppColors.GreenPrimary.copy(alpha = 0.45f),
                    disabledContentColor = Color.White.copy(alpha = 0.75f)
                )
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null)
                Spacer(modifier = Modifier.size(8.dp))
                Text(stringResource(R.string.profile_setup_complete), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun profileSetupTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = AppColors.GreenPrimary,
    unfocusedBorderColor = AppColors.Divider,
    focusedLabelColor = AppColors.GreenPrimary,
    unfocusedLabelColor = AppColors.TextSecondary,
    focusedTextColor = AppColors.TextPrimary,
    unfocusedTextColor = AppColors.TextPrimary,
    cursorColor = AppColors.GreenPrimary,
    focusedContainerColor = AppColors.SurfaceContainer,
    unfocusedContainerColor = AppColors.SurfaceContainer
)

@Composable
fun RoleSelectionCard(
    modifier: Modifier = Modifier,
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    locked: Boolean = false,
    subtitle: String? = null
) {
    val backgroundColor = when {
        locked -> AppColors.SurfaceContainer
        isSelected -> AppColors.GreenPrimary.copy(alpha = 0.12f)
        else -> AppColors.CardBg
    }
    val contentColor = when {
        locked -> AppColors.TextSecondary.copy(alpha = 0.5f)
        isSelected -> AppColors.GreenPrimary
        else -> AppColors.TextPrimary
    }
    val borderColor = when {
        locked -> AppColors.Divider
        isSelected -> AppColors.GreenPrimary
        else -> AppColors.Divider
    }

    Card(
        modifier = modifier
            .height(140.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 0.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    modifier = Modifier.size(44.dp),
                    tint = contentColor
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = contentColor.copy(alpha = 0.7f)
                    )
                }
            }
            if (locked) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = stringResource(R.string.profile_setup_locked_content),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(18.dp),
                    tint = contentColor.copy(alpha = 0.7f)
                )
            }
        }
    }
}

