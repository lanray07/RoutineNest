package com.routinenest.app.ui

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.routinenest.core.VoicePolicy
import com.routinenest.core.VoiceAvailability
import com.routinenest.app.*
import com.routinenest.app.R
import com.routinenest.app.data.*
import com.routinenest.app.voice.VoiceController
import com.routinenest.core.Command
import com.routinenest.core.CommandParser
import com.routinenest.core.Progress
import kotlinx.coroutines.launch
import java.time.*
import java.time.format.*
import java.util.Locale
import org.json.JSONArray

private val light = lightColorScheme(primary = Color(0xFF205B47), onPrimary = Color.White, primaryContainer = Color(0xFFDCEADE), secondary = Color(0xFF8C5D36), background = Color(0xFFF6F4ED), surface = Color(0xFFFCFBF7), surfaceVariant = Color(0xFFECEDE5))
private val dark = darkColorScheme(primary = Color(0xFFB0D7BB), onPrimary = Color(0xFF123B2B), primaryContainer = Color(0xFF284B3C), secondary = Color(0xFFE2BA91), background = Color(0xFF131D18), surface = Color(0xFF1C2821), surfaceVariant = Color(0xFF293A2F))
private data class Destination(val key: String, val label: Int, val icon: ImageVector)
private val destinations = listOf(Destination("today", R.string.dashboard, Icons.Default.WbSunny), Destination("library", R.string.library, Icons.AutoMirrored.Filled.ViewList), Destination("progress", R.string.progress, Icons.Default.Insights), Destination("voice", R.string.voice, Icons.Default.Mic), Destination("settings", R.string.settings, Icons.Default.Settings))
private val taskDraftSaver = Saver<List<Pair<String?, String>>, ArrayList<String>>(save = { tasks -> ArrayList(tasks.map { JSONArray().put(it.first).put(it.second).toString() }) }, restore = { values -> values.map { value -> val item = JSONArray(value); (if (item.isNull(0)) null else item.getString(0)) to item.getString(1) } })
private val dayDraftSaver = Saver<Set<DayOfWeek>, IntArray>(save = { days -> days.map { it.value }.toIntArray() }, restore = { values -> values.map(DayOfWeek::of).toSet() })

