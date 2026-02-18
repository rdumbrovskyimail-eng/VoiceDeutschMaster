package com.voicedeutsch.master.app.di

import com.voicedeutsch.master.domain.usecase.book.AdvanceBookProgressUseCase
import com.voicedeutsch.master.domain.usecase.book.GetCurrentLessonUseCase
import com.voicedeutsch.master.domain.usecase.knowledge.BuildKnowledgeSummaryUseCase
import com.voicedeutsch.master.domain.usecase.knowledge.GetPhrasesForRepetitionUseCase
import com.voicedeutsch.master.domain.usecase.knowledge.GetRulesForRepetitionUseCase
import com.voicedeutsch.master.domain.usecase.knowledge.GetUserKnowledgeUseCase
import com.voicedeutsch.master.domain.usecase.knowledge.GetWeakPointsUseCase
import com.voicedeutsch.master.domain.usecase.knowledge.GetWordsForRepetitionUseCase
import com.voicedeutsch.master.domain.usecase.knowledge.LogMistakeUseCase
import com.voicedeutsch.master.domain.usecase.knowledge.UpdatePhraseKnowledgeUseCase
import com.voicedeutsch.master.domain.usecase.knowledge.UpdateRuleKnowledgeUseCase
import com.voicedeutsch.master.domain.usecase.knowledge.UpdateWordKnowledgeUseCase
import com.voicedeutsch.master.domain.usecase.learning.EndLearningSessionUseCase
import com.voicedeutsch.master.domain.usecase.learning.SelectStrategyUseCase
import com.voicedeutsch.master.domain.usecase.learning.StartLearningSessionUseCase
import com.voicedeutsch.master.domain.usecase.progress.CalculateOverallProgressUseCase
import com.voicedeutsch.master.domain.usecase.progress.GetDailyProgressUseCase
import com.voicedeutsch.master.domain.usecase.session.SaveSessionEventUseCase
import com.voicedeutsch.master.domain.usecase.speech.RecordPronunciationResultUseCase
import com.voicedeutsch.master.domain.usecase.user.ConfigureUserPreferencesUseCase
import com.voicedeutsch.master.domain.usecase.user.GetUserProfileUseCase
import com.voicedeutsch.master.domain.usecase.user.UpdateUserLevelUseCase
import org.koin.dsl.module

val domainModule = module {

    // ─── User ───────────────────────────────────────────────────
    factory { GetUserProfileUseCase(get()) }
    factory { ConfigureUserPreferencesUseCase(get()) }
    factory { UpdateUserLevelUseCase(get()) }

    // ─── Knowledge ──────────────────────────────────────────────
    factory { UpdateWordKnowledgeUseCase(get()) }
    factory { UpdateRuleKnowledgeUseCase(get()) }
    factory { UpdatePhraseKnowledgeUseCase(get()) }
    factory { BuildKnowledgeSummaryUseCase(get(), get(), get(), get()) }
    factory { GetUserKnowledgeUseCase(get()) }
    factory { GetWordsForRepetitionUseCase(get()) }
    factory { GetRulesForRepetitionUseCase(get()) }
    factory { GetPhrasesForRepetitionUseCase(get()) }
    factory { GetWeakPointsUseCase(get()) }
    factory { LogMistakeUseCase(get()) }

    // ─── Session ────────────────────────────────────────────────
    factory { SaveSessionEventUseCase(get()) }

    // ─── Learning ───────────────────────────────────────────────
    factory { StartLearningSessionUseCase(get(), get()) }
    factory { EndLearningSessionUseCase(get(), get()) }
    factory { SelectStrategyUseCase(get(), get(), get()) }

    // ─── Speech ─────────────────────────────────────────────────
    factory { RecordPronunciationResultUseCase(get()) }

    // ─── Progress ───────────────────────────────────────────────
    factory { CalculateOverallProgressUseCase(get()) }
    factory { GetDailyProgressUseCase(get()) }

    // ─── Book ───────────────────────────────────────────────────
    factory { GetCurrentLessonUseCase(get()) }
    factory { AdvanceBookProgressUseCase(get()) }
}