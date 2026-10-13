package com.routinenest.core

enum class VoiceAvailability { NEED_PERMISSION, READY, TEXT_ONLY }
object VoicePolicy {
    fun input(recognitionAvailable: Boolean, microphoneGranted: Boolean): VoiceAvailability = when {
        !recognitionAvailable -> VoiceAvailability.TEXT_ONLY
        !microphoneGranted -> VoiceAvailability.NEED_PERMISSION
        else -> VoiceAvailability.READY
    }
    fun speechOutput(engineReady: Boolean, languageSupported: Boolean, enabled: Boolean) = engineReady && languageSupported && enabled
}
