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
import com.voicedeutsch.master.domain.usecase.user.GetUserStatisticsUseCase
import com.voicedeutsch.master.domain.usecase.user.UpdateUserLevelUseCase
import org.koin.dsl.module

/**
 * Domain layer module: all Use Cases as [factory] (new instance per injection point).
 *
 * Use cases are stateless by design — factory is always the correct scope here.
 * Constructor parameters are resolved automatically by Koin from [dataModule].
 */
val domainModule = module {

    // ─── User ─────────────────────────────────────────────────────────────────
    // GetUserProfileUseCase(userRepository)
    factory { GetUserProfileUseCase(get()) }
    // ConfigureUserPreferencesUseCase(userRepository)
    factory { ConfigureUserPreferencesUseCase(get()) }
    // UpdateUserLevelUseCase(userRepository, knowledgeRepository)
    factory { UpdateUserLevelUseCase(get(), get()) }
    // GetUserStatisticsUseCase(userRepository)
    factory { GetUserStatisticsUseCase(get()) }

    // ─── Knowledge ────────────────────────────────────────────────────────────
    // UpdateWordKnowledgeUseCase(knowledgeRepository)
    factory { UpdateWordKnowledgeUseCase(get()) }
    // UpdateRuleKnowledgeUseCase(knowledgeRepository)
    factory { UpdateRuleKnowledgeUseCase(get()) }
    // UpdatePhraseKnowledgeUseCase(knowledgeRepository)
    factory { UpdatePhraseKnowledgeUseCase(get()) }
    // BuildKnowledgeSummaryUseCase(knowledgeRepository, sessionRepository, bookRepository, progressRepository)
    factory { BuildKnowledgeSummaryUseCase(get(), get(), get(), get()) }
    // GetUserKnowledgeUseCase(knowledgeRepository)
    factory { GetUserKnowledgeUseCase(get()) }
    // GetWordsForRepetitionUseCase(knowledgeRepository)
    factory { GetWordsForRepetitionUseCase(get()) }
    // GetRulesForRepetitionUseCase(knowledgeRepository)
    factory { GetRulesForRepetitionUseCase(get()) }
    // GetPhrasesForRepetitionUseCase(knowledgeRepository)
    factory { GetPhrasesForRepetitionUseCase(get()) }
    // GetWeakPointsUseCase(knowledgeRepository)
    factory { GetWeakPointsUseCase(get()) }
    // LogMistakeUseCase(knowledgeRepository)
    factory { LogMistakeUseCase(get()) }

    // ─── Session ──────────────────────────────────────────────────────────────
    // SaveSessionEventUseCase(sessionRepository)
    factory { SaveSessionEventUseCase(get()) }

    // ─── Learning ─────────────────────────────────────────────────────────────
    // StartLearningSessionUseCase(sessionRepository, userRepository, bookRepository)
    factory { StartLearningSessionUseCase(get(), get(), get()) }
    // EndLearningSessionUseCase(sessionRepository, userRepository, bookRepository)
    factory { EndLearningSessionUseCase(get(), get(), get()) }
    // SelectStrategyUseCase(knowledgeRepository, bookRepository, sessionRepository, userRepository)
    factory { SelectStrategyUseCase(get(), get(), get(), get()) }

    // ─── Speech ───────────────────────────────────────────────────────────────
    // RecordPronunciationResultUseCase(knowledgeRepository)
    factory { RecordPronunciationResultUseCase(get()) }

    // ─── Progress ─────────────────────────────────────────────────────────────
    // CalculateOverallProgressUseCase(progressRepository, userRepository)
    factory { CalculateOverallProgressUseCase(get(), get()) }
    // GetDailyProgressUseCase(sessionRepository, progressRepository)
    factory { GetDailyProgressUseCase(get(), get()) }

    // ─── Book ─────────────────────────────────────────────────────────────────
    // GetCurrentLessonUseCase(bookRepository)
    factory { GetCurrentLessonUseCase(get()) }
    // AdvanceBookProgressUseCase(bookRepository)
    factory { AdvanceBookProgressUseCase(get()) }
}