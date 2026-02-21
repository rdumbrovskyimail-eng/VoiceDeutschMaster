package com.voicedeutsch.master.data.local.file

import android.content.Context
import java.io.File

/**
 * Manages cached TTS audio files for frequently used words/phrases.
 * Reduces Gemini API calls for repeated pronunciation demos.
 */
class AudioCacheManager(
    private val context: Context
) {
    private val cacheDir by lazy {
        File(context.cacheDir, "audio_cache").also { it.mkdirs() }
    }

    /** Returns cached audio file for a word, or null if not cached. */
    fun getCachedAudio(word: String): File? {
        val file = File(cacheDir, "${word.sanitize()}.pcm")
        return if (file.exists()) file else null
    }

    /** Saves audio bytes to cache for a word. */
    fun cacheAudio(word: String, audioBytes: ByteArray) {
        val file = File(cacheDir, "${word.sanitize()}.pcm")
        file.writeBytes(audioBytes)
    }

    /** Removes a specific cached audio file. */
    fun evict(word: String) {
        File(cacheDir, "${word.sanitize()}.pcm").delete()
    }

    /** Returns total cache size in bytes. */
    fun getCacheSizeBytes(): Long {
        return cacheDir.listFiles()?.sumOf { it.length() } ?: 0L
    }

    /** Clears audio files not accessed in [days] days. */
    fun evictOlderThan(days: Int = 30) {
        val cutoff = System.currentTimeMillis() - days * 24 * 60 * 60 * 1000L
        cacheDir.listFiles()?.filter { it.lastModified() < cutoff }?.forEach { it.delete() }
    }

    /** Clears all cached audio. */
    fun clearAll() {
        cacheDir.listFiles()?.forEach { it.delete() }
    }

    private fun String.sanitize(): String =
        this.lowercase().replace(Regex("[^a-zäöüß0-9]"), "_").take(100)
}