package com.voicedeutsch.master.data.remote.sync

/**
 * Placeholder for cloud synchronization.
 * MVP scope: local-only storage. Cloud sync planned for v2.0.
 *
 * Future implementation will use Firebase Firestore or custom backend
 * to sync user knowledge base across devices.
 */
class CloudSyncService {

    enum class SyncStatus { IDLE, SYNCING, SUCCESS, ERROR }

    /** Upload local changes to cloud. */
    suspend fun pushChanges(userId: String): SyncStatus {
        // TODO: Implement for v2.0
        return SyncStatus.IDLE
    }

    /** Download cloud changes to local DB. */
    suspend fun pullChanges(userId: String): SyncStatus {
        // TODO: Implement for v2.0
        return SyncStatus.IDLE
    }
}