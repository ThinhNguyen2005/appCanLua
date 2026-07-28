package com.giathinh.canlua.ui

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.FabPosition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.derivedStateOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.giathinh.canlua.ui.navigation.AppNavHost
import com.giathinh.canlua.ui.navigation.BottomNavItem
import com.giathinh.canlua.ui.viewmodel.SettingsViewModel
import com.giathinh.canlua.ui.viewmodel.CardListViewModel
import com.giathinh.canlua.ui.component.CreateCardBottomSheet
import com.giathinh.canlua.ui.component.CreateCardMode
import com.giathinh.canlua.ui.theme.AppColors
import com.giathinh.canlua.repository.WeighDefaults
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.Add

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MainScreen(deeplinkCardId: String? = null) {
    val navController = rememberNavController()
    var currentDeeplinkCardId by remember(deeplinkCardId) { mutableStateOf(deeplinkCardId) }
    
    LaunchedEffect(navController) {
        navController.addOnDestinationChangedListener { _, destination, _ ->
            val route = destination.route
            if (route != null) {
                com.giathinh.canlua.util.PerformanceTracker.onScreenChanged(route)
            }
        }
    }

    val navBackStackEntry = navController.currentBackStackEntryAsState()
    val currentRoute by remember {
        derivedStateOf { navBackStackEntry.value?.destination?.route }
    }

    var showCreateDialog by remember { mutableStateOf(false) }
    val cardListViewModel: CardListViewModel = hiltViewModel()
    val suggestedVarieties by cardListViewModel.suggestedRiceVarieties.collectAsStateWithLifecycle()
    val profileViewModel: com.giathinh.canlua.ui.viewmodel.ProfileViewModel = hiltViewModel()
    val profile by profileViewModel.profile.collectAsStateWithLifecycle(initialValue = null)

    val navItems = if (profile?.role.equals("TRADER", ignoreCase = true)) {
        BottomNavItem.traderNavItems
    } else {
        BottomNavItem.farmerNavItems
    }

    val currentTab = navItems.find { it.route == currentRoute }
    val isOnTabScreen = currentTab != null
    val isImeVisible = WindowInsets.isImeVisible

    val scrollVisible by com.giathinh.canlua.ui.util.BottomBarVisibility.visible.collectAsStateWithLifecycle()
    val showBottomBar = isOnTabScreen && !isImeVisible && scrollVisible

    val defaultScaleTitle = stringResource(com.giathinh.canlua.R.string.nav_scale)
    val topBarTitleRes = remember(currentRoute, currentTab) {
        currentTab?.labelRes
    }
    val topBarTitle = if (topBarTitleRes != null) stringResource(topBarTitleRes) else defaultScaleTitle

    val showTopBar = false

    val rawPinnedBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val rawEnterAlwaysBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val pinnedRoutes = listOf(
        BottomNavItem.SCALE.route,
        BottomNavItem.HISTORY.route,
        BottomNavItem.MARKET.route,
        BottomNavItem.AI_CHAT.route,
        BottomNavItem.PROFILE.route,
        BottomNavItem.STATISTICS.route
    )

    val scrollBehavior = remember(currentRoute) {
        if (currentRoute in pinnedRoutes) rawPinnedBehavior else rawEnterAlwaysBehavior
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        modifier = if (showTopBar) {
            Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
        } else {
            Modifier
        },
        topBar = {
            if (showTopBar) {
                CenterAlignedTopAppBar(
                    scrollBehavior = scrollBehavior,
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = topBarTitle,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },

                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        scrolledContainerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                        actionIconContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        },
        bottomBar = {
            androidx.compose.animation.AnimatedVisibility(
                visible = showBottomBar,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it })
            ) {
                NavigationBar(
                    containerColor = AppColors.CardBg,
                    tonalElevation = 8.dp
                ) {
                    navItems.forEach { item ->
                        val selected = currentRoute == item.route
                        val labelText = stringResource(item.labelRes)
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (currentRoute != item.route) {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selected) item.selectedIcon else item.icon,
                                    contentDescription = labelText
                                )
                            },
                            label = {
                                Text(
                                    text = labelText,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            alwaysShowLabel = true,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.White,
                                selectedTextColor = AppColors.GreenPrimary,
                                indicatorColor = AppColors.GreenPrimary,
                                unselectedIconColor = AppColors.TextSecondary,
                                unselectedTextColor = AppColors.TextSecondary
                            )
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (isOnTabScreen) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = showBottomBar,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut()
                ) {
                    val haptic = LocalHapticFeedback.current
                    androidx.compose.material3.FloatingActionButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                            showCreateDialog = true
                        },
                        containerColor = AppColors.GreenPrimary,
                        contentColor = Color.White,
                        shape = androidx.compose.foundation.shape.CircleShape
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(com.giathinh.canlua.R.string.fab_create_new_description)
                        )
                    }
                }
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AppNavHost(
                    navController = navController,
                    startDestination = navItems.first().route,
                    modifier = Modifier.fillMaxSize(),
                    deeplinkCardId = currentDeeplinkCardId,
                    onDeeplinkConsumed = { currentDeeplinkCardId = null }
                )
            }
        }
    }



    if (showCreateDialog) {
        CreateCardBottomSheet(
            ownerName = "Nông dân",
            suggestedVarieties = suggestedVarieties,
            onDismiss = { showCreateDialog = false },
            onCreate = { counterpartyName, counterpartyPhone, variety, season, moisture, price, deposit, cccd, impurityWeight, recordLocation ->
                cardListViewModel.createNewCard(
                    name = "Nông dân",
                    cccd = cccd,
                    traderName = counterpartyName,
                    pricePerKg = price,
                    depositAmount = deposit,
                    riceVariety = variety,
                    moisturePercent = moisture,
                    seasonLabel = season,
                    traderPhone = counterpartyPhone,
                    impurityWeight = impurityWeight,
                    recordLocation = recordLocation,
                    onCreated = { cardId ->
                        navController.navigate("card_detail/$cardId")
                        showCreateDialog = false
                    }
                )
            }
        )
    }
}


