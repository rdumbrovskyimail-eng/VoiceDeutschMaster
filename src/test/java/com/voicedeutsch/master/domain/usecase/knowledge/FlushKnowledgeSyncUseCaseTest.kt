// Путь: src/test/java/com/voicedeutsch/master/domain/usecase/knowledge/FlushKnowledgeSyncUseCaseTest.kt
package com.voicedeutsch.master.domain.usecase.knowledge

import com.voicedeutsch.master.domain.repository.KnowledgeRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class FlushKnowledgeSyncUseCaseTest {

    private lateinit var knowledgeRepository: KnowledgeRepository
    private lateinit var useCase: FlushKnowledgeSyncUseCase

    @BeforeEach
    fun setUp() {
        knowledgeRepository = mockk()
        useCase = FlushKnowledgeSyncUseCase(knowledgeRepository)
    }

    // ── invoke — delegates to repository ─────────────────────────────────────

    @Test
    fun invoke_repositoryReturnsTrue_returnsTrue() = runTest {
        coEvery { knowledgeRepository.flushSync() } returns true

        val result = useCase()

        assertTrue(result)
    }

    @Test
    fun invoke_repositoryReturnsFalse_returnsFalse() = runTest {
        coEvery { knowledgeRepository.flushSync() } returns false

        val result = useCase()

        assertFalse(result)
    }

    @Test
    fun invoke_calledOnce_flushSyncCalledExactlyOnce() = runTest {
        coEvery { knowledgeRepository.flushSync() } returns true

        useCase()

        coVerify(exactly = 1) { knowledgeRepository.flushSync() }
    }

    @Test
    fun invoke_repositoryThrows_exceptionPropagates() = runTest {
        coEvery { knowledgeRepository.flushSync() } throws RuntimeException("network error")

        val exception = org.junit.jupiter.api.assertThrows<RuntimeException> {
            useCase()
        }

        assertEquals("network error", exception.message)
    }

    @Test
    fun invoke_calledMultipleTimes_flushSyncCalledEachTime() = runTest {
        coEvery { knowledgeRepository.flushSync() } returns true

        useCase()
        useCase()
        useCase()

        coVerify(exactly = 3) { knowledgeRepository.flushSync() }
    }

    @Test
    fun invoke_emptyQueue_repositoryReturnsTrueAndUseCaseReturnsTrue() = runTest {
        // empty queue → flushSync returns true (as per KDoc contract)
        coEvery { knowledgeRepository.flushSync() } returns true

        val result = useCase()

        assertTrue(result)
        coVerify(exactly = 1) { knowledgeRepository.flushSync() }
    }
}
