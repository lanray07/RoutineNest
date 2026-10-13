package com.routinenest.app.reminders

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.*
import com.routinenest.app.MainActivity
import com.routinenest.app.R
import com.routinenest.app.data.*
import com.routinenest.core.Progress
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderScheduler @Inject constructor(@ApplicationContext private val context: Context) {
    fun schedule(routine: RoutineEntity) {
        val work = WorkManager.getInstance(context)
        work.cancelUniqueWork("routine-${routine.id}")
        if (routine.paused || routine.archived || routine.reminder.isBlank()) return
        val now = ZonedDateTime.now()
        val next = Progress.nextReminder(LocalTime.parse(routine.reminder), routine.schedule(), now) ?: return
        enqueue("routine-${routine.id}", routine.id, routine.name, Duration.between(now, next))
    }
    fun once(time: ZonedDateTime) { require(time.isAfter(ZonedDateTime.now())); enqueue("voice-reminder-${time.toEpochSecond()}", "", context.getString(R.string.notification_title), Duration.between(ZonedDateTime.now(), time)) }
    private fun enqueue(key: String, id: String, title: String, delay: Duration) {
        val request = OneTimeWorkRequestBuilder<ReminderWorker>().setInitialDelay(delay.toMillis().coerceAtLeast(0), TimeUnit.MILLISECONDS).setInputData(workDataOf("id" to id, "title" to title)).addTag("nest-reminders").build()
        WorkManager.getInstance(context).enqueueUniqueWork(key, ExistingWorkPolicy.REPLACE, request)
    }
    fun cancelAll() { WorkManager.getInstance(context).cancelAllWorkByTag("nest-reminders") }
}
@EntryPoint @InstallIn(SingletonComponent::class)
interface ReminderDependencies { fun repository(): NestRepository; fun scheduler(): ReminderScheduler }
class ReminderWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val dependencies = EntryPointAccessors.fromApplication(applicationContext, ReminderDependencies::class.java)
        val id = inputData.getString("id").orEmpty()
        val routine = if (id.isNotBlank()) dependencies.repository().all().find { it.routine.id == id }?.routine else null
        if (id.isNotBlank() && (routine == null || routine.paused || routine.archived)) return Result.success()
        if (routine != null && !routine.schedule().due(LocalDate.now())) { dependencies.scheduler().schedule(routine); return Result.success() }
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel("routines", applicationContext.getString(R.string.notification_channel), NotificationManager.IMPORTANCE_DEFAULT))
        val granted = Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            val pending = PendingIntent.getActivity(applicationContext, 0, Intent(applicationContext, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            manager.notify((id.ifBlank { this.id.toString() }).hashCode(), NotificationCompat.Builder(applicationContext, "routines").setSmallIcon(R.drawable.nest_mark).setContentTitle(applicationContext.getString(R.string.notification_title)).setContentText(routine?.name ?: inputData.getString("title")).setContentIntent(pending).setAutoCancel(true).build())
        }
        if (routine != null) dependencies.scheduler().schedule(routine)
        return Result.success()
    }
}
