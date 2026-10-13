package com.routinenest.app.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.*

class ClockChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val dependencies = EntryPointAccessors.fromApplication(context.applicationContext, ReminderDependencies::class.java)
                dependencies.repository().all().forEach { dependencies.scheduler().schedule(it.routine) }
            } finally { pending.finish() }
        }
    }
}
