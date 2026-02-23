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

val presentationModule = module {
    viewModel {
        SessionViewModel(
            voiceCoreEngine      = get(),
            userRepository       = get(),
            preferencesDataStore = get(),
            context              = get(),
        )
    }
    viewModel {
        OnboardingViewModel(
            userRepository       = get(),
            bookRepository       = get(),
            preferencesDataStore = get(),
            getUserProfile       = get(),
        )
    }
    viewModel {
        DashboardViewModel(
            getUserProfile           = get(),
            calculateOverallProgress = get(),
            getDailyProgress         = get(),
            userRepository           = get(),
        )
    }
    viewModel {
        KnowledgeViewModel(
            getUserKnowledge = get(),
            getWeakPoints    = get(),
            userRepository   = get(),
        )
    }
    viewModel {
        BookViewModel(
            getCurrentLesson = get(),
            bookRepository   = get(),
            userRepository   = get(),
        )
    }
    viewModel {
        SettingsViewModel(
            configureUserPreferences = get(),
            preferencesDataStore     = get(),
            userRepository           = get(),
        )
    }
    viewModel {
        StatisticsViewModel(
            calculateOverallProgress = get(),
            getDailyProgress         = get(),
            progressRepository       = get(),
            userRepository           = get(),
        )
    }
}