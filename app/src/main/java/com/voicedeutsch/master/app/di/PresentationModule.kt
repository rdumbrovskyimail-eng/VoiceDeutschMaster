package com.voicedeutsch.master.app.di

import org.koin.dsl.module

/** Заглушка — ViewModels будут добавлены в сессиях 7-9 */
val presentationModule = module {
    // ─── Onboarding (сессия 7) ──────────────────────────────────
    // OnboardingViewModel

    // ─── Dashboard (сессия 7) ───────────────────────────────────
    // DashboardViewModel

    // ─── Session (сессия 8) ─────────────────────────────────────
    // SessionViewModel

    // ─── Settings (сессия 9) ────────────────────────────────────
    // SettingsViewModel

    // ─── Book (сессия 9) ────────────────────────────────────────
    // BookViewModel

    // ─── Progress (сессия 9) ────────────────────────────────────
    // StatisticsViewModel
}