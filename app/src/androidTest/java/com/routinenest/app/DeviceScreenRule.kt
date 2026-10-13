package com.routinenest.app

import android.app.KeyguardManager
import android.content.Context
import android.os.PowerManager
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.rules.ExternalResource

/** Wakes an unsecured test display; never enters or bypasses a user's PIN. */
class DeviceScreenRule : ExternalResource() {
    override fun before() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        fun shell(command: String) { instrumentation.uiAutomation.executeShellCommand(command).use { descriptor -> java.io.FileInputStream(descriptor.fileDescriptor).use { it.readBytes() } } }
        shell("input keyevent KEYCODE_WAKEUP")
        shell("wm dismiss-keyguard")
        val context = instrumentation.targetContext
        val power = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val keyguard = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        repeat(20) { if (power.isInteractive && !keyguard.isKeyguardLocked) return; Thread.sleep(100) }
        check(power.isInteractive && !keyguard.isKeyguardLocked) { "Unlock the connected tablet before running UI tests; secure locks are never bypassed." }
    }
}
