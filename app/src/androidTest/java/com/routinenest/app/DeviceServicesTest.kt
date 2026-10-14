package com.routinenest.app

import android.app.NotificationManager
import android.content.Context
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.WorkManager
import com.routinenest.app.reminders.ReminderDependencies
import dagger.hilt.android.EntryPointAccessors
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.time.ZonedDateTime
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Uses the real device services; never selects fixture notification or speech engines. */
class DeviceServicesTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext

    @Test fun scheduledWorkPostsAnActualNotification() {
        check(context.packageName.endsWith(".debug"))
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        assertTrue("Enable notifications for the dedicated debug package before testing", manager.areNotificationsEnabled())
        val original = manager.activeNotifications.map { it.id }.toSet()
        val scheduler = EntryPointAccessors.fromApplication(context, ReminderDependencies::class.java).scheduler()
        val whenDue = ZonedDateTime.now().plusSeconds(5)
        try {
            scheduler.once(whenDue)
            val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(60)
            while (System.nanoTime() < deadline && manager.activeNotifications.none { it.id !in original && it.notification.channelId == "routines" }) Thread.sleep(250)
            assertTrue("A real WorkManager reminder must reach NotificationManager", manager.activeNotifications.any { it.id !in original && it.notification.channelId == "routines" })
        } finally {
            WorkManager.getInstance(context).cancelUniqueWork("voice-reminder-${whenDue.toEpochSecond()}")
            manager.activeNotifications.filter { it.id !in original }.forEach { manager.cancel(it.id) }
        }
    }

    @Test fun englishDeviceTtsSynthesizesAudio() {
        val initialized = CountDownLatch(1)
        val finished = CountDownLatch(1)
        var initStatus = TextToSpeech.ERROR
        var outputStatus = TextToSpeech.ERROR
        var engine: TextToSpeech? = null
        val output = File(context.cacheDir, "device-tts-test.wav")
        try {
            instrumentation.runOnMainSync { engine = TextToSpeech(context) { initStatus = it; initialized.countDown() } }
            assertTrue("Device TTS initialization timed out", initialized.await(20, TimeUnit.SECONDS))
            assertEquals("Device must have a working TTS engine", TextToSpeech.SUCCESS, initStatus)
            instrumentation.runOnMainSync {
                assertTrue("English voice data is required", engine!!.setLanguage(Locale.UK) >= TextToSpeech.LANG_AVAILABLE)
                engine!!.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(id: String?) {}
                    override fun onDone(id: String?) { outputStatus = TextToSpeech.SUCCESS; finished.countDown() }
                    @Deprecated("Legacy callback required by platform")
                    override fun onError(id: String?) { finished.countDown() }
                })
                assertEquals(TextToSpeech.SUCCESS, engine!!.synthesizeToFile("Your routine is ready.", null, output, "device-tts-test"))
            }
            assertTrue("Device speech synthesis timed out", finished.await(30, TimeUnit.SECONDS))
            assertEquals(TextToSpeech.SUCCESS, outputStatus)
            assertTrue("Synthesis must produce audio beyond the WAV header", output.length() > 44)
            println("DEVICE_SERVICES tts_engine=${engine!!.defaultEngine} audio_bytes=${output.length()} recognition_available=${SpeechRecognizer.isRecognitionAvailable(context)}")
        } finally {
            instrumentation.runOnMainSync { engine?.stop(); engine?.shutdown() }
            output.delete()
        }
    }
}
