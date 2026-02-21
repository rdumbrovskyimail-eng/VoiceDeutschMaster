package com.voicedeutsch.master.data.repository

import com.voicedeutsch.master.data.local.database.dao.AchievementDao
import com.voicedeutsch.master.data.local.database.entity.AchievementEntity
import com.voicedeutsch.master.data.local.database.entity.UserAchievementEntity
import com.voicedeutsch.master.data.mapper.AchievementMapper
import com.voicedeutsch.master.domain.model.achievement.Achievement
import com.voicedeutsch.master.domain.model.achievement.AchievementCategory
import com.voicedeutsch.master.domain.model.achievement.AchievementCondition
import com.voicedeutsch.master.domain.model.achievement.UserAchievement
import com.voicedeutsch.master.domain.repository.AchievementRepository
import com.voicedeutsch.master.util.DateUtils
import com.voicedeutsch.master.util.generateUUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

class AchievementRepositoryImpl(
    private val achievementDao: AchievementDao,
    private val json: Json
) : AchievementRepository {

    override suspend fun getAllAchievements(): List<Achievement> {
        return achievementDao.getAllAchievements().map { it.toDomain() }
    }

    override suspend fun getUserAchievements(userId: String): List<UserAchievement> {
        return achievementDao.getUserAchievements(userId).map { it.toDomain() }
    }

    override fun observeUserAchievements(userId: String): Flow<List<UserAchievement>> {
        return achievementDao.observeUserAchievements(userId)
            .map { list -> list.map { it.toDomain() } }
    }

    override suspend fun hasAchievement(userId: String, achievementId: String): Boolean {
        return achievementDao.hasAchievement(userId, achievementId) > 0
    }

    override suspend fun grantAchievement(userId: String, achievementId: String): UserAchievement? {
        if (hasAchievement(userId, achievementId)) return null
        val now = DateUtils.nowTimestamp()
        val entity = UserAchievementEntity(
            id = generateUUID(),
            userId = userId,
            achievementId = achievementId,
            earnedAt = now,
            announced = false,
            createdAt = now
        )
        achievementDao.insertUserAchievement(entity)
        return entity.toDomain()
    }

    override suspend fun getUnannouncedAchievements(userId: String): List<UserAchievement> {
        return achievementDao.getUnannounced(userId).map { it.toDomain() }
    }

    override suspend fun markAnnounced(userId: String, achievementId: String) {
        achievementDao.markAnnounced(userId, achievementId)
    }

    override suspend fun seedDefaultAchievements() {
        if (achievementDao.getAllAchievements().isNotEmpty()) return
        achievementDao.insertAchievements(DEFAULT_ACHIEVEMENTS.map { it.toEntity() })
    }

    // ── Mappers ───────────────────────────────────────────────────────────────

    private fun AchievementEntity.toDomain() = Achievement(
        id = id,
        nameRu = nameRu,
        nameDe = nameDe,
        descriptionRu = descriptionRu,
        icon = icon,
        condition = runCatching {
            json.decodeFromString<AchievementCondition>(conditionJson)
        }.getOrDefault(AchievementCondition("unknown", 0)),
        category = runCatching {
            AchievementCategory.valueOf(category.uppercase())
        }.getOrDefault(AchievementCategory.SPECIAL)
    )

    private fun UserAchievementEntity.toDomain() = UserAchievement(
        id = id,
        userId = userId,
        achievementId = achievementId,
        earnedAt = earnedAt,
        announced = announced
    )

    private fun Achievement.toEntity() = AchievementEntity(
        id = id,
        nameRu = nameRu,
        nameDe = nameDe,
        descriptionRu = descriptionRu,
        icon = icon,
        conditionJson = json.encodeToString(AchievementCondition.serializer(), condition),
        category = category.name.lowercase()
    )

    companion object {
        val DEFAULT_ACHIEVEMENTS = listOf(
            Achievement("streak_3", "3 дня подряд", "3 Tage in Folge", "Занимайся 3 дня подряд", "🔥", AchievementCondition("streak_days", 3), AchievementCategory.STREAK),
            Achievement("streak_7", "Неделя без перерыва", "Eine Woche ohne Pause", "Занимайся 7 дней подряд", "🔥🔥", AchievementCondition("streak_days", 7), AchievementCategory.STREAK),
            Achievement("streak_30", "Месяц упорства", "Ein Monat Ausdauer", "Занимайся 30 дней подряд", "🔥🔥🔥", AchievementCondition("streak_days", 30), AchievementCategory.STREAK),
            Achievement("streak_100", "Сотня!", "Hundert!", "100 дней подряд", "🌟", AchievementCondition("streak_days", 100), AchievementCategory.STREAK),
            Achievement("words_50", "Первые 50 слов", "Erste 50 Wörter", "Выучи 50 слов", "📚", AchievementCondition("word_count", 50), AchievementCategory.VOCABULARY),
            Achievement("words_100", "Сотня слов", "Hundert Wörter", "Выучи 100 слов", "📖", AchievementCondition("word_count", 100), AchievementCategory.VOCABULARY),
            Achievement("words_500", "Полтысячи", "Fünfhundert", "Выучи 500 слов", "🎯", AchievementCondition("word_count", 500), AchievementCategory.VOCABULARY),
            Achievement("words_1000", "Тысячник", "Tausend", "Выучи 1000 слов", "🏆", AchievementCondition("word_count", 1000), AchievementCategory.VOCABULARY),
            Achievement("words_3000", "Мастер лексики", "Wortschatzmeister", "Выучи 3000 слов", "👑", AchievementCondition("word_count", 3000), AchievementCategory.VOCABULARY),
            Achievement("rules_1", "Первое правило", "Erste Regel", "Изучи первое грамматическое правило", "📐", AchievementCondition("rule_count", 1), AchievementCategory.GRAMMAR),
            Achievement("rules_10", "Грамотей", "Grammatiker", "Изучи 10 правил", "🔧", AchievementCondition("rule_count", 10), AchievementCategory.GRAMMAR),
            Achievement("rules_25", "Знаток грамматики", "Grammatikkenner", "Изучи 25 правил", "⚙️", AchievementCondition("rule_count", 25), AchievementCategory.GRAMMAR),
            Achievement("pron_perfect_1", "Идеальное слово", "Perfektes Wort", "Произнеси слово на оценку >0.9", "🎤", AchievementCondition("perfect_pronunciation", 1), AchievementCategory.PRONUNCIATION),
            Achievement("pron_perfect_10", "10 идеальных", "10 Perfekte", "10 слов с оценкой >0.9", "🎵", AchievementCondition("perfect_pronunciation", 10), AchievementCategory.PRONUNCIATION),
            Achievement("pron_avg_80", "Чистая речь", "Klare Sprache", "Средняя оценка произношения >0.8", "🏅", AchievementCondition("avg_pronunciation", 80), AchievementCategory.PRONUNCIATION),
            Achievement("book_ch1", "Первая глава", "Erstes Kapitel", "Заверши первую главу книги", "📘", AchievementCondition("chapters_completed", 1), AchievementCategory.BOOK),
            Achievement("book_ch5", "Пять глав", "Fünf Kapitel", "Заверши 5 глав", "📗", AchievementCondition("chapters_completed", 5), AchievementCategory.BOOK),
            Achievement("time_1h", "Первый час", "Erste Stunde", "1 час общего обучения", "⏱", AchievementCondition("total_minutes", 60), AchievementCategory.TIME),
            Achievement("time_10h", "Десять часов", "Zehn Stunden", "10 часов обучения", "⏱", AchievementCondition("total_minutes", 600), AchievementCategory.TIME),
            Achievement("time_50h", "Полсотни часов", "Fünfzig Stunden", "50 часов обучения", "⏱", AchievementCondition("total_minutes", 3000), AchievementCategory.TIME),
            Achievement("cefr_a1", "Уровень A1", "Niveau A1", "Достигни уровня A1", "🏳️", AchievementCondition("cefr_level", 1), AchievementCategory.CEFR),
            Achievement("cefr_a2", "Уровень A2", "Niveau A2", "Достигни уровня A2", "🏴", AchievementCondition("cefr_level", 2), AchievementCategory.CEFR),
            Achievement("cefr_b1", "Уровень B1", "Niveau B1", "Достигни уровня B1", "🏁", AchievementCondition("cefr_level", 3), AchievementCategory.CEFR),
        )
    }
}