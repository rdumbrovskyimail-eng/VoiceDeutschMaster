package com.voicedeutsch.master.domain.usecase.knowledge

import com.voicedeutsch.master.domain.repository.KnowledgeRepository

/**
 * Сбрасывает очередь батч-синхронизации знания в Firestore.
 *
 * Вызывается ОДИН РАЗ в конце сессии из [VoiceCoreEngineImpl.endSession()].
 *
 * 50 слов за сессию SRS = было 50 Firestore-записей в квоту →
 * стало 1 batch-commit.
 *
 * @return true если синхронизация прошла успешно или очередь была пуста.
 *         false при ошибке сети — данные в Room в безопасности,
 *         очередь сохранится и будет отправлена в следующем сеансе.
 */
class FlushKnowledgeSyncUseCase(
    private val knowledgeRepository: KnowledgeRepository,
) {
    suspend operator fun invoke(): Boolean =
        knowledgeRepository.flushSync()
}
