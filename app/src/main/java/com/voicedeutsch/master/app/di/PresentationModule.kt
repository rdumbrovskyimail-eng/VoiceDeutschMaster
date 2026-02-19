package com.voicedeutsch.master.app.di

import com.voicedeutsch.master.presentation.screen.book.BookViewModel
import com.voicedeutsch.master.presentation.screen.dashboard.DashboardViewModel
import com.voicedeutsch.master.presentation.screen.knowledge.KnowledgeViewModel
import com.voicedeutsch.master.presentation.screen.onboarding.OnboardingViewModel
import com.voicedeutsch.master.presentation.screen.session.SessionViewModel
import com.voicedeutsch.master.presentation.screen.settings.SettingsViewModel
import com.voicedeutsch.master.presentation.screen.statistics.StatisticsViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

/**
 * Koin module for all Presentation-layer ViewModels.
 *
 * Each ViewModel is declared with `viewModel { }` so Koin creates a new instance
 * per screen destination (lifecycle-scoped by the NavBackStackEntry).
 *
 * Dependencies (Use Cases, Repositories, DataStore) are resolved automatically
 * from domainModule, dataModule and voiceCoreModule.
 *
 * Sessions implemented:
 *  - Session 7 : SessionViewModel (voice session)
 *  - Session 9 : All remaining screens
 */
val presentationModule = module {

    // ── Session (Session 8) ───────────────────────────────────────────────────
    viewModel {
        SessionViewModel(
            voiceCoreEngine      = get(),
            userRepository       = get(),
            preferencesDataStore = get(),
        )
    }

    // ── Onboarding (Session 9) ────────────────────────────────────────────────
    viewModel {
        OnboardingViewModel(
            userRepository       = get(),
            bookRepository       = get(),
            preferencesDataStore = get(),
            getUserProfile       = get(),
        )
    }

    // ── Dashboard (Session 9) ─────────────────────────────────────────────────
    viewModel {
        DashboardViewModel(
            getUserProfile           = get(),
            calculateOverallProgress = get(),
            getDailyProgress         = get(),
            userRepository           = get(),
        )
    }

    // ── Knowledge Map (Session 9) ─────────────────────────────────────────────
    viewModel {
        KnowledgeViewModel(
            getUserKnowledge = get(),
            getWeakPoints    = get(),
            userRepository   = get(),
        )
    }

    // ── Book (Session 9) ──────────────────────────────────────────────────────
    viewModel {
        BookViewModel(
            getCurrentLesson = get(),
            bookRepository   = get(),
            userRepository   = get(),
        )
    }

    // ── Settings (Session 9) ──────────────────────────────────────────────────
    viewModel {
        SettingsViewModel(
            configureUserPreferences = get(),
            preferencesDataStore     = get(),
            userRepository           = get(),
        )
    }

    // ── Statistics (Session 9) ────────────────────────────────────────────────
    viewModel {
        StatisticsViewModel(
            calculateOverallProgress = get(),
            getDailyProgress         = get(),
            progressRepository       = get(),
            userRepository           = get(),
        )
    }
}