@Composable
fun NestApp(model: NestViewModel) {
    val prefs by model.prefs.collectAsStateWithLifecycle()
    val ready by model.ready.collectAsStateWithLifecycle()
    val screen by model.screen.collectAsStateWithLifecycle()
    val message by model.message.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val darkTheme = prefs.theme == "dark" || (prefs.theme == "system" && isSystemInDarkTheme())
    SideEffect { if (context is Activity) WindowCompat.getInsetsController(context.window, context.window.decorView).apply { isAppearanceLightStatusBars = !darkTheme; isAppearanceLightNavigationBars = !darkTheme } }
    val configuration = Configuration(LocalConfiguration.current).apply { setLocale(Locale.forLanguageTag(prefs.language)); uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or if (darkTheme) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO }
    val localized = context.createConfigurationContext(configuration)
    MaterialTheme(colorScheme = if (darkTheme) dark else light) {
        CompositionLocalProvider(LocalContext provides localized, LocalConfiguration provides configuration) {
            Surface(Modifier.fillMaxSize()) {
                if (!ready) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                else if (!prefs.onboarded) Onboarding(model)
                else {
                    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
                    val routines by model.routines.collectAsStateWithLifecycle()
                    val editing = routines.find { it.routine.id == editingId }
                    var editor by rememberSaveable { mutableStateOf(false) }
                    val wide = LocalConfiguration.current.screenWidthDp >= 600
                    val snackbar = remember { SnackbarHostState() }
                    LaunchedEffect(message) { if (message.isNotBlank()) snackbar.showSnackbar(message) }
                    BackHandler(editor || screen !in destinations.map { it.key }) { if (editor) editor = false else model.screen.value = "settings" }
                    val notifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> if (!granted) model.message.value = localized.getString(R.string.notifications_disabled) }
                    fun openEditor(routine: RoutineWithTasks?) { editingId = routine?.routine?.id; if (routine != null || model.createAllowed()) editor = true else model.screen.value = "premium" }
                    Scaffold(snackbarHost = { SnackbarHost(snackbar) }, bottomBar = {
                        if (!wide && !editor) NavigationBar { destinations.forEach { item -> NavigationBarItem(selected = screen == item.key, onClick = { model.screen.value = item.key }, icon = { Icon(item.icon, null) }, label = { Text(stringResource(item.label)) }) } }
                    }) { padding ->
                        Row(Modifier.fillMaxSize().padding(padding)) {
                            if (wide && !editor) NavigationRail { Spacer(Modifier.height(24.dp)); Icon(Icons.Default.Spa, null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.height(32.dp)); destinations.forEach { item -> NavigationRailItem(selected = screen == item.key, onClick = { model.screen.value = item.key }, icon = { Icon(item.icon, null) }, label = { Text(stringResource(item.label)) }) } }
                            Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()).padding(horizontal = if (wide) 40.dp else 20.dp, vertical = 24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                                if (editor && editingId != null && editing == null) CircularProgressIndicator()
                                else if (editor) Editor(editing, model, onCancel = { editor = false }, onSaved = { hasReminder -> editor = false; if (hasReminder && Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) notifications.launch(Manifest.permission.POST_NOTIFICATIONS) })
                                else when (screen) {
                                    "today" -> Today(model, { openEditor(null) }, { model.screen.value = "library" })
                                    "library" -> Library(model, { openEditor(it) })
                                    "progress" -> Analytics(model)
                                    "history" -> History(model)
                                    "voice" -> Assistant(model)
                                    "premium" -> Paywall(model)
                                    "privacy" -> Privacy(model)
                                    else -> Settings(model)
                                }
                                Spacer(Modifier.height(12.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable private fun Header(eyebrow: String, title: String, subtitle: String? = null) {
    Text(eyebrow.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
    Text(title, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
    if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
@Composable private fun NestArt() {
    Box(Modifier.fillMaxWidth().height(180.dp).background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(32.dp)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) { Image(painterResource(R.drawable.onboarding), null, Modifier.size(140.dp)); Text("RoutineNest", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
    }
}
@Composable private fun Onboarding(model: NestViewModel) {
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(32.dp), verticalArrangement = Arrangement.Center) {
        NestArt(); Spacer(Modifier.height(32.dp)); Header("RoutineNest", stringResource(R.string.welcome), stringResource(R.string.welcome_body)); Spacer(Modifier.height(28.dp))
        Button(onClick = { scope.launch { model.preferences.onboardingDone() }; model.screen.value = "library" }, Modifier.fillMaxWidth()) { Text(stringResource(R.string.get_started)) }
        Spacer(Modifier.height(16.dp)); Text(stringResource(R.string.privacy_body), style = MaterialTheme.typography.bodySmall)
    }
}
@Composable private fun Today(model: NestViewModel, create: () -> Unit, library: () -> Unit) {
    val routines by model.routines.collectAsStateWithLifecycle(); val done by model.completions.collectAsStateWithLifecycle(); val today by model.day.collectAsStateWithLifecycle()
    val prefs by model.prefs.collectAsStateWithLifecycle()
    Header(today.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(Locale.forLanguageTag(prefs.language))), stringResource(R.string.today_heading), stringResource(R.string.today_subheading))
    val due = routines.filter { !it.routine.paused && !it.routine.archived && it.routine.schedule().due(today) }
    val tasks = due.flatMap { it.activeTasks(today) }; val completed = tasks.count { task -> done.any { it.taskId == task.id && it.day == today.toString() } }
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), shape = RoundedCornerShape(24.dp)) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.completed_count, completed, tasks.size), style = MaterialTheme.typography.titleLarge)
            LinearProgressIndicator(progress = { if (tasks.isEmpty()) 0f else completed.toFloat() / tasks.size }, Modifier.fillMaxWidth())
            Text(stringResource(if (tasks.isNotEmpty() && completed == tasks.size) R.string.day_complete else R.string.day_encouragement))
        }
    }
    if (due.isEmpty()) { NestArt(); Text(stringResource(R.string.empty_today), style = MaterialTheme.typography.titleLarge); Text(stringResource(R.string.empty_body)); Button(onClick = library) { Text(stringResource(R.string.templates)) } }
    due.forEach { routine ->
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
            Column(Modifier.padding(20.dp)) {
                Text(routine.routine.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text(stringResource(R.string.streak_count, model.streak(routine)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                routine.activeTasks(today).forEach { task -> val checked = done.any { it.taskId == task.id && it.day == today.toString() }
                    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) { Checkbox(checked, { model.toggle(task, it) }); Text(task.name, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge) }
                }
            }
        }
    }
    OutlinedButton(onClick = create, Modifier.fillMaxWidth()) { Icon(Icons.Default.Add, null); Text(stringResource(R.string.add_routine)) }
}
@Composable private fun Library(model: NestViewModel, edit: (RoutineWithTasks?) -> Unit) {
    val context = LocalContext.current
    val routines by model.routines.collectAsStateWithLifecycle()
    Header(stringResource(R.string.collection_heading), stringResource(R.string.library), stringResource(R.string.collection_subheading))
    Button(onClick = { edit(null) }, Modifier.fillMaxWidth()) { Icon(Icons.Default.Add, null); Text(stringResource(R.string.add_routine)) }
    routines.forEach { routine ->
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(routine.routine.name, style = MaterialTheme.typography.titleLarge)
                Text(stringResource(R.string.routine_summary, routine.activeTasks().size, stringResource(if (routine.routine.archived) R.string.archived_state else if (routine.routine.paused) R.string.paused_state else if (routine.routine.days == "1,2,3,4,5,6,7") R.string.daily else R.string.weekly)))
                if (routine.routine.reminder.isNotBlank()) Text("${stringResource(R.string.notification_channel)} · ${routine.routine.reminder}", style = MaterialTheme.typography.bodySmall)
                Row(Modifier.horizontalScroll(rememberScrollState())) {
                    TextButton(onClick = { edit(routine) }) { Text(stringResource(R.string.edit)) }
                    TextButton(onClick = { model.changeState(routine.routine, paused = !routine.routine.paused) }) { Text(stringResource(if (routine.routine.paused) R.string.resume else R.string.pause)) }
                    TextButton(onClick = { model.changeState(routine.routine, archived = !routine.routine.archived) }) { Text(stringResource(if (routine.routine.archived) R.string.restore else R.string.archive)) }
                    TextButton(onClick = { model.duplicate(routine) }) { Text(stringResource(R.string.duplicate)) }
                }
            }
        }
    }
    Text(stringResource(R.string.templates), style = MaterialTheme.typography.titleMedium)
    listOf(Triple(R.string.morning, Icons.Default.WbSunny, listOf(R.string.drink_water, R.string.stretch, R.string.plan_day)), Triple(R.string.evening, Icons.Default.NightsStay, listOf(R.string.reading, R.string.tidy, R.string.reflect)), Triple(R.string.focus, Icons.Default.CenterFocusStrong, listOf(R.string.clear_desk, R.string.focus_work, R.string.take_break))).forEach { (title, icon, tasks) ->
        OutlinedCard(onClick = { model.template(title, tasks) }, Modifier.fillMaxWidth()) { Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(16.dp)); Column { Text(stringResource(title), fontWeight = FontWeight.SemiBold); Text(tasks.joinToString(" · ") { context.getString(it) }, style = MaterialTheme.typography.bodySmall) } } }
    }
}
@Composable private fun Editor(routine: RoutineWithTasks?, model: NestViewModel, onCancel: () -> Unit, onSaved: (Boolean) -> Unit) {
    var name by rememberSaveable(routine?.routine?.id) { mutableStateOf(routine?.routine?.name ?: "") }
    var reminder by rememberSaveable(routine?.routine?.id) { mutableStateOf(routine?.routine?.reminder ?: "") }
    var days by rememberSaveable(routine?.routine?.id, stateSaver = dayDraftSaver) { mutableStateOf(routine?.routine?.schedule()?.days ?: DayOfWeek.entries.toSet()) }
    var tasks by rememberSaveable(routine?.routine?.id, stateSaver = taskDraftSaver) { mutableStateOf(routine?.activeTasks()?.map { it.id to it.name }.orEmpty()) }
    var newTask by rememberSaveable { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    Header(stringResource(R.string.editor_heading), stringResource(if (routine == null) R.string.add_routine else R.string.edit))
    OutlinedTextField(name, { name = it }, label = { Text(stringResource(R.string.routine_name)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { FilterChip(days.size == 7, { days = DayOfWeek.entries.toSet() }, label = { Text(stringResource(R.string.daily)) }); FilterChip(days.size < 7, { days = setOf(DayOfWeek.MONDAY) }, label = { Text(stringResource(R.string.weekly)) }) }
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) { DayOfWeek.entries.forEach { day -> FilterChip(day in days, { days = if (day in days) days - day else days + day }, label = { Text(day.getDisplayName(TextStyle.SHORT, Locale.getDefault())) }) } }
    tasks.forEachIndexed { index, task -> Row(verticalAlignment = Alignment.CenterVertically) { OutlinedTextField(task.second, { value -> tasks = tasks.toMutableList().also { it[index] = task.first to value } }, Modifier.weight(1f), singleLine = true, label = { Text(stringResource(R.string.task_name)) }); IconButton(onClick = { tasks = tasks.filterIndexed { i, _ -> i != index } }) { Icon(Icons.Default.Close, stringResource(R.string.remove_task)) } } }
    Row(verticalAlignment = Alignment.CenterVertically) { OutlinedTextField(newTask, { newTask = it }, Modifier.weight(1f), singleLine = true, label = { Text(stringResource(R.string.task_name)) }); IconButton(onClick = { if (newTask.isNotBlank()) { tasks = tasks + (null to newTask.trim()); newTask = "" } }, enabled = newTask.isNotBlank()) { Icon(Icons.Default.Add, stringResource(R.string.add_task)) } }
    OutlinedTextField(reminder, { reminder = it }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.reminder)) }, singleLine = true, isError = reminder.isNotBlank() && runCatching { LocalTime.parse(reminder) }.isFailure)
    Text(stringResource(R.string.reminder_delivery), style = MaterialTheme.typography.bodySmall)
    Button(onClick = { saving = true; model.save(routine?.routine?.id, name, tasks.toList(), days, reminder, onSaved = { saving = false; onSaved(reminder.isNotBlank()) }, onError = { saving = false }) }, Modifier.fillMaxWidth(), enabled = !saving && name.isNotBlank() && tasks.isNotEmpty() && tasks.all { it.second.isNotBlank() } && days.isNotEmpty() && (reminder.isBlank() || runCatching { LocalTime.parse(reminder) }.isSuccess)) { Text(stringResource(R.string.save)) }
    TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel)) }
}
@Composable private fun Analytics(model: NestViewModel) {
    val routines by model.routines.collectAsStateWithLifecycle(); val completions by model.completions.collectAsStateWithLifecycle(); val premium by model.billing.state.collectAsStateWithLifecycle()
    val today by model.day.collectAsStateWithLifecycle()
    Header(stringResource(R.string.progress_heading), stringResource(R.string.progress), stringResource(R.string.progress_subheading))
    // Reading these flows causes recalculation after persistence updates.
    @Suppress("UNUSED_VARIABLE") val revision = routines.size + completions.size + today.dayOfYear
    listOf(7L to R.string.weekly_progress, 30L to R.string.monthly_progress).forEach { (length, title) ->
        if (length == 7L || premium.premium) { val (done, total) = model.stats(length)
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) { Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { Text(stringResource(title), style = MaterialTheme.typography.titleLarge); Text(stringResource(R.string.completion_percent, Progress.percentage(done, total)), style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary); LinearProgressIndicator(progress = { if (total == 0) 0f else done.toFloat() / total }, Modifier.fillMaxWidth()); Text(stringResource(R.string.completed_count, done, total)) } }
        } else OutlinedButton(onClick = { model.screen.value = "premium" }) { Text(stringResource(R.string.unlock_monthly)) }
    }
    Text(stringResource(R.string.progress_method), style = MaterialTheme.typography.bodySmall)
    OutlinedButton(onClick = { model.screen.value = "history" }) { Text(stringResource(R.string.history)) }
}
@Composable private fun History(model: NestViewModel) {
    val history by model.completions.collectAsStateWithLifecycle()
    Header(stringResource(R.string.history_heading), stringResource(R.string.history))
    if (history.isEmpty()) Text(stringResource(R.string.no_history))
    history.groupBy { it.day }.forEach { (date, completions) -> Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Text(date, fontWeight = FontWeight.Bold); completions.forEach { Text("✓ ${it.taskName}") } } } }
    TextButton(onClick = { model.screen.value = "progress" }) { Text(stringResource(R.string.back)) }
}
@Composable private fun Assistant(model: NestViewModel) {
    val context = LocalContext.current; val prefs by model.prefs.collectAsStateWithLifecycle(); val response by model.message.collectAsStateWithLifecycle()
    var text by rememberSaveable { mutableStateOf("") }; var status by remember { mutableStateOf("") }; var pending by remember { mutableStateOf<Command?>(null) }
    val voice = remember { VoiceController(context, { text = it }, { status = it }) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val locale = Locale.forLanguageTag(prefs.language)
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { if (it) voice.listen(locale) else status = context.getString(R.string.voice_denied) }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { if (!it) status = context.getString(R.string.notifications_disabled) }
    DisposableEffect(voice) { onDispose { voice.close() } }
    DisposableEffect(lifecycle, voice) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_STOP) voice.stop() }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(response) { if (prefs.speak && response.isNotBlank()) voice.speak(response, locale) }
    Header(stringResource(R.string.assistant_heading), stringResource(R.string.voice), stringResource(R.string.voice_help))
    if (!voice.available) Text(stringResource(R.string.voice_unavailable))
    FilledTonalButton(onClick = { when (VoicePolicy.input(voice.available, ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)) { VoiceAvailability.READY -> voice.listen(locale); VoiceAvailability.NEED_PERMISSION -> permission.launch(Manifest.permission.RECORD_AUDIO); VoiceAvailability.TEXT_ONLY -> status = context.getString(R.string.voice_unavailable) } }, enabled = voice.available, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Icon(Icons.Default.Mic, null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.microphone)) }
    if (status.isNotBlank()) Text(status, style = MaterialTheme.typography.bodyMedium)
    OutlinedTextField(text, { text = it }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.command)) }, minLines = 2)
    Button(onClick = { val command = CommandParser.parse(text, locale); when (command) { is Command.Create, is Command.Add, is Command.Complete, is Command.Remind -> pending = command; else -> model.execute(command) } }, enabled = text.isNotBlank()) { Text(stringResource(R.string.execute)) }
    if (response.isNotBlank()) Card(Modifier.fillMaxWidth()) { Text(response, Modifier.padding(20.dp), style = MaterialTheme.typography.bodyLarge) }
    if (pending != null) AlertDialog(onDismissRequest = { pending = null }, title = { Text(stringResource(R.string.confirm_command)) }, text = { val reminder = pending as? Command.Remind; Text(if (reminder == null) text else stringResource(R.string.reminder_confirmation, stringResource(if (reminder.tomorrow) R.string.tomorrow else R.string.today_word), reminder.time.toString())) }, confirmButton = { TextButton(onClick = { val command = pending!!; pending = null; model.execute(command); if (command is Command.Remind && Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS) }) { Text(stringResource(R.string.confirm)) } }, dismissButton = { TextButton(onClick = { pending = null }) { Text(stringResource(R.string.cancel)) } })
}
@Composable private fun Paywall(model: NestViewModel) {
    val billing by model.billing.state.collectAsStateWithLifecycle()
    Header(stringResource(R.string.premium_heading), stringResource(R.string.premium), stringResource(R.string.premium_body)); NestArt()
    Text(if (billing.premium) stringResource(R.string.premium_active) else billing.price ?: stringResource(R.string.price_unavailable), style = MaterialTheme.typography.headlineSmall)
    if (!billing.available) Text(stringResource(R.string.billing_blocked))
    Button(onClick = { model.billing.purchase() }, enabled = billing.available && !billing.busy && !billing.premium, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.purchase)) }
    OutlinedButton(onClick = { model.billing.restore() }, enabled = !billing.busy) { Text(stringResource(R.string.restore_purchases)) }
    Text(billing.message, style = MaterialTheme.typography.bodySmall)
    TextButton(onClick = { model.screen.value = "settings" }) { Text(stringResource(R.string.back)) }
}
@Composable private fun Settings(model: NestViewModel) {
    val prefs by model.prefs.collectAsStateWithLifecycle(); val scope = rememberCoroutineScope()
    Header(stringResource(R.string.settings_heading), stringResource(R.string.settings))
    Text(stringResource(R.string.theme), style = MaterialTheme.typography.titleMedium)
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("system" to R.string.system_theme, "light" to R.string.light_theme, "dark" to R.string.dark_theme).forEach { (value, title) -> FilterChip(prefs.theme == value, { scope.launch { model.preferences.theme(value) } }, label = { Text(stringResource(title)) }) } }
    Text(stringResource(R.string.language), style = MaterialTheme.typography.titleMedium)
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) { validatedLocales.forEach { (tag, label) -> FilterChip(prefs.language == tag, { scope.launch { model.preferences.language(tag) } }, label = { Text(label) }) } }
    Text(stringResource(R.string.languages_pending), style = MaterialTheme.typography.bodySmall)
    Row(verticalAlignment = Alignment.CenterVertically) { Text(stringResource(R.string.tts), Modifier.weight(1f)); Switch(prefs.speak, { scope.launch { model.preferences.speak(it) } }) }
    OutlinedButton(onClick = { model.screen.value = "premium" }, Modifier.fillMaxWidth()) { Text(stringResource(R.string.premium)) }
    OutlinedButton(onClick = { model.screen.value = "privacy" }, Modifier.fillMaxWidth()) { Text(stringResource(R.string.privacy)) }
    Text("RoutineNest ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodySmall)
}
@Composable private fun Privacy(model: NestViewModel) {
    val context = LocalContext.current; val scope = rememberCoroutineScope(); var deleting by remember { mutableStateOf(false) }
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri -> if (uri != null) scope.launch { runCatching { val json = model.export(); context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray(Charsets.UTF_8)) } ?: error("Could not open destination"); model.message.value = "Export saved." }.onFailure { model.message.value = "Export failed: ${it.message}" } } }
    Header(stringResource(R.string.privacy_heading), stringResource(R.string.privacy), stringResource(R.string.privacy_body))
    OutlinedButton(onClick = { exporter.launch("routinenest-export.json") }, Modifier.fillMaxWidth()) { Text(stringResource(R.string.export_data)) }
    OutlinedButton(onClick = { deleting = true }, Modifier.fillMaxWidth()) { Text(stringResource(R.string.delete_data)) }
    TextButton(onClick = { model.screen.value = "settings" }) { Text(stringResource(R.string.back)) }
    if (deleting) AlertDialog(onDismissRequest = { deleting = false }, title = { Text(stringResource(R.string.delete_data)) }, text = { Text(stringResource(R.string.delete_confirm)) }, confirmButton = { TextButton(onClick = { deleting = false; model.deleteAll() }) { Text(stringResource(R.string.delete)) } }, dismissButton = { TextButton(onClick = { deleting = false }) { Text(stringResource(R.string.cancel)) } })
}
