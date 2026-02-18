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
    factory { GetUserProfileUseCase(get()) }                         // (UserRepository)
    factory { ConfigureUserPreferencesUseCase(get()) }               // (UserRepository)
    factory { UpdateUserLevelUseCase(get(), get()) }                 // (UserRepository, KnowledgeRepository)

    // ─── Knowledge ──────────────────────────────────────────────
    factory { UpdateWordKnowledgeUseCase(get()) }                    // (KnowledgeRepository)
    factory { UpdateRuleKnowledgeUseCase(get()) }                    // (KnowledgeRepository)
    factory { UpdatePhraseKnowledgeUseCase(get()) }                  // (KnowledgeRepository)
    factory { BuildKnowledgeSummaryUseCase(get(), get(), get(), get()) } // (KnowledgeRepo, SessionRepo, BookRepo, ProgressRepo)
    factory { GetUserKnowledgeUseCase(get()) }                       // (KnowledgeRepository)
    factory { GetWordsForRepetitionUseCase(get()) }                  // (KnowledgeRepository)
    factory { GetRulesForRepetitionUseCase(get()) }                  // (KnowledgeRepository)
    factory { GetPhrasesForRepetitionUseCase(get()) }                // (KnowledgeRepository)
    factory { GetWeakPointsUseCase(get()) }                          // (KnowledgeRepository)
    factory { LogMistakeUseCase(get()) }                             // (KnowledgeRepository)

    // ─── Session ────────────────────────────────────────────────
    factory { SaveSessionEventUseCase(get()) }                       // (SessionRepository)

    // ─── Learning ───────────────────────────────────────────────
    factory { StartLearningSessionUseCase(get(), get(), get()) }     // (SessionRepo, UserRepo, BookRepo)
    factory { EndLearningSessionUseCase(get(), get(), get()) }       // (SessionRepo, UserRepo, BookRepo)
    factory { SelectStrategyUseCase(get(), get(), get(), get()) }    // (KnowledgeRepo, BookRepo, SessionRepo, UserRepo)

    // ─── Speech ─────────────────────────────────────────────────
    factory { RecordPronunciationResultUseCase(get()) }              // (KnowledgeRepository)

    // ─── Progress ───────────────────────────────────────────────
    factory { CalculateOverallProgressUseCase(get(), get()) }        // (ProgressRepository, UserRepository)
    factory { GetDailyProgressUseCase(get(), get()) }                // (SessionRepository, ProgressRepository)

    // ─── Book ───────────────────────────────────────────────────
    factory { GetCurrentLessonUseCase(get()) }                       // (BookRepository)
    factory { AdvanceBookProgressUseCase(get()) }                    // (BookRepository)
}