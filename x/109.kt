// Путь: src/test/java/com/voicedeutsch/master/domain/model/knowledge/KnowledgeSnapshotTest.kt
package com.voicedeutsch.master.domain.model.knowledge

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class KnowledgeSnapshotTest {

    // ── Builders ──────────────────────────────────────────────────────────

    private fun vocabulary(
        totalWords: Int = 50,
        byLevel: Map<Int, Int> = emptyMap(),
        byTopic: Map<String, TopicStats> = emptyMap(),
        recentNewWords: List<String> = emptyList(),
        problemWords: List<ProblemWordInfo> = emptyList(),
        wordsForReviewToday: Int = 0,
    ) = VocabularySnapshot(totalWords, byLevel, byTopic, recentNewWords, problemWords, wordsForReviewToday)

    private fun grammar(
        totalRules: Int = 10,
        byLevel: Map<Int, Int> = emptyMap(),
        knownRules: List<KnownRuleInfo> = emptyList(),
        problemRules: List<String> = emptyList(),
        rulesForReviewToday: Int = 0,
    ) = GrammarSnapshot(totalRules, byLevel, knownRules, problemRules, rulesForReviewToday)

    private fun pronunciation(
        overallScore: Float = 0.8f,
        problemSounds: List<String> = emptyList(),
        goodSounds: List<String> = emptyList(),
        averageWordScore: Float = 0.75f,
        trend: String = "stable",
    ) = PronunciationSnapshot(overallScore, problemSounds, goodSounds, averageWordScore, trend)

    private fun bookProgress(
        currentChapter: Int = 1,
        currentLesson: Int = 1,
        totalChapters: Int = 12,
        completionPercentage: Float = 0.1f,
        currentTopic: String = "Begrüßung",
    ) = BookProgressSnapshot(currentChapter, currentLesson, totalChapters, completionPercentage, currentTopic)

    private fun sessionHistory(
        lastSession: String = "2026-03-01",
        lastSessionSummary: String = "Изучено 5 слов",
        averageSessionDuration: String = "20 min",
        streak: Int = 3,
        totalSessions: Int = 10,
    ) = SessionHistorySnapshot(lastSession, lastSessionSummary, averageSessionDuration, streak, totalSessions)

    private fun recommendations(
        primaryStrategy: String = "LINEAR_BOOK",
        secondaryStrategy: String = "REPETITION",
        focusAreas: List<String> = emptyList(),
        suggestedSessionDuration: String = "20 min",
    ) = RecommendationsSnapshot(primaryStrategy, secondaryStrategy, focusAreas, suggestedSessionDuration)

    private fun snapshot(
        vocab: VocabularySnapshot = vocabulary(),
        gram: GrammarSnapshot = grammar(),
        pron: PronunciationSnapshot = pronunciation(),
        book: BookProgressSnapshot = bookProgress(),
        hist: SessionHistorySnapshot = sessionHistory(),
        weakPoints: List<String> = emptyList(),
        recs: RecommendationsSnapshot = recommendations(),
    ) = KnowledgeSnapshot(vocab, gram, pron, book, hist, weakPoints, recs)

    // ── KnowledgeSnapshot — creation ──────────────────────────────────────

    @Test
    fun creation_allFieldsStoredCorrectly() {
        val vocab = vocabulary(totalWords = 100)
        val gram  = grammar(totalRules = 20)
        val snap  = snapshot(vocab = vocab, gram = gram, weakPoints = listOf("Dativ", "Akkusativ"))
        assertEquals(100, snap.vocabulary.totalWords)
        assertEquals(20, snap.grammar.totalRules)
        assertEquals(2, snap.weakPoints.size)
    }

    @Test
    fun creation_emptyWeakPoints() {
        assertTrue(snapshot().weakPoints.isEmpty())
    }

    @Test
    fun equals_twoIdentical_returnsTrue() {
        val a = snapshot()
        val b = snapshot()
        assertEquals(a, b)
    }

    @Test
    fun equals_differentWeakPoints_returnsFalse() {
        val a = snapshot(weakPoints = listOf("Dativ"))
        val b = snapshot(weakPoints = emptyList())
        assertNotEquals(a, b)
    }

    @Test
    fun copy_changesOnlySpecifiedField() {
        val original = snapshot(weakPoints = listOf("A"))
        val copied = original.copy(weakPoints = listOf("B", "C"))
        assertEquals(listOf("B", "C"), copied.weakPoints)
        assertEquals(original.vocabulary, copied.vocabulary)
    }

    @Test
    fun hashCode_equalSnapshots_sameHash() {
        assertEquals(snapshot().hashCode(), snapshot().hashCode())
    }

    // ── VocabularySnapshot ────────────────────────────────────────────────

    @Test
    fun vocabularySnapshot_totalWords_storedCorrectly() {
        assertEquals(75, vocabulary(totalWords = 75).totalWords)
    }

    @Test
    fun vocabularySnapshot_wordsForReviewToday_storedCorrectly() {
        assertEquals(12, vocabulary(wordsForReviewToday = 12).wordsForReviewToday)
    }

    @Test
    fun vocabularySnapshot_byLevel_storedCorrectly() {
        val byLevel = mapOf(1 to 10, 2 to 20)
        assertEquals(byLevel, vocabulary(byLevel = byLevel).byLevel)
    }

    @Test
    fun vocabularySnapshot_problemWords_storedCorrectly() {
        val problems = listOf(ProblemWordInfo("Haus", 1, 5))
        assertEquals(1, vocabulary(problemWords = problems).problemWords.size)
    }

    @Test
    fun vocabularySnapshot_recentNewWords_storedCorrectly() {
        val words = listOf("Wasser", "Buch", "Tisch")
        assertEquals(words, vocabulary(recentNewWords = words).recentNewWords)
    }

    @Test
    fun vocabularySnapshot_equals_twoIdentical() {
        assertEquals(vocabulary(), vocabulary())
    }

    @Test
    fun vocabularySnapshot_copy_changesOnlyTotalWords() {
        val original = vocabulary(totalWords = 10)
        val copied = original.copy(totalWords = 99)
        assertEquals(99, copied.totalWords)
        assertEquals(original.wordsForReviewToday, copied.wordsForReviewToday)
    }

    // ── TopicStats ────────────────────────────────────────────────────────

    @Test
    fun topicStats_fieldsStoredCorrectly() {
        val stats = TopicStats(known = 8, total = 10)
        assertEquals(8, stats.known)
        assertEquals(10, stats.total)
    }

    @Test
    fun topicStats_equals_twoIdentical() {
        assertEquals(TopicStats(5, 10), TopicStats(5, 10))
    }

    @Test
    fun topicStats_notEquals_differentKnown() {
        assertNotEquals(TopicStats(3, 10), TopicStats(4, 10))
    }

    @Test
    fun topicStats_copy_changesOnlyTotal() {
        val original = TopicStats(5, 10)
        val copied = original.copy(total = 20)
        assertEquals(5, copied.known)
        assertEquals(20, copied.total)
    }

    // ── ProblemWordInfo ───────────────────────────────────────────────────

    @Test
    fun problemWordInfo_fieldsStoredCorrectly() {
        val info = ProblemWordInfo(word = "Hund", level = 2, attempts = 7)
        assertEquals("Hund", info.word)
        assertEquals(2, info.level)
        assertEquals(7, info.attempts)
    }

    @Test
    fun problemWordInfo_equals_twoIdentical() {
        assertEquals(ProblemWordInfo("A", 1, 3), ProblemWordInfo("A", 1, 3))
    }

    @Test
    fun problemWordInfo_notEquals_differentWord() {
        assertNotEquals(ProblemWordInfo("A", 1, 3), ProblemWordInfo("B", 1, 3))
    }

    @Test
    fun problemWordInfo_copy_changesOnlyLevel() {
        val original = ProblemWordInfo("Wort", 1, 5)
        val copied = original.copy(level = 3)
        assertEquals("Wort", copied.word)
        assertEquals(3, copied.level)
        assertEquals(5, copied.attempts)
    }

    // ── GrammarSnapshot ───────────────────────────────────────────────────

    @Test
    fun grammarSnapshot_totalRules_storedCorrectly() {
        assertEquals(25, grammar(totalRules = 25).totalRules)
    }

    @Test
    fun grammarSnapshot_rulesForReviewToday_storedCorrectly() {
        assertEquals(4, grammar(rulesForReviewToday = 4).rulesForReviewToday)
    }

    @Test
    fun grammarSnapshot_knownRules_storedCorrectly() {
        val rules = listOf(KnownRuleInfo("Dativ", 3))
        assertEquals(1, grammar(knownRules = rules).knownRules.size)
    }

    @Test
    fun grammarSnapshot_problemRules_storedCorrectly() {
        val problems = listOf("Konjunktiv", "Plusquamperfekt")
        assertEquals(2, grammar(problemRules = problems).problemRules.size)
    }

    @Test
    fun grammarSnapshot_equals_twoIdentical() {
        assertEquals(grammar(), grammar())
    }

    // ── KnownRuleInfo ─────────────────────────────────────────────────────

    @Test
    fun knownRuleInfo_fieldsStoredCorrectly() {
        val info = KnownRuleInfo(name = "Akkusativ", level = 4)
        assertEquals("Akkusativ", info.name)
        assertEquals(4, info.level)
    }

    @Test
    fun knownRuleInfo_equals_twoIdentical() {
        assertEquals(KnownRuleInfo("A", 2), KnownRuleInfo("A", 2))
    }

    @Test
    fun knownRuleInfo_copy_changesOnlyLevel() {
        val original = KnownRuleInfo("Regel", 1)
        val copied = original.copy(level = 5)
        assertEquals("Regel", copied.name)
        assertEquals(5, copied.level)
    }

    // ── PronunciationSnapshot ─────────────────────────────────────────────

    @Test
    fun pronunciationSnapshot_overallScore_storedCorrectly() {
        assertEquals(0.9f, pronunciation(overallScore = 0.9f).overallScore)
    }

    @Test
    fun pronunciationSnapshot_problemSounds_storedCorrectly() {
        val sounds = listOf("ü", "ö", "ch")
        assertEquals(sounds, pronunciation(problemSounds = sounds).problemSounds)
    }

    @Test
    fun pronunciationSnapshot_trend_storedCorrectly() {
        assertEquals("improving", pronunciation(trend = "improving").trend)
    }

    @Test
    fun pronunciationSnapshot_averageWordScore_storedCorrectly() {
        assertEquals(0.65f, pronunciation(averageWordScore = 0.65f).averageWordScore)
    }

    @Test
    fun pronunciationSnapshot_equals_twoIdentical() {
        assertEquals(pronunciation(), pronunciation())
    }

    // ── BookProgressSnapshot ──────────────────────────────────────────────

    @Test
    fun bookProgressSnapshot_fieldsStoredCorrectly() {
        val prog = bookProgress(currentChapter = 5, currentLesson = 3, totalChapters = 20)
        assertEquals(5, prog.currentChapter)
        assertEquals(3, prog.currentLesson)
        assertEquals(20, prog.totalChapters)
    }

    @Test
    fun bookProgressSnapshot_completionPercentage_storedCorrectly() {
        assertEquals(0.5f, bookProgress(completionPercentage = 0.5f).completionPercentage)
    }

    @Test
    fun bookProgressSnapshot_currentTopic_storedCorrectly() {
        assertEquals("Familie", bookProgress(currentTopic = "Familie").currentTopic)
    }

    @Test
    fun bookProgressSnapshot_equals_twoIdentical() {
        assertEquals(bookProgress(), bookProgress())
    }

    @Test
    fun bookProgressSnapshot_copy_changesOnlyChapter() {
        val original = bookProgress(currentChapter = 1)
        val copied = original.copy(currentChapter = 7)
        assertEquals(7, copied.currentChapter)
        assertEquals(original.currentLesson, copied.currentLesson)
    }

    // ── SessionHistorySnapshot ────────────────────────────────────────────

    @Test
    fun sessionHistorySnapshot_fieldsStoredCorrectly() {
        val hist = sessionHistory(streak = 7, totalSessions = 42)
        assertEquals(7, hist.streak)
        assertEquals(42, hist.totalSessions)
    }

    @Test
    fun sessionHistorySnapshot_lastSession_storedCorrectly() {
        assertEquals("2026-01-15", sessionHistory(lastSession = "2026-01-15").lastSession)
    }

    @Test
    fun sessionHistorySnapshot_equals_twoIdentical() {
        assertEquals(sessionHistory(), sessionHistory())
    }

    @Test
    fun sessionHistorySnapshot_copy_changesOnlyStreak() {
        val original = sessionHistory(streak = 3)
        val copied = original.copy(streak = 10)
        assertEquals(10, copied.streak)
        assertEquals(original.totalSessions, copied.totalSessions)
    }

    // ── RecommendationsSnapshot ───────────────────────────────────────────

    @Test
    fun recommendationsSnapshot_fieldsStoredCorrectly() {
        val recs = recommendations(
            primaryStrategy = "REPETITION",
            secondaryStrategy = "GAP_FILLING",
            focusAreas = listOf("Dativ", "Artikel"),
            suggestedSessionDuration = "30 min",
        )
        assertEquals("REPETITION", recs.primaryStrategy)
        assertEquals("GAP_FILLING", recs.secondaryStrategy)
        assertEquals(2, recs.focusAreas.size)
        assertEquals("30 min", recs.suggestedSessionDuration)
    }

    @Test
    fun recommendationsSnapshot_equals_twoIdentical() {
        assertEquals(recommendations(), recommendations())
    }

    @Test
    fun recommendationsSnapshot_notEquals_differentPrimaryStrategy() {
        val a = recommendations(primaryStrategy = "REPETITION")
        val b = recommendations(primaryStrategy = "LINEAR_BOOK")
        assertNotEquals(a, b)
    }

    @Test
    fun recommendationsSnapshot_copy_changesOnlyFocusAreas() {
        val original = recommendations(focusAreas = listOf("A"))
        val copied = original.copy(focusAreas = listOf("B", "C"))
        assertEquals(listOf("B", "C"), copied.focusAreas)
        assertEquals(original.primaryStrategy, copied.primaryStrategy)
    }
}
