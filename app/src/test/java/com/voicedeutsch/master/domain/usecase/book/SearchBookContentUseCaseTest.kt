// Путь: src/test/java/com/voicedeutsch/master/domain/usecase/book/SearchBookContentUseCaseTest.kt
package com.voicedeutsch.master.domain.usecase.book

import com.voicedeutsch.master.domain.model.knowledge.Word
import com.voicedeutsch.master.domain.repository.BookRepository
import com.voicedeutsch.master.domain.repository.KnowledgeRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SearchBookContentUseCaseTest {

    private lateinit var bookRepository: BookRepository
    private lateinit var knowledgeRepository: KnowledgeRepository
    private lateinit var useCase: SearchBookContentUseCase

    // ── Builders ──────────────────────────────────────────────────────────────

    private fun makeWord(id: String = "w1"): Word =
        mockk<Word>(relaxed = true).also { every { it.id } returns id }

    private fun makeContentHit(
        chapter: Int = 1,
        lesson: Int = 1,
        snippet: String = "example"
    ) = SearchBookContentUseCase.ChapterHit(
        chapter = chapter,
        lesson  = lesson,
        snippet = snippet,
    )

    // ── Setup ─────────────────────────────────────────────────────────────────

    @BeforeEach
    fun setUp() {
        bookRepository      = mockk()
        knowledgeRepository = mockk()
        useCase = SearchBookContentUseCase(bookRepository, knowledgeRepository)

        coEvery { knowledgeRepository.searchWords(any()) } returns emptyList()
        coEvery { bookRepository.searchContent(any()) }    returns emptyList()
    }

    // ── invoke — happy path ───────────────────────────────────────────────────

    @Test
    fun invoke_bothSourcesEmpty_returnsEmptySearchResult() = runTest {
        val result = useCase(makeParams("Hund"))

        assertTrue(result.words.isEmpty())
        assertTrue(result.chapterHits.isEmpty())
    }

    @Test
    fun invoke_wordsFound_returnedInResult() = runTest {
        val words = listOf(makeWord("w1"), makeWord("w2"))
        coEvery { knowledgeRepository.searchWords("Hund") } returns words

        val result = useCase(makeParams("Hund"))

        assertEquals(2, result.words.size)
        assertEquals("w1", result.words[0].id)
        assertEquals("w2", result.words[1].id)
    }

    @Test
    fun invoke_chapterHitsFound_mappedToChapterHit() = runTest {
        coEvery { bookRepository.searchContent("Hund") } returns listOf(
            makeContentHit(chapter = 2, lesson = 5, snippet = "Der Hund läuft")
        )

        val result = useCase(makeParams("Hund"))

        assertEquals(1, result.chapterHits.size)
        assertEquals(2,                   result.chapterHits[0].chapter)
        assertEquals(5,                   result.chapterHits[0].lesson)
        assertEquals("Der Hund läuft",    result.chapterHits[0].snippet)
    }

    @Test
    fun invoke_queryPassedToKnowledgeRepository() = runTest {
        useCase(makeParams("Katze"))

        coVerify(exactly = 1) { knowledgeRepository.searchWords("Katze") }
    }

    @Test
    fun invoke_queryPassedToBookRepository() = runTest {
        useCase(makeParams("Katze"))

        coVerify(exactly = 1) { bookRepository.searchContent("Katze") }
    }

    // ── limit enforcement ─────────────────────────────────────────────────────

    @Test
    fun invoke_moreWordsThanLimit_wordsTruncatedToLimit() = runTest {
        val words = List(30) { i -> makeWord("w$i") }
        coEvery { knowledgeRepository.searchWords(any()) } returns words

        val result = useCase(makeParams("x", limit = 10))

        assertEquals(10, result.words.size)
    }

    @Test
    fun invoke_fewerWordsThanLimit_allWordsReturned() = runTest {
        val words = List(5) { i -> makeWord("w$i") }
        coEvery { knowledgeRepository.searchWords(any()) } returns words

        val result = useCase(makeParams("x", limit = 20))

        assertEquals(5, result.words.size)
    }

    @Test
    fun invoke_moreChapterHitsThanLimit_hitsTruncatedToLimit() = runTest {
        val hits = List(30) { i -> makeContentHit(chapter = i, lesson = 1, snippet = "s$i") }
        coEvery { bookRepository.searchContent(any()) } returns hits

        val result = useCase(makeParams("x", limit = 7))

        assertEquals(7, result.chapterHits.size)
    }

    @Test
    fun invoke_fewerChapterHitsThanLimit_allHitsReturned() = runTest {
        val hits = List(3) { i -> makeContentHit(chapter = i, lesson = 1) }
        coEvery { bookRepository.searchContent(any()) } returns hits

        val result = useCase(makeParams("x", limit = 20))

        assertEquals(3, result.chapterHits.size)
    }

    @Test
    fun invoke_defaultLimit_wordsAndHitsCappedAt20() = runTest {
        val words = List(25) { i -> makeWord("w$i") }
        val hits  = List(25) { i -> makeContentHit(chapter = i, lesson = 1) }
        coEvery { knowledgeRepository.searchWords(any()) } returns words
        coEvery { bookRepository.searchContent(any()) }    returns hits

        val result = useCase(makeParams("x"))

        assertEquals(20, result.words.size)
        assertEquals(20, result.chapterHits.size)
    }

    @Test
    fun invoke_exactlyLimitItems_allReturned() = runTest {
        val words = List(20) { i -> makeWord("w$i") }
        val hits  = List(20) { i -> makeContentHit(chapter = i, lesson = 1) }
        coEvery { knowledgeRepository.searchWords(any()) } returns words
        coEvery { bookRepository.searchContent(any()) }    returns hits

        val result = useCase(makeParams("x", limit = 20))

        assertEquals(20, result.words.size)
        assertEquals(20, result.chapterHits.size)
    }

    // ── ChapterHit mapping ────────────────────────────────────────────────────

    @Test
    fun invoke_multipleChapterHits_allMappedCorrectly() = runTest {
        coEvery { bookRepository.searchContent("Verb") } returns listOf(
            makeContentHit(chapter = 1, lesson = 2, snippet = "Verb A"),
            makeContentHit(chapter = 3, lesson = 4, snippet = "Verb B")
        )

        val result = useCase(makeParams("Verb"))

        assertEquals(1, result.chapterHits[0].chapter)
        assertEquals(2, result.chapterHits[0].lesson)
        assertEquals("Verb A", result.chapterHits[0].snippet)
        assertEquals(3, result.chapterHits[1].chapter)
        assertEquals(4, result.chapterHits[1].lesson)
        assertEquals("Verb B", result.chapterHits[1].snippet)
    }

    @Test
    fun invoke_wordsAndHitsIndependentlyLimited() = runTest {
        // words = 25, hits = 5, limit = 10 → words truncated, hits returned in full
        val words = List(25) { i -> makeWord("w$i") }
        val hits  = List(5)  { i -> makeContentHit(chapter = i, lesson = 1) }
        coEvery { knowledgeRepository.searchWords(any()) } returns words
        coEvery { bookRepository.searchContent(any()) }    returns hits

        val result = useCase(makeParams("x", limit = 10))

        assertEquals(10, result.words.size)
        assertEquals(5,  result.chapterHits.size)
    }

    // ── Params data class ─────────────────────────────────────────────────────

    @Test
    fun params_defaultLimit_is20() {
        val params = SearchBookContentUseCase.Params(query = "test")
        assertEquals(20, params.limit)
    }

    @Test
    fun params_creation_storesAllFields() {
        val params = SearchBookContentUseCase.Params(query = "Hund", limit = 5)
        assertEquals("Hund", params.query)
        assertEquals(5, params.limit)
    }

    @Test
    fun params_copy_changesOnlySpecifiedField() {
        val original = SearchBookContentUseCase.Params("Hund", 10)
        val copy     = original.copy(limit = 3)

        assertEquals(3,      copy.limit)
        assertEquals("Hund", copy.query)
    }

    @Test
    fun params_equals_twoIdenticalInstancesAreEqual() {
        val a = SearchBookContentUseCase.Params("x", 5)
        val b = SearchBookContentUseCase.Params("x", 5)

        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun params_equals_differentQueryNotEqual() {
        val a = SearchBookContentUseCase.Params("a", 5)
        val b = SearchBookContentUseCase.Params("b", 5)

        assertNotEquals(a, b)
    }

    // ── SearchResult data class ───────────────────────────────────────────────

    @Test
    fun searchResult_creation_storesAllFields() {
        val words = listOf(makeWord())
        val hits  = listOf(SearchBookContentUseCase.ChapterHit(1, 2, "snippet"))
        val result = SearchBookContentUseCase.SearchResult(words = words, chapterHits = hits)

        assertEquals(words, result.words)
        assertEquals(hits,  result.chapterHits)
    }

    @Test
    fun searchResult_copy_changesOnlySpecifiedField() {
        val words = listOf(makeWord())
        val original = SearchBookContentUseCase.SearchResult(words, emptyList())
        val copy     = original.copy(chapterHits = listOf(
            SearchBookContentUseCase.ChapterHit(1, 1, "s")
        ))

        assertEquals(1,      copy.chapterHits.size)
        assertEquals(words,  copy.words)
    }

    @Test
    fun searchResult_equals_twoIdenticalInstancesAreEqual() {
        val a = SearchBookContentUseCase.SearchResult(emptyList(), emptyList())
        val b = SearchBookContentUseCase.SearchResult(emptyList(), emptyList())

        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    // ── ChapterHit data class ─────────────────────────────────────────────────

    @Test
    fun chapterHit_creation_storesAllFields() {
        val hit = SearchBookContentUseCase.ChapterHit(chapter = 3, lesson = 7, snippet = "hello")

        assertEquals(3,       hit.chapter)
        assertEquals(7,       hit.lesson)
        assertEquals("hello", hit.snippet)
    }

    @Test
    fun chapterHit_copy_changesOnlySpecifiedField() {
        val original = SearchBookContentUseCase.ChapterHit(1, 2, "old")
        val copy     = original.copy(snippet = "new")

        assertEquals("new", copy.snippet)
        assertEquals(1,     copy.chapter)
        assertEquals(2,     copy.lesson)
    }

    @Test
    fun chapterHit_equals_twoIdenticalInstancesAreEqual() {
        val a = SearchBookContentUseCase.ChapterHit(1, 1, "s")
        val b = SearchBookContentUseCase.ChapterHit(1, 1, "s")

        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun chapterHit_equals_differentSnippetNotEqual() {
        val a = SearchBookContentUseCase.ChapterHit(1, 1, "s1")
        val b = SearchBookContentUseCase.ChapterHit(1, 1, "s2")

        assertNotEquals(a, b)
    }

    @Test
    fun chapterHit_equals_differentChapterNotEqual() {
        val a = SearchBookContentUseCase.ChapterHit(1, 1, "s")
        val b = SearchBookContentUseCase.ChapterHit(2, 1, "s")

        assertNotEquals(a, b)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun makeParams(query: String, limit: Int = 20) =
        SearchBookContentUseCase.Params(query = query, limit = limit)
}
