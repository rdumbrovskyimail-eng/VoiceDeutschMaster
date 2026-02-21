package com.voicedeutsch.master.presentation.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.NavBackStackEntry

/**
 * Centralized navigation animations for all screen transitions.
 * Architecture line 1009 (NavAnimations.kt).
 *
 * Design guideline: "Все анимации плавные (Spring-based), 200-300ms".
 */
object NavAnimations {

    private const val DURATION_MS = 250

    val enterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        slideInHorizontally(
            initialOffsetX = { fullWidth -> fullWidth },
            animationSpec = tween(DURATION_MS)
        ) + fadeIn(animationSpec = tween(DURATION_MS))
    }

    val exitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        slideOutHorizontally(
            targetOffsetX = { fullWidth -> -fullWidth / 3 },
            animationSpec = tween(DURATION_MS)
        ) + fadeOut(animationSpec = tween(DURATION_MS))
    }

    val popEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        slideInHorizontally(
            initialOffsetX = { fullWidth -> -fullWidth / 3 },
            animationSpec = tween(DURATION_MS)
        ) + fadeIn(animationSpec = tween(DURATION_MS))
    }

    val popExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        slideOutHorizontally(
            targetOffsetX = { fullWidth -> fullWidth },
            animationSpec = tween(DURATION_MS)
        ) + fadeOut(animationSpec = tween(DURATION_MS))
    }
}