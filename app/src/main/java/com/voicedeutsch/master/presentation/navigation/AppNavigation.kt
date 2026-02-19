package com.voicedeutsch.master.presentation.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
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

private const val NAV_ANIM_DURATION = 300 // ms

// ── Transition helpers ────────────────────────────────────────────────────────

private fun enterSlide(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    slideInHorizontally(
        animationSpec = tween(NAV_ANIM_DURATION),
        initialOffsetX = { it },
    ) + fadeIn(tween(NAV_ANIM_DURATION))
}

private fun exitSlide(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    slideOutHorizontally(
        animationSpec = tween(NAV_ANIM_DURATION),
        targetOffsetX = { -it / 2 },
    ) + fadeOut(tween(NAV_ANIM_DURATION))
}

private fun popEnterSlide(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    slideInHorizontally(
        animationSpec = tween(NAV_ANIM_DURATION),
        initialOffsetX = { -it / 2 },
    ) + fadeIn(tween(NAV_ANIM_DURATION))
}

private fun popExitSlide(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    slideOutHorizontally(
        animationSpec = tween(NAV_ANIM_DURATION),
        targetOffsetX = { it },
    ) + fadeOut(tween(NAV_ANIM_DURATION))
}

/**
 * Root navigation graph for the entire application.
 *
 * Start destination is determined asynchronously by checking whether the user
 * has completed onboarding (stored in [UserPreferencesDataStore]).
 *
 * Architecture reference: lines 306-309 (Navigation Compose 3.x, type-safe routes).
 */
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val preferencesDataStore: UserPreferencesDataStore = koinInject()

    // Determine start destination based on onboarding status.
    var startDestination: NavRoute by remember { mutableStateOf(NavRoute.Onboarding) }
    var isReady by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val hasCompletedOnboarding = runCatching {
            preferencesDataStore.isOnboardingComplete()
        }.getOrDefault(false)

        startDestination = if (hasCompletedOnboarding) NavRoute.Dashboard else NavRoute.Onboarding
        isReady = true
    }

    // Render NavHost only after we know the correct start destination.
    if (!isReady) return

    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = enterSlide(),
        exitTransition = exitSlide(),
        popEnterTransition = popEnterSlide(),
        popExitTransition = popExitSlide(),
    ) {

        // ── Onboarding ────────────────────────────────────────────────────────
        composable<NavRoute.Onboarding> {
            OnboardingScreen(
                onOnboardingComplete = {
                    navController.navigate(NavRoute.Dashboard) {
                        popUpTo(NavRoute.Onboarding) { inclusive = true }
                    }
                },
            )
        }

        // ── Dashboard ─────────────────────────────────────────────────────────
        composable<NavRoute.Dashboard> {
            DashboardScreen(
                onStartSession = { navController.navigate(NavRoute.Session) },
                onNavigateToKnowledge = { navController.navigate(NavRoute.KnowledgeMap) },
                onNavigateToBook = { navController.navigate(NavRoute.Book) },
                onNavigateToSettings = { navController.navigate(NavRoute.Settings) },
                onNavigateToStatistics = { navController.navigate(NavRoute.Statistics) },
            )
        }

        // ── Session (★★ main screen) ──────────────────────────────────────────
        composable<NavRoute.Session> {
            SessionScreen(
                onSessionEnd = { navController.popBackStack() },
                onNavigateToDashboard = {
                    navController.navigate(NavRoute.Dashboard) {
                        popUpTo(NavRoute.Dashboard) { inclusive = true }
                    }
                },
            )
        }

        // ── Knowledge Map ─────────────────────────────────────────────────────
        composable<NavRoute.KnowledgeMap> {
            KnowledgeScreen(
                onBack = { navController.popBackStack() },
            )
        }

        // ── Book ──────────────────────────────────────────────────────────────
        composable<NavRoute.Book> {
            BookScreen(
                onBack = { navController.popBackStack() },
            )
        }

        // ── Settings ──────────────────────────────────────────────────────────
        composable<NavRoute.Settings> {
            SettingsScreen(
                onBack = { navController.popBackStack() },
            )
        }

        // ── Statistics ────────────────────────────────────────────────────────
        composable<NavRoute.Statistics> {
            StatisticsScreen(
                onBack = { navController.popBackStack() },
            )
        }
    }
}
