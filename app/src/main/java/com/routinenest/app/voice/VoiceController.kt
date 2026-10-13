package com.routinenest.app.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.*
import android.speech.tts.TextToSpeech
import java.util.Locale
import com.routinenest.core.VoicePolicy

class VoiceController(private val context: Context, private val onText: (String) -> Unit, private val onStatus: (String) -> Unit) {
    val available: Boolean get() = SpeechRecognizer.isRecognitionAvailable(context)
    private var recognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var language = Locale.UK
    init {
        tts = TextToSpeech(context) { status ->
            ttsReady = status == TextToSpeech.SUCCESS
            if (ttsReady) ttsReady = (tts?.setLanguage(language) ?: TextToSpeech.LANG_NOT_SUPPORTED) >= 0
        }
    }
    fun listen(locale: Locale) {
        if (!available) { onStatus("Speech recognition is unavailable. Use text commands."); return }
        language = locale
        recognizer?.destroy()
        try { recognizer = SpeechRecognizer.createSpeechRecognizer(context).also { speech ->
            speech.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) { onStatus("Listening…") }
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() { onStatus("Recognising…") }
                override fun onError(error: Int) { onStatus(when (error) { SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission denied. Use text commands."; SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech recognised. Try again or type a command."; else -> "Recognition failed ($error). Use text commands." }) }
                override fun onResults(results: Bundle?) { val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull(); if (text != null) { onText(text); onStatus("Command recognised. Review it before sending.") } else onStatus("No speech recognised.") }
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
            speech.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM).putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale.toLanguageTag()))
        } } catch (error: RuntimeException) { onStatus("Recognition could not start. Use text commands.") }
    }
    fun speak(text: String, locale: Locale) {
        if (!VoicePolicy.speechOutput(ttsReady, tts?.setLanguage(locale) in 0..2, true)) { onStatus("Text-to-speech is unavailable for this language. The response is shown on screen."); return }
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "nest-response")
    }
    fun stop() { recognizer?.cancel(); tts?.stop() }
    fun close() { recognizer?.destroy(); tts?.stop(); tts?.shutdown() }
}
