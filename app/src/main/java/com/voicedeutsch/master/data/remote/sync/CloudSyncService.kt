package com.voicedeutsch.master.data.remote.sync

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await

/**
 * CloudSyncService — реал-тайм синхронизация прогресса пользователя через Firebase Firestore.
 *
 * ════════════════════════════════════════════════════════════════════════════
 * СТРУКТУРА FIRESTORE:
 * ════════════════════════════════════════════════════════════════════════════
 *
 *   users/
 *   └── {uid}/
 *       ├── profile          (документ: UserProfile)
 *       ├── preferences      (документ: UserPreferences)
 *       ├── statistics/      (коллекция: DailyStatistics)
 *       │   └── {date}       (документ: dailyStats)
 *       ├── progress/        (коллекция: WordKnowledge, PhraseKnowledge)
 *       │   └── {wordId}
 *       └── backups/         (коллекция: управляется BackupManager)
 *
 * ════════════════════════════════════════════════════════════════════════════
 * СТРАТЕГИЯ СИНХРОНИЗАЦИИ:
 * ════════════════════════════════════════════════════════════════════════════
 *
 *   Push (Room → Firestore):
 *     SetOptions.merge() — частичное обновление, не перезаписывает весь документ.
 *
 *   Pull (Firestore → Room):
 *     get().await() — одноразовое чтение (восстановление на новом устройстве).
 *
 *   Real-time наблюдение (Firestore → UI):
 *     callbackFlow + addSnapshotListener.
 *     awaitClose { registration.remove() } — очистка при отмене Flow.
 *
 * ════════════════════════════════════════════════════════════════════════════
 * BATCHING — ПАКЕТНАЯ ЗАПИСЬ (решение проблемы квот):
 * ════════════════════════════════════════════════════════════════════════════
 *
 *   ПРОБЛЕМА: pushKnowledgeItem() вызывается после каждого слова в SRS.
 *   50 слов за сессию = 50 сетевых запросов = 50 записей в квоту Firestore.
 *   Бесплатная квота: 50 000 записей/день. При активном использовании — легко
 *   исчерпать. Плюс: лишний расход батареи на радио-активность.
 *
 *   РЕШЕНИЕ: enqueueKnowledgeItem() складывает изменения в pendingQueue (Map).
 *   Map по itemId гарантирует, что если слово обновилось дважды — хранится
 *   только последнее состояние (дедупликация).
 *
 *   flushPendingQueue() отправляет всё одним firestore.batch().commit().
 *   Firestore batch: максимум 500 операций за раз.
 *   Большие очереди автоматически разбиваются на чанки по BATCH_CHUNK_SIZE.
 *
 *   Вызывать flushPendingQueue() нужно один раз в endSession() или
 *   в BackupWorker при завершении работы приложения.
 *
 * ════════════════════════════════════════════════════════════════════════════
 * SECURITY RULES (Firestore):
 * ════════════════════════════════════════════════════════════════════════════
 *
 *   rules_version = '2';
 *   service cloud.firestore {
 *     match /databases/{database}/documents {
 *       match /users/{uid}/{document=**} {
 *         allow read, write: if request.auth != null && request.auth.uid == uid;
 *       }
 *     }
 *   }
 */
