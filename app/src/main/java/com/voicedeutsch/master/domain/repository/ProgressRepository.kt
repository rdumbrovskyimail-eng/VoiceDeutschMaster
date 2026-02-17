package com.voicedeutsch.master.domain.repository

import com.voicedeutsch.master.domain.model.progress.BookOverallProgress
import com.voicedeutsch.master.domain.model.progress.DailyProgress
import com.voicedeutsch.master.domain.model.progress.GrammarProgress
import com.voicedeutsch.master.domain.model.progress.OverallProgress
import com.voicedeutsch.master.domain.model.progress.PronunciationProgress
import com.voicedeutsch.master.domain.model.progress.SkillProgress
import com.voicedeutsch.master.domain.model.progress.VocabularyProgress
import com.voicedeutsch.master.domain.model.user.CefrLevel
import kotlinx.coroutines.flow.Flow

interface ProgressRepository {

    suspend fun calculateOverallProgress(userId: String): OverallProgress

    fun getOverallProgressFlow(userId: String): Flow<OverallProgress>

    suspend fun calculateCefrLevel(userId: String): Pair<CefrLevel, Int>

    suspend fun getVocabularyProgress(userId: String): VocabularyProgress

    suspend fun getGrammarProgress(userId: String): GrammarProgress

    suspend fun getPronunciationProgress(userId: String): PronunciationProgress

    suspend fun getBookOverallProgress(userId: String): BookOverallProgress

    suspend fun getSkillProgress(userId: String): SkillProgress

    suspend fun getWeeklyProgress(userId: String): List<DailyProgress>

    suspend fun getMonthlyProgress(userId: String): List<DailyProgress>
}