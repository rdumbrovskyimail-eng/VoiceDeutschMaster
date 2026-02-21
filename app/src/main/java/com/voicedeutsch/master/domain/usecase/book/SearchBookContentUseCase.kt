package com.voicedeutsch.master.domain.usecase.book

import com.voicedeutsch.master.domain.model.knowledge.Word
import com.voicedeutsch.master.domain.repository.BookRepository
import com.voicedeutsch.master.domain.repository.KnowledgeRepository

/**
 * Searches book content for words, phrases, or grammar rules.
 * Architecture line 920 (SearchBookContentUseCase.kt).
 */
class SearchBookContentUseCase(
    private val bookRepository: BookRepository,
    private val knowledgeRepository: KnowledgeRepository,
) {

    data class Params(
        val query: String,
        val limit: Int = 20,
    )

    data class SearchResult(
        val words: List<Word>,
        val chapterHits: List<ChapterHit>,
    )

    data class ChapterHit(
        val chapter: Int,
        val lesson: Int,
        val snippet: String,
    )

    suspend operator fun invoke(params: Params): SearchResult {
        val words = knowledgeRepository.searchWords(params.query)
            .take(params.limit)

        val chapters = bookRepository.searchContent(params.query)
            .take(params.limit)
            .map { ChapterHit(it.chapter, it.lesson, it.snippet) }

        return SearchResult(words = words, chapterHits = chapters)
    }
}