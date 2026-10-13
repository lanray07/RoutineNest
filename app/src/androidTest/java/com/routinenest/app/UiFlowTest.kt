package com.routinenest.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.Before
import androidx.test.platform.app.InstrumentationRegistry
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.runBlocking
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UiFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Before fun resetDedicatedDebugData() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        check(context.packageName.endsWith(".debug"))
        val dependencies = EntryPointAccessors.fromApplication(context, DebugDataAccess::class.java)
        dependencies.scheduler().cancelAll()
        dependencies.database().dao().clearCompletions(); dependencies.database().dao().clearRoutines()
        dependencies.preferences().language("en-GB"); dependencies.preferences().theme("light")
    }
    @Test fun onboardingTemplateChecklistAndNavigation() {
        compose.waitForIdle()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Build my first routine").fetchSemanticsNodes().isNotEmpty() || compose.onAllNodesWithText("Routines").fetchSemanticsNodes().isNotEmpty() }
        if (compose.onAllNodesWithText("Build my first routine").fetchSemanticsNodes().isNotEmpty()) compose.onNodeWithText("Build my first routine").performClick()
        compose.onNode(hasText("Routines") and hasClickAction()).performClick()
        compose.onNodeWithText("Morning rhythm").performScrollTo().performClick()
        compose.onNode(hasText("Today") and hasClickAction()).performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Drink water").fetchSemanticsNodes().isNotEmpty() }
        compose.onAllNodes(isToggleable()).onFirst().performScrollTo().performClick()
        compose.onNodeWithText("1 of 3 steps complete").assertIsDisplayed()
        compose.onNode(hasText("Assistant") and hasClickAction()).performClick()
        compose.onNodeWithText("Type a command").performTextInput("What is left on my list?")
        compose.onNodeWithText("Send command").performClick()
        compose.onNode(hasText("Settings") and hasClickAction()).performClick()
        compose.onNodeWithText("Dark").performClick()
        compose.onNodeWithText("Privacy and data").performScrollTo().performClick()
        compose.onNodeWithText("Export my data").assertIsDisplayed()
    }
}