class CloudSyncService(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
) {
    companion object {
        private const val TAG = "CloudSyncService"

        // Firestore collection / document paths
        private const val USERS_COLLECTION      = "users"
        private const val PROFILE_DOCUMENT      = "profile"
        private const val STATISTICS_COLLECTION = "statistics"
        private const val PROGRESS_COLLECTION   = "progress"

        /**
         * Максимум операций в одном Firestore batch.
         * Лимит Firestore — 500. Берём 400 с запасом.
         */
        private const val BATCH_CHUNK_SIZE = 400
    }

    enum class SyncStatus {
        IDLE,
        SYNCING,
        SUCCESS,
        ERROR,
        OFFLINE,
    }

    // ── Batching queue ────────────────────────────────────────────────────────

    /**
     * Локальная очередь ожидающих отправки обновлений знания.
     *
     * Ключ — itemId (wordId / phraseId).
     * Значение — последняя версия данных. Если слово обновлялось несколько раз
     * за сессию, в Firestore уйдёт только итоговое состояние (дедупликация).
     *
     * Защищена [queueMutex] — enqueue и flush могут вызываться из разных корутин.
     */
    private val pendingQueue  = mutableMapOf<String, Map<String, Any>>()
    private val queueMutex    = Mutex()

    /**
     * Добавляет обновление знания в локальную очередь БЕЗ сетевого запроса.
     *
     * Заменяет прямой вызов pushKnowledgeItem() внутри сессии.
     * Сеть не используется — только память. Быстро, не расходует квоту Firestore.
     *
     * Дедупликация: повторный enqueue для того же itemId перезаписывает данные —
     * в итоге в Firestore уйдёт только последнее состояние слова за сессию.
     *
     * @param itemId  ID слова или фразы
     * @param data    Map с полями знания (из KnowledgeMapper)
     */
    suspend fun enqueueKnowledgeItem(itemId: String, data: Map<String, Any>) {
        queueMutex.withLock {
            pendingQueue[itemId] = data
        }
        Log.d(TAG, "📥 enqueued: $itemId (queue size=${pendingQueue.size})")
    }

    /**
     * Отправляет всю очередь в Firestore одним или несколькими batch-запросами.
     *
     * Вызывать ОДИН РАЗ в конце сессии (endSession) или при сохранении в фоне.
     *
     * Firestore batch лимит — 500 операций. Метод автоматически разбивает
     * очередь на чанки по [BATCH_CHUNK_SIZE] и отправляет последовательно.
     *
     * После успешного flush очередь очищается. При ошибке — очередь сохраняется,
     * следующий вызов повторит попытку (at-least-once семантика).
     *
     * @return [SyncStatus.SUCCESS] если все чанки отправлены.
     *         [SyncStatus.OFFLINE] если сеть недоступна (Firestore сохранит в кеш).
     *         [SyncStatus.ERROR]   если uid недоступен или произошла нераспознанная ошибка.
     */
    suspend fun flushPendingQueue(): SyncStatus {
        val uid = currentUid() ?: return SyncStatus.ERROR.also {
            Log.w(TAG, "flushPendingQueue: user not authenticated")
        }

        val snapshot: Map<String, Map<String, Any>> = queueMutex.withLock {
            if (pendingQueue.isEmpty()) {
                Log.d(TAG, "flushPendingQueue: queue is empty, nothing to sync")
                return SyncStatus.SUCCESS
            }
            // Копируем и очищаем атомарно под локом
            val copy = pendingQueue.toMap()
            pendingQueue.clear()
            copy
        }

        Log.d(TAG, "🚀 flushPendingQueue: sending ${snapshot.size} items in chunks of $BATCH_CHUNK_SIZE")

        return runCatching {
            val chunks = snapshot.entries.chunked(BATCH_CHUNK_SIZE)

            chunks.forEachIndexed { index, chunk ->
                val batch = firestore.batch()

                chunk.forEach { (itemId, data) ->
                    val ref = firestore
                        .collection(USERS_COLLECTION)
                        .document(uid)
                        .collection(PROGRESS_COLLECTION)
                        .document(itemId)
                    batch.set(ref, data, SetOptions.merge())
                }

                batch.commit().await()
                Log.d(TAG, "✅ batch chunk ${index + 1}/${chunks.size} committed (${chunk.size} ops)")
            }

            Log.d(TAG, "✅ flushPendingQueue: all ${snapshot.size} items synced")
            SyncStatus.SUCCESS

        }.getOrElse { e ->
            // При ошибке возвращаем данные обратно в очередь — не теряем их
            queueMutex.withLock {
                snapshot.forEach { (k, v) -> pendingQueue.putIfAbsent(k, v) }
            }
            Log.w(TAG, "⚠️ flushPendingQueue failed, items restored to queue: ${e.message}")
            SyncStatus.OFFLINE
        }
    }

    /** Количество элементов в очереди ожидающих синхронизации. */
    suspend fun pendingQueueSize(): Int = queueMutex.withLock { pendingQueue.size }

    // ── Push: Room → Firestore (одиночные операции для не-SRS данных) ─────────

    /**
     * Отправляет профиль пользователя в Firestore.
     *
     * Вызывается редко (регистрация, смена имени, уровня) — батчинг не нужен.
     */
    suspend fun pushUserProfile(data: Map<String, Any>): SyncStatus {
        val uid = currentUid() ?: return SyncStatus.ERROR.also {
            Log.w(TAG, "pushUserProfile: user not authenticated")
        }
        return runCatching {
            firestore
                .collection(USERS_COLLECTION)
                .document(uid)
                .collection("profile")
                .document(PROFILE_DOCUMENT)
                .set(data, SetOptions.merge())
                .await()

            Log.d(TAG, "✅ pushUserProfile: ${data.size} fields synced")
            SyncStatus.SUCCESS

        }.getOrElse { e ->
            Log.e(TAG, "❌ pushUserProfile failed: ${e.message}", e)
            SyncStatus.ERROR
        }
    }

    /**
     * Прямая запись одного элемента прогресса в Firestore.
     *
     * ⚠️ ВНИМАНИЕ: не вызывай этот метод в цикле по словам SRS —
     * используй [enqueueKnowledgeItem] + [flushPendingQueue] вместо этого.
     *
     * Оставлен для редких случаев: ручная синхронизация одного слова,
     * критичные данные которые нельзя откладывать до endSession.
     */
    suspend fun pushKnowledgeItem(itemId: String, data: Map<String, Any>): SyncStatus {
        val uid = currentUid() ?: return SyncStatus.ERROR

        return runCatching {
            firestore
                .collection(USERS_COLLECTION)
                .document(uid)
                .collection(PROGRESS_COLLECTION)
                .document(itemId)
                .set(data, SetOptions.merge())
                .await()

            Log.d(TAG, "✅ pushKnowledgeItem (direct): $itemId synced")
            SyncStatus.SUCCESS

        }.getOrElse { e ->
            Log.w(TAG, "⚠️ pushKnowledgeItem offline (will retry): ${e.message}")
            SyncStatus.OFFLINE
        }
    }

    /**
     * Отправляет дневную статистику сессии в Firestore.
     * Вызывается один раз в конце сессии — батчинг не нужен.
     */
    suspend fun pushDailyStatistics(date: String, data: Map<String, Any>): SyncStatus {
        val uid = currentUid() ?: return SyncStatus.ERROR

        return runCatching {
            firestore
                .collection(USERS_COLLECTION)
                .document(uid)
                .collection(STATISTICS_COLLECTION)
                .document(date)
                .set(data, SetOptions.merge())
                .await()

            Log.d(TAG, "✅ pushDailyStatistics: $date synced")
            SyncStatus.SUCCESS

        }.getOrElse { e ->
            Log.w(TAG, "⚠️ pushDailyStatistics offline: ${e.message}")
            SyncStatus.OFFLINE
        }
    }

    // ── Pull: Firestore → Room ────────────────────────────────────────────────

    /**
     * Скачивает прогресс пользователя из Firestore (одноразово).
     * Используется при первом входе на новом устройстве.
     */
    suspend fun pullKnowledgeProgress(): List<Map<String, Any>> {
        val uid = currentUid() ?: return emptyList()

        return runCatching {
            val snapshot = firestore
                .collection(USERS_COLLECTION)
                .document(uid)
                .collection(PROGRESS_COLLECTION)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                doc.data
            }.also {
                Log.d(TAG, "✅ pullKnowledgeProgress: ${it.size} items pulled")
            }

        }.getOrElse { e ->
            Log.e(TAG, "❌ pullKnowledgeProgress failed: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Скачивает дневную статистику за последние [days] дней.
     */
    suspend fun pullStatistics(days: Int = 30): List<Map<String, Any>> {
        val uid = currentUid() ?: return emptyList()

        val cutoffDate = java.time.LocalDate.now()
            .minusDays(days.toLong())
            .toString()

        return runCatching {
            val snapshot = firestore
                .collection(USERS_COLLECTION)
                .document(uid)
                .collection(STATISTICS_COLLECTION)
                .whereGreaterThanOrEqualTo("date", cutoffDate)
                .orderBy("date", Query.Direction.DESCENDING)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                doc.data
            }.also {
                Log.d(TAG, "✅ pullStatistics: ${it.size} days pulled")
            }

        }.getOrElse { e ->
            Log.e(TAG, "❌ pullStatistics failed: ${e.message}", e)
            emptyList()
        }
    }

    // ── Real-time наблюдение ──────────────────────────────────────────────────

    /**
     * Cold Flow реал-тайм обновлений прогресса из Firestore.
     *
     * Пример использования в ViewModel:
     * ```kotlin
     * cloudSyncService.observeProgress()
     *     .onEach { progressList -> updateLocalDatabase(progressList) }
     *     .catch { e -> Log.e(TAG, "Sync error", e) }
     *     .launchIn(viewModelScope)
     * ```
     */
    fun observeProgress(): Flow<List<Map<String, Any>>> = callbackFlow {
        val uid = currentUid()
        if (uid == null) {
            Log.w(TAG, "observeProgress: user not authenticated")
            close()
            return@callbackFlow
        }

        val registration: ListenerRegistration = firestore
            .collection(USERS_COLLECTION)
            .document(uid)
            .collection(PROGRESS_COLLECTION)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "observeProgress error: ${error.message}", error)
                    return@addSnapshotListener
                }
                val items = snapshot?.documents?.mapNotNull { it.data } ?: emptyList()
                trySend(items)
            }

        awaitClose {
            registration.remove()
            Log.d(TAG, "observeProgress: snapshot listener removed")
        }
    }

    /**
     * Cold Flow реал-тайм обновлений профиля пользователя.
     */
    fun observeUserProfile(): Flow<Map<String, Any>?> = callbackFlow {
        val uid = currentUid()
        if (uid == null) {
            close()
            return@callbackFlow
        }

        val registration = firestore
            .collection(USERS_COLLECTION)
            .document(uid)
            .collection("profile")
            .document(PROFILE_DOCUMENT)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "observeUserProfile error: ${error.message}")
                    return@addSnapshotListener
                }
                trySend(snapshot?.data)
            }

        awaitClose { registration.remove() }
    }

    // ── Полная синхронизация ──────────────────────────────────────────────────

    /**
     * Инициирует полную двустороннюю синхронизацию.
     *
     * Порядок:
     *   1. Сначала сбрасываем очередь батча (если есть ожидающие элементы)
     *   2. Push переданных данных через батч
     *   3. Pull из Firestore делается вызывающей стороной при необходимости
     *
     * Вызывается из BackupWorker и при восстановлении сетевого соединения.
     *
     * @return [SyncStatus.SUCCESS] если всё прошло успешно.
     */
    suspend fun syncAll(
        localProgressData: List<Pair<String, Map<String, Any>>>,
    ): SyncStatus {
        val uid = currentUid() ?: return SyncStatus.ERROR.also {
            Log.w(TAG, "syncAll: user not authenticated")
        }

        Log.d(TAG, "Starting full sync for uid=$uid, items=${localProgressData.size}")

        // Добавляем переданные данные в очередь и сбрасываем всё разом
        localProgressData.forEach { (itemId, data) ->
            enqueueKnowledgeItem(itemId, data)
        }

        return flushPendingQueue()
    }

    // ── Вспомогательные методы ────────────────────────────────────────────────

    /** UID текущего авторизованного пользователя или null. */
    private fun currentUid(): String? = auth.currentUser?.uid
}
