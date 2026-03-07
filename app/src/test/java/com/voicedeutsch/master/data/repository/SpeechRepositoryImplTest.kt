// Путь: src/test/java/com/voicedeutsch/master/data/repository/SpeechRepositoryImplTest.kt
package com.voicedeutsch.master.data.repository

import com.voicedeutsch.master.data.local.database.dao.KnowledgeDao
import com.voicedeutsch.master.data.local.database.entity.PronunciationRecordEntity
import com.voicedeutsch.master.domain.model.speech.PronunciationResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SpeechRepositoryImplTest {

    private lateinit var knowledgeDao: KnowledgeDao
    private lateinit var json: Json
    private lateinit var repository: SpeechRepositoryImpl

    private val fixedUUID   = "generated-uuid-1"
    private val fixedMillis = 1_700_000_000_000L

    // ── Builders ──────────────────────────────────────────────────────────────

    private fun makePronunciationResult(
        id: String = "pr1",
        userId: String = "user1",
        word: String = "Hund",
        score: Float = 0.8f,
        problemSounds: List<String> = emptyList(),
        attemptNumber: Int = 1,
        sessionId: String? = "session1",
        timestamp: Long = 1_000L
    ) = PronunciationResult(
        id            = id,
        userId        = userId,
        word          = word,
        score         = score,
        problemSounds = problemSounds,
        attemptNumber = attemptNumber,
        sessionId     = sessionId,
        timestamp     = timestamp
    )

    private fun makeRecordEntity(
        id: String = "pr1",
        userId: String = "user1",
        word: String = "Hund",
        score: Float = 0.8f,
        problemSoundsJson: String = "[]",
        attemptNumber: Int = 1,
        sessionId: String = "session1",
        timestamp: Long = 1_000L,
        createdAt: Long = fixedMillis
    ): PronunciationRecordEntity = mockk<PronunciationRecordEntity>(relaxed = true).also {
        every { it.id }               returns id
        every { it.userId }           returns userId
        every { it.word }             returns word
        every { it.score }            returns score
        every { it.problemSoundsJson } returns problemSoundsJson
        every { it.attemptNumber }    returns attemptNumber
        every { it.sessionId }        returns sessionId
        every { it.timestamp }        returns timestamp
        every { it.createdAt }        returns createdAt
    }

    // ── Setup / Teardown ──────────────────────────────────────────────────────

    @BeforeEach
    fun setUp() {
        knowledgeDao = mockk()
        json         = Json { ignoreUnknownKeys = true }
        repository   = SpeechRepositoryImpl(knowledgeDao, json)

        mockkStatic("com.voicedeutsch.master.util.UUIDKt")
        mockkStatic(System::class)

        every { com.voicedeutsch.master.util.generateUUID() } returns fixedUUID
        every { System.currentTimeMillis() }                  returns fixedMillis

        coEvery { knowledgeDao.insertPronunciationRecord(any()) }           returns Unit
        coEvery { knowledgeDao.getPronunciationRecords(any(), any()) }      returns emptyList()
        coEvery { knowledgeDao.getRecentPronunciationRecords(any(), any()) } returns emptyList()
        coEvery { knowledgeDao.getAveragePronunciationScore(any()) }        returns null
        every  { knowledgeDao.observeAveragePronunciationScore(any()) }     returns flowOf(null)
    }

    @AfterEach
    fun tearDown() = unmockkAll()

    // ── savePronunciationResult ───────────────────────────────────────────────

    @Test
    fun savePronunciationResult_insertsEntityIntoDao() = runTest {
        repository.savePronunciationResult(makePronunciationResult())

        coVerify(exactly = 1) { knowledgeDao.insertPronunciationRecord(any()) }
    }

    @Test
    fun savePronunciationResult_nonEmptyId_preservesId() = runTest {
        val slot = slot<PronunciationRecordEntity>()
        coEvery { knowledgeDao.insertPronunciationRecord(capture(slot)) } returns Unit

        repository.savePronunciationResult(makePronunciationResult(id = "existing-id"))

        assertEquals("existing-id", slot.captured.id)
    }

    @Test
    fun savePronunciationResult_emptyId_usesGeneratedUUID() = runTest {
        val slot = slot<PronunciationRecordEntity>()
        coEvery { knowledgeDao.insertPronunciationRecord(capture(slot)) } returns Unit

        repository.savePronunciationResult(makePronunciationResult(id = ""))

        assertEquals(fixedUUID, slot.captured.id)
    }

    @Test
    fun savePronunciationResult_allFieldsMappedCorrectly() = runTest {
        val slot = slot<PronunciationRecordEntity>()
        coEvery { knowledgeDao.insertPronunciationRecord(capture(slot)) } returns Unit

        repository.savePronunciationResult(
            makePronunciationResult(
                id            = "pr99",
                userId        = "user42",
                word          = "Katze",
                score         = 0.75f,
                problemSounds = listOf("tz", "e"),
                attemptNumber = 3,
                sessionId     = "sess-1",
                timestamp     = 9_000L
            )
        )

        assertEquals("pr99",   slot.captured.id)
        assertEquals("user42", slot.captured.userId)
        assertEquals("Katze",  slot.captured.word)
        assertEquals(0.75f,    slot.captured.score, 0.001f)
        assertEquals(3,        slot.captured.attemptNumber)
        assertEquals("sess-1", slot.captured.sessionId)
        assertEquals(9_000L,   slot.captured.timestamp)
        assertEquals(fixedMillis, slot.captured.createdAt)
    }

    @Test
    fun savePronunciationResult_problemSoundsEncodedToJson() = runTest {
        val slot = slot<PronunciationRecordEntity>()
        coEvery { knowledgeDao.insertPronunciationRecord(capture(slot)) } returns Unit

        repository.savePronunciationResult(
            makePronunciationResult(problemSounds = listOf("ü", "ch"))
        )

        val decoded = json.decodeFromString<List<String>>(slot.captured.problemSoundsJson)
        assertEquals(listOf("ü", "ch"), decoded)
    }

    @Test
    fun savePronunciationResult_emptyProblemSounds_encodedAsEmptyArray() = runTest {
        val slot = slot<PronunciationRecordEntity>()
        coEvery { knowledgeDao.insertPronunciationRecord(capture(slot)) } returns Unit

        repository.savePronunciationResult(makePronunciationResult(problemSounds = emptyList()))

        assertEquals("[]", slot.captured.problemSoundsJson)
    }

    @Test
    fun savePronunciationResult_nullSessionId_savedAsEmptyString() = runTest {
        val slot = slot<PronunciationRecordEntity>()
        coEvery { knowledgeDao.insertPronunciationRecord(capture(slot)) } returns Unit

        repository.savePronunciationResult(makePronunciationResult(sessionId = null))

        assertEquals("", slot.captured.sessionId)
    }

    @Test
    fun savePronunciationResult_nonNullSessionId_preservedCorrectly() = runTest {
        val slot = slot<PronunciationRecordEntity>()
        coEvery { knowledgeDao.insertPronunciationRecord(capture(slot)) } returns Unit

        repository.savePronunciationResult(makePronunciationResult(sessionId = "sess-42"))

        assertEquals("sess-42", slot.captured.sessionId)
    }

    @Test
    fun savePronunciationResult_createdAtIsSystemCurrentTimeMillis() = runTest {
        val slot = slot<PronunciationRecordEntity>()
        coEvery { knowledgeDao.insertPronunciationRecord(capture(slot)) } returns Unit

        repository.savePronunciationResult(makePronunciationResult())

        assertEquals(fixedMillis, slot.captured.createdAt)
    }

    // ── getPronunciationHistory ───────────────────────────────────────────────

    @Test
    fun getPronunciationHistory_mapsAllEntities() = runTest {
        coEvery { knowledgeDao.getPronunciationRecords("user1", "Hund") } returns listOf(
            makeRecordEntity("r1"), makeRecordEntity("r2")
        )

        val result = repository.getPronunciationHistory("user1", "Hund")

        assertEquals(2, result.size)
    }

    @Test
    fun getPronunciationHistory_emptyDao_returnsEmptyList() = runTest {
        val result = repository.getPronunciationHistory("user1", "Hund")

        assertTrue(result.isEmpty())
    }

    @Test
    fun getPronunciationHistory_passesCorrectArguments() = runTest {
        repository.getPronunciationHistory("user99", "Bach")

        coVerify { knowledgeDao.getPronunciationRecords("user99", "Bach") }
    }

    @Test
    fun getPronunciationHistory_resultFieldsMappedCorrectly() = runTest {
        coEvery { knowledgeDao.getPronunciationRecords("user1", "Hund") } returns listOf(
            makeRecordEntity(
                id            = "pr1",
                userId        = "user1",
                word          = "Hund",
                score         = 0.9f,
                problemSoundsJson = """["u","nd"]""",
                attemptNumber = 2,
                sessionId     = "s1",
                timestamp     = 5000L
            )
        )

        val result = repository.getPronunciationHistory("user1", "Hund").single()

        assertEquals("pr1",          result.id)
        assertEquals("user1",        result.userId)
        assertEquals("Hund",         result.word)
        assertEquals(0.9f,           result.score, 0.001f)
        assertEquals(listOf("u","nd"), result.problemSounds)
        assertEquals(2,              result.attemptNumber)
        assertEquals("s1",           result.sessionId)
        assertEquals(5000L,          result.timestamp)
    }

    @Test
    fun getPronunciationHistory_invalidProblemSoundsJson_fallsBackToEmptyList() = runTest {
        coEvery { knowledgeDao.getPronunciationRecords("user1", "Hund") } returns listOf(
            makeRecordEntity(problemSoundsJson = "not-valid-json")
        )

        val result = repository.getPronunciationHistory("user1", "Hund").single()

        assertTrue(result.problemSounds.isEmpty())
    }

    @Test
    fun getPronunciationHistory_emptySessionId_mappedToNull() = runTest {
        coEvery { knowledgeDao.getPronunciationRecords("user1", "Hund") } returns listOf(
            makeRecordEntity(sessionId = "")
        )

        val result = repository.getPronunciationHistory("user1", "Hund").single()

        assertNull(result.sessionId)
    }

    @Test
    fun getPronunciationHistory_nonEmptySessionId_preserved() = runTest {
        coEvery { knowledgeDao.getPronunciationRecords("user1", "Hund") } returns listOf(
            makeRecordEntity(sessionId = "sess-99")
        )

        val result = repository.getPronunciationHistory("user1", "Hund").single()

        assertEquals("sess-99", result.sessionId)
    }

    // ── getRecentPronunciationResults ─────────────────────────────────────────

    @Test
    fun getRecentPronunciationResults_mapsAllEntities() = runTest {
        coEvery { knowledgeDao.getRecentPronunciationRecords("user1", 10) } returns
            listOf(makeRecordEntity(), makeRecordEntity(), makeRecordEntity())

        val result = repository.getRecentPronunciationResults("user1", 10)

        assertEquals(3, result.size)
    }

    @Test
    fun getRecentPronunciationResults_emptyDao_returnsEmptyList() = runTest {
        val result = repository.getRecentPronunciationResults("user1", 10)

        assertTrue(result.isEmpty())
    }

    @Test
    fun getRecentPronunciationResults_passesLimitToDao() = runTest {
        repository.getRecentPronunciationResults("user1", 50)

        coVerify { knowledgeDao.getRecentPronunciationRecords("user1", 50) }
    }

    // ── getAveragePronunciationScore ──────────────────────────────────────────

    @Test
    fun getAveragePronunciationScore_daoReturnsValue_returnsIt() = runTest {
        coEvery { knowledgeDao.getAveragePronunciationScore("user1") } returns 0.73f

        assertEquals(0.73f, repository.getAveragePronunciationScore("user1"), 0.001f)
    }

    @Test
    fun getAveragePronunciationScore_daoReturnsNull_returns0() = runTest {
        coEvery { knowledgeDao.getAveragePronunciationScore("user1") } returns null

        assertEquals(0f, repository.getAveragePronunciationScore("user1"))
    }

    @Test
    fun getAveragePronunciationScore_passesCorrectUserId() = runTest {
        repository.getAveragePronunciationScore("user77")

        coVerify { knowledgeDao.getAveragePronunciationScore("user77") }
    }

    // ── getProblematicSounds ──────────────────────────────────────────────────

    @Test
    fun getProblematicSounds_noRecords_returnsEmptyList() = runTest {
        val result = repository.getProblematicSounds("user1")

        assertTrue(result.isEmpty())
    }

    @Test
    fun getProblematicSounds_singleSoundInMultipleRecords_returnsThatSound() = runTest {
        coEvery { knowledgeDao.getRecentPronunciationRecords("user1", 100) } returns listOf(
            makeRecordEntity(problemSoundsJson = """["ü"]"""),
            makeRecordEntity(problemSoundsJson = """["ü"]"""),
            makeRecordEntity(problemSoundsJson = """["ü"]""")
        )

        val result = repository.getProblematicSounds("user1")

        assertEquals(listOf("ü"), result)
    }

    @Test
    fun getProblematicSounds_sortedByCountDescending() = runTest {
        coEvery { knowledgeDao.getRecentPronunciationRecords("user1", 100) } returns listOf(
            makeRecordEntity(problemSoundsJson = """["ü","ch"]"""),
            makeRecordEntity(problemSoundsJson = """["ü"]"""),
            makeRecordEntity(problemSoundsJson = """["ch","z"]"""),
            makeRecordEntity(problemSoundsJson = """["ch"]""")
        )
        // ü: 2, ch: 3, z: 1 → ch, ü, z

        val result = repository.getProblematicSounds("user1")

        assertEquals("ch", result[0])
        assertEquals("ü",  result[1])
        assertEquals("z",  result[2])
    }

    @Test
    fun getProblematicSounds_cappedAt10() = runTest {
        val sounds = (1..15).map { "sound$it" }
        val jsonSounds = json.encodeToString(sounds)
        coEvery { knowledgeDao.getRecentPronunciationRecords("user1", 100) } returns
            listOf(makeRecordEntity(problemSoundsJson = jsonSounds))

        val result = repository.getProblematicSounds("user1")

        assertEquals(10, result.size)
    }

    @Test
    fun getProblematicSounds_invalidJson_skipsRecord() = runTest {
        coEvery { knowledgeDao.getRecentPronunciationRecords("user1", 100) } returns listOf(
            makeRecordEntity(problemSoundsJson = "invalid-json"),
            makeRecordEntity(problemSoundsJson = """["ü"]""")
        )

        val result = repository.getProblematicSounds("user1")

        assertEquals(listOf("ü"), result)
    }

    @Test
    fun getProblematicSounds_allInvalidJson_returnsEmptyList() = runTest {
        coEvery { knowledgeDao.getRecentPronunciationRecords("user1", 100) } returns listOf(
            makeRecordEntity(problemSoundsJson = "bad"),
            makeRecordEntity(problemSoundsJson = "also-bad")
        )

        val result = repository.getProblematicSounds("user1")

        assertTrue(result.isEmpty())
    }

    @Test
    fun getProblematicSounds_queriesDaoWith100Limit() = runTest {
        repository.getProblematicSounds("user1")

        coVerify { knowledgeDao.getRecentPronunciationRecords("user1", 100) }
    }

    @Test
    fun getProblematicSounds_emptySoundsArrayInRecord_skippedGracefully() = runTest {
        coEvery { knowledgeDao.getRecentPronunciationRecords("user1", 100) } returns listOf(
            makeRecordEntity(problemSoundsJson = "[]")
        )

        val result = repository.getProblematicSounds("user1")

        assertTrue(result.isEmpty())
    }

    // ── observePronunciationScore ─────────────────────────────────────────────

    @Test
    fun observePronunciationScore_daoEmitsValue_emittedDirectly() = runTest {
        every { knowledgeDao.observeAveragePronunciationScore("user1") } returns flowOf(0.65f)

        val result = repository.observePronunciationScore("user1").first()

        assertEquals(0.65f, result, 0.001f)
    }

    @Test
    fun observePronunciationScore_daoEmitsNull_emits0() = runTest {
        every { knowledgeDao.observeAveragePronunciationScore("user1") } returns flowOf(null)

        val result = repository.observePronunciationScore("user1").first()

        assertEquals(0f, result)
    }

    @Test
    fun observePronunciationScore_passesCorrectUserId() = runTest {
        repository.observePronunciationScore("user55").first()

        coVerify { knowledgeDao.observeAveragePronunciationScore("user55") }
    }

    @Test
    fun observePronunciationScore_multipleEmissions_allForwarded() = runTest {
        every { knowledgeDao.observeAveragePronunciationScore("user1") } returns
            flowOf(0.3f, null, 0.8f)

        val results = mutableListOf<Float>()
        repository.observePronunciationScore("user1").collect { results.add(it) }

        assertEquals(listOf(0.3f, 0f, 0.8f), results)
    }
}
