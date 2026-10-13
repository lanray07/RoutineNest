package com.routinenest.app.data

import androidx.room.withTransaction
import com.routinenest.core.*
import java.time.*
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

fun RoutineEntity.schedule() = Schedule(days.split(',').mapNotNull { it.toIntOrNull()?.let(DayOfWeek::of) }.toSet(), LocalDate.parse(start))
fun RoutineWithTasks.activeTasks(day: LocalDate = LocalDate.now()) = tasks.filter { !LocalDate.parse(it.created).isAfter(day) && (it.removed == null || LocalDate.parse(it.removed).isAfter(day)) }.sortedBy { it.position }
fun RoutineWithTasks.dueOn(day: LocalDate): Boolean {
    val state = states.filter { it.effectiveDay <= day.toString() }.maxByOrNull { it.effectiveDay }
    return if (state == null) !routine.paused && !routine.archived && routine.schedule().due(day)
    else !state.paused && !state.archived && routine.copy(days = state.days).schedule().due(day)
}

@Singleton
class NestRepository @Inject constructor(private val db: NestDatabase) {
    val routines = db.dao().observeRoutines()
    val completions = db.dao().observeCompletions()
    suspend fun all() = db.dao().all()
    suspend fun history() = db.dao().completions()
    suspend fun save(id: String?, name: String, taskNames: List<Pair<String?, String>>, days: Set<DayOfWeek>, reminder: String): String {
        require(name.isNotBlank() && taskNames.isNotEmpty() && taskNames.all { it.second.isNotBlank() } && days.isNotEmpty())
        require(reminder.isBlank() || runCatching { LocalTime.parse(reminder) }.isSuccess)
        val key = id ?: UUID.randomUUID().toString()
        val today = LocalDate.now().toString()
        db.withTransaction {
            val old = db.dao().routine(key)
            val updated = (old?.routine ?: RoutineEntity(key, name.trim(), "", today)).copy(name = name.trim(), days = days.sortedBy { it.value }.joinToString(",") { it.value.toString() }, reminder = reminder)
            db.dao().upsertRoutine(updated)
            db.dao().upsertState(RoutineStateEntity(key, today, updated.days, updated.paused, updated.archived))
            val tasks = taskNames.mapIndexed { index, (taskId, taskName) ->
                val existing = old?.tasks?.find { it.id == taskId }
                TaskEntity(existing?.id ?: UUID.randomUUID().toString(), key, taskName.trim(), index, existing?.created ?: today)
            }
            db.dao().upsertTasks(tasks + old?.tasks.orEmpty().filter { original -> tasks.none { it.id == original.id } }.map { if (it.removed != null) it else it.copy(removed = today) })
        }
        return key
    }
    suspend fun setState(routine: RoutineEntity, paused: Boolean = routine.paused, archived: Boolean = routine.archived) = db.withTransaction {
        db.dao().upsertRoutine(routine.copy(paused = paused, archived = archived))
        db.dao().upsertState(RoutineStateEntity(routine.id, LocalDate.now().toString(), routine.days, paused, archived))
    }
    suspend fun complete(task: TaskEntity, day: LocalDate, done: Boolean) {
        val routine = db.dao().routine(task.routineId) ?: return
        require(routine.routine.schedule().due(day) && routine.activeTasks(day).any { it.id == task.id })
        if (done) db.dao().complete(CompletionEntity(task.id, task.routineId, day.toString(), task.name)) else db.dao().undo(task.id, day.toString())
    }
    suspend fun duplicate(routine: RoutineWithTasks) = save(null, "${routine.routine.name} copy", routine.activeTasks().map { null to it.name }, routine.routine.schedule().days, routine.routine.reminder)
    suspend fun deleteAll() = db.withTransaction { db.dao().clearCompletions(); db.dao().clearRoutines() }
}
