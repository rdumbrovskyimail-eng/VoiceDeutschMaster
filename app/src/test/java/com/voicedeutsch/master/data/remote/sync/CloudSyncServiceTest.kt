// Путь: src/test/java/com/voicedeutsch/master/data/remote/sync/CloudSyncServiceTest.kt
package com.voicedeutsch.master.data.remote.sync

import app.cash.turbine.test
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.WriteBatch
import io.mockk.mockkStatic
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CloudSyncServiceTest {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var firebaseUser: FirebaseUser
    private lateinit var sut: CloudSyncService

    // Firestore mock chain
    private lateinit var usersCollection: CollectionReference
    private lateinit var userDocument: DocumentReference
    private lateinit var progressCollection: CollectionReference
    private lateinit var statisticsCollection: CollectionReference
    private lateinit var profileCollection: CollectionReference
    private lateinit var progressDocument: DocumentReference
    private lateinit var statisticsDocument: DocumentReference
    private lateinit var profileDocument: DocumentReference
    private lateinit var writeBatch: WriteBatch

    @BeforeEach
    fun setUp() {
        mockkStatic("kotlinx.coroutines.tasks.TasksKt")
        firestore = mockk(relaxed = true)
        auth = mockk(relaxed = true)
        firebaseUser = mockk(relaxed = true)

        usersCollection = mockk(relaxed = true)
        userDocument = mockk(relaxed = true)
        progressCollection = mockk(relaxed = true)
        statisticsCollection = mockk(relaxed = true)
        profileCollection = mockk(relaxed = true)
        progressDocument = mockk(relaxed = true)
        statisticsDocument = mockk(relaxed = true)
        profileDocument = mockk(relaxed = true)
        writeBatch = mockk(relaxed = true)

        every { auth.currentUser } returns firebaseUser
        every { firebaseUser.uid } returns "uid_test"

        every { firestore.collection("users") } returns usersCollection
        every { usersCollection.document("uid_test") } returns userDocument
        every { userDocument.collection("progress") } returns progressCollection
        every { userDocument.collection("statistics") } returns statisticsCollection
        every { userDocument.collection("profile") } returns profileCollection
        every { progressCollection.document(any()) } returns progressDocument
        every { statisticsCollection.document(any()) } returns statisticsDocument
        every { profileCollection.document(any()) } returns profileDocument

        every { firestore.batch() } returns writeBatch
        every { writeBatch.set(any(), any<Map<String, Any>>(), any<SetOptions>()) } returns writeBatch

        sut = CloudSyncService(firestore, auth)
    }

    // ── enqueueKnowledgeItem ──────────────────────────────────────────────────

    @Test
    fun enqueueKnowledgeItem_addsItemToQueue() = runTest {
        sut.enqueueKnowledgeItem("word_1", mapOf("level" to 2))
        assertEquals(1, sut.pendingQueueSize())
    }

    @Test
    fun enqueueKnowledgeItem_multipleItems_allQueued() = runTest {
        sut.enqueueKnowledgeItem("word_1", mapOf("level" to 1))
        sut.enqueueKnowledgeItem("word_2", mapOf("level" to 2))
        sut.enqueueKnowledgeItem("word_3", mapOf("level" to 3))
        assertEquals(3, sut.pendingQueueSize())
    }

    @Test
    fun enqueueKnowledgeItem_sameItemTwice_deduplicatesKeepsLatest() = runTest {
        sut.enqueueKnowledgeItem("word_1", mapOf("level" to 1))
        sut.enqueueKnowledgeItem("word_1", mapOf("level" to 5))
        assertEquals(1, sut.pendingQueueSize())
    }

    // ── pendingQueueSize ──────────────────────────────────────────────────────

    @Test
    fun pendingQueueSize_emptyQueue_returnsZero() = runTest {
        assertEquals(0, sut.pendingQueueSize())
    }

    @Test
    fun pendingQueueSize_afterFlush_returnsZero() = runTest {
        sut.enqueueKnowledgeItem("word_1", mapOf("level" to 1))

        val batchTask = mockk<com.google.android.gms.tasks.Task<Void>>(relaxed = true)
        every { writeBatch.commit() } returns batchTask
        coEvery { batchTask.await() } answers { null }

        sut.flushPendingQueue()
        assertEquals(0, sut.pendingQueueSize())
    }

    // ── flushPendingQueue ─────────────────────────────────────────────────────

    @Test
    fun flushPendingQueue_notAuthenticated_returnsError() = runTest {
        every { auth.currentUser } returns null

        val result = sut.flushPendingQueue()

        assertEquals(CloudSyncService.SyncStatus.ERROR, result)
    }

    @Test
    fun flushPendingQueue_emptyQueue_returnsSuccess() = runTest {
        val result = sut.flushPendingQueue()
        assertEquals(CloudSyncService.SyncStatus.SUCCESS, result)
    }

    @Test
    fun flushPendingQueue_itemsInQueue_commitsBatch() = runTest {
        sut.enqueueKnowledgeItem("word_1", mapOf("level" to 2))
        sut.enqueueKnowledgeItem("word_2", mapOf("level" to 3))

        val batchTask = mockk<com.google.android.gms.tasks.Task<Void>>(relaxed = true)
        every { writeBatch.commit() } returns batchTask
        coEvery { batchTask.await() } answers { null }

        val result = sut.flushPendingQueue()

        assertEquals(CloudSyncService.SyncStatus.SUCCESS, result)
        verify { writeBatch.commit() }
    }

    @Test
    fun flushPendingQueue_success_clearsQueue() = runTest {
        sut.enqueueKnowledgeItem("word_1", mapOf("level" to 1))

        val batchTask = mockk<com.google.android.gms.tasks.Task<Void>>(relaxed = true)
        every { writeBatch.commit() } returns batchTask
        coEvery { batchTask.await() } answers { null }

        sut.flushPendingQueue()

        assertEquals(0, sut.pendingQueueSize())
    }

    @Test
    fun flushPendingQueue_batchCommitThrows_returnsOffline() = runTest {
        sut.enqueueKnowledgeItem("word_1", mapOf("level" to 1))

        val batchTask = mockk<com.google.android.gms.tasks.Task<Void>>(relaxed = true)
        every { writeBatch.commit() } returns batchTask
        coEvery { batchTask.await() } throws RuntimeException("Network error")

        val result = sut.flushPendingQueue()

        assertEquals(CloudSyncService.SyncStatus.OFFLINE, result)
    }

    @Test
    fun flushPendingQueue_batchCommitFails_restoresItemsToQueue() = runTest {
        sut.enqueueKnowledgeItem("word_1", mapOf("level" to 1))
        sut.enqueueKnowledgeItem("word_2", mapOf("level" to 2))

        val batchTask = mockk<com.google.android.gms.tasks.Task<Void>>(relaxed = true)
        every { writeBatch.commit() } returns batchTask
        coEvery { batchTask.await() } throws RuntimeException("Firestore unavailable")

        sut.flushPendingQueue()

        // Items must be restored to queue for retry
        assertEquals(2, sut.pendingQueueSize())
    }

    @Test
    fun flushPendingQueue_largeBatch_chunkedIntoMultipleBatches() = runTest {
        // Enqueue 450 items → should produce 2 batches (400 + 50)
        repeat(450) { i ->
            sut.enqueueKnowledgeItem("word_$i", mapOf("level" to i))
        }

        val batchTask = mockk<com.google.android.gms.tasks.Task<Void>>(relaxed = true)
        every { writeBatch.commit() } returns batchTask
        coEvery { batchTask.await() } answers { null }

        val result = sut.flushPendingQueue()

        assertEquals(CloudSyncService.SyncStatus.SUCCESS, result)
        verify(exactly = 2) { writeBatch.commit() }
    }

    @Test
    fun flushPendingQueue_exactlyChunkSize_oneBatch() = runTest {
        repeat(400) { i ->
            sut.enqueueKnowledgeItem("word_$i", mapOf("level" to i))
        }

        val batchTask = mockk<com.google.android.gms.tasks.Task<Void>>(relaxed = true)
        every { writeBatch.commit() } returns batchTask
        coEvery { batchTask.await() } answers { null }

        val result = sut.flushPendingQueue()

        assertEquals(CloudSyncService.SyncStatus.SUCCESS, result)
        verify(exactly = 1) { writeBatch.commit() }
    }

    @Test
    fun flushPendingQueue_usesSetOptionsMerge() = runTest {
        sut.enqueueKnowledgeItem("word_1", mapOf("level" to 3))

        val batchTask = mockk<com.google.android.gms.tasks.Task<Void>>(relaxed = true)
        every { writeBatch.commit() } returns batchTask
        coEvery { batchTask.await() } answers { null }

        sut.flushPendingQueue()

        verify { writeBatch.set(any(), any<Map<String, Any>>(), SetOptions.merge()) }
    }

    // ── pushUserProfile ───────────────────────────────────────────────────────

    @Test
    fun pushUserProfile_notAuthenticated_returnsError() = runTest {
        every { auth.currentUser } returns null

        val result = sut.pushUserProfile(mapOf("name" to "Max"))

        assertEquals(CloudSyncService.SyncStatus.ERROR, result)
    }

    @Test
    fun pushUserProfile_success_returnsSuccess() = runTest {
        val task = mockk<com.google.android.gms.tasks.Task<Void>>(relaxed = true)
        every { profileDocument.set(any(), any<SetOptions>()) } returns task
        coEvery { task.await() } answers { null }

        val result = sut.pushUserProfile(mapOf("name" to "Max", "level" to "B1"))

        assertEquals(CloudSyncService.SyncStatus.SUCCESS, result)
    }

    @Test
    fun pushUserProfile_firestoreThrows_returnsError() = runTest {
        val task = mockk<com.google.android.gms.tasks.Task<Void>>(relaxed = true)
        every { profileDocument.set(any(), any<SetOptions>()) } returns task
        coEvery { task.await() } throws RuntimeException("Firestore error")

        val result = sut.pushUserProfile(mapOf("name" to "Max"))

        assertEquals(CloudSyncService.SyncStatus.ERROR, result)
    }

    @Test
    fun pushUserProfile_usesSetOptionsMerge() = runTest {
        val task = mockk<com.google.android.gms.tasks.Task<Void>>(relaxed = true)
        every { profileDocument.set(any(), any<SetOptions>()) } returns task
        coEvery { task.await() } answers { null }

        sut.pushUserProfile(mapOf("name" to "Max"))

        verify { profileDocument.set(any(), SetOptions.merge()) }
    }

    // ── pushKnowledgeItem ─────────────────────────────────────────────────────

    @Test
    fun pushKnowledgeItem_notAuthenticated_returnsError() = runTest {
        every { auth.currentUser } returns null

        val result = sut.pushKnowledgeItem("word_1", mapOf("level" to 2))

        assertEquals(CloudSyncService.SyncStatus.ERROR, result)
    }

    @Test
    fun pushKnowledgeItem_success_returnsSuccess() = runTest {
        val task = mockk<com.google.android.gms.tasks.Task<Void>>(relaxed = true)
        every { progressDocument.set(any(), any<SetOptions>()) } returns task
        coEvery { task.await() } answers { null }

        val result = sut.pushKnowledgeItem("word_1", mapOf("level" to 3))

        assertEquals(CloudSyncService.SyncStatus.SUCCESS, result)
    }

    @Test
    fun pushKnowledgeItem_firestoreThrows_returnsOffline() = runTest {
        val task = mockk<com.google.android.gms.tasks.Task<Void>>(relaxed = true)
        every { progressDocument.set(any(), any<SetOptions>()) } returns task
        coEvery { task.await() } throws RuntimeException("No network")

        val result = sut.pushKnowledgeItem("word_1", mapOf("level" to 3))

        assertEquals(CloudSyncService.SyncStatus.OFFLINE, result)
    }

    @Test
    fun pushKnowledgeItem_writesToCorrectDocument() = runTest {
        val task = mockk<com.google.android.gms.tasks.Task<Void>>(relaxed = true)
        every { progressDocument.set(any(), any<SetOptions>()) } returns task
        coEvery { task.await() } answers { null }

        sut.pushKnowledgeItem("word_42", mapOf("level" to 1))

        verify { progressCollection.document("word_42") }
    }

    // ── pushDailyStatistics ───────────────────────────────────────────────────

    @Test
    fun pushDailyStatistics_notAuthenticated_returnsError() = runTest {
        every { auth.currentUser } returns null

        val result = sut.pushDailyStatistics("2024-01-15", mapOf("sessions" to 3))

        assertEquals(CloudSyncService.SyncStatus.ERROR, result)
    }

    @Test
    fun pushDailyStatistics_success_returnsSuccess() = runTest {
        val task = mockk<com.google.android.gms.tasks.Task<Void>>(relaxed = true)
        every { statisticsDocument.set(any(), any<SetOptions>()) } returns task
        coEvery { task.await() } answers { null }

        val result = sut.pushDailyStatistics("2024-01-15", mapOf("sessions" to 3))

        assertEquals(CloudSyncService.SyncStatus.SUCCESS, result)
    }

    @Test
    fun pushDailyStatistics_firestoreThrows_returnsOffline() = runTest {
        val task = mockk<com.google.android.gms.tasks.Task<Void>>(relaxed = true)
        every { statisticsDocument.set(any(), any<SetOptions>()) } returns task
        coEvery { task.await() } throws RuntimeException("Offline")

        val result = sut.pushDailyStatistics("2024-01-15", mapOf("sessions" to 3))

        assertEquals(CloudSyncService.SyncStatus.OFFLINE, result)
    }

    @Test
    fun pushDailyStatistics_writesToCorrectDateDocument() = runTest {
        val task = mockk<com.google.android.gms.tasks.Task<Void>>(relaxed = true)
        every { statisticsDocument.set(any(), any<SetOptions>()) } returns task
        coEvery { task.await() } answers { null }

        sut.pushDailyStatistics("2024-06-30", mapOf("sessions" to 1))

        verify { statisticsCollection.document("2024-06-30") }
    }

    // ── pullKnowledgeProgress ─────────────────────────────────────────────────

    @Test
    fun pullKnowledgeProgress_notAuthenticated_returnsEmptyList() = runTest {
        every { auth.currentUser } returns null

        val result = sut.pullKnowledgeProgress()

        assertEquals(emptyList<Map<String, Any>>(), result)
    }

    @Test
    fun pullKnowledgeProgress_success_returnsMappedDocuments() = runTest {
        val data1 = mapOf<String, Any>("level" to 2L, "wordId" to "word_1")
        val data2 = mapOf<String, Any>("level" to 3L, "wordId" to "word_2")

        val doc1 = mockk<DocumentSnapshot>(relaxed = true) { every { data } returns data1 }
        val doc2 = mockk<DocumentSnapshot>(relaxed = true) { every { data } returns data2 }
        val snapshot = mockk<QuerySnapshot>(relaxed = true) { every { documents } returns listOf(doc1, doc2) }

        val queryTask = mockk<com.google.android.gms.tasks.Task<QuerySnapshot>>(relaxed = true)
        every { progressCollection.get() } returns queryTask
        coEvery { queryTask.await() } returns snapshot

        val result = sut.pullKnowledgeProgress()

        assertEquals(2, result.size)
        assertEquals(data1, result[0])
        assertEquals(data2, result[1])
    }

    @Test
    fun pullKnowledgeProgress_documentWithNullData_filtered() = runTest {
        val doc1 = mockk<DocumentSnapshot>(relaxed = true) { every { data } returns null }
        val doc2 = mockk<DocumentSnapshot>(relaxed = true) {
            every { data } returns mapOf<String, Any>("level" to 1L)
        }
        val snapshot = mockk<QuerySnapshot>(relaxed = true) { every { documents } returns listOf(doc1, doc2) }

        val queryTask = mockk<com.google.android.gms.tasks.Task<QuerySnapshot>>(relaxed = true)
        every { progressCollection.get() } returns queryTask
        coEvery { queryTask.await() } returns snapshot

        val result = sut.pullKnowledgeProgress()

        assertEquals(1, result.size)
    }

    @Test
    fun pullKnowledgeProgress_firestoreThrows_returnsEmptyList() = runTest {
        val queryTask = mockk<com.google.android.gms.tasks.Task<QuerySnapshot>>(relaxed = true)
        every { progressCollection.get() } returns queryTask
        coEvery { queryTask.await() } throws RuntimeException("Network error")

        val result = sut.pullKnowledgeProgress()

        assertEquals(emptyList<Map<String, Any>>(), result)
    }

    // ── pullStatistics ────────────────────────────────────────────────────────

    @Test
    fun pullStatistics_notAuthenticated_returnsEmptyList() = runTest {
        every { auth.currentUser } returns null

        val result = sut.pullStatistics()

        assertEquals(emptyList<Map<String, Any>>(), result)
    }

    @Test
    fun pullStatistics_success_returnsMappedDocuments() = runTest {
        val data = mapOf<String, Any>("date" to "2024-01-10", "sessions" to 2L)
        val doc = mockk<DocumentSnapshot>(relaxed = true) { every { this@mockk.data } returns data }
        val snapshot = mockk<QuerySnapshot>(relaxed = true) { every { documents } returns listOf(doc) }

        val query = mockk<Query>(relaxed = true)
        val queryFiltered = mockk<Query>(relaxed = true)
        val queryOrdered = mockk<Query>(relaxed = true)
        val queryTask = mockk<com.google.android.gms.tasks.Task<QuerySnapshot>>(relaxed = true)

        every { statisticsCollection.whereGreaterThanOrEqualTo("date", any<String>()) } returns query
        every { query.orderBy("date", Query.Direction.DESCENDING) } returns queryOrdered
        every { queryOrdered.get() } returns queryTask
        coEvery { queryTask.await() } returns snapshot

        val result = sut.pullStatistics(days = 30)

        assertEquals(1, result.size)
        assertEquals(data, result[0])
    }

    @Test
    fun pullStatistics_firestoreThrows_returnsEmptyList() = runTest {
        val query = mockk<Query>(relaxed = true)
        val queryOrdered = mockk<Query>(relaxed = true)
        val queryTask = mockk<com.google.android.gms.tasks.Task<QuerySnapshot>>(relaxed = true)

        every { statisticsCollection.whereGreaterThanOrEqualTo("date", any<String>()) } returns query
        every { query.orderBy("date", Query.Direction.DESCENDING) } returns queryOrdered
        every { queryOrdered.get() } returns queryTask
        coEvery { queryTask.await() } throws RuntimeException("Offline")

        val result = sut.pullStatistics()

        assertEquals(emptyList<Map<String, Any>>(), result)
    }

    // ── observeProgress ───────────────────────────────────────────────────────

    @Test
    fun observeProgress_notAuthenticated_closesFlow() = runTest {
        every { auth.currentUser } returns null

        sut.observeProgress().test {
            awaitComplete()
        }
    }

    @Test
    fun observeProgress_snapshotReceived_emitsItems() = runTest {
        val registration = mockk<ListenerRegistration>(relaxed = true)
        val listenerSlot = slot<com.google.firebase.firestore.EventListener<QuerySnapshot>>()

        every {
            progressCollection.addSnapshotListener(capture(listenerSlot))
        } returns registration

        sut.observeProgress().test {
            val data = mapOf<String, Any>("level" to 1L)
            val doc = mockk<DocumentSnapshot>(relaxed = true) { every { this@mockk.data } returns data }
            val snapshot = mockk<QuerySnapshot>(relaxed = true) { every { documents } returns listOf(doc) }

            listenerSlot.captured.onEvent(snapshot, null)

            val items = awaitItem()
            assertEquals(1, items.size)
            assertEquals(data, items[0])

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun observeProgress_errorFromFirestore_doesNotEmit() = runTest {
        val registration = mockk<ListenerRegistration>(relaxed = true)
        val listenerSlot = slot<com.google.firebase.firestore.EventListener<QuerySnapshot>>()

        every {
            progressCollection.addSnapshotListener(capture(listenerSlot))
        } returns registration

        sut.observeProgress().test {
            val firestoreException = mockk<FirebaseFirestoreException>(relaxed = true)
            listenerSlot.captured.onEvent(null, firestoreException)

            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun observeProgress_cancelled_removesListener() = runTest {
        val registration = mockk<ListenerRegistration>(relaxed = true)
        every { progressCollection.addSnapshotListener(any()) } returns registration

        sut.observeProgress().test {
            cancelAndIgnoreRemainingEvents()
        }

        verify { registration.remove() }
    }

    @Test
    fun observeProgress_nullSnapshotData_emitsEmptyList() = runTest {
        val registration = mockk<ListenerRegistration>(relaxed = true)
        val listenerSlot = slot<com.google.firebase.firestore.EventListener<QuerySnapshot>>()

        every {
            progressCollection.addSnapshotListener(capture(listenerSlot))
        } returns registration

        sut.observeProgress().test {
            listenerSlot.captured.onEvent(null, null)

            val items = awaitItem()
            assertEquals(emptyList<Map<String, Any>>(), items)

            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── observeUserProfile ────────────────────────────────────────────────────

    @Test
    fun observeUserProfile_notAuthenticated_closesFlow() = runTest {
        every { auth.currentUser } returns null

        sut.observeUserProfile().test {
            awaitComplete()
        }
    }

    @Test
    fun observeUserProfile_snapshotReceived_emitsData() = runTest {
        val registration = mockk<ListenerRegistration>(relaxed = true)
        val listenerSlot = slot<com.google.firebase.firestore.EventListener<DocumentSnapshot>>()

        every { profileDocument.addSnapshotListener(capture(listenerSlot)) } returns registration

        sut.observeUserProfile().test {
            val profileData = mapOf<String, Any>("name" to "Max", "level" to "B1")
            val docSnapshot = mockk<DocumentSnapshot>(relaxed = true) {
                every { data } returns profileData
            }

            listenerSlot.captured.onEvent(docSnapshot, null)

            val result = awaitItem()
            assertEquals(profileData, result)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun observeUserProfile_nullSnapshot_emitsNull() = runTest {
        val registration = mockk<ListenerRegistration>(relaxed = true)
        val listenerSlot = slot<com.google.firebase.firestore.EventListener<DocumentSnapshot>>()

        every { profileDocument.addSnapshotListener(capture(listenerSlot)) } returns registration

        sut.observeUserProfile().test {
            listenerSlot.captured.onEvent(null, null)

            val result = awaitItem()
            assertNull(result)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun observeUserProfile_cancelled_removesListener() = runTest {
        val registration = mockk<ListenerRegistration>(relaxed = true)
        every { profileDocument.addSnapshotListener(any()) } returns registration

        sut.observeUserProfile().test {
            cancelAndIgnoreRemainingEvents()
        }

        verify { registration.remove() }
    }

    // ── syncAll ───────────────────────────────────────────────────────────────

    @Test
    fun syncAll_notAuthenticated_returnsError() = runTest {
        every { auth.currentUser } returns null

        val result = sut.syncAll(listOf("word_1" to mapOf("level" to 1)))

        assertEquals(CloudSyncService.SyncStatus.ERROR, result)
    }

    @Test
    fun syncAll_emptyList_returnsSuccess() = runTest {
        val result = sut.syncAll(emptyList())
        assertEquals(CloudSyncService.SyncStatus.SUCCESS, result)
    }

    @Test
    fun syncAll_validItems_enqueuedAndFlushed() = runTest {
        val batchTask = mockk<com.google.android.gms.tasks.Task<Void>>(relaxed = true)
        every { writeBatch.commit() } returns batchTask
        coEvery { batchTask.await() } answers { null }

        val items = listOf(
            "word_1" to mapOf<String, Any>("level" to 1),
            "word_2" to mapOf<String, Any>("level" to 2),
        )

        val result = sut.syncAll(items)

        assertEquals(CloudSyncService.SyncStatus.SUCCESS, result)
        verify { writeBatch.commit() }
    }

    @Test
    fun syncAll_batchFails_returnsOfflineAndRestoresQueue() = runTest {
        val batchTask = mockk<com.google.android.gms.tasks.Task<Void>>(relaxed = true)
        every { writeBatch.commit() } returns batchTask
        coEvery { batchTask.await() } throws RuntimeException("Network down")

        val items = listOf(
            "word_1" to mapOf<String, Any>("level" to 1),
            "word_2" to mapOf<String, Any>("level" to 2),
        )

        val result = sut.syncAll(items)

        assertEquals(CloudSyncService.SyncStatus.OFFLINE, result)
        assertEquals(2, sut.pendingQueueSize())
    }

    @Test
    fun syncAll_mergesWithExistingQueue() = runTest {
        sut.enqueueKnowledgeItem("word_existing", mapOf("level" to 3))

        val batchTask = mockk<com.google.android.gms.tasks.Task<Void>>(relaxed = true)
        every { writeBatch.commit() } returns batchTask
        coEvery { batchTask.await() } answers { null }

        val result = sut.syncAll(listOf("word_new" to mapOf("level" to 1)))

        assertEquals(CloudSyncService.SyncStatus.SUCCESS, result)
        // All items flushed — queue is empty
        assertEquals(0, sut.pendingQueueSize())
    }

    // ── SyncStatus enum ───────────────────────────────────────────────────────

    @Test
    fun syncStatus_allValuesPresent() {
        val values = CloudSyncService.SyncStatus.entries
        assertTrue(values.contains(CloudSyncService.SyncStatus.IDLE))
        assertTrue(values.contains(CloudSyncService.SyncStatus.SYNCING))
        assertTrue(values.contains(CloudSyncService.SyncStatus.SUCCESS))
        assertTrue(values.contains(CloudSyncService.SyncStatus.ERROR))
        assertTrue(values.contains(CloudSyncService.SyncStatus.OFFLINE))
        assertEquals(5, values.size)
    }
}
