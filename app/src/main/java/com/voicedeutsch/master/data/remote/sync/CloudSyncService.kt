package com.voicedeutsch.master.data.remote.sync

/**
 * Placeholder for cloud synchronization.
 * MVP scope: local-only storage. Cloud sync planned for v2.0.
 *
 * Future implementation will use Firebase Firestore or custom backend
 * to sync user knowledge base across devices.
 */
class CloudSyncService {

    enum class SyncStatus { IDLE, SYNCING, SUCCESS, ERROR, NOT_IMPLEMENTED }

    /** Upload local changes to cloud. */
    suspend fun pushChanges(userId: String): SyncStatus {
        android.util.Log.d("CloudSyncService", "pushChanges: not implemented in v1.0")
        return SyncStatus.NOT_IMPLEMENTED
    }

    /** Download cloud changes to local DB. */
    suspend fun pullChanges(userId: String): SyncStatus {
        android.util.Log.d("CloudSyncService", "pullChanges: not implemented in v1.0")
        return SyncStatus.NOT_IMPLEMENTED
    }
}