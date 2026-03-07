// Путь: src/test/java/com/voicedeutsch/master/data/local/file/AudioCacheManagerTest.kt
package com.voicedeutsch.master.data.local.file

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class AudioCacheManagerTest {

    @TempDir
    lateinit var tempDir: File

    private lateinit var context: Context
    private lateinit var sut: AudioCacheManager

    @BeforeEach
    fun setUp() {
        context = mockk(relaxed = true)
        every { context.cacheDir } returns tempDir
        sut = AudioCacheManager(context)
    }

    // ── getCachedAudio ────────────────────────────────────────────────────────

    @Test
    fun getCachedAudio_fileNotCached_returnsNull() {
        val result = sut.getCachedAudio("Hund")
        assertNull(result)
    }

    @Test
    fun getCachedAudio_fileExistsAfterCache_returnsFile() {
        sut.cacheAudio("Hund", ByteArray(64))
        val result = sut.getCachedAudio("Hund")
        assertNotNull(result)
    }

    @Test
    fun getCachedAudio_returnedFile_exists() {
        sut.cacheAudio("Katze", ByteArray(32))
        val file = sut.getCachedAudio("Katze")
        assertTrue(file!!.exists())
    }

    @Test
    fun getCachedAudio_fileHasPcmExtension() {
        sut.cacheAudio("Hund", ByteArray(16))
        val file = sut.getCachedAudio("Hund")
        assertTrue(file!!.name.endsWith(".pcm"))
    }

    @Test
    fun getCachedAudio_afterEvict_returnsNull() {
        sut.cacheAudio("Hund", ByteArray(16))
        sut.evict("Hund")
        assertNull(sut.getCachedAudio("Hund"))
    }

    @Test
    fun getCachedAudio_differentWords_independent() {
        sut.cacheAudio("Hund", ByteArray(16))
        assertNotNull(sut.getCachedAudio("Hund"))
        assertNull(sut.getCachedAudio("Katze"))
    }

    // ── cacheAudio ────────────────────────────────────────────────────────────

    @Test
    fun cacheAudio_writesCorrectBytes() {
        val bytes = byteArrayOf(1, 2, 3, 4, 5)
        sut.cacheAudio("Hund", bytes)
        val file = sut.getCachedAudio("Hund")!!
        assertArrayEquals(bytes, file.readBytes())
    }

    @Test
    fun cacheAudio_overwrite_replacesContent() {
        sut.cacheAudio("Hund", byteArrayOf(1, 2, 3))
        sut.cacheAudio("Hund", byteArrayOf(9, 8, 7, 6))
        val file = sut.getCachedAudio("Hund")!!
        assertArrayEquals(byteArrayOf(9, 8, 7, 6), file.readBytes())
    }

    @Test
    fun cacheAudio_emptyBytes_createsFileWithZeroLength() {
        sut.cacheAudio("Hund", ByteArray(0))
        val file = sut.getCachedAudio("Hund")
        assertNotNull(file)
        assertEquals(0L, file!!.length())
    }

    @Test
    fun cacheAudio_createsCacheDirIfMissing() {
        val freshTempDir = File(tempDir, "fresh").apply { mkdirs() }
        every { context.cacheDir } returns freshTempDir
        val freshSut = AudioCacheManager(context)

        freshSut.cacheAudio("Wort", ByteArray(8))

        assertTrue(File(freshTempDir, "audio_cache").exists())
    }

    @Test
    fun cacheAudio_wordWithSpecialChars_sanitizesFileName() {
        sut.cacheAudio("Guten Morgen!", ByteArray(8))
        val audioCacheDir = File(tempDir, "audio_cache")
        val files = audioCacheDir.listFiles()
        assertNotNull(files)
        assertTrue(files!!.any { it.name.contains("guten_morgen_") })
    }

    @Test
    fun cacheAudio_wordWithUppercase_normalizedToLowercase() {
        sut.cacheAudio("HUND", ByteArray(8))
        val audioCacheDir = File(tempDir, "audio_cache")
        val files = audioCacheDir.listFiles()!!
        assertTrue(files.any { it.name.startsWith("hund") })
    }

    @Test
    fun cacheAudio_wordWithUmlauts_preserved() {
        sut.cacheAudio("Schüler", ByteArray(8))
        assertNotNull(sut.getCachedAudio("Schüler"))
    }

    @Test
    fun cacheAudio_wordLongerThan100chars_truncatedTo100() {
        val longWord = "a".repeat(150)
        sut.cacheAudio(longWord, ByteArray(8))
        val audioCacheDir = File(tempDir, "audio_cache")
        val files = audioCacheDir.listFiles()!!
        assertTrue(files.all { it.nameWithoutExtension.length <= 100 })
    }

    // ── evict ─────────────────────────────────────────────────────────────────

    @Test
    fun evict_existingFile_deletesIt() {
        sut.cacheAudio("Hund", ByteArray(16))
        sut.evict("Hund")
        assertNull(sut.getCachedAudio("Hund"))
    }

    @Test
    fun evict_nonExistentWord_doesNotThrow() {
        assertDoesNotThrow { sut.evict("NichtVorhanden") }
    }

    @Test
    fun evict_onlyDeletesTargetWord() {
        sut.cacheAudio("Hund", ByteArray(16))
        sut.cacheAudio("Katze", ByteArray(16))
        sut.evict("Hund")
        assertNull(sut.getCachedAudio("Hund"))
        assertNotNull(sut.getCachedAudio("Katze"))
    }

    @Test
    fun evict_sameWordTwice_doesNotThrow() {
        sut.cacheAudio("Hund", ByteArray(8))
        sut.evict("Hund")
        assertDoesNotThrow { sut.evict("Hund") }
    }

    // ── getCacheSizeBytes ─────────────────────────────────────────────────────

    @Test
    fun getCacheSizeBytes_emptyCache_returnsZero() {
        assertEquals(0L, sut.getCacheSizeBytes())
    }

    @Test
    fun getCacheSizeBytes_afterCachingOneFile_returnsByteCount() {
        sut.cacheAudio("Hund", ByteArray(128))
        assertEquals(128L, sut.getCacheSizeBytes())
    }

    @Test
    fun getCacheSizeBytes_multipleFiles_returnsSumOfSizes() {
        sut.cacheAudio("Hund", ByteArray(100))
        sut.cacheAudio("Katze", ByteArray(200))
        sut.cacheAudio("Maus", ByteArray(50))
        assertEquals(350L, sut.getCacheSizeBytes())
    }

    @Test
    fun getCacheSizeBytes_afterEvict_decreases() {
        sut.cacheAudio("Hund", ByteArray(100))
        sut.cacheAudio("Katze", ByteArray(200))
        sut.evict("Hund")
        assertEquals(200L, sut.getCacheSizeBytes())
    }

    @Test
    fun getCacheSizeBytes_afterClearAll_returnsZero() {
        sut.cacheAudio("Hund", ByteArray(100))
        sut.cacheAudio("Katze", ByteArray(200))
        sut.clearAll()
        assertEquals(0L, sut.getCacheSizeBytes())
    }

    // ── evictOlderThan ────────────────────────────────────────────────────────

    @Test
    fun evictOlderThan_oldFile_deleted() {
        sut.cacheAudio("Hund", ByteArray(16))
        val audioCacheDir = File(tempDir, "audio_cache")
        val file = audioCacheDir.listFiles()!!.first()
        val oldTimestamp = System.currentTimeMillis() - 31L * 24 * 60 * 60 * 1000
        file.setLastModified(oldTimestamp)

        sut.evictOlderThan(days = 30)

        assertNull(sut.getCachedAudio("Hund"))
    }

    @Test
    fun evictOlderThan_recentFile_notDeleted() {
        sut.cacheAudio("Katze", ByteArray(16))

        sut.evictOlderThan(days = 30)

        assertNotNull(sut.getCachedAudio("Katze"))
    }

    @Test
    fun evictOlderThan_deletesOnlyOldFiles() {
        sut.cacheAudio("Hund", ByteArray(16))
        sut.cacheAudio("Katze", ByteArray(16))

        val audioCacheDir = File(tempDir, "audio_cache")
        val hundFile = audioCacheDir.listFiles()!!.first { it.name.startsWith("hund") }
        hundFile.setLastModified(System.currentTimeMillis() - 31L * 24 * 60 * 60 * 1000)

        sut.evictOlderThan(days = 30)

        assertNull(sut.getCachedAudio("Hund"))
        assertNotNull(sut.getCachedAudio("Katze"))
    }

    @Test
    fun evictOlderThan_emptyCache_doesNotThrow() {
        assertDoesNotThrow { sut.evictOlderThan() }
    }

    @Test
    fun evictOlderThan_customDays_usesCorrectCutoff() {
        sut.cacheAudio("Hund", ByteArray(16))
        val audioCacheDir = File(tempDir, "audio_cache")
        val file = audioCacheDir.listFiles()!!.first()
        // 8 days old
        file.setLastModified(System.currentTimeMillis() - 8L * 24 * 60 * 60 * 1000)

        sut.evictOlderThan(days = 7)

        assertNull(sut.getCachedAudio("Hund"))
    }

    @Test
    fun evictOlderThan_fileExactlyAtCutoff_deleted() {
        sut.cacheAudio("Hund", ByteArray(16))
        val audioCacheDir = File(tempDir, "audio_cache")
        val file = audioCacheDir.listFiles()!!.first()
        val cutoff = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
        file.setLastModified(cutoff - 1)

        sut.evictOlderThan(days = 30)

        assertNull(sut.getCachedAudio("Hund"))
    }

    // ── clearAll ──────────────────────────────────────────────────────────────

    @Test
    fun clearAll_deletesAllFiles() {
        sut.cacheAudio("Hund", ByteArray(16))
        sut.cacheAudio("Katze", ByteArray(16))
        sut.cacheAudio("Maus", ByteArray(16))

        sut.clearAll()

        assertNull(sut.getCachedAudio("Hund"))
        assertNull(sut.getCachedAudio("Katze"))
        assertNull(sut.getCachedAudio("Maus"))
    }

    @Test
    fun clearAll_emptyCache_doesNotThrow() {
        assertDoesNotThrow { sut.clearAll() }
    }

    @Test
    fun clearAll_cacheIsEmptyAfterwards() {
        sut.cacheAudio("Hund", ByteArray(64))
        sut.clearAll()
        assertEquals(0L, sut.getCacheSizeBytes())
    }

    @Test
    fun clearAll_canCacheAgainAfterClear() {
        sut.cacheAudio("Hund", ByteArray(16))
        sut.clearAll()
        sut.cacheAudio("Hund", ByteArray(32))
        val file = sut.getCachedAudio("Hund")
        assertNotNull(file)
        assertEquals(32L, file!!.length())
    }

    // ── sanitize ─────────────────────────────────────────────────────────────

    @Test
    fun sanitize_spacesReplacedWithUnderscore() {
        sut.cacheAudio("guten morgen", ByteArray(8))
        assertNotNull(sut.getCachedAudio("guten morgen"))
    }

    @Test
    fun sanitize_sameWordDifferentCase_sameFile() {
        sut.cacheAudio("HUND", ByteArray(16))
        val file = sut.getCachedAudio("hund")
        assertNotNull(file)
    }

    @Test
    fun sanitize_digitsPreserved() {
        sut.cacheAudio("lektion1", ByteArray(8))
        assertNotNull(sut.getCachedAudio("lektion1"))
    }

    @Test
    fun sanitize_germanUmlauts_preserved() {
        listOf("ä", "ö", "ü", "ß").forEach { umlaut ->
            sut.cacheAudio(umlaut, ByteArray(4))
            assertNotNull(sut.getCachedAudio(umlaut), "Expected cached file for '$umlaut'")
        }
    }

    @Test
    fun sanitize_specialCharsReplaced_filesWithSameSanitizedName_overwrite() {
        // "Hund!" and "Hund?" sanitize to "hund_" — same file
        sut.cacheAudio("Hund!", byteArrayOf(1))
        sut.cacheAudio("Hund?", byteArrayOf(2))
        val audioCacheDir = File(tempDir, "audio_cache")
        val matching = audioCacheDir.listFiles()!!.filter { it.name.startsWith("hund_") }
        assertEquals(1, matching.size)
    }
}
