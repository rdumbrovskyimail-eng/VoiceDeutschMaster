package com.voicedeutsch.master.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.voicedeutsch.master.data.local.datastore.UserPreferencesDataStore
import com.voicedeutsch.master.presentation.screen.book.BookScreen
import com.voicedeutsch.master.presentation.screen.dashboard.DashboardScreen
import com.voicedeutsch.master.presentation.screen.knowledge.KnowledgeScreen
import com.voicedeutsch.master.presentation.screen.onboarding.OnboardingScreen
import com.voicedeutsch.master.presentation.screen.session.SessionScreen
import com.voicedeutsch.master.presentation.screen.settings.SettingsScreen
import com.voicedeutsch.master.presentation.screen.statistics.StatisticsScreen
import kotlinx.coroutines.flow.firstOrNull
import org.koin.compose.koinInject

// Элемент нижней навигации
private data class BottomNavItem(
    val route: NavRoute,
    val icon: ImageVector,
    val label: String,
)

private val bottomNavItems = listOf(
    BottomNavItem(NavRoute.Dashboard,     Icons.Outlined.Home,        "Главная"),
    BottomNavItem(NavRoute.KnowledgeMap,  Icons.Outlined.Map,         "Знания"),
    BottomNavItem(NavRoute.Book,          Icons.Outlined.AutoStories, "Книга"),
    BottomNavItem(NavRoute.Statistics,    Icons.Outlined.BarChart,    "Статистика"),
)

// Экраны С BottomNav
private val screensWithBottomNav = setOf(
    NavRoute.Dashboard::class,
    NavRoute.KnowledgeMap::class,
    NavRoute.Book::class,
    NavRoute.Statistics::class,
)

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val preferencesDataStore: UserPreferencesDataStore = koinInject()

    var startDestination: NavRoute by remember { mutableStateOf(NavRoute.Onboarding) }
    var isReady by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val hasCompletedOnboarding = runCatching {
            preferencesDataStore.isOnboardingComplete()
        }.getOrDefault(false)
        startDestination = if (hasCompletedOnboarding) NavRoute.Dashboard else NavRoute.Onboarding
        isReady = true
    }

    if (!isReady) return

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Показывать ли BottomNav
    val showBottomBar = screensWithBottomNav.any { routeClass ->
        currentDestination?.hasRoute(routeClass) == true
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                ) {
                    // 4 обычных пункта
                    bottomNavItems.forEach { item ->
                        val selected = currentDestination?.hasRoute(item.route::class) == true
                        NavigationBarItem(
                            icon  = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            selected = selected,
                            onClick = {
                                if (!selected) {
                                    navController.navigate(item.route) {
                                        popUpTo(NavRoute.Dashboard) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                        )
                    }

                    // Центральная кнопка Session — выделенная
                    NavigationBarItem(
                        icon = {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Filled.Mic,
                                        contentDescription = "Занятие",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp),
                                    )
                                }
                            }
                        },
                        label = { Text("Занятие") },
                        selected = false,
                        onClick = { navController.navigate(NavRoute.Session) },
                    )
                }
            }
        },
    ) { innerPadding ->

        NavHost(
            navController    = navController,
            startDestination = startDestination,
            enterTransition  = NavAnimations.enterTransition,
            exitTransition   = NavAnimations.exitTransition,
            popEnterTransition  = NavAnimations.popEnterTransition,
            popExitTransition   = NavAnimations.popExitTransition,
        ) {

            composable<NavRoute.Onboarding> {
                OnboardingScreen(
                    onOnboardingComplete = {
                        navController.navigate(NavRoute.Dashboard) {
                            popUpTo(NavRoute.Onboarding) { inclusive = true }
                        }
                    },
                )
            }

            composable<NavRoute.Dashboard> {
                DashboardScreen(
                    onStartSession          = { navController.navigate(NavRoute.Session) },
                    onNavigateToKnowledge   = { navController.navigate(NavRoute.KnowledgeMap) },
                    onNavigateToBook        = { navController.navigate(NavRoute.Book) },
                    onNavigateToSettings    = { navController.navigate(NavRoute.Settings) },
                    onNavigateToStatistics  = { navController.navigate(NavRoute.Statistics) },
                )
            }

            composable<NavRoute.Session> {
                SessionScreen(
                    onSessionEnd = { navController.popBackStack() },
                    onNavigateToDashboard = {
                        navController.navigate(NavRoute.Dashboard) {
                            popUpTo(NavRoute.Dashboard) { inclusive = true }
                        }
                    },
                    onNavigateToStatistics = {
                        navController.navigate(NavRoute.Statistics)
                    },
                )
            }

            composable<NavRoute.KnowledgeMap> {
                KnowledgeScreen(
                    onBack = { navController.popBackStack() },
                    onStartSession = { navController.navigate(NavRoute.Session) },
                )
            }

            composable<NavRoute.Book> {
                BookScreen(
                    onBack = { navController.popBackStack() },
                    onStartSession = { navController.navigate(NavRoute.Session) },
                )
            }

            composable<NavRoute.Settings> {
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                )
            }

            composable<NavRoute.Statistics> {
                StatisticsScreen(
                    onBack = { navController.popBackStack() },
                    onStartSession = { navController.navigate(NavRoute.Session) },
                )
            }
        }
    }
}