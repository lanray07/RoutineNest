package com.routinenest.app

import android.graphics.Bitmap
import android.content.res.Configuration
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.routinenest.app.data.*
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.time.LocalDate
import java.util.Locale

/** Run only against the dedicated .debug package; never a user's release database. */
class ScreenshotCaptureTest {
    @get:Rule(order = 0) val display = DeviceScreenRule()
    @get:Rule(order = 1) val compose = createAndroidComposeRule<MainActivity>()
    @Test fun captureStoreScreens() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        check(context.packageName.endsWith(".debug"))
        val args = InstrumentationRegistry.getArguments()
        val locale = args.getString("locale") ?: "en-GB"
        val dependencies = EntryPointAccessors.fromApplication(context, DebugDataAccess::class.java)
        val localized = context.createConfigurationContext(Configuration(context.resources.configuration).apply { setLocale(Locale.forLanguageTag(locale)) })
        val db = dependencies.database()
        val today = LocalDate.now()
        runBlocking {
            dependencies.preferences().onboardingDone(); dependencies.preferences().language(locale); dependencies.preferences().theme("light"); dependencies.preferences().speak(false)
            dependencies.scheduler().cancelAll()
            db.dao().clearCompletions(); db.dao().clearRoutines()
            listOf(R.string.morning to listOf(R.string.drink_water,R.string.stretch,R.string.plan_day), R.string.evening to listOf(R.string.reading,R.string.tidy,R.string.reflect), R.string.focus to listOf(R.string.clear_desk,R.string.focus_work,R.string.take_break)).forEachIndexed { index, (nameId, taskIds) ->
                val name = localized.getString(nameId)
                val tasks = taskIds.map { localized.getString(it) }
                val start = today.minusDays(13L - index).toString()
                val id = "demo-routine-$index"
                db.dao().upsertRoutine(RoutineEntity(id,name,"1,2,3,4,5,6,7",start,reminder = if (index == 0) "08:00" else ""))
                db.dao().upsertState(RoutineStateEntity(id,start,"1,2,3,4,5,6,7",false,false))
                db.dao().upsertTasks(tasks.mapIndexed { pos, title -> TaskEntity("demo-task-$index-$pos",id,title,pos,start) })
                for (offset in 1L..6L) if (offset != 4L) tasks.forEachIndexed { pos, title -> db.dao().complete(CompletionEntity("demo-task-$index-$pos",id,today.minusDays(offset).toString(),title)) }
                if (index == 0) db.dao().complete(CompletionEntity("demo-task-0-0",id,today.toString(),tasks.first()))
            }
        }
        lateinit var model: NestViewModel
        compose.runOnUiThread { compose.activity.window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); model = ViewModelProvider(compose.activity)[NestViewModel::class.java] }
        compose.waitUntil(10_000) { model.routines.value.size == 3 && model.completions.value.size == 46 && model.prefs.value.onboarded && model.prefs.value.language == locale }
        val output = File(context.getExternalFilesDir(null), "screenshots/$locale").apply { mkdirs() }
        listOf("today","today","voice","library","progress","library","library","settings").forEachIndexed { index, screen ->
            runBlocking { dependencies.preferences().theme(if (index == 6) "dark" else "light") }
            compose.waitUntil(10_000) { model.prefs.value.theme == if (index == 6) "dark" else "light" }
            compose.runOnUiThread { model.screen.value = screen }
            compose.waitForIdle()
            val bitmap = instrumentation.uiAutomation.takeScreenshot() ?: error("Screenshot unavailable")
            val colors = (0 until bitmap.height step 40).flatMap { y -> (0 until bitmap.width step 40).map { x -> bitmap.getPixel(x,y) } }.toSet()
            check(colors.size > 5) { "Blank capture: check the device display and app visibility." }
            File(output,"${(index+1).toString().padStart(2,'0')}.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) }; bitmap.recycle()
        }
        File(output,"seed-date.txt").writeText(today.toString())
    }
}
