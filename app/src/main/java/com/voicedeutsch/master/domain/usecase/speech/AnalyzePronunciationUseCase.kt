package com.voicedeutsch.master.domain.usecase.speech

import com.voicedeutsch.master.domain.model.speech.PhoneticTarget
import com.voicedeutsch.master.domain.model.speech.PronunciationTrend
import com.voicedeutsch.master.domain.repository.KnowledgeRepository

/**
 * Analyzes pronunciation history and identifies problematic sounds.
 *
 * Architecture line 922 (AnalyzePronunciationUseCase.kt).
 *
 * Weak point criteria:
 *   - score < 0.5 after > 5 attempts
 *   - trend STABLE or DECLINING
 */
class AnalyzePronunciationUseCase(
    private val knowledgeRepository: KnowledgeRepository,
) {

    suspend operator fun invoke(userId: String): List<PhoneticTarget> {
        val records = knowledgeRepository.getRecentPronunciationRecords(userId, limit = 200)
        if (records.isEmpty()) return emptyList()

        // Group by problem sounds
        val soundStats = mutableMapOf<String, SoundAccumulator>()
        records.forEach { record ->
            record.problemSounds.forEach { sound ->
                val acc = soundStats.getOrPut(sound) { SoundAccumulator() }
                acc.totalAttempts++
                acc.scores.add(record.score)
                acc.words.add(record.word)
                acc.lastPracticed = maxOf(acc.lastPracticed, record.timestamp)
            }
        }

        return soundStats.map { (sound, acc) ->
            val avgScore = acc.scores.average().toFloat()
            val recentScores = acc.scores.takeLast(5)
            val trend = when {
                recentScores.size < 3 -> PronunciationTrend.STABLE
                recentScores.takeLast(3).average() > recentScores.take(3).average() + 0.1 ->
                    PronunciationTrend.IMPROVING
                recentScores.takeLast(3).average() < recentScores.take(3).average() - 0.1 ->
                    PronunciationTrend.DECLINING
                else -> PronunciationTrend.STABLE
            }
            PhoneticTarget(
                sound = sound,
                ipa = mapToIPA(sound),
                detectionDate = acc.lastPracticed,
                totalAttempts = acc.totalAttempts,
                successfulAttempts = acc.scores.count { it >= 0.7f },
                currentScore = avgScore,
                trend = trend,
                lastPracticed = acc.lastPracticed,
                inWords = acc.words.distinct().take(10),
            )
        }.filter { it.currentScore < 0.7f }
            .sortedBy { it.currentScore }
    }

    private data class SoundAccumulator(
        var totalAttempts: Int = 0,
        val scores: MutableList<Float> = mutableListOf(),
        val words: MutableList<String> = mutableListOf(),
        var lastPracticed: Long = 0,
    )

    private fun mapToIPA(sound: String): String = when (sound.lowercase()) {
        "ü" -> "[yː]"; "ö" -> "[øː]"; "ä" -> "[ɛː]"
        "sch" -> "[ʃ]"; "ch" -> "[ç]/[x]"; "sp" -> "[ʃp]"; "st" -> "[ʃt]"
        "ei" -> "[aɪ]"; "eu", "äu" -> "[ɔʏ]"; "ie" -> "[iː]"
        "z" -> "[ts]"; "w" -> "[v]"; "v" -> "[f]"; "j" -> "[j]"
        "r" -> "[ʁ]"; "ß" -> "[s]"
        else -> "[$sound]"
    }
}