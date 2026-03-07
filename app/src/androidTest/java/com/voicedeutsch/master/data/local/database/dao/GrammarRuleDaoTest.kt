// Путь: src/androidTest/java/com/voicedeutsch/master/data/local/database/dao/GrammarRuleDaoTest.kt
package com.voicedeutsch.master.data.local.database.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.voicedeutsch.master.data.local.database.VoiceDeutschDatabase
import com.voicedeutsch.master.data.local.database.entity.GrammarRuleEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class GrammarRuleDaoTest {

    private lateinit var db: VoiceDeutschDatabase
    private lateinit var grammarRuleDao: GrammarRuleDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            VoiceDeutschDatabase::class.java
        ).allowMainThreadQueries().build()
        grammarRuleDao = db.grammarRuleDao()
    }

    @After
    fun tearDown() = db.close()

    private fun makeRule(id: String, category: String = "articles", level: String = "A1", chapter: Int? = null) =
        GrammarRuleEntity(id = id, nameRu = "Правило $id", nameDe = "Regel $id",
            category = category, descriptionRu = "Описание $id",
            difficultyLevel = level, bookChapter = chapter)

    @Test
    fun getRule_existing_returnsRule() = runTest {
        grammarRuleDao.insertRule(makeRule("gr_1"))
        val result = grammarRuleDao.getRule("gr_1")
        assertNotNull(result)
        assertEquals("gr_1", result!!.id)
    }

    @Test
    fun getRule_nonExisting_returnsNull() = runTest {
        assertNull(grammarRuleDao.getRule("missing"))
    }

    @Test
    fun getAllRules_empty_returnsEmptyList() = runTest {
        assertTrue(grammarRuleDao.getAllRules().isEmpty())
    }

    @Test
    fun getAllRules_afterBulkInsert_returnsAll() = runTest {
        grammarRuleDao.insertRules(listOf(makeRule("gr_1"), makeRule("gr_2"), makeRule("gr_3")))
        assertEquals(3, grammarRuleDao.getAllRules().size)
    }

    @Test
    fun getRulesByCategory_matchingCategory_returnsMatches() = runTest {
        grammarRuleDao.insertRules(listOf(
            makeRule("gr_1", category = "articles"),
            makeRule("gr_2", category = "articles"),
            makeRule("gr_3", category = "verbs")))
        val result = grammarRuleDao.getRulesByCategory("articles")
        assertEquals(2, result.size)
        assertTrue(result.all { it.category == "articles" })
    }

    @Test
    fun getRulesByCategory_noMatch_returnsEmpty() = runTest {
        grammarRuleDao.insertRule(makeRule("gr_1", category = "articles"))
        assertTrue(grammarRuleDao.getRulesByCategory("cases").isEmpty())
    }

    @Test
    fun getRulesByLevel_matchingLevel_returnsMatches() = runTest {
        grammarRuleDao.insertRules(listOf(
            makeRule("gr_1", level = "A1"), makeRule("gr_2", level = "A1"), makeRule("gr_3", level = "B1")))
        assertEquals(2, grammarRuleDao.getRulesByLevel("A1").size)
    }

    @Test
    fun getRulesByLevel_noMatch_returnsEmpty() = runTest {
        grammarRuleDao.insertRule(makeRule("gr_1", level = "A1"))
        assertTrue(grammarRuleDao.getRulesByLevel("C2").isEmpty())
    }

    @Test
    fun getRulesByChapter_matchingChapter_returnsMatches() = runTest {
        grammarRuleDao.insertRules(listOf(
            makeRule("gr_1", chapter = 3), makeRule("gr_2", chapter = 3), makeRule("gr_3", chapter = 5)))
        assertEquals(2, grammarRuleDao.getRulesByChapter(3).size)
    }

    @Test
    fun getRulesByChapter_noMatch_returnsEmpty() = runTest {
        grammarRuleDao.insertRule(makeRule("gr_1", chapter = 1))
        assertTrue(grammarRuleDao.getRulesByChapter(99).isEmpty())
    }

    @Test
    fun insertRule_replace_updatesExisting() = runTest {
        grammarRuleDao.insertRule(makeRule("gr_1").copy(category = "articles"))
        grammarRuleDao.insertRule(makeRule("gr_1").copy(category = "verbs"))
        assertEquals("verbs", grammarRuleDao.getRule("gr_1")!!.category)
    }

    @Test
    fun insertRules_empty_doesNotCrash() = runTest {
        grammarRuleDao.insertRules(emptyList())
        assertEquals(0, grammarRuleDao.getRuleCount())
    }

    @Test
    fun getRuleCount_noRules_returnsZero() = runTest {
        assertEquals(0, grammarRuleDao.getRuleCount())
    }

    @Test
    fun getRuleCount_afterInserts_returnsCorrectCount() = runTest {
        grammarRuleDao.insertRules(listOf(makeRule("gr_1"), makeRule("gr_2"), makeRule("gr_3")))
        assertEquals(3, grammarRuleDao.getRuleCount())
    }
}
