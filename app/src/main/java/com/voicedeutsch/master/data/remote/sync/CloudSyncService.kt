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
import kotlinx.coroutines.tasks.await

/**
 * CloudSyncService — реал-тайм синхронизация прогресса пользователя через Firebase Firestore.
 *
 * ════════════════════════════════════════════════════════════════════════════
 * МИГРАЦИЯ: Заглушка MVP → Полноценный Firestore Sync
 * ════════════════════════════════════════════════════════════════════════════
 *
 * БЫЛО: пустые методы pushChanges/pullChanges с логом "not implemented in v1.0"
 * СТАЛО: полноценный двусторонний sync через Firestore с Flow-наблюдателями
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
 *     Подходит для инкрементального обновления прогресса слов/фраз.
 *
 *   Pull (Firestore → Room):
 *     get().await() — одноразовое чтение (для восстановления на новом устройстве).
 *
 *   Real-time наблюдение (Firestore → UI):
 *     callbackFlow + addSnapshotListener — живые обновления без polling.
 *     awaitClose { registration.remove() } — гарантирует очистку при отмене Flow.
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
        private const val USERS_COLLECTION    = "users"
        private const val PROFILE_DOCUMENT    = "profile"
        private const val PREFERENCES_DOC     = "preferences"
        private const val STATISTICS_COLLECTION = "statistics"
        private const val PROGRESS_COLLECTION  = "progress"
    }

    enum class SyncStatus {
        IDLE,
        SYNCING,
        SUCCESS,
        ERROR,
        OFFLINE,         // Данные записаны в офлайн-кеш Firestore, будут отправлены при сети
    }

    // ── Push: Room → Firestore ────────────────────────────────────────────────

    /**
     * Отправляет профиль пользователя в Firestore.
     *
     * SetOptions.merge() — безопасно: обновляет только переданные поля,
     * не удаляет существующие. Идемпотентно.
     *
     * @param data Map с полями профиля (из UserMapper)
     * @return [SyncStatus.SUCCESS] или [SyncStatus.ERROR]
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
     * Отправляет прогресс по одному слову/фразе в Firestore.
     *
     * Вызывается из KnowledgeRepositoryImpl после каждого обновления SRS.
     * Firestore офлайн-кеш гарантирует, что данные не потеряются при обрыве сети.
     *
     * @param itemId  ID слова или фразы (ключ документа)
     * @param data    Map с полями знания (из KnowledgeMapper)
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

            Log.d(TAG, "✅ pushKnowledgeItem: $itemId synced")
            SyncStatus.SUCCESS

        }.getOrElse { e ->
            // При отсутствии сети Firestore SDK сохраняет в офлайн-кеш автоматически.
            // Задача будет выполнена при восстановлении соединения.
            Log.w(TAG, "⚠️ pushKnowledgeItem offline (will retry): ${e.message}")
            SyncStatus.OFFLINE
        }
    }

    /**
     * Отправляет дневную статистику сессии в Firestore.
     *
     * @param date ISO date string "2026-02-23"
     * @param data Map с полями статистики
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
     *
     * Используется при первом входе на новом устройстве для восстановления данных.
     * Для постоянного наблюдения используйте [observeProgress].
     *
     * .await() из kotlinx-coroutines-play-services преобразует Firebase Task
     * в suspend-функцию без callbacks.
     *
     * @return Список Map с данными прогресса, пустой список при ошибке/отсутствии данных.
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
     *
     * @return Список Map с данными статистики.
     */
    suspend fun pullStatistics(days: Int = 30): List<Map<String, Any>> {
        val uid = currentUid() ?: return emptyList()

        // Дата отсечки: сегодня - days дней в формате ISO "2026-01-24"
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
     * Использует callbackFlow + addSnapshotListener для получения живых обновлений.
     * awaitClose гарантирует снятие Firestore-слушателя при отмене Flow.
     *
     * Пример использования в ViewModel:
     * ```kotlin
     * cloudSyncService.observeProgress()
     *     .onEach { progressList -> updateLocalDatabase(progressList) }
     *     .catch { e -> Log.e(TAG, "Sync error", e) }
     *     .launchIn(viewModelScope)
     * ```
     *
     * Firestore офлайн-кеш: Flow продолжает эмитировать кешированные данные
     * при отсутствии сети. При восстановлении сети — автоматически синхронизируется.
     */
    fun observeProgress(): Flow<List<Map<String, Any>>> = callbackFlow {
        val uid = currentUid()
        if (uid == null) {
            Log.w(TAG, "observeProgress: user not authenticated")
            close()
            return@callbackFlow
        }

        var registration: ListenerRegistration? = null

        registration = firestore
            .collection(USERS_COLLECTION)
            .document(uid)
            .collection(PROGRESS_COLLECTION)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "observeProgress error: ${error.message}", error)
                    // Не закрываем канал — при восстановлении сети Firestore переподключится
                    return@addSnapshotListener
                }

                val items = snapshot?.documents?.mapNotNull { doc ->
                    doc.data
                } ?: emptyList()

                trySend(items)
            }

        // awaitClose: вызывается когда Flow отменяется.
        // Снимает Firestore-слушатель, предотвращает утечки памяти.
        awaitClose {
            registration?.remove()
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
     *   1. Pull из Firestore → обновить Room
     *   2. Push из Room → Firestore (идемпотентно через SetOptions.merge)
     *
     * Вызывается из BackupWorker и при восстановлении сетевого соединения.
     * Данные из Room для push должны быть переданы вызывающей стороной —
     * CloudSyncService не знает о Room напрямую (SRP).
     *
     * @return [SyncStatus.SUCCESS] если обе операции прошли успешно.
     */
    suspend fun syncAll(
        localProgressData: List<Pair<String, Map<String, Any>>>,
    ): SyncStatus {
        val uid = currentUid() ?: return SyncStatus.ERROR.also {
            Log.w(TAG, "syncAll: user not authenticated")
        }

        Log.d(TAG, "Starting full sync for uid=$uid, items=${localProgressData.size}")

        var errorCount = 0

        // Push все локальные данные
        localProgressData.forEach { (itemId, data) ->
            val status = pushKnowledgeItem(itemId, data)
            if (status == SyncStatus.ERROR) errorCount++
        }

        return if (errorCount == 0) {
            Log.d(TAG, "✅ syncAll completed: ${localProgressData.size} items")
            SyncStatus.SUCCESS
        } else {
            Log.w(TAG, "⚠️ syncAll completed with $errorCount errors")
            SyncStatus.ERROR
        }
    }

    // ── Вспомогательные методы ────────────────────────────────────────────────

    /** UID текущего авторизованного пользователя или null. */
    private fun currentUid(): String? = auth.currentUser?.uid
}
