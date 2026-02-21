package com.voicedeutsch.master.domain.usecase.book

import com.voicedeutsch.master.domain.model.book.Chapter
import com.voicedeutsch.master.domain.repository.BookRepository

/**
 * Loads full chapter content including all lessons, vocabulary and grammar.
 * Architecture line 918 (GetChapterContentUseCase.kt).
 */
class GetChapterContentUseCase(
    private val bookRepository: BookRepository,
) {

    data class Params(
        val chapterNumber: Int,
    )

    suspend operator fun invoke(params: Params): Chapter? {
        return bookRepository.getChapter(params.chapterNumber)
    }
}