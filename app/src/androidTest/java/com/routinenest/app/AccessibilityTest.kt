package com.routinenest.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*
import android.content.res.Configuration
import java.util.Locale

class AccessibilityTest {
    @get:Rule(order = 0) val display = DeviceScreenRule()
    @get:Rule(order = 1) val compose = createAndroidComposeRule<MainActivity>()

    @Before fun dedicatedDebugData() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        check(context.packageName.endsWith(".debug"))
        val dependencies = EntryPointAccessors.fromApplication(context, DebugDataAccess::class.java)
        dependencies.scheduler().cancelAll()
        dependencies.database().dao().clearCompletions()
        dependencies.database().dao().clearRoutines()
        dependencies.preferences().language("en-GB")
        dependencies.preferences().onboardingDone()
    }

    @Test fun checklistIsOneNamedAccessibleControl() {
        compose.waitUntil(30_000) { compose.onAllNodes(hasText("Routines") and hasClickAction()).fetchSemanticsNodes().isNotEmpty() }
        compose.onNode(hasText("Routines") and hasClickAction()).performClick()
        compose.onNodeWithText("Morning rhythm").performScrollTo().performClick()
        compose.onNode(hasText("Today") and hasClickAction()).performClick()
        compose.waitUntil(10_000) { compose.onAllNodes(isToggleable() and hasText("Drink water")).fetchSemanticsNodes().size == 1 }
        val checkbox = compose.onNode(isToggleable() and hasText("Drink water"))
        checkbox.performScrollTo().assertIsOff()
        val bounds = checkbox.fetchSemanticsNode().boundsInRoot
        val density = compose.activity.resources.displayMetrics.density
        assertTrue("Accessible touch target must be at least 48dp", bounds.width >= 48 * density && bounds.height >= 48 * density)
        checkbox.performClick()
        // Room/Flow persistence updates the semantics asynchronously after the click.
        compose.waitUntil(10_000) { compose.onAllNodes(isOn() and hasText("Drink water")).fetchSemanticsNodes().size == 1 }
        checkbox.assertIsOn()
        compose.onNodeWithText("1 of 3 steps complete").performScrollTo().assertIsDisplayed()
    }

    @Test fun androidPluralSelectionUsesSingularAndPolishCategories() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        fun summary(tag: String, number: Int): String {
            val localized = context.createConfigurationContext(Configuration(context.resources.configuration).apply { setLocale(Locale.forLanguageTag(tag)) })
            return localized.resources.getQuantityString(R.plurals.routine_summary, number, number, "Daily")
        }
        assertEquals("1 step · Daily", summary("en-GB", 1))
        assertEquals("2 steps · Daily", summary("en-GB", 2))
        assertTrue(summary("pl-PL", 2).contains("kroki"))
        assertTrue(summary("pl-PL", 5).contains("kroków"))
    }
}
