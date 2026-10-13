package com.routinenest.app

import com.routinenest.app.data.NestDatabase
import com.routinenest.app.data.PreferenceStore
import com.routinenest.app.reminders.ReminderScheduler
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** App-component access for instrumentation. This interface does not exist in release. */
@EntryPoint @InstallIn(SingletonComponent::class)
interface DebugDataAccess {
    fun database(): NestDatabase
    fun preferences(): PreferenceStore
    fun scheduler(): ReminderScheduler
}
