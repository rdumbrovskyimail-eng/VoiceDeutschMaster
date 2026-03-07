// Путь: src/test/java/com/voicedeutsch/master/data/repository/AchievementRepositoryImplTest.kt
package com.voicedeutsch.master.data.repository

import com.voicedeutsch.master.data.local.database.dao.AchievementDao
import com.voicedeutsch.master.data.local.database.entity.AchievementEntity
import com.voicedeutsch.master.data.local.database.entity.UserAchievementEntity
import com.voicedeutsch.master.domain.model.achievement.Achievement
import com.voicedeutsch.master.domain.model.achievement.AchievementCategory
import com.voicedeutsch.master.domain.model.achievement.AchievementCondition
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AchievementRepositoryImplTest {

    private lateinit var achievementDao: AchievementDao
    private lateinit var json: Json
    private lateinit var repository: AchievementRepositoryImpl

    private val fixedNow  = 1_700_000_000_000L
    private val fixedUUID = "test-uuid-1"

    // ── Builders ──────────────────────────────────────────────────────────────

    private fun makeAchievementEntity(
        id: String = "ach1",
        nameRu: String = "Название",
        nameDe: String = "Name",
        descriptionRu: String = "Описание",
        icon: String = "🏆",
        conditionJson: String = """{"type":"streak_days","threshold":3}""",
        category: String = "streak"
    ): AchievementEntity = mockk<AchievementEntity>(relaxed = true).also {
        every { it.id }            returns id
        every { it.nameRu }        returns nameRu
        every { it.nameDe }        returns nameDe
        every { it.descriptionRu } returns descriptionRu
        every { it.icon }          returns icon
        every { it.conditionJson } returns conditionJson
        every { it.category }      returns category
    }

    private fun makeUserAchievementEntity(
        id: String = "ua1",
        userId: String = "user1",
        achievementId: String = "ach1",
        earnedAt: Long = 1_000L,
        announced: Boolean = false,
        createdAt: Long = 1_000L
    ): UserAchievementEntity = mockk<UserAchievementEntity>(relaxed = true).also {
        every { it.id }            returns id
        every { it.userId }        returns userId
        every { it.achievementId } returns achievementId
        every { it.earnedAt }      returns earnedAt
        every { it.announced }     returns announced
        every { it.createdAt }     returns createdAt
    }

    // ── Setup / Teardown ──────────────────────────────────────────────────────

    @BeforeEach
    fun setUp() {
        achievementDao = mockk()
        json = Json { ignoreUnknownKeys = true }
        repository = AchievementRepositoryImpl(achievementDao, json)

        mockkStatic("com.voicedeutsch.master.util.DateUtils")
        mockkStatic("com.voicedeutsch.master.util.UUIDKt")

        every { com.voicedeutsch.master.util.DateUtils.nowTimestamp() } returns fixedNow
        every { com.voicedeutsch.master.util.generateUUID() }           returns fixedUUID

        coEvery { achievementDao.getAllAchievements() }                     returns emptyList()
        coEvery { achievementDao.getUserAchievements(any()) }               returns emptyList()
        coEvery { achievementDao.hasAchievement(any(), any()) }             returns 0
        coEvery { achievementDao.insertUserAchievement(any()) }             returns Unit
        coEvery { achievementDao.getUnannounced(any()) }                    returns emptyList()
        coEvery { achievementDao.markAnnounced(any(), any()) }              returns Unit
        coEvery { achievementDao.insertAchievements(any()) }                returns Unit
        every  { achievementDao.observeUserAchievements(any()) }            returns flowOf(emptyList())
    }

    @AfterEach
    fun tearDown() = unmockkAll()

    // ── getAllAchievements ────────────────────────────────────────────────────

    @Test
    fun getAllAchievements_emptyDao_returnsEmptyList() = runTest {
        val result = repository.getAllAchievements()

        assertTrue(result.isEmpty())
    }

    @Test
    fun getAllAchievements_mapsEntitiesToDomain() = runTest {
        coEvery { achievementDao.getAllAchievements() } returns listOf(
            makeAchievementEntity(id = "ach1", nameRu = "Тест", category = "streak",
                conditionJson = """{"type":"streak_days","threshold":3}""")
        )

        val result = repository.getAllAchievements()

        assertEquals(1, result.size)
        assertEquals("ach1",   result[0].id)
        assertEquals("Тест",   result[0].nameRu)
    }

    @Test
    fun getAllAchievements_conditionJsonParsedCorrectly() = runTest {
        coEvery { achievementDao.getAllAchievements() } returns listOf(
            makeAchievementEntity(conditionJson = """{"type":"word_count","threshold":100}""")
        )

        val result = repository.getAllAchievements()

        assertEquals("word_count", result[0].condition.type)
        assertEquals(100,          result[0].condition.threshold)
    }

    @Test
    fun getAllAchievements_invalidConditionJson_fallsBackToUnknown() = runTest {
        coEvery { achievementDao.getAllAchievements() } returns listOf(
            makeAchievementEntity(conditionJson = "invalid-json")
        )

        val result = repository.getAllAchievements()

        assertEquals("unknown", result[0].condition.type)
        assertEquals(0,         result[0].condition.threshold)
    }

    @Test
    fun getAllAchievements_categoryMappedCorrectly() = runTest {
        coEvery { achievementDao.getAllAchievements() } returns listOf(
            makeAchievementEntity(category = "vocabulary",
                conditionJson = """{"type":"word_count","threshold":50}""")
        )

        val result = repository.getAllAchievements()

        assertEquals(AchievementCategory.VOCABULARY, result[0].category)
    }

    @Test
    fun getAllAchievements_categoryUppercaseConversion() = runTest {
        coEvery { achievementDao.getAllAchievements() } returns listOf(
            makeAchievementEntity(category = "GRAMMAR",
                conditionJson = """{"type":"rule_count","threshold":10}""")
        )

        val result = repository.getAllAchievements()

        assertEquals(AchievementCategory.GRAMMAR, result[0].category)
    }

    @Test
    fun getAllAchievements_invalidCategory_fallsBackToSpecial() = runTest {
        coEvery { achievementDao.getAllAchievements() } returns listOf(
            makeAchievementEntity(category = "nonexistent_category",
                conditionJson = """{"type":"streak_days","threshold":1}""")
        )

        val result = repository.getAllAchievements()

        assertEquals(AchievementCategory.SPECIAL, result[0].category)
    }

    @Test
    fun getAllAchievements_multipleEntities_allMapped() = runTest {
        coEvery { achievementDao.getAllAchievements() } returns listOf(
            makeAchievementEntity(id = "a1", conditionJson = """{"type":"streak_days","threshold":3}"""),
            makeAchievementEntity(id = "a2", conditionJson = """{"type":"word_count","threshold":100}""")
        )

        val result = repository.getAllAchievements()

        assertEquals(2, result.size)
        assertEquals("a1", result[0].id)
        assertEquals("a2", result[1].id)
    }

    @Test
    fun getAllAchievements_allFieldsMapped() = runTest {
        coEvery { achievementDao.getAllAchievements() } returns listOf(
            makeAchievementEntity(
                id = "ach1", nameRu = "Имя", nameDe = "Name",
                descriptionRu = "Описание", icon = "🏆",
                conditionJson = """{"type":"streak_days","threshold":3}""",
                category = "streak"
            )
        )

        val result = repository.getAllAchievements().single()

        assertEquals("ach1",     result.id)
        assertEquals("Имя",      result.nameRu)
        assertEquals("Name",     result.nameDe)
        assertEquals("Описание", result.descriptionRu)
        assertEquals("🏆",       result.icon)
    }

    // ── getUserAchievements ───────────────────────────────────────────────────

    @Test
    fun getUserAchievements_emptyDao_returnsEmptyList() = runTest {
        val result = repository.getUserAchievements("user1")

        assertTrue(result.isEmpty())
    }

    @Test
    fun getUserAchievements_mapsEntitiesToDomain() = runTest {
        coEvery { achievementDao.getUserAchievements("user1") } returns listOf(
            makeUserAchievementEntity(id = "ua1", userId = "user1",
                achievementId = "ach1", earnedAt = 5000L, announced = false)
        )

        val result = repository.getUserAchievements("user1")

        assertEquals(1,      result.size)
        assertEquals("ua1",  result[0].id)
        assertEquals("user1", result[0].userId)
        assertEquals("ach1", result[0].achievementId)
        assertEquals(5000L,  result[0].earnedAt)
        assertFalse(result[0].announced)
    }

    @Test
    fun getUserAchievements_announcedTrue_mappedCorrectly() = runTest {
        coEvery { achievementDao.getUserAchievements("user1") } returns listOf(
            makeUserAchievementEntity(announced = true)
        )

        val result = repository.getUserAchievements("user1")

        assertTrue(result.single().announced)
    }

    @Test
    fun getUserAchievements_repositoryCalledWithCorrectUserId() = runTest {
        repository.getUserAchievements("user42")

        coVerify(exactly = 1) { achievementDao.getUserAchievements("user42") }
    }

    // ── observeUserAchievements ───────────────────────────────────────────────

    @Test
    fun observeUserAchievements_emptyFlow_emitsEmptyList() = runTest {
        every { achievementDao.observeUserAchievements("user1") } returns flowOf(emptyList())

        val result = repository.observeUserAchievements("user1").first()

        assertTrue(result.isEmpty())
    }

    @Test
    fun observeUserAchievements_mapsEntitiesInFlow() = runTest {
        val entity = makeUserAchievementEntity(id = "ua1", achievementId = "ach1")
        every { achievementDao.observeUserAchievements("user1") } returns flowOf(listOf(entity))

        val result = repository.observeUserAchievements("user1").first()

        assertEquals(1,     result.size)
        assertEquals("ua1", result[0].id)
        assertEquals("ach1", result[0].achievementId)
    }

    @Test
    fun observeUserAchievements_repositoryCalledWithCorrectUserId() = runTest {
        repository.observeUserAchievements("user99").first()

        coVerify { achievementDao.observeUserAchievements("user99") }
    }

    // ── hasAchievement ────────────────────────────────────────────────────────

    @Test
    fun hasAchievement_daoReturnsPositive_returnsTrue() = runTest {
        coEvery { achievementDao.hasAchievement("user1", "ach1") } returns 1

        assertTrue(repository.hasAchievement("user1", "ach1"))
    }

    @Test
    fun hasAchievement_daoReturnsZero_returnsFalse() = runTest {
        coEvery { achievementDao.hasAchievement("user1", "ach1") } returns 0

        assertFalse(repository.hasAchievement("user1", "ach1"))
    }

    @Test
    fun hasAchievement_daoReturnsGreaterThan1_returnsTrue() = runTest {
        coEvery { achievementDao.hasAchievement("user1", "ach1") } returns 2

        assertTrue(repository.hasAchievement("user1", "ach1"))
    }

    @Test
    fun hasAchievement_passesCorrectArguments() = runTest {
        repository.hasAchievement("user42", "ach99")

        coVerify(exactly = 1) { achievementDao.hasAchievement("user42", "ach99") }
    }

    // ── grantAchievement ─────────────────────────────────────────────────────

    @Test
    fun grantAchievement_alreadyHas_returnsNull() = runTest {
        coEvery { achievementDao.hasAchievement("user1", "ach1") } returns 1

        val result = repository.grantAchievement("user1", "ach1")

        assertNull(result)
    }

    @Test
    fun grantAchievement_alreadyHas_insertNotCalled() = runTest {
        coEvery { achievementDao.hasAchievement("user1", "ach1") } returns 1

        repository.grantAchievement("user1", "ach1")

        coVerify(exactly = 0) { achievementDao.insertUserAchievement(any()) }
    }

    @Test
    fun grantAchievement_notYetGranted_insertsEntity() = runTest {
        coEvery { achievementDao.hasAchievement("user1", "ach1") } returns 0

        repository.grantAchievement("user1", "ach1")

        coVerify(exactly = 1) { achievementDao.insertUserAchievement(any()) }
    }

    @Test
    fun grantAchievement_notYetGranted_entityHasGeneratedUUID() = runTest {
        coEvery { achievementDao.hasAchievement("user1", "ach1") } returns 0
        val slot = slot<UserAchievementEntity>()
        coEvery { achievementDao.insertUserAchievement(capture(slot)) } returns Unit

        repository.grantAchievement("user1", "ach1")

        assertEquals(fixedUUID, slot.captured.id)
    }

    @Test
    fun grantAchievement_notYetGranted_entityHasCorrectUserAndAchievementId() = runTest {
        coEvery { achievementDao.hasAchievement("user1", "ach1") } returns 0
        val slot = slot<UserAchievementEntity>()
        coEvery { achievementDao.insertUserAchievement(capture(slot)) } returns Unit

        repository.grantAchievement("user1", "ach1")

        assertEquals("user1", slot.captured.userId)
        assertEquals("ach1",  slot.captured.achievementId)
    }

    @Test
    fun grantAchievement_notYetGranted_entityTimestampsAreNow() = runTest {
        coEvery { achievementDao.hasAchievement("user1", "ach1") } returns 0
        val slot = slot<UserAchievementEntity>()
        coEvery { achievementDao.insertUserAchievement(capture(slot)) } returns Unit

        repository.grantAchievement("user1", "ach1")

        assertEquals(fixedNow, slot.captured.earnedAt)
        assertEquals(fixedNow, slot.captured.createdAt)
    }

    @Test
    fun grantAchievement_notYetGranted_entityAnnouncedIsFalse() = runTest {
        coEvery { achievementDao.hasAchievement("user1", "ach1") } returns 0
        val slot = slot<UserAchievementEntity>()
        coEvery { achievementDao.insertUserAchievement(capture(slot)) } returns Unit

        repository.grantAchievement("user1", "ach1")

        assertFalse(slot.captured.announced)
    }

    @Test
    fun grantAchievement_notYetGranted_returnsMappedUserAchievement() = runTest {
        coEvery { achievementDao.hasAchievement("user1", "ach1") } returns 0

        val result = repository.grantAchievement("user1", "ach1")

        assertNotNull(result)
        assertEquals(fixedUUID, result?.id)
        assertEquals("user1",   result?.userId)
        assertEquals("ach1",    result?.achievementId)
        assertEquals(fixedNow,  result?.earnedAt)
        assertFalse(result?.announced ?: true)
    }

    // ── getUnannouncedAchievements ────────────────────────────────────────────

    @Test
    fun getUnannouncedAchievements_emptyDao_returnsEmptyList() = runTest {
        val result = repository.getUnannouncedAchievements("user1")

        assertTrue(result.isEmpty())
    }

    @Test
    fun getUnannouncedAchievements_mapsEntitiesCorrectly() = runTest {
        coEvery { achievementDao.getUnannounced("user1") } returns listOf(
            makeUserAchievementEntity(id = "ua1", achievementId = "ach1", announced = false)
        )

        val result = repository.getUnannouncedAchievements("user1")

        assertEquals(1,     result.size)
        assertEquals("ua1", result[0].id)
        assertFalse(result[0].announced)
    }

    @Test
    fun getUnannouncedAchievements_repositoryCalledWithCorrectUserId() = runTest {
        repository.getUnannouncedAchievements("user77")

        coVerify(exactly = 1) { achievementDao.getUnannounced("user77") }
    }

    // ── markAnnounced ─────────────────────────────────────────────────────────

    @Test
    fun markAnnounced_delegatesToDao() = runTest {
        repository.markAnnounced("user1", "ach1")

        coVerify(exactly = 1) { achievementDao.markAnnounced("user1", "ach1") }
    }

    @Test
    fun markAnnounced_passesCorrectArguments() = runTest {
        repository.markAnnounced("user42", "ach99")

        coVerify { achievementDao.markAnnounced("user42", "ach99") }
    }

    // ── seedDefaultAchievements ───────────────────────────────────────────────

    @Test
    fun seedDefaultAchievements_daoNotEmpty_insertNotCalled() = runTest {
        coEvery { achievementDao.getAllAchievements() } returns listOf(makeAchievementEntity())

        repository.seedDefaultAchievements()

        coVerify(exactly = 0) { achievementDao.insertAchievements(any()) }
    }

    @Test
    fun seedDefaultAchievements_daoEmpty_insertsCalled() = runTest {
        coEvery { achievementDao.getAllAchievements() } returns emptyList()

        repository.seedDefaultAchievements()

        coVerify(exactly = 1) { achievementDao.insertAchievements(any()) }
    }

    @Test
    fun seedDefaultAchievements_insertsAllDefaultAchievements() = runTest {
        coEvery { achievementDao.getAllAchievements() } returns emptyList()
        val slot = slot<List<AchievementEntity>>()
        coEvery { achievementDao.insertAchievements(capture(slot)) } returns Unit

        repository.seedDefaultAchievements()

        assertEquals(
            AchievementRepositoryImpl.DEFAULT_ACHIEVEMENTS.size,
            slot.captured.size
        )
    }

    @Test
    fun seedDefaultAchievements_entitiesHaveCorrectIds() = runTest {
        coEvery { achievementDao.getAllAchievements() } returns emptyList()
        val slot = slot<List<AchievementEntity>>()
        coEvery { achievementDao.insertAchievements(capture(slot)) } returns Unit

        repository.seedDefaultAchievements()

        val capturedIds = slot.captured.map { it.id }.toSet()
        val expectedIds = AchievementRepositoryImpl.DEFAULT_ACHIEVEMENTS.map { it.id }.toSet()
        assertEquals(expectedIds, capturedIds)
    }

    @Test
    fun seedDefaultAchievements_entitiesCategoriesAreLowercase() = runTest {
        coEvery { achievementDao.getAllAchievements() } returns emptyList()
        val slot = slot<List<AchievementEntity>>()
        coEvery { achievementDao.insertAchievements(capture(slot)) } returns Unit

        repository.seedDefaultAchievements()

        slot.captured.forEach { entity ->
            assertEquals(entity.category, entity.category.lowercase())
        }
    }

    @Test
    fun seedDefaultAchievements_entitiesConditionJsonIsValidJson() = runTest {
        coEvery { achievementDao.getAllAchievements() } returns emptyList()
        val slot = slot<List<AchievementEntity>>()
        coEvery { achievementDao.insertAchievements(capture(slot)) } returns Unit

        repository.seedDefaultAchievements()

        slot.captured.forEach { entity ->
            val decoded = runCatching {
                json.decodeFromString<AchievementCondition>(entity.conditionJson)
            }
            assertTrue(decoded.isSuccess, "Invalid conditionJson for ${entity.id}: ${entity.conditionJson}")
        }
    }

    // ── DEFAULT_ACHIEVEMENTS ──────────────────────────────────────────────────

    @Test
    fun defaultAchievements_containsExpectedStreakAchievements() {
        val ids = AchievementRepositoryImpl.DEFAULT_ACHIEVEMENTS.map { it.id }
        assertTrue(ids.contains("streak_3"))
        assertTrue(ids.contains("streak_7"))
        assertTrue(ids.contains("streak_30"))
        assertTrue(ids.contains("streak_100"))
    }

    @Test
    fun defaultAchievements_containsExpectedVocabularyAchievements() {
        val ids = AchievementRepositoryImpl.DEFAULT_ACHIEVEMENTS.map { it.id }
        assertTrue(ids.contains("words_50"))
        assertTrue(ids.contains("words_100"))
        assertTrue(ids.contains("words_500"))
        assertTrue(ids.contains("words_1000"))
        assertTrue(ids.contains("words_3000"))
    }

    @Test
    fun defaultAchievements_containsExpectedGrammarAchievements() {
        val ids = AchievementRepositoryImpl.DEFAULT_ACHIEVEMENTS.map { it.id }
        assertTrue(ids.contains("rules_1"))
        assertTrue(ids.contains("rules_10"))
        assertTrue(ids.contains("rules_25"))
    }

    @Test
    fun defaultAchievements_containsExpectedPronunciationAchievements() {
        val ids = AchievementRepositoryImpl.DEFAULT_ACHIEVEMENTS.map { it.id }
        assertTrue(ids.contains("pron_perfect_1"))
        assertTrue(ids.contains("pron_perfect_10"))
        assertTrue(ids.contains("pron_avg_80"))
    }

    @Test
    fun defaultAchievements_containsExpectedCefrAchievements() {
        val ids = AchievementRepositoryImpl.DEFAULT_ACHIEVEMENTS.map { it.id }
        assertTrue(ids.contains("cefr_a1"))
        assertTrue(ids.contains("cefr_a2"))
        assertTrue(ids.contains("cefr_b1"))
    }

    @Test
    fun defaultAchievements_allIdsAreUnique() {
        val ids = AchievementRepositoryImpl.DEFAULT_ACHIEVEMENTS.map { it.id }
        assertEquals(ids.size, ids.distinct().size)
    }

    @Test
    fun defaultAchievements_allHaveNonBlankNameRu() {
        AchievementRepositoryImpl.DEFAULT_ACHIEVEMENTS.forEach { a ->
            assertTrue(a.nameRu.isNotBlank(), "nameRu blank for ${a.id}")
        }
    }

    @Test
    fun defaultAchievements_allHaveNonBlankNameDe() {
        AchievementRepositoryImpl.DEFAULT_ACHIEVEMENTS.forEach { a ->
            assertTrue(a.nameDe.isNotBlank(), "nameDe blank for ${a.id}")
        }
    }

    @Test
    fun defaultAchievements_allThresholdsPositive() {
        AchievementRepositoryImpl.DEFAULT_ACHIEVEMENTS.forEach { a ->
            assertTrue(a.condition.threshold > 0, "threshold <= 0 for ${a.id}")
        }
    }

    @Test
    fun defaultAchievements_streakCategoryAssignedCorrectly() {
        AchievementRepositoryImpl.DEFAULT_ACHIEVEMENTS
            .filter { it.id.startsWith("streak_") }
            .forEach { a ->
                assertEquals(AchievementCategory.STREAK, a.category, "Wrong category for ${a.id}")
            }
    }

    @Test
    fun defaultAchievements_vocabularyCategoryAssignedCorrectly() {
        AchievementRepositoryImpl.DEFAULT_ACHIEVEMENTS
            .filter { it.id.startsWith("words_") }
            .forEach { a ->
                assertEquals(AchievementCategory.VOCABULARY, a.category, "Wrong category for ${a.id}")
            }
    }

    @Test
    fun defaultAchievements_grammarCategoryAssignedCorrectly() {
        AchievementRepositoryImpl.DEFAULT_ACHIEVEMENTS
            .filter { it.id.startsWith("rules_") }
            .forEach { a ->
                assertEquals(AchievementCategory.GRAMMAR, a.category, "Wrong category for ${a.id}")
            }
    }

    @Test
    fun defaultAchievements_pronunciationCategoryAssignedCorrectly() {
        AchievementRepositoryImpl.DEFAULT_ACHIEVEMENTS
            .filter { it.id.startsWith("pron_") }
            .forEach { a ->
                assertEquals(AchievementCategory.PRONUNCIATION, a.category, "Wrong category for ${a.id}")
            }
    }

    @Test
    fun defaultAchievements_timeCategoryAssignedCorrectly() {
        AchievementRepositoryImpl.DEFAULT_ACHIEVEMENTS
            .filter { it.id.startsWith("time_") }
            .forEach { a ->
                assertEquals(AchievementCategory.TIME, a.category, "Wrong category for ${a.id}")
            }
    }

    @Test
    fun defaultAchievements_bookCategoryAssignedCorrectly() {
        AchievementRepositoryImpl.DEFAULT_ACHIEVEMENTS
            .filter { it.id.startsWith("book_") }
            .forEach { a ->
                assertEquals(AchievementCategory.BOOK, a.category, "Wrong category for ${a.id}")
            }
    }

    @Test
    fun defaultAchievements_cefrCategoryAssignedCorrectly() {
        AchievementRepositoryImpl.DEFAULT_ACHIEVEMENTS
            .filter { it.id.startsWith("cefr_") }
            .forEach { a ->
                assertEquals(AchievementCategory.CEFR, a.category, "Wrong category for ${a.id}")
            }
    }

    @Test
    fun defaultAchievements_streakThresholdsInAscendingOrder() {
        val streakThresholds = AchievementRepositoryImpl.DEFAULT_ACHIEVEMENTS
            .filter { it.condition.type == "streak_days" }
            .map { it.condition.threshold }
        assertEquals(streakThresholds.sorted(), streakThresholds)
    }

    @Test
    fun defaultAchievements_wordCountThresholdsInAscendingOrder() {
        val wordThresholds = AchievementRepositoryImpl.DEFAULT_ACHIEVEMENTS
            .filter { it.condition.type == "word_count" }
            .map { it.condition.threshold }
        assertEquals(wordThresholds.sorted(), wordThresholds)
    }

    @Test
    fun defaultAchievements_totalCount_isExpected() {
        assertEquals(23, AchievementRepositoryImpl.DEFAULT_ACHIEVEMENTS.size)
    }
}
