package com.voicedeutsch.master.presentation.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe navigation routes for Navigation Compose 3.x.
 *
 * Each object/class is annotated with [@Serializable] so the Navigation
 * library can serialise route arguments automatically via Kotlin Serialization.
 *
 * Architecture reference: lines 306-309 (Navigation Compose, type-safe routes).
 */
sealed interface NavRoute {

    /** First-launch onboarding — voice-driven user setup. */
    @Serializable
    data object Onboarding : NavRoute

    /**
     * Dashboard — home screen with daily progress, quick stats and
     * the "Start Session" entry point.
     */
    @Serializable
    data object Dashboard : NavRoute

    /**
     * Voice Session — the primary voice interaction screen.
     * This is the heart of the app (★★ in architecture).
     */
    @Serializable
    data object Session : NavRoute

    /** Knowledge Map — visual overview of learned words and grammar rules. */
    @Serializable
    data object KnowledgeMap : NavRoute

    /** Book — structured lesson content browser. */
    @Serializable
    data object Book : NavRoute

    /** Settings — API key, voice preferences, notification settings. */
    @Serializable
    data object Settings : NavRoute

    /** Statistics — session history, CEFR progress, streak. */
    @Serializable
    data object Statistics : NavRoute
}
