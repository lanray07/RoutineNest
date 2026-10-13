package com.routinenest.app

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.routinenest.app.billing.AmazonBilling
import com.routinenest.app.data.*
import com.routinenest.app.reminders.ReminderScheduler
import com.routinenest.core.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import java.time.*
import javax.inject.Inject

@HiltViewModel
class NestViewModel @Inject constructor(val repository: NestRepository, val preferences: PreferenceStore, val billing: AmazonBilling, private val reminders: ReminderScheduler, private val application: Application) : ViewModel() {
    val routines = repository.routines.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val completions = repository.completions.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val prefs = preferences.flow.stateIn(viewModelScope, SharingStarted.Eagerly, NestPreferences())
    val ready = MutableStateFlow(false)
    val message = MutableStateFlow("")
    val day = MutableStateFlow(LocalDate.now())
    val screen = MutableStateFlow("today")
    private val mutation = Mutex()
    init {
        viewModelScope.launch { preferences.flow.first(); ready.value = true }
        viewModelScope.launch { while (isActive) { val current = LocalDate.now(); if (day.value != current) { day.value = current; repository.all().forEach { reminders.schedule(it.routine) } }; delay(30_000) } }
        viewModelScope.launch { repository.all().forEach { reminders.schedule(it.routine) } }
    }
    fun action(onError: () -> Unit = {}, block: suspend () -> Unit) { viewModelScope.launch { runCatching { mutation.withLock { block() } }.onFailure { message.value = it.message ?: "Could not save. Please try again."; onError() } } }
    fun createAllowed() = billing.state.value.premium || routines.value.count { !it.routine.archived } < 3
    fun save(id: String?, name: String, tasks: List<Pair<String?, String>>, days: Set<DayOfWeek>, reminder: String, onSaved: () -> Unit, onError: () -> Unit = {}) = action(onError) {
        require(id != null || billing.state.value.premium || repository.all().count { !it.routine.archived } < 3) { "The free plan includes three routines. Premium is needed for more." }
        val key = repository.save(id, name, tasks, days, reminder)
        repository.all().find { it.routine.id == key }?.let { reminders.schedule(it.routine) }
        onSaved()
    }
    fun toggle(task: TaskEntity, done: Boolean) = action { repository.complete(task, day.value, done) }
    fun changeState(routine: RoutineEntity, paused: Boolean = routine.paused, archived: Boolean = routine.archived) = action {
        require(archived || !routine.archived || billing.state.value.premium || repository.all().count { !it.routine.archived } < 3) { "Three free routines are already active." }
        repository.setState(routine, paused, archived); reminders.schedule(routine.copy(paused = paused, archived = archived))
    }
    fun duplicate(routine: RoutineWithTasks) = action {
        require(billing.state.value.premium) { "Duplication requires Premium." }
        val id = repository.duplicate(routine); repository.all().find { it.routine.id == id }?.let { reminders.schedule(it.routine) }
    }
    fun template(title: Int, tasks: List<Int>) = save(null, application.getString(title), tasks.map { null to application.getString(it) }, DayOfWeek.entries.toSet(), "", {})
    fun deleteAll() = action { reminders.cancelAll(); repository.deleteAll(); message.value = "Routine data deleted." }
    fun streak(routine: RoutineWithTasks): Int {
        val doneDays = completions.value.filter { it.routineId == routine.routine.id }.groupBy { LocalDate.parse(it.day) }.filter { (date, done) -> val tasks = routine.activeTasks(date); tasks.isNotEmpty() && tasks.all { task -> done.any { it.taskId == task.id } } }.keys
        return Progress.streak(LocalDate.parse(routine.routine.start), routine::dueOn, doneDays, day.value)
    }
    fun stats(length: Long): Pair<Int, Int> {
        var total = 0; var done = 0
        for (offset in 0 until length) {
            val date = day.value.minusDays(offset)
            routines.value.filter { it.dueOn(date) }.forEach { routine ->
                val tasks = routine.activeTasks(date); total += tasks.size; done += tasks.count { task -> completions.value.any { it.taskId == task.id && it.day == date.toString() } }
            }
        }
        return done to total
    }
    fun execute(command: Command) = action {
        val all = repository.all().filter { !it.routine.archived && !it.routine.paused }
        val today = day.value
        when (command) {
            is Command.Create -> {
                require(billing.state.value.premium || repository.all().count { !it.routine.archived } < 3) { "The free plan includes three routines." }
                require(all.none { it.routine.name.equals(command.name, true) }) { "A routine already has that name." }
                val starters = if (command.name.contains("morning", true)) listOf(R.string.drink_water, R.string.stretch, R.string.plan_day) else if (command.name.contains("evening", true)) listOf(R.string.reading, R.string.tidy, R.string.reflect) else listOf(R.string.plan_day)
                repository.save(null, command.name, starters.map { null to application.getString(it) }, DayOfWeek.entries.toSet(), "")
                message.value = "Created ${command.name}. Edit it to personalise your steps."; screen.value = "library"
            }
            is Command.Add -> {
                val matches = all.filter { it.routine.name.removeSuffix(" routine").equals(command.routine, true) }
                require(matches.size == 1) { "Please name exactly one routine. ${matches.size} matching routines found." }
                val routine = matches.single()
                repository.save(routine.routine.id, routine.routine.name, routine.activeTasks().map { it.id to it.name } + (null to command.task), routine.routine.schedule().days, routine.routine.reminder)
                message.value = "Added ${command.task} to ${routine.routine.name}."
            }
            is Command.Complete -> {
                val matches = all.filter { it.routine.schedule().due(today) }.flatMap { it.activeTasks() }.filter { it.name.equals(command.task, true) }
                require(matches.size == 1) { "${matches.size} matching tasks found for today. Use the checklist to select the right task." }
                repository.complete(matches.single(), today, true); message.value = "Completed ${command.task}."
            }
            Command.Remaining, Command.Read -> {
                val tasks = all.filter { it.routine.schedule().due(today) }.flatMap { it.activeTasks() }
                val done = repository.history().filter { it.day == today.toString() }.map { it.taskId }.toSet()
                val result = if (command == Command.Remaining) tasks.filter { it.id !in done } else tasks
                message.value = if (result.isEmpty()) "Your list is clear for today." else result.joinToString(". ") { it.name }
            }
            Command.WeeklyProgress -> { val (done, total) = stats(7); message.value = "$done of $total steps completed this week (${Progress.percentage(done, total)}%)."; screen.value = "progress" }
            is Command.Remind -> {
                val now = ZonedDateTime.now(); var date = now.toLocalDate(); if (command.tomorrow) date = date.plusDays(1)
                val time = date.atTime(command.time).atZone(now.zone)
                require(time.isAfter(now)) { "That time has already passed. Say tomorrow or choose a later time." }
                reminders.once(time); message.value = "Reminder scheduled for $date at ${command.time}. Delivery may be delayed by device power settings."
            }
            is Command.Unknown -> message.value = command.explanation
        }
    }
    suspend fun export(): String {
        val root = JSONObject().put("formatVersion", 1).put("exportedAt", Instant.now().toString())
        root.put("routines", JSONArray().also { array -> repository.all().forEach { r -> array.put(JSONObject().put("id", r.routine.id).put("name", r.routine.name).put("days", r.routine.days).put("start", r.routine.start).put("paused", r.routine.paused).put("archived", r.routine.archived).put("reminder", r.routine.reminder).put("tasks", JSONArray().also { tasks -> r.tasks.forEach { tasks.put(JSONObject().put("id", it.id).put("name", it.name).put("created", it.created).put("removed", it.removed).put("position", it.position)) } })) } })
        root.put("completions", JSONArray().also { array -> repository.history().forEach { array.put(JSONObject().put("taskId", it.taskId).put("routineId", it.routineId).put("day", it.day).put("name", it.taskName)) } })
        root.put("routineStates", JSONArray().also { array -> repository.all().flatMap { it.states }.forEach { array.put(JSONObject().put("routineId", it.routineId).put("effectiveDay", it.effectiveDay).put("days", it.days).put("paused", it.paused).put("archived", it.archived)) } })
        return root.toString(2)
    }
}
