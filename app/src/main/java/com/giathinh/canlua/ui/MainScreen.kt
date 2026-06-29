package com.giathinh.canlua.ui

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.giathinh.canlua.ui.component.BottomBarItemSpec
import com.giathinh.canlua.ui.component.ModernBottomBar
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
    
    val navItems = BottomNavItem.navItems

    val bottomBarItems = remember(navItems) {
        navItems.map { nav ->
            BottomBarItemSpec(
                route = nav.route,
                icon = nav.icon,
                selectedIcon = nav.selectedIcon,
                label = "",
                labelRes = nav.labelRes
            )
        }
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

    val showTopBar = currentRoute == BottomNavItem.STATISTICS.route

    val rawPinnedBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val rawEnterAlwaysBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val pinnedRoutes = listOf(
        BottomNavItem.SCALE.route,
        BottomNavItem.HISTORY.route,
        BottomNavItem.STATISTICS.route
    )
    val scrollBehavior = remember(currentRoute) {
        if (currentRoute in pinnedRoutes) rawPinnedBehavior else rawEnterAlwaysBehavior
    }

    Scaffold(
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
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = paddingValues.calculateTopPadding(),
                    start = paddingValues.calculateStartPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
                    end = paddingValues.calculateEndPadding(androidx.compose.ui.unit.LayoutDirection.Ltr)
                )
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AppNavHost(
                    navController = navController,
                    startDestination = navItems.first().route,
                    modifier = Modifier.fillMaxSize(),
                    deeplinkCardId = currentDeeplinkCardId,
                    onDeeplinkConsumed = { currentDeeplinkCardId = null }
                )

                androidx.compose.animation.AnimatedVisibility(
                    visible = showBottomBar,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                    exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        ModernBottomBar(
                            items = bottomBarItems,
                            currentRoute = currentRoute,
                            onItemClick = { item ->
                                if (currentRoute != item.route) {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        )

                        val context = androidx.compose.ui.platform.LocalContext.current
                        androidx.compose.material3.FloatingActionButton(
                            onClick = {
                                com.giathinh.canlua.util.HapticUtil.tick(context)
                                showCreateDialog = true
                            },
                            containerColor = AppColors.GreenPrimary,
                            contentColor = Color.White,
                            shape = androidx.compose.foundation.shape.CircleShape,
                            modifier = Modifier
                                .offset(y = (-18).dp)
                                .size(56.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Tạo phiếu mới"
                            )
                        }
                    }
                }
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


