package com.voicedeutsch.master.voicecore.engine

data class VoiceFunctionCall(
    val id: String,
    val name: String,
    val argsJson: String,
)

data class VoiceResponse(
    val audioData: ByteArray?,
    val transcript: String?,
    val functionCall: VoiceFunctionCall?,
    val isTurnComplete: Boolean = false,
) {
    fun hasAudio() = audioData != null && audioData.isNotEmpty()
    fun hasFunctionCall() = functionCall != null
    fun hasTranscript() = !transcript.isNullOrEmpty() && audioData == null && functionCall == null
    override fun equals(other: Any?) = false
    override fun hashCode() = transcript.hashCode() * 31 + isTurnComplete.hashCode()
}