package com.routinenest.core

import org.junit.Assert.*
import org.junit.Test

class VoicePolicyTest {
    @Test fun unavailableRecognitionUsesText() { assertEquals(VoiceAvailability.TEXT_ONLY, VoicePolicy.input(false, false)); assertEquals(VoiceAvailability.TEXT_ONLY, VoicePolicy.input(false, true)) }
    @Test fun denialDoesNotStartRecognition() { assertEquals(VoiceAvailability.NEED_PERMISSION, VoicePolicy.input(true, false)) }
    @Test fun permittedRecognitionStarts() { assertEquals(VoiceAvailability.READY, VoicePolicy.input(true, true)) }
    @Test fun noTtsEngineUsesVisibleText() { assertFalse(VoicePolicy.speechOutput(false, true, true)) }
    @Test fun unsupportedTtsLanguageUsesVisibleText() { assertFalse(VoicePolicy.speechOutput(true, false, true)) }
    @Test fun disabledTtsDoesNotSpeak() { assertFalse(VoicePolicy.speechOutput(true, true, false)); assertTrue(VoicePolicy.speechOutput(true, true, true)) }
}
