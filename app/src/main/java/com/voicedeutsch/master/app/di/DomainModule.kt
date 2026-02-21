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
import com.voicedeutsch.master.domain.usecase.achievement.CheckAchievementsUseCase
import com.voicedeutsch.master.domain.usecase.achievement.GetUnannouncedAchievementsUseCase
import com.voicedeutsch.master.domain.usecase.achievement.GetUserAchievementsUseCase
import com.voicedeutsch.master.domain.usecase.book.GetChapterContentUseCase
import com.voicedeutsch.master.domain.usecase.book.SearchBookContentUseCase
import com.voicedeutsch.master.domain.usecase.learning.EvaluateAnswerUseCase
import com.voicedeutsch.master.domain.usecase.learning.GetNextExerciseUseCase
import com.voicedeutsch.master.domain.usecase.session.AnalyzeSessionResultsUseCase
import com.voicedeutsch.master.domain.usecase.session.GetSessionHistoryUseCase
import com.voicedeutsch.master.domain.usecase.session.SaveSessionUseCase
import com.voicedeutsch.master.domain.usecase.speech.AnalyzePronunciationUseCase
import com.voicedeutsch.master.domain.usecase.speech.GetPronunciationTargetsUseCase
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
    // BuildKnowledgeSummaryUseCase(knowledgeRepository, sessionRepository, bookRepository, progressRepository, getWeakPointsUseCase)
    factory { BuildKnowledgeSummaryUseCase(get(), get(), get(), get(), get()) }
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
    // SaveSessionUseCase(sessionRepository, userRepository)
    factory { SaveSessionUseCase(get(), get()) }
    // GetSessionHistoryUseCase(sessionRepository)
    factory { GetSessionHistoryUseCase(get()) }
    // AnalyzeSessionResultsUseCase(sessionRepository)
    factory { AnalyzeSessionResultsUseCase(get()) }

    // ─── Learning ─────────────────────────────────────────────────────────────
    // StartLearningSessionUseCase(sessionRepository, userRepository, bookRepository)
    factory { StartLearningSessionUseCase(get(), get(), get()) }
    // EndLearningSessionUseCase(sessionRepository, userRepository, bookRepository)
    factory { EndLearningSessionUseCase(get(), get(), get()) }
    // SelectStrategyUseCase(knowledgeRepository, bookRepository, sessionRepository, userRepository, getWeakPointsUseCase)
    factory { SelectStrategyUseCase(get(), get(), get(), get(), get()) }
    // GetNextExerciseUseCase(bookRepository, knowledgeRepository)
    factory { GetNextExerciseUseCase(get(), get()) }
    // EvaluateAnswerUseCase()
    factory { EvaluateAnswerUseCase() }

    // ─── Speech ───────────────────────────────────────────────────────────────
    // RecordPronunciationResultUseCase(knowledgeRepository)
    factory { RecordPronunciationResultUseCase(get()) }
    // AnalyzePronunciationUseCase(speechRepository)
    factory { AnalyzePronunciationUseCase(get()) }
    // GetPronunciationTargetsUseCase(speechRepository, knowledgeRepository)
    factory { GetPronunciationTargetsUseCase(get(), get()) }

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
    // GetChapterContentUseCase(bookRepository)
    factory { GetChapterContentUseCase(get()) }
    // SearchBookContentUseCase(bookRepository, knowledgeRepository)
    factory { SearchBookContentUseCase(get(), get()) }

    // ─── Achievement ──────────────────────────────────────────────────────────
    // CheckAchievementsUseCase(achievementRepository, userRepository, knowledgeRepository, progressRepository)
    factory { CheckAchievementsUseCase(get(), get(), get(), get()) }
    // GetUserAchievementsUseCase(achievementRepository)
    factory { GetUserAchievementsUseCase(get()) }
    // GetUnannouncedAchievementsUseCase(achievementRepository)
    factory { GetUnannouncedAchievementsUseCase(get()) }
}