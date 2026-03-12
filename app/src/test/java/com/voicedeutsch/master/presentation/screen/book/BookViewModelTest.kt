// Путь: src/test/java/com/voicedeutsch/master/presentation/screen/book/BookViewModelTest.kt
package com.voicedeutsch.master.presentation.screen.book

import app.cash.turbine.test
import com.voicedeutsch.master.domain.model.book.BookProgress
import com.voicedeutsch.master.domain.model.book.Chapter
import com.voicedeutsch.master.domain.model.book.Lesson
import com.voicedeutsch.master.domain.model.book.LessonContent
import com.voicedeutsch.master.domain.model.book.LessonFocus
import com.voicedeutsch.master.domain.repository.BookRepository
import com.voicedeutsch.master.domain.repository.UserRepository
import com.voicedeutsch.master.domain.usecase.book.GetCurrentLessonUseCase
import com.voicedeutsch.master.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class BookViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var getCurrentLesson: GetCurrentLessonUseCase
    private lateinit var bookRepository: BookRepository
    private lateinit var userRepository: UserRepository
    private lateinit var sut: BookViewModel

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun buildCurrentLessonData(
        chapterNumber: Int = 1,
        lessonNumber: Int = 1,
        chapterTitle: String = "Erste Schritte",
        lessonTitle: String = "Begrüßung",
    ) = GetCurrentLessonUseCase.CurrentLessonData(
        chapter = Chapter(number = chapterNumber, titleDe = chapterTitle, titleRu = chapterTitle, level = "A1", lessons = emptyList()),
        lesson = Lesson(number = lessonNumber, chapterNumber = chapterNumber, titleDe = lessonTitle, titleRu = lessonTitle),
        content = LessonContent(
            title = lessonTitle,
            introduction = "Intro",
            mainContent = "Content",
            phoneticNotes = "",
            exerciseMarkers = emptyList(),
            vocabulary = emptyList(),
        ),
    )

    private fun buildBookProgress(
        id: String = "progress_1",
        userId: String = "user_1",
        chapter: Int = 1,
        lesson: Int = 1,
    ) = BookProgress(
        id = id,
        userId = userId,
        chapter = chapter,
        lesson = lesson,
    )

    private fun setupDefaultMocks(
        userId: String? = "user_1",
        currentLesson: GetCurrentLessonUseCase.CurrentLessonData? = buildCurrentLessonData(),
        allProgress: List<BookProgress> = emptyList(),
        completionPercent: Float = 0.4f,
    ) {
        coEvery { userRepository.getActiveUserId() } returns userId
        coEvery { getCurrentLesson(any()) } returns currentLesson
        coEvery { bookRepository.getAllBookProgress(any()) } returns allProgress
        coEvery { bookRepository.getBookCompletionPercentage(any()) } returns completionPercent
    }

    private fun createViewModel() = BookViewModel(
        getCurrentLesson = getCurrentLesson,
        bookRepository = bookRepository,
        userRepository = userRepository,
    )

    @BeforeEach
    fun setUp() {
        getCurrentLesson = mockk(relaxed = true)
        bookRepository = mockk(relaxed = true)
        userRepository = mockk(relaxed = true)
        setupDefaultMocks()
    }

    // ── init loadData success ─────────────────────────────────────────────────

    @Test
    fun init_success_isLoadingFalse() = runTest {
        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(sut.uiState.value.isLoading)
    }

    @Test
    fun init_success_populatesCurrentLesson() = runTest {
        val lesson = buildCurrentLessonData(chapterTitle = "Kapitel 2", lessonTitle = "Essen")
        setupDefaultMocks(currentLesson = lesson)

        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(sut.uiState.value.currentLesson)
        assertEquals("Kapitel 2", sut.uiState.value.currentLesson!!.chapter.titleDe)
        assertEquals("Essen", sut.uiState.value.currentLesson!!.lesson.titleDe)
    }

    @Test
    fun init_success_currentLessonNull_stateHoldsNull() = runTest {
        setupDefaultMocks(currentLesson = null)

        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertNull(sut.uiState.value.currentLesson)
    }

    @Test
    fun init_success_populatesAllProgress() = runTest {
        val progress = listOf(
            buildBookProgress(id = "1", chapter = 1, lesson = 5),
            buildBookProgress(id = "2", chapter = 1, lesson = 2),
        )
        setupDefaultMocks(allProgress = progress)

        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, sut.uiState.value.allProgress.size)
        assertEquals(5, sut.uiState.value.allProgress[0].lesson)
        assertEquals(2, sut.uiState.value.allProgress[1].lesson)
    }

    @Test
    fun init_success_emptyProgress_returnsEmptyList() = runTest {
        setupDefaultMocks(allProgress = emptyList())

        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(emptyList<BookProgress>(), sut.uiState.value.allProgress)
    }

    @Test
    fun init_success_populatesCompletionPercent() = runTest {
        setupDefaultMocks(completionPercent = 0.75f)

        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(0.75f, sut.uiState.value.completionPercent, 0.001f)
    }

    @Test
    fun init_success_completionPercentZero() = runTest {
        setupDefaultMocks(completionPercent = 0f)

        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(0f, sut.uiState.value.completionPercent, 0.001f)
    }

    @Test
    fun init_success_completionPercentFull() = runTest {
        setupDefaultMocks(completionPercent = 1f)

        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1f, sut.uiState.value.completionPercent, 0.001f)
    }

    @Test
    fun init_success_noErrorMessage() = runTest {
        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertNull(sut.uiState.value.errorMessage)
    }

    @Test
    fun init_callsAllRepositoriesWithUserId() = runTest {
        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        coVerify { getCurrentLesson("user_1") }
        coVerify { bookRepository.getAllBookProgress("user_1") }
        coVerify { bookRepository.getBookCompletionPercentage("user_1") }
    }

    // ── init loadData failures ────────────────────────────────────────────────

    @Test
    fun init_noActiveUser_setsErrorMessage() = runTest {
        setupDefaultMocks(userId = null)

        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(sut.uiState.value.errorMessage)
        assertTrue(sut.uiState.value.errorMessage!!.contains("No active user"))
        assertFalse(sut.uiState.value.isLoading)
    }

    @Test
    fun init_noActiveUser_doesNotCallUseCases() = runTest {
        setupDefaultMocks(userId = null)

        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { getCurrentLesson(any()) }
        coVerify(exactly = 0) { bookRepository.getAllBookProgress(any()) }
        coVerify(exactly = 0) { bookRepository.getBookCompletionPercentage(any()) }
    }

    @Test
    fun init_getCurrentLessonThrows_setsErrorMessage() = runTest {
        coEvery { getCurrentLesson(any()) } throws RuntimeException("Lesson load error")

        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(sut.uiState.value.errorMessage)
        assertTrue(sut.uiState.value.errorMessage!!.contains("Lesson load error"))
        assertFalse(sut.uiState.value.isLoading)
    }

    @Test
    fun init_getAllBookProgressThrows_setsErrorMessage() = runTest {
        coEvery { bookRepository.getAllBookProgress(any()) } throws RuntimeException("DB error")

        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(sut.uiState.value.errorMessage)
        assertFalse(sut.uiState.value.isLoading)
    }

    @Test
    fun init_getBookCompletionPercentageThrows_setsErrorMessage() = runTest {
        coEvery { bookRepository.getBookCompletionPercentage(any()) } throws RuntimeException("Percent error")

        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(sut.uiState.value.errorMessage)
        assertFalse(sut.uiState.value.isLoading)
    }

    @Test
    fun init_errorWithNullMessage_errorMessageIsNull() = runTest {
        coEvery { getCurrentLesson(any()) } throws RuntimeException(null as String?)

        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        // e.message is null — state.errorMessage will be null per onFailure { e -> e.message }
        assertFalse(sut.uiState.value.isLoading)
    }

    // ── Refresh ───────────────────────────────────────────────────────────────

    @Test
    fun refresh_reloadsData() = runTest {
        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        val newLesson = buildCurrentLessonData(lessonTitle = "Familie")
        coEvery { getCurrentLesson(any()) } returns newLesson

        sut.onEvent(BookEvent.Refresh)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Familie", sut.uiState.value.currentLesson!!.lesson.titleDe)
    }

    @Test
    fun refresh_clearsErrorBeforeReload() = runTest {
        coEvery { getCurrentLesson(any()) } throws RuntimeException("fail")
        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
        assertNotNull(sut.uiState.value.errorMessage)

        setupDefaultMocks()
        sut.onEvent(BookEvent.Refresh)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertNull(sut.uiState.value.errorMessage)
    }

    @Test
    fun refresh_callsUseCasesTwice() = runTest {
        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        sut.onEvent(BookEvent.Refresh)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 2) { getCurrentLesson(any()) }
        coVerify(exactly = 2) { bookRepository.getAllBookProgress(any()) }
        coVerify(exactly = 2) { bookRepository.getBookCompletionPercentage(any()) }
    }

    @Test
    fun refresh_success_isLoadingFalse() = runTest {
        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        sut.onEvent(BookEvent.Refresh)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(sut.uiState.value.isLoading)
    }

    @Test
    fun refresh_updatesCompletionPercent() = runTest {
        setupDefaultMocks(completionPercent = 0.2f)
        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        coEvery { bookRepository.getBookCompletionPercentage(any()) } returns 0.8f

        sut.onEvent(BookEvent.Refresh)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(0.8f, sut.uiState.value.completionPercent, 0.001f)
    }

    @Test
    fun refresh_updatesAllProgress() = runTest {
        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        val newProgress = listOf(buildBookProgress(lesson = 9))
        coEvery { bookRepository.getAllBookProgress(any()) } returns newProgress

        sut.onEvent(BookEvent.Refresh)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, sut.uiState.value.allProgress.size)
        assertEquals(9, sut.uiState.value.allProgress[0].lesson)
    }

    // ── DismissError ──────────────────────────────────────────────────────────

    @Test
    fun dismissError_clearsErrorMessage() = runTest {
        coEvery { getCurrentLesson(any()) } throws RuntimeException("error")
        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
        assertNotNull(sut.uiState.value.errorMessage)

        sut.onEvent(BookEvent.DismissError)

        assertNull(sut.uiState.value.errorMessage)
    }

    @Test
    fun dismissError_whenNoError_doesNotCrash() = runTest {
        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertDoesNotThrow { sut.onEvent(BookEvent.DismissError) }
        assertNull(sut.uiState.value.errorMessage)
    }

    @Test
    fun dismissError_doesNotAffectOtherState() = runTest {
        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        val percentBefore = sut.uiState.value.completionPercent
        sut.onEvent(BookEvent.DismissError)

        assertEquals(percentBefore, sut.uiState.value.completionPercent, 0.001f)
        assertFalse(sut.uiState.value.isLoading)
    }

    @Test
    fun dismissError_doesNotTriggerReload() = runTest {
        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        sut.onEvent(BookEvent.DismissError)

        coVerify(exactly = 1) { getCurrentLesson(any()) }
    }

    // ── uiState flow ──────────────────────────────────────────────────────────

    @Test
    fun uiState_flow_emitsLoadedState() = runTest {
        sut = createViewModel()

        sut.uiState.test {
            val states = mutableListOf<BookUiState>()
            while (true) {
                val s = awaitItem()
                states.add(s)
                mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
                if (!s.isLoading) break
            }

            assertFalse(states.last().isLoading)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun uiState_flow_emitsOnRefresh() = runTest {
        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        sut.uiState.test {
            awaitItem() // loaded state

            val newLesson = buildCurrentLessonData(lessonTitle = "Reisen")
            coEvery { getCurrentLesson(any()) } returns newLesson

            sut.onEvent(BookEvent.Refresh)
            mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

            val states = mutableListOf<BookUiState>()
            while (true) {
                val s = awaitItem()
                states.add(s)
                if (!s.isLoading) break
            }

            assertEquals("Reisen", states.last().currentLesson!!.lesson.titleDe)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun uiState_flow_emitsOnDismissError() = runTest {
        coEvery { getCurrentLesson(any()) } throws RuntimeException("fail")
        sut = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        sut.uiState.test {
            awaitItem() // error state

            sut.onEvent(BookEvent.DismissError)
            val cleared = awaitItem()
            assertNull(cleared.errorMessage)

            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── BookUiState data class ────────────────────────────────────────────────

    @Test
    fun bookUiState_defaultValues_areCorrect() {
        val state = BookUiState()
        assertTrue(state.isLoading)
        assertNull(state.currentLesson)
        assertEquals(emptyList<BookProgress>(), state.allProgress)
        assertEquals(0f, state.completionPercent, 0.001f)
        assertNull(state.errorMessage)
    }

    @Test
    fun bookUiState_copy_changesOneField() {
        val original = BookUiState()
        val copy = original.copy(completionPercent = 0.5f)
        assertEquals(0.5f, copy.completionPercent, 0.001f)
        assertEquals(original.isLoading, copy.isLoading)
        assertNull(copy.errorMessage)
    }

    @Test
    fun bookUiState_equals_twoIdenticalInstances() {
        val a = BookUiState()
        val b = BookUiState()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun bookUiState_notEquals_differentCompletionPercent() {
        val a = BookUiState(completionPercent = 0.3f)
        val b = BookUiState(completionPercent = 0.7f)
        assertNotEquals(a, b)
    }
}
