package com.geepscontrol.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.geepscontrol.core.data.DataFileGateway
import com.geepscontrol.core.data.DataPortabilityService
import com.geepscontrol.core.database.ItineraryEntryEntity
import com.geepscontrol.core.database.AlarmProfileEntity
import com.geepscontrol.core.database.ChecklistEntity
import com.geepscontrol.core.database.ChecklistItemEntity
import com.geepscontrol.core.database.LifeEventEntity
import com.geepscontrol.core.database.MedicationEntity
import com.geepscontrol.core.database.MedicationOccurrenceEntity
import com.geepscontrol.core.database.ReminderEntity
import com.geepscontrol.core.database.RoutineEntity
import com.geepscontrol.core.database.RoutineOccurrenceEntity
import com.geepscontrol.core.database.StandaloneAlarmEntity
import com.geepscontrol.core.database.StandaloneTimerEntity
import com.geepscontrol.core.database.TripEntity
import com.geepscontrol.core.domain.ActiveTimer
import com.geepscontrol.core.domain.AlarmDefinition
import com.geepscontrol.core.domain.AlarmRepeatRule
import com.geepscontrol.core.domain.AlarmRepeatEngine
import com.geepscontrol.core.domain.AlarmRepeatUnit
import com.geepscontrol.core.domain.bundledAlarmSounds
import com.geepscontrol.core.domain.AlarmScheduler
import com.geepscontrol.core.domain.AnalyticsService
import com.geepscontrol.core.domain.AppDestination
import com.geepscontrol.core.domain.DailyPlanItem
import com.geepscontrol.core.domain.DeviceCalendar
import com.geepscontrol.core.domain.DeviceCalendarEvent
import com.geepscontrol.core.domain.EnergyRequirement
import com.geepscontrol.core.domain.NotificationCategory
import com.geepscontrol.core.domain.PlatformActions
import com.geepscontrol.core.domain.Priority
import com.geepscontrol.core.domain.Project
import com.geepscontrol.core.domain.ProjectStatus
import com.geepscontrol.core.domain.RecommendationAction
import com.geepscontrol.core.domain.RecommendationContext
import com.geepscontrol.core.domain.RecommendationService
import com.geepscontrol.core.domain.RecurrenceMode
import com.geepscontrol.core.domain.RecurrenceEngine
import com.geepscontrol.core.domain.RecurrenceRule
import com.geepscontrol.core.domain.RecurrenceUnit
import com.geepscontrol.core.domain.ReviewRecord
import com.geepscontrol.core.domain.ReviewType
import com.geepscontrol.core.domain.Task
import com.geepscontrol.core.domain.TaskContext
import com.geepscontrol.core.domain.TaskStatus
import com.geepscontrol.core.domain.TaskTag
import com.geepscontrol.core.domain.TimelineItem
import com.geepscontrol.core.domain.TimerEngine
import com.geepscontrol.core.domain.TimerMode
import com.geepscontrol.core.domain.TimerState
import com.geepscontrol.core.domain.TimelineItemType
import com.geepscontrol.core.domain.UnifiedTimelineService
import com.geepscontrol.core.domain.WorkSession
import com.geepscontrol.core.domain.currentTimeMillis
import com.geepscontrol.core.domain.dayKey
import com.geepscontrol.core.repository.ContextRepository
import com.geepscontrol.core.repository.LifeItemRepository
import com.geepscontrol.core.repository.PlanningRepository
import com.geepscontrol.core.repository.ProjectRepository
import com.geepscontrol.core.repository.ReviewRepository
import com.geepscontrol.core.repository.TagRepository
import com.geepscontrol.core.repository.TaskRepository
import com.geepscontrol.core.repository.TimerRepository
import com.geepscontrol.core.settings.AppSettings
import com.geepscontrol.core.settings.ThemeMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Temporary recording-safe label: keep the real account internal for calendar access.
private const val CALENDAR_ACCOUNT_RECORDING_PLACEHOLDER = "PlaceHolder@gmail.com"
private const val CALENDAR_NAME_RECORDING_PLACEHOLDER = "Private calendar"
private val CALENDAR_NAME_RECORDING_SUFFIXES = listOf(".edu", ".com", ".net")

private fun calendarNameForRecording(name: String): String {
    val normalized = name.trim().lowercase()
    return if (CALENDAR_NAME_RECORDING_SUFFIXES.any(normalized::endsWith)) CALENDAR_NAME_RECORDING_PLACEHOLDER else name
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LifeMinderApp(
    projectRepository: ProjectRepository,
    taskRepository: TaskRepository,
    contextRepository: ContextRepository,
    tagRepository: TagRepository,
    planningRepository: PlanningRepository,
    timerRepository: TimerRepository,
    lifeItemRepository: LifeItemRepository,
    alarmScheduler: AlarmScheduler,
    appSettings: AppSettings,
    reviewRepository: ReviewRepository,
    dataPortability: DataPortabilityService,
    dataFiles: DataFileGateway,
    platformActions: PlatformActions,
    calendarPermissionGranted: Boolean,
    calendarPermissionRevision: Int,
    onRequestCalendarPermission: () -> Unit,
    onOpenCalendarPermissionSettings: () -> Unit,
) {
    var destination by remember { mutableStateOf(AppDestination.TODAY) }
    var taskBeingEdited by remember { mutableStateOf<Task?>(null) }
    var taskBeingViewed by remember { mutableStateOf<Task?>(null) }
    var medicationBeingViewedId by remember { mutableStateOf<Long?>(null) }
    var medicationBeingEdited by remember { mutableStateOf<MedicationEntity?>(null) }
    var lifeItemBeingViewed by remember { mutableStateOf<TimelineItem?>(null) }
    var projectBeingEdited by remember { mutableStateOf<Project?>(null) }
    var selectedProject by remember { mutableStateOf<Project?>(null) }
    var selectedTripId by remember { mutableStateOf<Long?>(null) }
    var pendingTripTaskId by remember { mutableStateOf<Long?>(null) }
    var showPlanTypeDialog by remember { mutableStateOf(false) }
    var showNewListDialog by remember { mutableStateOf(false) }
    var newListName by remember { mutableStateOf("") }
    var selectedChecklistId by remember { mutableStateOf<Long?>(null) }
    var showTripAddDialog by remember { mutableStateOf(false) }
    var tripEntryRequest by remember { mutableStateOf(0) }
    var showTimerAddDialog by remember { mutableStateOf(false) }
    var timerCreationRequest by remember { mutableStateOf(0) }
    var alarmCreationRequest by remember { mutableStateOf(0) }
    var showOnboarding by remember { mutableStateOf(!appSettings.onboardingCompleted) }
    var showTopMenu by remember { mutableStateOf(false) }
    var showQuickAdd by remember { mutableStateOf(false) }
    var selectedCalendarEvent by remember { mutableStateOf<DeviceCalendarEvent?>(null) }
    var lifeItemQuickType by remember { mutableStateOf("Reminder") }
    var lifeItemsShowAdd by remember { mutableStateOf(false) }
    var themeMode by remember { mutableStateOf(appSettings.themeMode) }
    val projects by projectRepository.observeProjects().collectAsState(emptyList())
    val tasks by taskRepository.observeTasks().collectAsState(emptyList())
    val contexts by contextRepository.observeContexts().collectAsState(emptyList())
    val tags by tagRepository.observeTags().collectAsState(emptyList())
    val now = currentTimeMillis()
    var selectedCalendarIds by remember { mutableStateOf(appSettings.selectedCalendarIds) }
    var calendarRefreshRequest by remember { mutableIntStateOf(0) }
    val deviceCalendars =
        remember(destination, selectedCalendarIds, calendarPermissionRevision, calendarRefreshRequest) {
            platformActions.deviceCalendars().map { it.copy(name = calendarNameForRecording(it.name)) }
        }
    val deviceCalendarEvents = remember(destination, selectedCalendarIds, now / 60_000L) { platformActions.deviceCalendarEvents(dayKey(now) * DAY_MILLIS, dayKey(now) * DAY_MILLIS + 31 * DAY_MILLIS, selectedCalendarIds) }
    val deviceCalendarNames = deviceCalendars.associate { it.id to it.name }
    val deviceCalendarTimeline =
        deviceCalendarEvents.map { event ->
            TimelineItem(
                event.id,
                TimelineItemType.EVENT,
                event.title,
                event.startAt,
                event.endAt,
                event.allDay,
                actionable = false,
                status = "CALENDAR",
                subtitle = listOfNotNull(deviceCalendarNames[event.calendarId]?.let { "From $it" }, event.location.takeIf { it.isNotBlank() }).joinToString(" · "),
                sourceId = event.calendarId,
            )
        }
    val todayKey = dayKey(now)
    val dailyPlan by planningRepository.observeDay(todayKey).collectAsState(emptyList())
    val activeTimer by timerRepository.observeActive().collectAsState(null)
    var stopwatchLaps by remember(activeTimer?.startedAt) { mutableStateOf(emptyList<Long>()) }
    val sessions by timerRepository.observeSessions().collectAsState(emptyList())
    val alarms by timerRepository.observeAlarms().collectAsState(emptyList())
    val reviews by reviewRepository.observeReviews().collectAsState(emptyList())
    val timeline by remember(taskRepository, lifeItemRepository) { UnifiedTimelineService(taskRepository, lifeItemRepository).observeItems(now) }.collectAsState(emptyList())
    val reminders by lifeItemRepository.reminders().collectAsState(emptyList())
    val routines by lifeItemRepository.routines().collectAsState(emptyList())
    val routineOccurrences by lifeItemRepository.routineOccurrences().collectAsState(emptyList())
    val medications by lifeItemRepository.medications().collectAsState(emptyList())
    val medicationOccurrences by lifeItemRepository.medicationOccurrences().collectAsState(emptyList())
    val medicationBeingViewed = medications.firstOrNull { it.id == medicationBeingViewedId }
    val events by lifeItemRepository.events().collectAsState(emptyList())
    val lifeAlarms by lifeItemRepository.alarms().collectAsState(emptyList())
    val standaloneTimers by lifeItemRepository.timers().collectAsState(emptyList())
    val timerPresets by lifeItemRepository.presets().collectAsState(emptyList())
    val alarmProfiles by lifeItemRepository.alarmProfiles().collectAsState(emptyList())
    val timerHistory by lifeItemRepository.timerHistory().collectAsState(emptyList())
    val trips by lifeItemRepository.trips().collectAsState(emptyList())
    val checklists by lifeItemRepository.checklists().collectAsState(emptyList())
    val checklistItems by lifeItemRepository.checklistItems().collectAsState(emptyList())
    val itinerary by lifeItemRepository.itineraryEntries().collectAsState(emptyList())
    val tripReferences by lifeItemRepository.tripReferences().collectAsState(emptyList())
    val tripTasks by lifeItemRepository.tripTasks().collectAsState(emptyList())
    val transportationDetails by lifeItemRepository.transportationDetails().collectAsState(emptyList())
    val lodgingDetails by lifeItemRepository.lodgingDetails().collectAsState(emptyList())
    val reviewDayStart = dayKey(now) * DAY_MILLIS
    val reviewWeekStart = reviewDayStart - 6 * DAY_MILLIS
    val weeklyDeferrals by planningRepository.observeDeferralCount(reviewWeekStart, reviewDayStart + DAY_MILLIS).collectAsState(0)
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val handlesSystemBack =
        taskBeingEdited != null || medicationBeingEdited != null || projectBeingEdited != null ||
            selectedCalendarEvent != null || showPlanTypeDialog || showQuickAdd || showNewListDialog ||
            selectedChecklistId != null || showTripAddDialog || showTimerAddDialog || showTopMenu ||
            taskBeingViewed != null || medicationBeingViewedId != null || lifeItemBeingViewed != null ||
            selectedProject != null || selectedTripId != null || destination != AppDestination.TODAY
    SystemBackHandler(enabled = handlesSystemBack) {
        when {
            taskBeingEdited != null -> {
                taskBeingEdited = null
                pendingTripTaskId = null
            }
            medicationBeingEdited != null -> medicationBeingEdited = null
            projectBeingEdited != null -> projectBeingEdited = null
            selectedCalendarEvent != null -> selectedCalendarEvent = null
            showPlanTypeDialog -> showPlanTypeDialog = false
            showQuickAdd -> showQuickAdd = false
            showNewListDialog -> showNewListDialog = false
            selectedChecklistId != null -> selectedChecklistId = null
            showTripAddDialog -> showTripAddDialog = false
            showTimerAddDialog -> showTimerAddDialog = false
            showTopMenu -> showTopMenu = false
            taskBeingViewed != null -> taskBeingViewed = null
            medicationBeingViewedId != null -> medicationBeingViewedId = null
            lifeItemBeingViewed != null -> lifeItemBeingViewed = null
            selectedProject != null -> selectedProject = null
            selectedTripId != null -> selectedTripId = null
            destination != AppDestination.TODAY -> destination = AppDestination.TODAY
        }
    }
    LaunchedEffect(todayKey) {
        lifeItemRepository.removeHistoryBefore(todayKey * DAY_MILLIS)
    }
    LaunchedEffect(standaloneTimers) {
        standaloneTimers.filter { it.state == "COMPLETED" }.forEach { lifeItemRepository.removeTimer(it.id) }
    }
    LaunchedEffect(tasks) {
        val current = currentTimeMillis()
        tasks.forEach { task ->
            task.reminderAt?.takeIf { it > current }?.let { alarmScheduler.scheduleReminder(task.id * 10 + 1, it, task.title, NotificationCategory.TASK_REMINDER) }
            task.dueAt?.let { due ->
                (due - DAY_MILLIS).takeIf { it > current }?.let { alarmScheduler.scheduleReminder(task.id * 10 + 2, it, "Due tomorrow: ${task.title}", NotificationCategory.DUE_STATUS) }
                due.takeIf { it > current }?.let { alarmScheduler.scheduleReminder(task.id * 10 + 3, it, "Due now: ${task.title}", NotificationCategory.DUE_STATUS) }
            }
        }
    }
    LaunchedEffect(routines, medications, routineOccurrences, medicationOccurrences) {
        val generatedAt = currentTimeMillis()
        val dayStart = dayKey(generatedAt) * DAY_MILLIS
        routines.filter { it.active && it.startDate <= dayStart && (it.endDate == null || it.endDate >= dayStart) }.forEach { routine ->
            if (routineOccurrences.none { it.routineId == routine.id && it.scheduledAt in dayStart until dayStart + DAY_MILLIS }) lifeItemRepository.save(RoutineOccurrenceEntity(routineId = routine.id, scheduledAt = dayStart + (routine.preferredTimeMinutes ?: 9 * 60) * 60_000L, createdAt = generatedAt, updatedAt = generatedAt))
        }
        medications.filter { it.active && !it.asNeeded && (it.scheduleType != "ONCE" || it.recurrenceRule != null) && it.startDate <= dayStart && (it.endDate == null || it.endDate >= dayStart) }.forEach { medication ->
            val minutes =
                medication.timesOfDay
                    .split(',')
                    .mapNotNull { it.trim().toIntOrNull() }
                    .ifEmpty { listOf(9 * 60) }
            com.geepscontrol.core.domain.LifeScheduleEngine
                .medicationTimes(dayStart, minutes)
                .forEach { scheduled -> if (medicationOccurrences.none { it.medicationId == medication.id && it.scheduledAt == scheduled }) lifeItemRepository.save(MedicationOccurrenceEntity(medicationId = medication.id, scheduledAt = scheduled, createdAt = generatedAt, updatedAt = generatedAt)) }
        }
    }
    LaunchedEffect(reminders, lifeAlarms, standaloneTimers) {
        val schedulingNow = currentTimeMillis()
        reminders.filter { it.status in setOf("SCHEDULED", "SNOOZED") && it.scheduledAt > schedulingNow }.forEach { alarmScheduler.scheduleReminder(1_000_000 + it.id, it.scheduledAt, it.title, NotificationCategory.TASK_REMINDER) }
        lifeAlarms.filter { it.enabled && it.scheduledAt > schedulingNow }.forEach { alarmScheduler.schedule(AlarmDefinition(id = 3_000_000 + it.id, triggerAt = it.scheduledAt, label = it.label, vibration = it.vibration, sound = it.sound)) }
        standaloneTimers.filter { it.state == "RUNNING" }.forEach { timer -> alarmScheduler.scheduleTimer(ActiveTimer(id = 4_000_000 + timer.id, mode = TimerMode.COUNTDOWN, state = TimerState.RUNNING, startedAt = timer.startedAt, resumedAt = timer.resumedAt, accumulatedMillis = timer.accumulatedMillis, intendedDurationMillis = timer.durationMillis, updatedAt = timer.updatedAt)) }
    }
    val completeTask: (Task) -> Unit = { task ->
        scope.launch {
            timerRepository.disableAlarmsForTask(task.id)
            alarms.filter { it.taskId == task.id }.forEach { alarmScheduler.cancel(it.id) }
            alarmScheduler.cancelReminder(task.id * 10 + 1)
            alarmScheduler.cancelReminder(task.id * 10 + 2)
            alarmScheduler.cancelReminder(task.id * 10 + 3)
            if (activeTimer?.taskId == task.id) {
                timerRepository.clearActive()
                alarmScheduler.cancelTimer()
            }
        }
        completeWithUndo(task, taskRepository, snackbar, scope)
    }
    val completeTimelineItem: (TimelineItem) -> Unit = { item ->
        when (item.type) {
            TimelineItemType.TASK -> tasks.firstOrNull { it.id == item.id }?.let(completeTask)
            else -> scope.launch {
                val completedAt = currentTimeMillis()
                when (item.type) {
                    TimelineItemType.MEDICATION -> {
                        val occurrence = medicationOccurrences.firstOrNull { it.id == item.id }
                        val medication = medications.firstOrNull { it.id == item.sourceId }
                        if (occurrence != null && medication != null) {
                            lifeItemRepository.save(occurrence.copy(status = "TAKEN", recordedAt = completedAt, dosageTakenText = medication.dosageText, snoozedUntil = null, updatedAt = completedAt))
                            alarmScheduler.cancelReminder(2_000_000 + occurrence.id)
                            if (medication.active && (medication.scheduleType != "ONCE" || medication.recurrenceRule != null)) {
                                var nextAt = occurrence.scheduledAt + DAY_MILLIS
                                while (nextAt <= completedAt) nextAt += DAY_MILLIS
                                if (medicationOccurrences.none { it.medicationId == medication.id && it.scheduledAt == nextAt }) {
                                    val nextId = lifeItemRepository.save(MedicationOccurrenceEntity(medicationId = medication.id, scheduledAt = nextAt, createdAt = completedAt, updatedAt = completedAt))
                                    alarmScheduler.scheduleReminder(2_000_000 + nextId, nextAt, medication.name, NotificationCategory.TASK_REMINDER)
                                }
                            }
                        }
                    }
                    TimelineItemType.ROUTINE -> {
                        val occurrence = routineOccurrences.firstOrNull { it.id == item.id }
                        val routine = routines.firstOrNull { it.id == item.sourceId }
                        if (occurrence != null && routine != null) {
                            lifeItemRepository.save(occurrence.copy(status = "COMPLETED", recordedAt = completedAt, updatedAt = completedAt))
                            if (routine.active) {
                                val rule = RecurrenceRule.decode(routine.recurrenceRule)
                                val nextAt = rule?.let { RecurrenceEngine.nextTimestamp(it, occurrence.scheduledAt, completedAt) } ?: (occurrence.scheduledAt + DAY_MILLIS)
                                if (routineOccurrences.none { it.routineId == routine.id && it.scheduledAt == nextAt }) lifeItemRepository.save(RoutineOccurrenceEntity(routineId = routine.id, scheduledAt = nextAt, createdAt = completedAt, updatedAt = completedAt))
                            }
                        }
                    }
                    TimelineItemType.REMINDER -> {
                        reminders.firstOrNull { it.id == item.sourceId }?.let { reminder ->
                            lifeItemRepository.reminderStatus(reminder.id, "COMPLETED", completedAt)
                            alarmScheduler.cancelReminder(1_000_000 + reminder.id)
                            RecurrenceRule.decode(reminder.recurrenceRule)?.let { rule ->
                                val nextAt = RecurrenceEngine.nextTimestamp(rule, reminder.scheduledAt, completedAt)
                                val nextId = lifeItemRepository.save(reminder.copy(id = 0, scheduledAt = nextAt, status = "SCHEDULED", completedAt = null, createdAt = completedAt, updatedAt = completedAt))
                                alarmScheduler.scheduleReminder(1_000_000 + nextId, nextAt, reminder.title, NotificationCategory.TASK_REMINDER)
                            }
                        }
                    }
                    TimelineItemType.ALARM -> {
                        lifeAlarms.firstOrNull { it.id == item.sourceId }?.let { alarm ->
                            val rule = AlarmRepeatRule.decode(alarm.recurrenceRule)
                            if (rule == null) {
                                lifeItemRepository.setAlarmEnabled(alarm.id, false, completedAt)
                                alarmScheduler.cancel(3_000_000 + alarm.id)
                            } else {
                                val nextAt = AlarmRepeatEngine.nextFuture(rule, alarm.scheduledAt, completedAt)
                                val updated = alarm.copy(scheduledAt = nextAt, updatedAt = completedAt)
                                lifeItemRepository.save(updated)
                                alarmScheduler.schedule(AlarmDefinition(id = 3_000_000 + alarm.id, triggerAt = nextAt, label = alarm.label))
                            }
                        }
                    }
                    TimelineItemType.TASK, TimelineItemType.EVENT, TimelineItemType.TIMER, TimelineItemType.ITINERARY -> Unit
                }
            }
        }
    }

    val useDarkTheme = when (themeMode) { ThemeMode.DARK -> true; ThemeMode.LIGHT -> false; ThemeMode.SYSTEM -> isSystemInDarkTheme() }
    GeepsTheme(darkTheme = useDarkTheme) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(medicationBeingViewed?.name ?: lifeItemBeingViewed?.title ?: taskBeingViewed?.title ?: selectedProject?.name ?: "Life Minder — ${destination.title}", maxLines = 1) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = selectedProject?.let { Color(it.colorArgb) } ?: MaterialTheme.colorScheme.surface),
                    actions = {
                        Box {
                            TextButton(onClick = { showTopMenu = true }) { Text("Menu") }
                            DropdownMenu(expanded = showTopMenu, onDismissRequest = { showTopMenu = false }) {
                                listOf(
                                    AppDestination.UPCOMING,
                                    AppDestination.LIFE_ITEMS,
                                    AppDestination.TRIPS,
                                    AppDestination.REVIEW,
                                    AppDestination.SETTINGS,
                                ).forEach { item ->
                                    DropdownMenuItem(
                                        text = { Text(item.title) },
                                        onClick = {
                                            destination = item
                                            selectedProject = null
                                            taskBeingViewed = null
                                            medicationBeingViewedId = null
                                            lifeItemBeingViewed = null
                                            if (item == AppDestination.LIFE_ITEMS) lifeItemsShowAdd = false
                                            showTopMenu = false
                                        },
                                    )
                                }
                            }
                        }
                    },
                )
            },
            snackbarHost = { SnackbarHost(snackbar) },
            floatingActionButton = {
                if (taskBeingViewed == null && medicationBeingViewed == null && lifeItemBeingViewed == null) {
                    FloatingActionButton(
                        onClick = { showQuickAdd = true },
                        modifier = if (destination == AppDestination.TIMERS) Modifier.padding(bottom = 64.dp) else Modifier,
                    ) { Text("+", fontSize = 32.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold) }
                }
            },
            bottomBar = {
                NavigationBar {
                    AppDestination.entries.filter { it.primary }.forEach { item ->
                        NavigationBarItem(
                            selected = destination == item,
                            onClick = {
                                destination = item
                                selectedProject = null
                                taskBeingViewed = null
                                medicationBeingViewedId = null
                                lifeItemBeingViewed = null
                            },
                            icon = {
                                Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                                    Text(
                                        item.symbol,
                                        fontSize = when (item) { AppDestination.TIMERS -> 30.sp; AppDestination.DO_NOW -> 24.sp; else -> 22.sp },
                                        lineHeight = 32.sp,
                                    )
                                }
                            },
                            label = { Text(item.title) },
                            colors = NavigationBarItemDefaults.colors(indicatorColor = selectedProject?.let { Color(it.colorArgb) } ?: MaterialTheme.colorScheme.secondaryContainer),
                        )
                    }
                }
            },
        ) { padding ->
            when {
                medicationBeingViewed != null ->
                    MedicationDetailScreen(
                        medication = medicationBeingViewed,
                        occurrences = medicationOccurrences.filter { it.medicationId == medicationBeingViewed.id },
                        onBack = { medicationBeingViewedId = null },
                        onEdit = { medicationBeingEdited = medicationBeingViewed },
                        onDone = {
                            medicationOccurrences
                                .filter { it.medicationId == medicationBeingViewed.id && it.status in setOf("DUE", "SNOOZED") }
                                .minByOrNull { it.snoozedUntil ?: it.scheduledAt }
                                ?.let { occurrence ->
                                    completeTimelineItem(TimelineItem(occurrence.id, TimelineItemType.MEDICATION, medicationBeingViewed.name, occurrence.snoozedUntil ?: occurrence.scheduledAt, actionable = true, status = occurrence.status, sourceId = medicationBeingViewed.id))
                                }
                            medicationBeingViewedId = null
                        },
                        onDelete = {
                            medicationBeingViewed?.let { medication ->
                                scope.launch {
                                    alarmScheduler.cancelReminder(2_000_000 + medication.id)
                                    medicationOccurrences.filter { it.medicationId == medication.id }.forEach { occurrence -> alarmScheduler.cancelReminder(2_000_000 + occurrence.id) }
                                    lifeItemRepository.removeMedication(medication.id)
                                    snackbar.showSnackbar("Medication deleted")
                                }
                            }
                            medicationBeingViewedId = null
                        },
                        padding = padding,
                    )
                lifeItemBeingViewed != null ->
                    TimelineItemDetailScreen(
                        timelineItem = lifeItemBeingViewed!!,
                        onBack = { lifeItemBeingViewed = null },
                        onDone = {
                            lifeItemBeingViewed?.let(completeTimelineItem)
                            lifeItemBeingViewed = null
                        },
                        onDelete = {
                            lifeItemBeingViewed?.let { item ->
                                scope.launch {
                                    when (item.type) {
                                        TimelineItemType.REMINDER -> {
                                            alarmScheduler.cancelReminder(1_000_000 + item.sourceId)
                                            lifeItemRepository.removeReminder(item.sourceId)
                                        }
                                        TimelineItemType.ROUTINE -> lifeItemRepository.removeRoutine(item.sourceId)
                                        TimelineItemType.EVENT -> lifeItemRepository.removeEvent(item.sourceId)
                                        TimelineItemType.ALARM -> {
                                            alarmScheduler.cancel(3_000_000 + item.sourceId)
                                            lifeItemRepository.removeAlarm(item.sourceId)
                                        }
                                        TimelineItemType.TIMER -> {
                                            alarmScheduler.cancelTimer(4_000_000 + item.sourceId)
                                            lifeItemRepository.removeTimer(item.sourceId)
                                        }
                                        TimelineItemType.ITINERARY -> lifeItemRepository.removeItineraryEntry(item.id)
                                        TimelineItemType.TASK, TimelineItemType.MEDICATION -> Unit
                                    }
                                    snackbar.showSnackbar("${item.type.displayName()} deleted")
                                }
                            }
                            lifeItemBeingViewed = null
                        },
                        padding = padding,
                    )
                taskBeingViewed != null ->
                    TaskDetailScreen(
                        task = taskBeingViewed!!,
                        projectName = projects.firstOrNull { it.id == taskBeingViewed!!.projectId }?.name,
                        tripName = tripTasks.firstOrNull { it.taskId == taskBeingViewed!!.id }?.tripId?.let { tripId -> trips.firstOrNull { it.id == tripId }?.name },
                        contexts = contexts.filter { it.id in taskBeingViewed!!.contextIds }.map { it.name },
                        tags = tags.filter { it.id in taskBeingViewed!!.tagIds }.map { it.name },
                        dependencies = tasks.filter { it.id in taskBeingViewed!!.dependencyIds }.map { it.title },
                        onBack = { taskBeingViewed = null },
                        onEdit = { taskBeingEdited = taskBeingViewed },
                        onComplete = {
                            taskBeingViewed?.let(completeTask)
                            taskBeingViewed = null
                        },
                        onDelete = {
                            taskBeingViewed?.let { task ->
                                scope.launch {
                                    taskRepository.delete(task)
                                    if (snackbar.showSnackbar("Task deleted", "Undo") == SnackbarResult.ActionPerformed) taskRepository.restore(task)
                                }
                            }
                            taskBeingViewed = null
                        },
                        padding = padding,
                    )
                selectedProject != null ->
                    ProjectDetailScreen(
                        project = selectedProject!!,
                        tasks =
                            tasks.filter {
                                it.projectId == selectedProject!!.id && it.status != TaskStatus.ARCHIVED
                            },
                        sessions = sessions.filter { it.projectId == selectedProject!!.id },
                        onBack = { selectedProject = null },
                        onEditProject = { projectBeingEdited = selectedProject },
                        onAddTask = {
                            taskBeingEdited =
                                Task(
                                    title = "",
                                    projectId = selectedProject!!.id,
                                    status = TaskStatus.READY,
                                )
                        },
                        onEditTask = { taskBeingEdited = it },
                        onComplete = completeTask,
                        padding = padding,
                    )
                destination == AppDestination.INBOX ->
                    TaskListScreen(
                        title = "Unassigned tasks — capture now, organize later",
                        tasks = tasks.filter { it.status == TaskStatus.INBOX },
                        projects = projects,
                        onEdit = { taskBeingEdited = it },
                        onComplete = completeTask,
                        padding = padding,
                    )
                destination == AppDestination.PROJECTS ->
                    ProjectListScreen(
                        projects = projects,
                        tasks = tasks,
                        trips = trips,
                        checklists = checklists,
                        checklistItems = checklistItems,
                        onOpen = { selectedProject = it },
                        onEdit = { projectBeingEdited = it },
                        onOpenTrip = { tripId ->
                            selectedTripId = tripId
                            destination = AppDestination.TRIPS
                        },
                        onOpenChecklist = { selectedChecklistId = it },
                        onNewProject = { projectBeingEdited = Project(name = "") },
                        onNewTrip = {
                            selectedTripId = null
                            destination = AppDestination.TRIPS
                        },
                        padding = padding,
                    )
                destination == AppDestination.SETTINGS ->
                    MetadataSettingsScreen(
                        contexts = contexts,
                        tags = tags,
                        onSaveContext = { scope.launch { contextRepository.save(it) } },
                        onDeleteContext = { scope.launch { contextRepository.delete(it) } },
                        onSaveTag = { scope.launch { tagRepository.save(it) } },
                        onDeleteTag = { scope.launch { tagRepository.delete(it) } },
                        appSettings = appSettings,
                        darkMode = themeMode == ThemeMode.DARK,
                        onDarkMode = { enabled -> themeMode = if (enabled) ThemeMode.DARK else ThemeMode.LIGHT; appSettings.themeMode = themeMode },
                        dataPortability = dataPortability,
                        dataFiles = dataFiles,
                        deviceCalendars = deviceCalendars,
                        selectedCalendarIds = selectedCalendarIds,
                        onSelectedCalendarIds = { selectedCalendarIds = it; appSettings.selectedCalendarIds = it },
                        calendarPermissionGranted = calendarPermissionGranted,
                        onRequestCalendarPermission = onRequestCalendarPermission,
                        onOpenCalendarPermissionSettings = onOpenCalendarPermissionSettings,
                        onRefreshDeviceCalendars = { calendarRefreshRequest++ },
                        platformActions = platformActions,
                        padding = padding,
                    )
                destination == AppDestination.TODAY ->
                    UnifiedTodayScreen(
                        timeline = timeline.mapNotNull { item ->
                            if (item.type != TimelineItemType.ALARM) item else lifeAlarms.firstOrNull { it.id == item.id }?.takeIf { it.enabled }?.let { item.copy(startAt = it.scheduledAt, status = "SCHEDULED") }
                        } + deviceCalendarTimeline,
                        tasks = tasks,
                        plan = dailyPlan,
                        projects = projects,
                        onEdit = { showQuickAdd = false; taskBeingViewed = it },
                        onCompleteTask = completeTask,
                        onSavePlan = { scope.launch { planningRepository.save(it) } },
                        onRemove = { taskId -> scope.launch { planningRepository.remove(todayKey, taskId) } },
                        onOpenTimeline = { item ->
                            when (item.type) {
                                TimelineItemType.TASK -> tasks.firstOrNull { it.id == item.id }?.let { showQuickAdd = false; taskBeingViewed = it }
                                TimelineItemType.MEDICATION -> {
                                    showQuickAdd = false
                                    medicationBeingViewedId = item.sourceId
                                }
                                TimelineItemType.REMINDER, TimelineItemType.ROUTINE -> {
                                    showQuickAdd = false
                                    lifeItemBeingViewed = item
                                }
                                TimelineItemType.EVENT -> {
                                    if (item.status == "CALENDAR") {
                                        selectedCalendarEvent = deviceCalendarEvents.firstOrNull { it.id == item.id && it.calendarId == item.sourceId }
                                    } else {
                                        showQuickAdd = false
                                        lifeItemBeingViewed = item
                                    }
                                }
                                else -> {
                                    showQuickAdd = false
                                    lifeItemBeingViewed = item
                                }
                            }
                        },
                        onDoneTimeline = completeTimelineItem,
                        padding = padding,
                    )
                destination == AppDestination.LIFE_ITEMS ->
                    LifeItemsScreen(
                        reminders,
                        routines,
                        routineOccurrences,
                        medications,
                        medicationOccurrences,
                        events,
                        lifeAlarms,
                        standaloneTimers,
                        timerPresets,
                        lifeItemQuickType,
                        lifeItemsShowAdd,
                        onOpenMedication = { medicationId ->
                            lifeItemsShowAdd = false
                            medicationBeingViewedId = medicationId
                        },
                        repository = lifeItemRepository,
                        alarmScheduler = alarmScheduler,
                        padding = padding,
                    )
                destination == AppDestination.UPCOMING ->
                    UpcomingScreen(timeline + deviceCalendarTimeline, now, padding)
                destination == AppDestination.TRIPS ->
                    TripsScreen(
                        trips,
                        itinerary,
                        tripReferences,
                        tripTasks,
                        transportationDetails,
                        lodgingDetails,
                        tasks,
                        lifeItemRepository,
                        alarmScheduler,
                        platformActions,
                        selectedTripId,
                        tripEntryRequest,
                        onAddTask = { tripId ->
                            pendingTripTaskId = tripId
                            taskBeingEdited = Task(title = "")
                        },
                        padding,
                    )
                destination == AppDestination.DO_NOW ->
                    DoNowScreen(
                        tasks = tasks,
                        plan = dailyPlan,
                        now = now,
                        onEdit = { taskBeingEdited = it },
                        onComplete = completeTask,
                        onStart = { task ->
                            val started = currentTimeMillis()
                            val timer = ActiveTimer(taskId = task.id, mode = if (task.estimatedMinutes == null) TimerMode.STOPWATCH else TimerMode.COUNTDOWN, state = TimerState.RUNNING, startedAt = started, intendedDurationMillis = task.estimatedMinutes?.times(60_000L))
                            scope.launch {
                                taskRepository.save(task.copy(status = TaskStatus.IN_PROGRESS))
                                timerRepository.saveActive(timer)
                                alarmScheduler.scheduleTimer(timer)
                            }
                            destination = AppDestination.TIMERS
                        },
                        onFeedback = { taskId, action -> scope.launch { planningRepository.feedback(taskId, action, currentTimeMillis()) } },
                        onSavePlan = { scope.launch { planningRepository.save(it) } },
                        padding = padding,
                    )
                destination == AppDestination.TIMERS ->
                    TimerScreen(
                        active = activeTimer,
                        sessions = sessions,
                        alarms = alarms,
                        standaloneAlarms = lifeAlarms,
                        standaloneTimers = standaloneTimers,
                        presets = timerPresets,
                        history = timerHistory,
                        profiles = alarmProfiles,
                        platformActions = platformActions,
                        tasks = tasks,
                        timerCreationRequest = timerCreationRequest,
                        alarmCreationRequest = alarmCreationRequest,
                        stopwatchLaps = stopwatchLaps,
                        onLap = { stopwatchLaps = stopwatchLaps + it },
                        onSaveTimer = { timer ->
                            scope.launch {
                                timerRepository.saveActive(timer)
                                alarmScheduler.scheduleTimer(timer)
                            }
                        },
                        onStop = { timer, endedAt, accomplishment ->
                            scope.launch {
                                val activeMillis = TimerEngine.elapsed(timer, endedAt)
                                if (activeMillis >= 60_000L) timerRepository.addSession(WorkSession(taskId = timer.taskId, projectId = tasks.firstOrNull { it.id == timer.taskId }?.projectId, startedAt = timer.startedAt, endedAt = endedAt, pausedMillis = (endedAt - timer.startedAt - activeMillis).coerceAtLeast(0), activeMillis = activeMillis, plannedMillis = timer.intendedDurationMillis, mode = timer.mode, accomplishment = accomplishment))
                                timerRepository.clearActive()
                                alarmScheduler.cancelTimer()
                            }
                        },
                        onAddAlarm = { alarm ->
                            scope.launch {
                                val id = timerRepository.addAlarm(alarm)
                                alarmScheduler.schedule(alarm.copy(id = id))
                            }
                        },
                        onDismissAlarm = { alarm ->
                            scope.launch {
                                timerRepository.disableAlarm(alarm.id)
                                alarmScheduler.cancel(alarm.id)
                            }
                        },
                        onSaveStandaloneAlarm = { alarm ->
                            scope.launch {
                                val id = lifeItemRepository.save(alarm)
                                alarmScheduler.cancel(3_000_000 + id)
                                if (alarm.enabled) alarmScheduler.schedule(AlarmDefinition(id = 3_000_000 + id, triggerAt = alarm.scheduledAt, label = alarm.label, vibration = alarm.vibration, sound = alarm.sound))
                            }
                        },
                        onStartStandaloneTimer = { name, duration ->
                            scope.launch {
                                val started = currentTimeMillis()
                                val id = lifeItemRepository.save(StandaloneTimerEntity(name = name, startedAt = started, resumedAt = started, durationMillis = duration, updatedAt = started))
                                alarmScheduler.scheduleTimer(ActiveTimer(id = 4_000_000 + id, mode = TimerMode.COUNTDOWN, state = TimerState.RUNNING, startedAt = started, resumedAt = started, intendedDurationMillis = duration, updatedAt = started))
                            }
                        },
                        onSavePreset = { preset -> scope.launch { lifeItemRepository.save(preset) } },
                        onDeletePreset = { preset -> scope.launch { lifeItemRepository.removePreset(preset.id) } },
                        onSaveProfile = { profile -> scope.launch { lifeItemRepository.save(profile) } },
                        onSetDefaultProfile = { profile -> scope.launch { lifeItemRepository.setDefaultAlarmProfile(profile.id) } },
                        onDeleteProfile = { profile -> scope.launch { lifeItemRepository.removeAlarmProfile(profile.id) } },
                        onToggleStandaloneTimer = { timer ->
                            scope.launch {
                                if (timer.state == "RUNNING") {
                                    lifeItemRepository.save(timer.copy(state = "DISABLED", updatedAt = currentTimeMillis()))
                                    alarmScheduler.cancelTimer(4_000_000 + timer.id)
                                } else {
                                    val started = currentTimeMillis()
                                    val updated = timer.copy(state = "RUNNING", startedAt = started, resumedAt = started, accumulatedMillis = 0, completedAt = null, updatedAt = started)
                                    lifeItemRepository.save(updated)
                                    alarmScheduler.scheduleTimer(ActiveTimer(id = 4_000_000 + updated.id, mode = TimerMode.COUNTDOWN, state = TimerState.RUNNING, startedAt = started, resumedAt = started, intendedDurationMillis = updated.durationMillis, updatedAt = started))
                                }
                            }
                        },
                        onToggleStandaloneAlarm = { alarm ->
                            scope.launch {
                                val enabled = !alarm.enabled
                                lifeItemRepository.setAlarmEnabled(alarm.id, enabled, currentTimeMillis())
                                if (enabled) alarmScheduler.schedule(AlarmDefinition(id = 3_000_000 + alarm.id, triggerAt = alarm.scheduledAt, label = alarm.label, vibration = alarm.vibration, sound = alarm.sound))
                                else alarmScheduler.cancel(3_000_000 + alarm.id)
                            }
                        },
                        onDeleteStandaloneAlarm = { alarm ->
                            scope.launch {
                                alarmScheduler.cancel(3_000_000 + alarm.id)
                                lifeItemRepository.removeAlarm(alarm.id)
                            }
                        },
                        onDeleteStandaloneTimer = { timer ->
                            scope.launch {
                                alarmScheduler.cancelTimer(4_000_000 + timer.id)
                                lifeItemRepository.removeTimer(timer.id)
                            }
                        },
                        padding = padding,
                    )
                destination == AppDestination.REVIEW ->
                    ReviewScreen(
                        tasks = tasks,
                        projects = projects,
                        sessions = sessions,
                        reviews = reviews,
                        plannedTaskIds = dailyPlan.mapTo(mutableSetOf()) { it.taskId },
                        now = now,
                        weeklyDeferrals = weeklyDeferrals,
                        onSave = { scope.launch { reviewRepository.save(it) } },
                        onProcessInbox = { destination = AppDestination.INBOX },
                        padding = padding,
                    )
                else -> DestinationPlaceholder(destination, { destination = it }, padding)
            }
        }
    }

    selectedCalendarEvent?.let { event ->
        AlertDialog(
            onDismissRequest = { selectedCalendarEvent = null },
            title = { Text(event.title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("From ${deviceCalendarNames[event.calendarId] ?: "device calendar"}", fontWeight = FontWeight.SemiBold)
                    Text(
                        if (event.allDay) {
                            "${formatDate(event.startAt)} · All day"
                        } else {
                            "${formatAlarmTime(event.startAt)} – ${formatAlarmTime(event.endAt)}"
                        },
                    )
                    if (event.location.isNotBlank()) Text(event.location)
                }
            },
            confirmButton = { Button(onClick = { selectedCalendarEvent = null }) { Text("Done") } },
        )
    }

    medicationBeingEdited?.let { original ->
        MedicationEditorDialog(
            original = original,
            onDismiss = { medicationBeingEdited = null },
            onSave = { updated ->
                scope.launch {
                    lifeItemRepository.save(updated)
                    val minutes = updated.timesOfDay.substringBefore(',').trim().toIntOrNull()
                    val nextOccurrence = medicationOccurrences.filter { it.medicationId == updated.id && it.status in setOf("DUE", "SNOOZED") }.minByOrNull { it.snoozedUntil ?: it.scheduledAt }
                    if (minutes != null && nextOccurrence != null) {
                        val rescheduled = nextOccurrence.copy(scheduledAt = dayKey(nextOccurrence.scheduledAt) * DAY_MILLIS + minutes * 60_000L, snoozedUntil = null, updatedAt = currentTimeMillis())
                        lifeItemRepository.save(rescheduled)
                        alarmScheduler.cancelReminder(2_000_000 + nextOccurrence.id)
                        if (rescheduled.scheduledAt > currentTimeMillis()) alarmScheduler.scheduleReminder(2_000_000 + rescheduled.id, rescheduled.scheduledAt, updated.name, NotificationCategory.TASK_REMINDER)
                    }
                }
                medicationBeingEdited = null
            },
        )
    }

    taskBeingEdited?.let { original ->
        TaskEditorDialog(
            original = original,
            projects = projects,
            trips = trips,
            initialTripId = pendingTripTaskId ?: tripTasks.firstOrNull { it.taskId == original.id }?.tripId,
            contexts = contexts,
            tags = tags,
            availableDependencies = tasks.filter { it.id != original.id && it.status != TaskStatus.COMPLETED },
            onDismiss = { taskBeingEdited = null },
            onSave = { task, selectedTaskTripId ->
                val tripToLink = selectedTaskTripId
                scope.launch {
                    val id = taskRepository.save(task)
                    tripTasks.filter { it.taskId == id && it.tripId != tripToLink }.forEach { link ->
                        lifeItemRepository.unlinkTripTask(link.tripId, id)
                    }
                    tripToLink?.let { tripId ->
                        lifeItemRepository.save(com.geepscontrol.core.database.TripTaskEntity(tripId, id))
                    }
                    alarmScheduler.cancelReminder(id * 10 + 1)
                    alarmScheduler.cancelReminder(id * 10 + 2)
                    alarmScheduler.cancelReminder(id * 10 + 3)
                    task.reminderAt?.takeIf { it > currentTimeMillis() }?.let { alarmScheduler.scheduleReminder(id * 10 + 1, it, task.title, NotificationCategory.TASK_REMINDER) }
                    task.dueAt?.let { due ->
                        (due - DAY_MILLIS).takeIf { it > currentTimeMillis() }?.let { alarmScheduler.scheduleReminder(id * 10 + 2, it, "Due tomorrow: ${task.title}", NotificationCategory.DUE_STATUS) }
                        due.takeIf { it > currentTimeMillis() }?.let { alarmScheduler.scheduleReminder(id * 10 + 3, it, "Due now: ${task.title}", NotificationCategory.DUE_STATUS) }
                    }
                    if (original.id == 0L && task.estimatedMinutes?.let { it in 1..5 } == true) {
                        snackbar.showSnackbar("Tasks that take 5 minutes or less are usually best done right away.")
                    }
                }
                if (taskBeingViewed?.id == task.id) taskBeingViewed = task
                taskBeingEdited = null
                pendingTripTaskId = null
            },
            onDelete =
                if (original.id == 0L) {
                    null
                } else {
                    {
                        scope.launch {
                            taskRepository.delete(original)
                            if (snackbar.showSnackbar("Task deleted", "Undo") == SnackbarResult.ActionPerformed) {
                                taskRepository.restore(original)
                            }
                        }
                        taskBeingEdited = null
                    }
                },
        )
    }

    projectBeingEdited?.let { original ->
        ProjectEditorDialog(
            original = original,
            onDismiss = { projectBeingEdited = null },
            onSave = { project ->
                scope.launch { projectRepository.save(project) }
                projectBeingEdited = null
            },
        )
    }
    if (showPlanTypeDialog) {
        AlertDialog(
            onDismissRequest = { showPlanTypeDialog = false },
            title = { Text("New plan") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("What kind of plan are you creating?")
                    Button(onClick = { showPlanTypeDialog = false; projectBeingEdited = Project(name = "") }, modifier = Modifier.fillMaxWidth()) { Text("Project") }
                    Button(onClick = { showPlanTypeDialog = false; selectedTripId = null; destination = AppDestination.TRIPS }, modifier = Modifier.fillMaxWidth()) { Text("Trip") }
                    Button(onClick = { showPlanTypeDialog = false; newListName = ""; showNewListDialog = true }, modifier = Modifier.fillMaxWidth()) { Text("List") }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showPlanTypeDialog = false }) { Text("Cancel") } },
        )
    }
    if (showQuickAdd) {
        AlertDialog(
            onDismissRequest = { showQuickAdd = false },
            title = { Text("Quick add") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    fun close(action: () -> Unit) { showQuickAdd = false; action() }
                    val purple = ButtonDefaults.buttonColors(containerColor = Color(0xFF6542B5), contentColor = Color.White)
                    val timeColor = ButtonDefaults.buttonColors(containerColor = Color(0xFFA94700), contentColor = Color.White)
                    val scheduleColor = ButtonDefaults.buttonColors(containerColor = Color(0xFF006B5F), contentColor = Color.White)
                    Button(onClick = { close { pendingTripTaskId = selectedTripId.takeIf { destination == AppDestination.TRIPS }; taskBeingEdited = Task(title = "", projectId = selectedProject?.id, status = if (selectedProject == null) TaskStatus.INBOX else TaskStatus.READY) } }, modifier = Modifier.fillMaxWidth()) { Text(if (selectedTripId != null && destination == AppDestination.TRIPS) "Trip task" else "Task") }
                    Button(onClick = { close { projectBeingEdited = Project(name = "") } }, modifier = Modifier.fillMaxWidth(), colors = purple) { Text("Project") }
                    Button(onClick = { close { lifeItemQuickType = "Reminder"; lifeItemsShowAdd = true; destination = AppDestination.LIFE_ITEMS } }, modifier = Modifier.fillMaxWidth(), colors = purple) { Text("Reminder") }
                    Button(onClick = { close { destination = AppDestination.TIMERS; timerCreationRequest += 1 } }, modifier = Modifier.fillMaxWidth(), colors = timeColor) { Text("Timer") }
                    Button(onClick = { close { destination = AppDestination.TIMERS; alarmCreationRequest += 1 } }, modifier = Modifier.fillMaxWidth(), colors = timeColor) { Text("Alarm") }
                    Button(onClick = { close { lifeItemQuickType = "Medication"; lifeItemsShowAdd = true; destination = AppDestination.LIFE_ITEMS } }, modifier = Modifier.fillMaxWidth(), colors = scheduleColor) { Text("Medication") }
                    Button(onClick = { close { lifeItemQuickType = "Event"; lifeItemsShowAdd = true; destination = AppDestination.LIFE_ITEMS } }, modifier = Modifier.fillMaxWidth(), colors = scheduleColor) { Text("Event") }
                    Button(onClick = { close { lifeItemQuickType = "Routine"; lifeItemsShowAdd = true; destination = AppDestination.LIFE_ITEMS } }, modifier = Modifier.fillMaxWidth(), colors = scheduleColor) { Text("Routine") }
                    OutlinedButton(onClick = { close { selectedTripId = null; destination = AppDestination.TRIPS } }, modifier = Modifier.fillMaxWidth()) { Text("Trip") }
                    if (destination == AppDestination.TRIPS && selectedTripId != null) OutlinedButton(onClick = { close { tripEntryRequest += 1 } }, modifier = Modifier.fillMaxWidth()) { Text("Place") }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showQuickAdd = false }) { Text("Cancel") } },
        )
    }
    if (showNewListDialog) {
        AlertDialog(
            onDismissRequest = { showNewListDialog = false },
            title = { Text("New list") },
            text = { OutlinedTextField(newListName, { newListName = it }, label = { Text("List name") }, singleLine = true) },
            confirmButton = { Button(enabled = newListName.isNotBlank(), onClick = { scope.launch { selectedChecklistId = lifeItemRepository.save(ChecklistEntity(name = newListName.trim(), createdAt = currentTimeMillis(), updatedAt = currentTimeMillis())) }; showNewListDialog = false }) { Text("Create") } },
            dismissButton = { TextButton(onClick = { showNewListDialog = false }) { Text("Cancel") } },
        )
    }
    selectedChecklistId?.let { listId ->
        checklists.firstOrNull { it.id == listId }?.let { checklist ->
            ChecklistDialog(
                checklist = checklist,
                items = checklistItems.filter { it.checklistId == listId },
                onDismiss = { selectedChecklistId = null },
                onAdd = { text -> scope.launch { val now = currentTimeMillis(); lifeItemRepository.save(ChecklistItemEntity(checklistId = listId, text = text, sortOrder = checklistItems.count { it.checklistId == listId }, createdAt = now, updatedAt = now)) } },
                onToggle = { item -> scope.launch { lifeItemRepository.save(item.copy(checked = !item.checked, updatedAt = currentTimeMillis())) } },
                onDelete = { scope.launch { lifeItemRepository.deleteChecklist(listId) }; selectedChecklistId = null },
            )
        }
    }
    if (showTripAddDialog) {
        AlertDialog(
            onDismissRequest = { showTripAddDialog = false },
            title = { Text("Add to trip") },
            text = { Text("Add something you need to do, or a place on the itinerary.") },
            confirmButton = {
                Button(onClick = {
                    showTripAddDialog = false
                    pendingTripTaskId = selectedTripId
                    taskBeingEdited = Task(title = "")
                }) { Text("Task") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showTripAddDialog = false
                    tripEntryRequest += 1
                }) { Text("Location") }
            },
        )
    }
    if (showTimerAddDialog) {
        AlertDialog(
            onDismissRequest = { showTimerAddDialog = false },
            title = { Text("Add timer or alarm") },
            text = { Text("Timers count a duration. Alarms ring at a clock time and can repeat.") },
            confirmButton = {
                Button(onClick = { showTimerAddDialog = false; timerCreationRequest += 1 }) { Text("Timer") }
            },
            dismissButton = {
                TextButton(onClick = { showTimerAddDialog = false; alarmCreationRequest += 1 }) { Text("Alarm") }
            },
        )
    }
    if (showOnboarding) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Welcome to Life Minder") },
            text = { Text("Life Minder keeps your tasks, timers, alarms, and reviews on this device. No account or network connection is required. Start empty or add a few editable starter projects.") },
            confirmButton = {
                Button(onClick = {
                    scope.launch { listOf("Home", "Health", "Learning").forEach { projectRepository.save(Project(name = it, desiredOutcome = "Define what success looks like")) } }
                    appSettings.onboardingCompleted = true
                    showOnboarding = false
                }) { Text("Add starter projects") }
            },
            dismissButton = {
                TextButton(onClick = {
                    appSettings.onboardingCompleted = true
                    showOnboarding = false
                }) { Text("Start empty") }
            },
        )
    }
}

private fun completeWithUndo(
    task: Task,
    repository: TaskRepository,
    snackbar: SnackbarHostState,
    scope: kotlinx.coroutines.CoroutineScope,
) {
    scope.launch {
        val previous = repository.complete(task.id) ?: return@launch
        if (snackbar.showSnackbar("Completed: ${task.title}", "Undo") == SnackbarResult.ActionPerformed) {
            repository.restore(previous)
        }
    }
}

@Composable
private fun TaskListScreen(
    title: String,
    tasks: List<Task>,
    projects: List<Project>,
    onEdit: (Task) -> Unit,
    onComplete: (Task) -> Unit,
    padding: PaddingValues,
) {
    var query by remember { mutableStateOf("") }
    var priorityFilter by remember { mutableStateOf<Priority?>(null) }
    val filtered =
        tasks.filter {
            (query.isBlank() || it.title.contains(query, true) || it.description.contains(query, true)) &&
                (priorityFilter == null || it.priority == priorityFilter)
        }
    Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(query, { query = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Search tasks") }, singleLine = true)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(null, Priority.CRITICAL, Priority.HIGH).forEach { priority ->
                OutlinedButton(onClick = { priorityFilter = priority }) { Text(priority?.displayName() ?: "All") }
            }
        }
        if (filtered.isEmpty()) {
            EmptyMessage("Nothing here yet. Tap + to capture a task.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filtered, key = { it.id }) { task ->
                    TaskCard(task, projects.firstOrNull { it.id == task.projectId }?.name, onEdit, onComplete)
                }
            }
        }
    }
}

@Composable
private fun TaskCard(
    task: Task,
    projectName: String?,
    onEdit: (Task) -> Unit,
    onComplete: (Task) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth().clickable { onEdit(task) }, colors = CardDefaults.cardColors(containerColor = priorityColor(task.priority).copy(alpha = 0.16f))) {
        Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = { onComplete(task) }) { Text("✓") }
            Column(Modifier.weight(1f)) {
                Text(task.title, fontWeight = FontWeight.SemiBold, textDecoration = if (task.status == TaskStatus.COMPLETED) TextDecoration.LineThrough else null)
                Text(
                    listOfNotNull(
                        projectName,
                        task.priority.displayName(),
                        task.estimatedMinutes?.let { "$it min" },
                        task.dueAt?.let(::dueLabel),
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                )
                if (task.isBlocked) {
                    Text(
                        "Blocked by ${task.blockingTaskTitles.joinToString()}",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProjectListScreen(
    projects: List<Project>,
    tasks: List<Task>,
    trips: List<TripEntity>,
    checklists: List<ChecklistEntity>,
    checklistItems: List<ChecklistItemEntity>,
    onOpen: (Project) -> Unit,
    onEdit: (Project) -> Unit,
    onOpenTrip: (Long) -> Unit,
    onOpenChecklist: (Long) -> Unit,
    onNewProject: () -> Unit,
    onNewTrip: () -> Unit,
    padding: PaddingValues,
) {
    var query by remember { mutableStateOf("") }
    val filtered = projects.filter { query.isBlank() || it.name.contains(query, true) || it.description.contains(query, true) }
    Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("New", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onNewProject, modifier = Modifier.weight(1f)) { Text("Project") }
            OutlinedButton(onClick = onNewTrip, modifier = Modifier.weight(1f)) { Text("Trip") }
        }
        OutlinedTextField(query, { query = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Search plans") }, singleLine = true)
        if (trips.isNotEmpty()) Text("Trips", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        trips.filter { query.isBlank() || it.name.contains(query, true) || it.description.contains(query, true) }.forEach { trip ->
            Card(Modifier.fillMaxWidth().clickable { onOpenTrip(trip.id) }, colors = CardDefaults.cardColors(containerColor = Color(trip.coverColor).copy(alpha = 0.22f))) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(trip.name, fontWeight = FontWeight.Bold)
                    Text("${formatDate(trip.startDate)} – ${formatDate(trip.endDate)} · ${trip.status.lowercase()}", style = MaterialTheme.typography.bodySmall)
                    if (trip.description.isNotBlank()) Text(trip.description, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                }
            }
        }
        val visibleLists = checklists.filter { query.isBlank() || it.name.contains(query, true) }
        if (visibleLists.isNotEmpty()) Text("Lists", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        visibleLists.forEach { checklist ->
            val listItems = checklistItems.filter { it.checklistId == checklist.id }
            Card(Modifier.fillMaxWidth().clickable { onOpenChecklist(checklist.id) }) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(checklist.name, fontWeight = FontWeight.Bold)
                    Text("${listItems.count { it.checked }} of ${listItems.size} checked", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        if (filtered.isNotEmpty()) Text("Projects", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        if (filtered.isEmpty()) {
            if (trips.isEmpty() && checklists.isEmpty()) EmptyMessage("No plans yet. Tap + to create a project, trip, or list.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filtered, key = { it.id }) { project ->
                    val projectTasks = tasks.filter { it.projectId == project.id }
                    Card(Modifier.fillMaxWidth().clickable { onOpen(project) }, colors = CardDefaults.cardColors(containerColor = Color(project.colorArgb).copy(alpha = 0.22f))) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(project.name, fontWeight = FontWeight.Bold)
                                TextButton(onClick = { onEdit(project) }) { Text("Edit") }
                            }
                            Text(project.desiredOutcome.ifBlank { project.description.ifBlank { "No desired outcome yet" } })
                            Text("${projectTasks.count { it.status != TaskStatus.COMPLETED }} open · ${projectTasks.count { it.status == TaskStatus.COMPLETED }} completed", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProjectDetailScreen(
    project: Project,
    tasks: List<Task>,
    sessions: List<WorkSession>,
    onBack: () -> Unit,
    onEditProject: () -> Unit,
    onAddTask: () -> Unit,
    onEditTask: (Task) -> Unit,
    onComplete: (Task) -> Unit,
    padding: PaddingValues,
) {
    Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onBack) { Text("Back") }
            OutlinedButton(onClick = onEditProject) { Text("Edit project") }
            Button(onClick = onAddTask) { Text("Add task") }
        }
        Text(project.desiredOutcome.ifBlank { "No desired outcome defined." })
        Text("Focused ${formatDuration(sessions.sumOf { it.activeMillis })} across ${sessions.size} sessions")
        if (tasks.isEmpty()) {
            EmptyMessage("This project has no tasks yet.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(tasks, key = { it.id }) { TaskCard(it, null, onEditTask, onComplete) }
            }
        }
    }
}

@Composable
private fun MedicationEditorDialog(
    original: MedicationEntity,
    onDismiss: () -> Unit,
    onSave: (MedicationEntity) -> Unit,
) {
    val originalMinutes = original.timesOfDay.substringBefore(',').trim().toIntOrNull() ?: 9 * 60
    var name by remember(original.id) { mutableStateOf(original.name) }
    var dosage by remember(original.id) { mutableStateOf(original.dosageText) }
    var instructions by remember(original.id) { mutableStateOf(original.instructions) }
    var hour by remember(original.id) { mutableStateOf((originalMinutes / 60).toString()) }
    var minute by remember(original.id) { mutableStateOf((originalMinutes % 60).toString().padStart(2, '0')) }
    var repeatDaily by remember(original.id) { mutableStateOf(original.scheduleType != "ONCE") }
    var active by remember(original.id) { mutableStateOf(original.active) }
    var quantityRemaining by remember(original.id) { mutableStateOf(original.quantityRemaining?.toString().orEmpty()) }
    var notes by remember(original.id) { mutableStateOf(original.generalNotes) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit ${original.name}") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { OutlinedTextField(name, { name = it }, label = { Text("Medication name") }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
                item { OutlinedTextField(dosage, { dosage = it }, label = { Text("Dosage") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(instructions, { instructions = it }, label = { Text("Instructions") }, modifier = Modifier.fillMaxWidth()) }
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(hour, { hour = it.filter(Char::isDigit).take(2) }, label = { Text("Hour (0–23)") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        OutlinedTextField(minute, { minute = it.filter(Char::isDigit).take(2) }, label = { Text("Minute") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    }
                }
                item { Row(verticalAlignment = Alignment.CenterVertically) { Switch(repeatDaily, { repeatDaily = it }); Text("Repeat daily", modifier = Modifier.padding(start = 8.dp)) } }
                item { Row(verticalAlignment = Alignment.CenterVertically) { Switch(active, { active = it }); Text("Active", modifier = Modifier.padding(start = 8.dp)) } }
                item { OutlinedTextField(quantityRemaining, { quantityRemaining = it.filter { char -> char.isDigit() || char == '.' } }, label = { Text("Quantity remaining") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)) }
                item { OutlinedTextField(notes, { notes = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth(), minLines = 2) }
            }
        },
        confirmButton = {
            Button(enabled = name.isNotBlank(), onClick = {
                val timeMinutes = (hour.toIntOrNull() ?: 9).coerceIn(0, 23) * 60 + (minute.toIntOrNull() ?: 0).coerceIn(0, 59)
                onSave(
                    original.copy(
                        name = name.trim(),
                        dosageText = dosage.trim(),
                        instructions = instructions.trim(),
                        timesOfDay = timeMinutes.toString(),
                        scheduleType = if (repeatDaily) "DAILY" else "ONCE",
                        recurrenceRule = if (repeatDaily) "1:DAY:FIXED" else null,
                        active = active,
                        quantityRemaining = quantityRemaining.toDoubleOrNull(),
                        generalNotes = notes.trim(),
                        updatedAt = currentTimeMillis(),
                    ),
                )
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun MedicationDetailScreen(
    medication: MedicationEntity,
    occurrences: List<MedicationOccurrenceEntity>,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDone: () -> Unit,
    onDelete: () -> Unit,
    padding: PaddingValues,
) {
    var confirmDelete by remember(medication.id) { mutableStateOf(false) }
    val nextOccurrence = occurrences.filter { it.status in setOf("DUE", "SNOOZED") }.minByOrNull { it.snoozedUntil ?: it.scheduledAt }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = onBack) { Text("Back") }
                Button(onClick = onEdit) { Text("Edit medication") }
            }
        }
        item { Text(medication.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
        item { Text("Medication · ${if (medication.active) "active" else "paused"}") }
        if (medication.dosageText.isNotBlank()) item { Text("Dosage: ${medication.dosageText}") }
        if (medication.instructions.isNotBlank()) item { Text("Instructions: ${medication.instructions}") }
        item { Text("Schedule: ${if (medication.scheduleType == "ONCE") "Once" else "Daily"} at ${formatMedicationTimes(medication.timesOfDay)}") }
        item { Text("Starts: ${formatDate(medication.startDate)}") }
        medication.endDate?.let { item { Text("Ends: ${formatDate(it)}") } }
        nextOccurrence?.let { item { Text("Next: ${formatAlarmTime(it.snoozedUntil ?: it.scheduledAt)}") } }
        medication.quantityRemaining?.let { item { Text("Quantity remaining: $it") } }
        if (medication.prescribingNotes.isNotBlank()) item { Text("Prescribing notes: ${medication.prescribingNotes}") }
        if (medication.generalNotes.isNotBlank()) item { Text("Notes: ${medication.generalNotes}") }
        if (occurrences.any { it.status in setOf("DUE", "SNOOZED") }) item { Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("Done") } }
        item { BottomLeftDeleteButton { confirmDelete = true } }
    }
    DeleteItemDialog(medication.name, confirmDelete, { confirmDelete = false }, onDelete)
}

@Composable
private fun TimelineItemDetailScreen(
    timelineItem: TimelineItem,
    onBack: () -> Unit,
    onDone: () -> Unit,
    onDelete: () -> Unit,
    padding: PaddingValues,
) {
    var confirmDelete by remember(timelineItem.type, timelineItem.id) { mutableStateOf(false) }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
    ) {
        item { OutlinedButton(onClick = onBack) { Text("Back") } }
        item { Text(timelineItem.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
        item { Text("${timelineItem.type.displayName()} · ${timelineItem.status.lowercase()}") }
        timelineItem.startAt?.let { start -> item { Text("Scheduled: ${formatAlarmTime(start)}") } }
        timelineItem.endAt?.let { end -> item { Text("Ends: ${formatAlarmTime(end)}") } }
        if (timelineItem.subtitle.isNotBlank()) item { Text(timelineItem.subtitle) }
        if (timelineItem.actionable && timelineItem.status in setOf("PENDING", "DUE", "SCHEDULED", "OPEN", "SNOOZED")) item { Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("Done") } }
        item { BottomLeftDeleteButton { confirmDelete = true } }
    }
    DeleteItemDialog(timelineItem.title, confirmDelete, { confirmDelete = false }, onDelete)
}

@Composable
private fun BottomLeftDeleteButton(onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = onClick,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
        ) { Text("Delete") }
    }
}

@Composable
private fun DeleteItemDialog(
    itemName: String,
    visible: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    if (!visible) return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete item?") },
        text = { Text("Delete \"$itemName\"? This cannot be undone.") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            ) { Text("Delete") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private fun formatMedicationTimes(value: String): String =
    value.split(',').mapNotNull { it.trim().toIntOrNull() }.joinToString { minutes ->
        (minutes / 60).toString().padStart(2, '0') + ":" + (minutes % 60).toString().padStart(2, '0')
    }.ifBlank { "Not set" }

@Composable
private fun TaskDetailScreen(
    task: Task,
    projectName: String?,
    tripName: String?,
    contexts: List<String>,
    tags: List<String>,
    dependencies: List<String>,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onComplete: () -> Unit,
    onDelete: () -> Unit,
    padding: PaddingValues,
) {
    var confirmDelete by remember(task.id) { mutableStateOf(false) }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = onBack) { Text("Back") }
                Button(onClick = onEdit) { Text("Edit task") }
            }
        }
        if (task.status !in setOf(TaskStatus.COMPLETED, TaskStatus.CANCELLED, TaskStatus.ARCHIVED)) {
            item { Button(onClick = onComplete, modifier = Modifier.fillMaxWidth()) { Text("Complete") } }
        }
        item { Text(task.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
        item { Text(task.description.ifBlank { "No description or notes." }) }
        item { Text("Status: ${task.status.displayName()}") }
        item { Text("Priority: ${task.priority.displayName()}") }
        item { Text("Effort: ${task.energyRequirement.displayName()}") }
        projectName?.let { item { Text("Project: $it") } }
        tripName?.let { item { Text("Trip: $it") } }
        task.estimatedMinutes?.let { item { Text("Estimated duration: $it minutes") } }
        task.dueAt?.let { item { Text("Due: ${formatAlarmTime(it)}") } }
        task.reminderAt?.let { item { Text("Reminder: ${formatAlarmTime(it)}") } }
        task.recurrenceRule?.let { encoded ->
            val rule = RecurrenceRule.decode(encoded)
            item { Text(rule?.let { "Repeats every ${it.interval} ${it.unit.displayName().lowercase()}${if (it.interval == 1) "" else "s"}, based on ${it.mode.displayName().lowercase()} date" } ?: "Repeats: $encoded") }
        }
        if (contexts.isNotEmpty()) item { Text("Contexts: ${contexts.joinToString()}") }
        if (tags.isNotEmpty()) item { Text("Tags: ${tags.joinToString()}") }
        if (dependencies.isNotEmpty()) item { Text("Depends on: ${dependencies.joinToString()}") }
        if (task.isBlocked) item { Text("Blocked by: ${task.blockingTaskTitles.joinToString()}", color = MaterialTheme.colorScheme.error) }
        task.completedAt?.let { item { Text("Completed: ${formatAlarmTime(it)}") } }
        item { BottomLeftDeleteButton { confirmDelete = true } }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete task?") },
            text = { Text("Delete \"${task.title}\"? You can undo immediately afterward.") },
            confirmButton = {
                Button(
                    onClick = {
                        confirmDelete = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun TaskEditorDialog(
    original: Task,
    projects: List<Project>,
    trips: List<TripEntity>,
    initialTripId: Long?,
    contexts: List<TaskContext>,
    tags: List<TaskTag>,
    availableDependencies: List<Task>,
    onDismiss: () -> Unit,
    onSave: (Task, Long?) -> Unit,
    onDelete: (() -> Unit)?,
) {
    var title by remember(original.id) { mutableStateOf(original.title) }
    var description by remember(original.id) { mutableStateOf(original.description) }
    var projectId by remember(original.id) { mutableStateOf(original.projectId) }
    var hasProject by remember(original.id) { mutableStateOf(original.projectId != null) }
    var taskTripId by remember(original.id, initialTripId) { mutableStateOf(initialTripId) }
    var hasTrip by remember(original.id, initialTripId) { mutableStateOf(initialTripId != null) }
    var priority by remember(original.id) { mutableStateOf(original.priority) }
    var status by remember(original.id) { mutableStateOf(original.status) }
    var minutes by remember(original.id) { mutableStateOf(original.estimatedMinutes?.toString().orEmpty()) }
    var energy by remember(original.id) { mutableStateOf(original.energyRequirement) }
    var contextIds by remember(original.id) { mutableStateOf(original.contextIds) }
    var tagIds by remember(original.id) { mutableStateOf(original.tagIds) }
    var dependencyIds by remember(original.id) { mutableStateOf(original.dependencyIds) }
    val originalRule = remember(original.id) { RecurrenceRule.decode(original.recurrenceRule) }
    var repeatEnabled by remember(original.id) { mutableStateOf(originalRule != null) }
    var repeatInterval by remember(original.id) { mutableStateOf(originalRule?.interval?.toString() ?: "1") }
    var repeatUnit by remember(original.id) { mutableStateOf(originalRule?.unit ?: RecurrenceUnit.WEEK) }
    var repeatMode by remember(original.id) { mutableStateOf(originalRule?.mode ?: RecurrenceMode.FIXED) }
    var reminderAmount by remember(original.id) { mutableStateOf(if (original.reminderAt != null && original.dueAt != null) (((original.dueAt - original.reminderAt) / 60_000L).coerceAtLeast(0)).toString() else "") }
    var reminderUnit by remember(original.id) { mutableStateOf("minutes") }
    var dueDate by remember(original.id) { mutableStateOf(original.dueAt?.let { dayKey(it) * DAY_MILLIS }) }
    var dueHour by remember(original.id) { mutableStateOf(original.dueAt?.let { ((it - dayKey(it) * DAY_MILLIS) / 3_600_000L).toString() } ?: "9") }
    var dueMinute by remember(original.id) { mutableStateOf(original.dueAt?.let { (((it - dayKey(it) * DAY_MILLIS) / 60_000L) % 60).toString().padStart(2, '0') } ?: "00") }
    var showDuePicker by remember(original.id) { mutableStateOf(false) }
    var dependencySearch by remember(original.id) { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (original.id == 0L) "New task" else "Edit task") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { OutlinedTextField(title, { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(description, { description = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth(), minLines = 2) }
                item { Row(verticalAlignment = Alignment.CenterVertically) { Switch(hasProject, { checked -> hasProject = checked; if (!checked) projectId = null }); Text("Part of an existing project?", modifier = Modifier.padding(start = 8.dp)) } }
                if (hasProject) item { ChoiceRow("Project", projects.map { it.id }, projectId, { id -> projects.firstOrNull { it.id == id }?.name ?: "Choose project" }, { projectId = it }) }
                item { Row(verticalAlignment = Alignment.CenterVertically) { Switch(hasTrip, { checked -> hasTrip = checked; if (!checked) taskTripId = null }); Text("Part of an existing trip?", modifier = Modifier.padding(start = 8.dp)) } }
                if (hasTrip) item { ChoiceRow("Trip", trips.map { it.id }, taskTripId, { id -> trips.firstOrNull { it.id == id }?.name ?: "Choose trip" }, { taskTripId = it }) }
                item { PrioritySlider(priority, { priority = it }) }
                if (!hasTrip) item { SelectorField("Status", TaskStatus.entries.filterNot { it == TaskStatus.ARCHIVED }, status, { it.displayName() }, { status = it }, Modifier.fillMaxWidth()) }
                item { EffortSlider(energy, { energy = it }) }
                item { OutlinedTextField(minutes, { minutes = it.filter(Char::isDigit) }, label = { Text("Estimated minutes") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)) }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(onClick = { showDuePicker = true }, modifier = Modifier.weight(1f)) { Text(dueDate?.let(::formatDate) ?: "Set Date") }
                        OutlinedTextField(dueHour, { dueHour = it.filter(Char::isDigit).take(2) }, label = { Text("Hour") }, modifier = Modifier.weight(.55f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        OutlinedTextField(dueMinute, { dueMinute = it.filter(Char::isDigit).take(2) }, label = { Text("Min") }, modifier = Modifier.weight(.55f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    }
                }
                item { Row(verticalAlignment = Alignment.CenterVertically) { Switch(repeatEnabled, { repeatEnabled = it }); Text("Repeat this task", modifier = Modifier.padding(start = 8.dp)) } }
                if (repeatEnabled) {
                    item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) { OutlinedTextField(repeatInterval, { repeatInterval = it.filter(Char::isDigit) }, label = { Text("Every") }, modifier = Modifier.weight(.5f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)); SelectorField("Interval", RecurrenceUnit.entries, repeatUnit, { it.name.lowercase() + if ((repeatInterval.toIntOrNull() ?: 1) == 1) "" else "s" }, { repeatUnit = it }, Modifier.weight(1f)) } }
                    item { ChoiceRow("Repeats from", RecurrenceMode.entries, repeatMode, { if (it == RecurrenceMode.FIXED) "Scheduled date" else "Completion date" }, { repeatMode = it }) }
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Reminder:", style = MaterialTheme.typography.labelMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(reminderAmount, { reminderAmount = it.filter(Char::isDigit) }, label = { Text("#") }, modifier = Modifier.weight(.55f), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                            SelectorField("", listOf("minutes", "hours", "days"), reminderUnit, { it }, { reminderUnit = it }, Modifier.weight(1f))
                            Text("Before")
                        }
                    }
                }
                if (tags.isNotEmpty()) {
                    item { ToggleChoices("Tags", tags, tagIds, { it.id }, { it.name }, { tagIds = it }) }
                }
                if (availableDependencies.isNotEmpty()) {
                    item {
                        OutlinedTextField(
                            dependencySearch,
                            { dependencySearch = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Search blocking tasks") },
                            singleLine = true,
                        )
                    }
                    item {
                        ToggleChoices(
                            "Blocked until these tasks are completed",
                            availableDependencies.filter { task -> dependencySearch.isBlank() || task.title.contains(dependencySearch, ignoreCase = true) },
                            dependencyIds,
                            { it.id },
                            { it.title },
                            { dependencyIds = it },
                        )
                    }
                }
                onDelete?.let { action -> item { TextButton(onClick = action) { Text("Delete task") } } }
            }
        },
        confirmButton = {
            Button(
                enabled = title.isNotBlank(),
                onClick = {
                    val hour = dueHour.toIntOrNull()?.coerceIn(0, 23) ?: 9
                    val minute = dueMinute.toIntOrNull()?.coerceIn(0, 59) ?: 0
                    val due = dueDate?.plus(hour * 3_600_000L + minute * 60_000L)
                    val reminderMultiplier = when (reminderUnit) { "days" -> DAY_MILLIS; "hours" -> 3_600_000L; else -> 60_000L }
                    val rule = if (repeatEnabled) RecurrenceRule((repeatInterval.toIntOrNull() ?: 1).coerceAtLeast(1), repeatUnit, repeatMode) else null
                    onSave(
                        original.copy(
                            title = title.trim(),
                            description = description.trim(),
                            projectId = if (hasProject) projectId else null,
                            status = if (!hasProject && status == TaskStatus.READY) TaskStatus.INBOX else status,
                            priority = priority,
                            energyRequirement = energy,
                            estimatedMinutes = minutes.toIntOrNull(),
                            dueAt = due,
                            reminderAt = reminderAmount.toLongOrNull()?.let { amount -> due?.minus(amount * reminderMultiplier) },
                            recurrenceRule = rule?.encode(),
                            contextIds = emptySet(),
                            tagIds = tagIds,
                            dependencyIds = dependencyIds,
                        ),
                        taskTripId.takeIf { hasTrip },
                    )
                },
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
    if (showDuePicker) {
        val picker = rememberDatePickerState(initialSelectedDateMillis = dueDate)
        DatePickerDialog(onDismissRequest = { showDuePicker = false }, confirmButton = { TextButton(onClick = { dueDate = picker.selectedDateMillis; showDuePicker = false }) { Text("Use date") } }, dismissButton = { TextButton(onClick = { dueDate = null; showDuePicker = false }) { Text("Clear") } }) { DatePicker(state = picker) }
    }
}

@Composable
private fun ChecklistDialog(
    checklist: ChecklistEntity,
    items: List<ChecklistItemEntity>,
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit,
    onToggle: (ChecklistItemEntity) -> Unit,
    onDelete: () -> Unit,
) {
    var newItem by remember(checklist.id) { mutableStateOf("") }
    var confirmDelete by remember(checklist.id) { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(checklist.name) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(newItem, { newItem = it }, label = { Text("New item") }, singleLine = true, modifier = Modifier.weight(1f))
                    Button(enabled = newItem.isNotBlank(), onClick = { onAdd(newItem.trim()); newItem = "" }) { Text("Add") }
                }
                if (items.isEmpty()) Text("This list is empty.")
                items.forEach { item ->
                    Row(Modifier.fillMaxWidth().clickable { onToggle(item) }, verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = item.checked, onCheckedChange = { onToggle(item) })
                        Text(item.text, textDecoration = if (item.checked) TextDecoration.LineThrough else null)
                    }
                }
                TextButton(onClick = { confirmDelete = true }) { Text("Delete list", color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete ${checklist.name}?") },
            text = { Text("The list and all of its items will be permanently deleted.") },
            confirmButton = { Button(onClick = onDelete, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun PrioritySlider(
    value: Priority,
    onChange: (Priority) -> Unit,
) {
    val levels = listOf(Priority.SOMEDAY, Priority.LOW, Priority.NORMAL, Priority.HIGH, Priority.CRITICAL)
    Column {
        Text("Priority: ${value.displayName()}")
        Slider(
            value = levels.indexOf(value).coerceAtLeast(0).toFloat(),
            onValueChange = { onChange(levels[it.toInt().coerceIn(0, 4)]) },
            valueRange = 0f..4f,
            steps = 3,
            colors = SliderDefaults.colors(thumbColor = priorityColor(value), activeTrackColor = priorityColor(value)),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            levels.forEach { Text(it.displayName(), color = priorityColor(it), style = MaterialTheme.typography.labelSmall) }
        }
    }
}

@Composable
private fun EffortSlider(
    value: EnergyRequirement,
    onChange: (EnergyRequirement) -> Unit,
) {
    val levels = listOf(EnergyRequirement.VERY_LOW, EnergyRequirement.LOW, EnergyRequirement.MEDIUM, EnergyRequirement.HIGH, EnergyRequirement.DEEP_FOCUS)
    Column {
        Text("Effort required: ${value.displayName()}")
        Slider(
            value = levels.indexOf(value).coerceAtLeast(0).toFloat(),
            onValueChange = { onChange(levels[it.toInt().coerceIn(0, 4)]) },
            valueRange = 0f..4f,
            steps = 3,
            colors = SliderDefaults.colors(thumbColor = effortColor(value), activeTrackColor = effortColor(value)),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { listOf("Minimal", "Light", "Medium", "High", "Deep").forEach { Text(it, style = MaterialTheme.typography.labelSmall) } }
    }
}

private fun priorityColor(priority: Priority): Color =
    when (priority) {
        Priority.SOMEDAY -> Color(0xff43a047)
        Priority.LOW -> Color(0xff1e88e5)
        Priority.NORMAL -> Color(0xfffbc02d)
        Priority.HIGH -> Color(0xfffb8c00)
        Priority.CRITICAL -> Color(0xffe53935)
    }

private fun effortColor(effort: EnergyRequirement): Color =
    when (effort) {
        EnergyRequirement.VERY_LOW -> Color(0xff43a047)
        EnergyRequirement.LOW -> Color(0xff1e88e5)
        EnergyRequirement.MEDIUM -> Color(0xfffbc02d)
        EnergyRequirement.HIGH -> Color(0xfffb8c00)
        EnergyRequirement.DEEP_FOCUS -> Color(0xffe53935)
    }

@Composable
private fun ProjectEditorDialog(
    original: Project,
    onDismiss: () -> Unit,
    onSave: (Project) -> Unit,
) {
    var name by remember(original.id) { mutableStateOf(original.name) }
    var outcome by remember(original.id) { mutableStateOf(original.desiredOutcome) }
    var description by remember(original.id) { mutableStateOf(original.description) }
    var priority by remember(original.id) { mutableStateOf(original.priority) }
    var colorArgb by remember(original.id) { mutableStateOf(original.colorArgb) }
    var status by remember(original.id) { mutableStateOf(original.status) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (original.id == 0L) "New project" else "Edit project") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              item { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(outcome, { outcome = it }, label = { Text("Desired outcome") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(description, { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
                PrioritySlider(priority, { priority = it })
                Text("Project color")
                listOf(0xffef9a9a, 0xffffcc80, 0xfffff59d, 0xffa5d6a7, 0xff90caf9, 0xffb39ddb, 0xff80cbc4).chunked(4).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { candidate ->
                            OutlinedButton(onClick = { colorArgb = candidate }, modifier = Modifier.weight(1f).height(64.dp), colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(candidate).copy(alpha = 0.45f))) {
                                Text("●", color = Color(candidate), style = MaterialTheme.typography.headlineMedium)
                                if (colorArgb == candidate) Text("✓")
                            }
                        }
                        repeat(4 - row.size) { Box(Modifier.weight(1f)) }
                    }
                }
                SelectorField("Status", ProjectStatus.entries, status, { it.displayName() }, { status = it }, Modifier.fillMaxWidth())
              } }
            }
        },
        confirmButton = { Button(enabled = name.isNotBlank(), onClick = { onSave(original.copy(name = name.trim(), desiredOutcome = outcome.trim(), description = description.trim(), priority = priority, colorArgb = colorArgb, status = status)) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun MetadataSettingsScreen(
    contexts: List<TaskContext>,
    tags: List<TaskTag>,
    onSaveContext: (TaskContext) -> Unit,
    onDeleteContext: (TaskContext) -> Unit,
    onSaveTag: (TaskTag) -> Unit,
    onDeleteTag: (TaskTag) -> Unit,
    appSettings: AppSettings,
    darkMode: Boolean,
    onDarkMode: (Boolean) -> Unit,
    dataPortability: DataPortabilityService,
    dataFiles: DataFileGateway,
    deviceCalendars: List<DeviceCalendar>,
    selectedCalendarIds: Set<Long>,
    onSelectedCalendarIds: (Set<Long>) -> Unit,
    calendarPermissionGranted: Boolean,
    onRequestCalendarPermission: () -> Unit,
    onOpenCalendarPermissionSettings: () -> Unit,
    onRefreshDeviceCalendars: () -> Unit,
    platformActions: PlatformActions,
    padding: PaddingValues,
) {
    var contextName by remember { mutableStateOf("") }
    var contextBeingEdited by remember { mutableStateOf<TaskContext?>(null) }
    var tagName by remember { mutableStateOf("") }
    var tagBeingEdited by remember { mutableStateOf<TaskTag?>(null) }
    var taskReminders by remember { mutableStateOf(appSettings.taskRemindersEnabled) }
    var dueNotifications by remember { mutableStateOf(appSettings.dueNotificationsEnabled) }
    var planningNotifications by remember { mutableStateOf(appSettings.planningNotificationsEnabled) }
    var reviewNotifications by remember { mutableStateOf(appSettings.reviewNotificationsEnabled) }
    var quietHours by remember { mutableStateOf(appSettings.quietHoursEnabled) }
    var dataStatus by remember { mutableStateOf("") }
    var pendingDanger by remember { mutableStateOf<String?>(null) }
    var testAlarmMethod by remember { mutableStateOf("STANDARD") }
    var testAlarmDifficulty by remember { mutableStateOf("NORMAL") }
    var testAlarmStatus by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { SettingSwitch("Dark mode", darkMode, onDarkMode) }
        item { Text("Notifications", style = MaterialTheme.typography.headlineSmall) }
        item {
            SettingSwitch("Task reminders", taskReminders) {
                taskReminders = it
                appSettings.taskRemindersEnabled = it
            }
        }
        item {
            SettingSwitch("Due soon and overdue", dueNotifications) {
                dueNotifications = it
                appSettings.dueNotificationsEnabled = it
            }
        }
        item {
            SettingSwitch("Daily planning", planningNotifications) {
                planningNotifications = it
                appSettings.planningNotificationsEnabled = it
            }
        }
        item {
            SettingSwitch("Evening and weekly reviews", reviewNotifications) {
                reviewNotifications = it
                appSettings.reviewNotificationsEnabled = it
            }
        }
        item {
            SettingSwitch("Quiet hours (10 PM–7 AM)", quietHours) {
                quietHours = it
                appSettings.quietHoursEnabled = it
            }
        }
        item { Text("Alarms and timers", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 12.dp)) }
        item { Text("Alarm profiles, bundled-sound previews, snooze limits, volume ramps, vibration modes, and dismissal methods are managed under Timers → Alarms. Favorite presets and recent timers are under Timers → Timers.") }
        if (!platformActions.canScheduleExactAlarms()) item { Text("Android 12+ requires Alarms & reminders access for dependable exact timers. Life Minder requests this system permission when unavailable.", style = MaterialTheme.typography.bodySmall) }
        item { Text("Test alarm screen", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp)) }
        item { Text("Open the real alarm screen immediately without saving an alarm.", style = MaterialTheme.typography.bodySmall) }
        item { SelectorField("Challenge", listOf("STANDARD", "HOLD", "NUMBER", "MATH", "MEMORY", "TRACE", "SORT", "SCAN", "NFC"), testAlarmMethod, { it.lowercase().replaceFirstChar(Char::uppercase) }, { testAlarmMethod = it }, Modifier.fillMaxWidth()) }
        if (testAlarmMethod != "STANDARD") item { SelectorField("Difficulty", listOf("EASY", "NORMAL", "HARD"), testAlarmDifficulty, { it.lowercase().replaceFirstChar(Char::uppercase) }, { testAlarmDifficulty = it }, Modifier.fillMaxWidth()) }
        item {
            Button(onClick = { testAlarmStatus = if (platformActions.launchTestAlarm(testAlarmMethod, testAlarmDifficulty)) "Test alarm opened." else "Could not open the test alarm." }, modifier = Modifier.fillMaxWidth()) { Text("Open test alarm now") }
        }
        if (testAlarmStatus.isNotBlank()) item { Text(testAlarmStatus, style = MaterialTheme.typography.bodySmall) }
        item { Text("Device calendars", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 12.dp)) }
        if (deviceCalendars.isEmpty()) {
            item {
                Text(
                    if (calendarPermissionGranted) "Calendar access is allowed, but no visible device calendars were found. Refresh after adding or syncing a calendar on this phone."
                    else "Allow Calendar access to choose which calendars appear in Today and Upcoming.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            item {
                Button(
                    onClick = if (calendarPermissionGranted) onRefreshDeviceCalendars else onRequestCalendarPermission,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(if (calendarPermissionGranted) "Refresh device calendars" else "Allow calendar access") }
            }
            if (!calendarPermissionGranted) {
                item {
                    OutlinedButton(onClick = onOpenCalendarPermissionSettings, modifier = Modifier.fillMaxWidth()) {
                        Text("Open Android permission settings")
                    }
                }
            }
        }
        items(deviceCalendars, key = { "calendar-${it.id}" }) { calendar ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(calendar.id in selectedCalendarIds, { checked -> onSelectedCalendarIds(if (checked) selectedCalendarIds + calendar.id else selectedCalendarIds - calendar.id) })
                Column { Text(calendar.name); Text(CALENDAR_ACCOUNT_RECORDING_PLACEHOLDER, style = MaterialTheme.typography.bodySmall) }
            }
        }
        item { Text("Data safety", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 12.dp)) }
        item { Button(onClick = { scope.launch { runCatching { dataFiles.writeExport("life-minder-export.json", "application/json", dataPortability.exportJson(currentTimeMillis())) }.onSuccess { dataStatus = "JSON exported to $it" }.onFailure { dataStatus = "Export failed: ${it.message}" } } }, modifier = Modifier.fillMaxWidth()) { Text("Export JSON") } }
        item { OutlinedButton(onClick = { scope.launch { runCatching { dataFiles.writeExport("life-minder-tasks.csv", "text/csv", dataPortability.exportCsv()) }.onSuccess { dataStatus = "CSV exported to $it" }.onFailure { dataStatus = "CSV export failed: ${it.message}" } } }, modifier = Modifier.fillMaxWidth()) { Text("Export tasks as CSV") } }
        item { OutlinedButton(onClick = { scope.launch { runCatching { dataFiles.backupDatabase() }.onSuccess { dataStatus = "Database backup created at $it" }.onFailure { dataStatus = "Backup failed: ${it.message}" } } }, modifier = Modifier.fillMaxWidth()) { Text("Back up database") } }
        item { OutlinedButton(onClick = { scope.launch { runCatching { dataFiles.readImport("life-minder-export.json") ?: error("Place life-minder-export.json in the LifeMinder documents folder") }.mapCatching { dataPortability.importValidated(it) }.onSuccess { dataStatus = "Imported ${it.projects} projects, ${it.tasks} tasks, and ${it.sessions} sessions" }.onFailure { dataStatus = "Import rejected before changes: ${it.message}" } } }, modifier = Modifier.fillMaxWidth()) { Text("Import validated JSON") } }
        item { OutlinedButton(onClick = { pendingDanger = "restore" }, modifier = Modifier.fillMaxWidth()) { Text("Restore latest database backup") } }
        item { TextButton(onClick = { pendingDanger = "delete" }, modifier = Modifier.fillMaxWidth()) { Text("Delete all local data") } }
        if (dataStatus.isNotBlank()) item { Text(dataStatus, style = MaterialTheme.typography.bodySmall) }
        item { Text("Tags", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 12.dp)) }
        item {
            OutlinedTextField(
                tagName,
                { tagName = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(if (tagBeingEdited == null) "New tag" else "Edit tag") },
                trailingIcon = {
                    TextButton(
                        enabled = tagName.isNotBlank(),
                        onClick = {
                            onSaveTag((tagBeingEdited ?: TaskTag(name = "")).copy(name = tagName.trim()))
                            tagName = ""
                            tagBeingEdited = null
                        },
                    ) { Text("Save") }
                },
            )
        }
        items(tags, key = { "tag-${it.id}" }) { tag ->
            MetadataRow(
                name = tag.name,
                canDelete = true,
                onEdit = {
                    tagBeingEdited = tag
                    tagName = tag.name
                },
                onDelete = { onDeleteTag(tag) },
            )
        }
    }
    pendingDanger?.let { action ->
        AlertDialog(
            onDismissRequest = { pendingDanger = null },
            title = { Text(if (action == "delete") "Delete all local data?" else "Restore database backup?") },
            text = { Text(if (action == "delete") "This permanently removes projects, tasks, sessions, alarms, and reviews. Export or back up first." else "The current database will be copied to a rollback folder, then replaced. Life Minder must be restarted afterward.") },
            confirmButton = {
                Button(onClick = {
                    pendingDanger = null
                    scope.launch {
                        runCatching { if (action == "delete") dataFiles.deleteAllLocalData() else dataFiles.restoreDatabase() }
                            .onSuccess { dataStatus = it }
                            .onFailure { dataStatus = "Operation failed safely: ${it.message}" }
                    }
                }) { Text(if (action == "delete") "Delete everything" else "Restore") }
            },
            dismissButton = { TextButton(onClick = { pendingDanger = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun SettingSwitch(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun MetadataRow(
    name: String,
    canDelete: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(name)
            Row {
                TextButton(onClick = onEdit) { Text("Edit") }
                if (canDelete) TextButton(onClick = onDelete) { Text("Delete") }
            }
        }
    }
}

@Composable
private fun <T> ToggleChoices(
    label: String,
    choices: List<T>,
    selectedIds: Set<Long>,
    id: (T) -> Long,
    text: (T) -> String,
    onChanged: (Set<Long>) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        choices.forEach { choice ->
            val choiceId = id(choice)
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onChanged(if (choiceId in selectedIds) selectedIds - choiceId else selectedIds + choiceId) },
            ) {
                Text(if (choiceId in selectedIds) "✓ ${text(choice)}" else text(choice))
            }
        }
    }
}

@Composable
private fun <T> ChoiceRow(
    label: String,
    choices: List<T>,
    selected: T,
    text: (T) -> String,
    onSelected: (T) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            choices.forEach { choice ->
                OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = { onSelected(choice) }) {
                    Text(if (choice == selected) "✓ ${text(choice)}" else text(choice))
                }
            }
        }
    }
}

@Composable
private fun <T> ChoiceGrid(
    label: String,
    choices: List<T>,
    selected: T,
    text: (T) -> String,
    onSelected: (T) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        choices.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { choice ->
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = { onSelected(choice) },
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = if (choice == selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent),
                    ) {
                        Text(text(choice), maxLines = 1)
                    }
                }
                if (row.size == 1) Box(Modifier.weight(1f))
            }
        }
    }
}

@Composable
internal fun <T> SelectorField(
    label: String,
    choices: List<T>,
    selected: T,
    text: (T) -> String,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(if (label.isBlank()) text(selected) else "$label: ${text(selected)}", maxLines = 1)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            choices.forEach { choice ->
                DropdownMenuItem(
                    text = { Text(text(choice)) },
                    onClick = {
                        onSelected(choice)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun EmptyMessage(message: String) {
    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { Text(message) }
}

@Composable
private fun ReviewScreen(
    tasks: List<Task>,
    projects: List<Project>,
    sessions: List<WorkSession>,
    reviews: List<ReviewRecord>,
    plannedTaskIds: Set<Long>,
    now: Long,
    weeklyDeferrals: Int,
    onSave: (ReviewRecord) -> Unit,
    onProcessInbox: () -> Unit,
    padding: PaddingValues,
) {
    var type by remember { mutableStateOf(ReviewType.DAILY) }
    var wentWell by remember { mutableStateOf("") }
    var friction by remember { mutableStateOf("") }
    var nextPriority by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    val dayStart = dayKey(now) * DAY_MILLIS
    val from = if (type == ReviewType.DAILY) dayStart else dayStart - 6 * DAY_MILLIS
    val snapshot = AnalyticsService.snapshot(from, dayStart + DAY_MILLIS, now, tasks, projects, sessions, plannedTaskIds, if (type == ReviewType.WEEKLY) weeklyDeferrals else 0)
    LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(vertical = 16.dp)) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { ReviewType.entries.forEach { choice -> OutlinedButton(onClick = { type = choice }) { Text(if (type == choice) "${choice.displayName()} ✓" else choice.displayName()) } } }
        }
        item { Text("${type.displayName()} review", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("${snapshot.summary.completedTasks} tasks · ${snapshot.summary.completedSessions} sessions · ${formatDuration(snapshot.summary.focusedMillis)} focused")
                Text("Planned ${formatDuration(snapshot.summary.plannedMillis)} · ${snapshot.summary.unfinishedPlannedTasks} planned tasks unfinished · ${snapshot.summary.deferredTasks} deferrals")
                snapshot.estimateAccuracyRatio?.let { Text("Actual time was ${(it * 100).toInt()}% of estimates") }
            }
        }
        if (snapshot.summary.focusByProject.isNotEmpty()) {
            item { Text("Focus time by project", style = MaterialTheme.typography.titleMedium) }
            val max =
                snapshot.summary.focusByProject.values
                    .maxOrNull()
                    ?.coerceAtLeast(1) ?: 1
            items(
                snapshot.summary.focusByProject.entries
                    .toList(),
                key = { "chart-${it.key}" },
            ) { entry ->
                Column {
                    Text(projects.firstOrNull { it.id == entry.key }?.name ?: "General")
                    Box(Modifier.fillMaxWidth((entry.value.toFloat() / max).coerceIn(0.04f, 1f)).height(12.dp).background(MaterialTheme.colorScheme.primary))
                    Text(formatDuration(entry.value), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        if (type == ReviewType.WEEKLY && snapshot.summary.focusByDay.isNotEmpty()) {
            item { Text("Focus trend by day", style = MaterialTheme.typography.titleMedium) }
            val maxDay =
                snapshot.summary.focusByDay.values
                    .maxOrNull()
                    ?.coerceAtLeast(1) ?: 1
            items(
                snapshot.summary.focusByDay.entries
                    .sortedBy { it.key },
                key = { "day-${it.key}" },
            ) { entry ->
                Column {
                    Text("Day ${entry.key}")
                    Box(Modifier.fillMaxWidth((entry.value.toFloat() / maxDay).coerceIn(0.04f, 1f)).height(10.dp).background(MaterialTheme.colorScheme.tertiary))
                    Text(formatDuration(entry.value), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        if (snapshot.summary.completedByProject.isNotEmpty()) {
            item { Text("Completed tasks by project", style = MaterialTheme.typography.titleMedium) }
            val maxCompleted =
                snapshot.summary.completedByProject.values
                    .maxOrNull()
                    ?.coerceAtLeast(1) ?: 1
            items(
                snapshot.summary.completedByProject.entries
                    .toList(),
                key = { "completed-${it.key}" },
            ) { entry ->
                Column {
                    Text("${projects.firstOrNull { it.id == entry.key }?.name ?: "Unassigned tasks"}: ${entry.value}")
                    Box(Modifier.fillMaxWidth((entry.value.toFloat() / maxCompleted).coerceIn(0.04f, 1f)).height(10.dp).background(MaterialTheme.colorScheme.secondary))
                }
            }
        }
        if (type == ReviewType.WEEKLY) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Weekly checklist", style = MaterialTheme.typography.titleMedium)
                    Text("1. Process unassigned tasks  2. Review active plans  3. Check waiting tasks  4. Review deadlines  5. Resolve stalled plans  6. Select priorities  7. Park inactive plans")
                    Button(onClick = onProcessInbox) { Text("Process unassigned tasks") }
                }
            }
        }
        if (snapshot.insights.isNotEmpty()) {
            item { Text("Insights", style = MaterialTheme.typography.titleMedium) }
            items(snapshot.insights, key = { it.title }) { insight ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(10.dp)) {
                        Text(insight.title, fontWeight = FontWeight.SemiBold)
                        Text(insight.explanation)
                    }
                }
            }
        }
        item { OutlinedTextField(wentWell, { wentWell = it }, label = { Text("What went well?") }, modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(friction, { friction = it }, label = { Text("What caused friction?") }, modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(nextPriority, { nextPriority = it }, label = { Text(if (type == ReviewType.DAILY) "Tomorrow's most important task" else "Next week's priority") }, modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(notes, { notes = it }, label = { Text("Review notes") }, modifier = Modifier.fillMaxWidth(), minLines = 2) }
        item {
            Button(onClick = {
                onSave(ReviewRecord(type = type, periodStart = from, periodEnd = dayStart + DAY_MILLIS, wentWell = wentWell, friction = friction, nextPriority = nextPriority, notes = notes, completedAt = currentTimeMillis()))
                wentWell = ""
                friction = ""
                nextPriority = ""
                notes = ""
            }, modifier = Modifier.fillMaxWidth()) { Text("Complete review") }
        }
        if (reviews.isNotEmpty()) {
            item { Text("Review history", style = MaterialTheme.typography.titleMedium) }
            items(reviews.take(10), key = { it.id }) { review ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(10.dp)) {
                        Text(review.type.displayName(), fontWeight = FontWeight.SemiBold)
                        Text(review.wentWell.ifBlank { review.notes.ifBlank { "Completed review" } })
                        if (review.nextPriority.isNotBlank()) Text("Next: ${review.nextPriority}")
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun TimerScreen(
    active: ActiveTimer?,
    sessions: List<WorkSession>,
    alarms: List<AlarmDefinition>,
    standaloneAlarms: List<StandaloneAlarmEntity>,
    standaloneTimers: List<StandaloneTimerEntity>,
    presets: List<com.geepscontrol.core.database.TimerPresetEntity>,
    history: List<com.geepscontrol.core.database.TimerHistoryEntity>,
    profiles: List<AlarmProfileEntity>,
    platformActions: PlatformActions,
    tasks: List<Task>,
    timerCreationRequest: Int,
    alarmCreationRequest: Int,
    stopwatchLaps: List<Long>,
    onLap: (Long) -> Unit,
    onSaveTimer: (ActiveTimer) -> Unit,
    onStop: (ActiveTimer, Long, String) -> Unit,
    onAddAlarm: (AlarmDefinition) -> Unit,
    onDismissAlarm: (AlarmDefinition) -> Unit,
    onSaveStandaloneAlarm: (StandaloneAlarmEntity) -> Unit,
    onStartStandaloneTimer: (String, Long) -> Unit,
    onSavePreset: (com.geepscontrol.core.database.TimerPresetEntity) -> Unit,
    onDeletePreset: (com.geepscontrol.core.database.TimerPresetEntity) -> Unit,
    onSaveProfile: (AlarmProfileEntity) -> Unit,
    onSetDefaultProfile: (AlarmProfileEntity) -> Unit,
    onDeleteProfile: (AlarmProfileEntity) -> Unit,
    onToggleStandaloneTimer: (StandaloneTimerEntity) -> Unit,
    onToggleStandaloneAlarm: (StandaloneAlarmEntity) -> Unit,
    onDeleteStandaloneAlarm: (StandaloneAlarmEntity) -> Unit,
    onDeleteStandaloneTimer: (StandaloneTimerEntity) -> Unit,
    padding: PaddingValues,
) {
    var tick by remember { mutableStateOf(currentTimeMillis()) }
    var timerTab by remember { mutableStateOf("Timers") }
    var showProfileManager by remember { mutableStateOf(false) }
    var showProfilePicker by remember { mutableStateOf(false) }
    var profileBeingEdited by remember { mutableStateOf<AlarmProfileEntity?>(null) }
    var selectedTaskId by remember { mutableStateOf<Long?>(null) }
    var showCustomTimer by remember { mutableStateOf(false) }
    var showTimerName by remember { mutableStateOf(false) }
    var timerName by remember { mutableStateOf("") }
    var pendingTimerDuration by remember { mutableStateOf(5 * 60_000L) }
    var showAlarmEditor by remember { mutableStateOf(false) }
    var editingAlarm by remember { mutableStateOf<StandaloneAlarmEntity?>(null) }
    var alarmDate by remember { mutableStateOf(dayKey(currentTimeMillis()) * DAY_MILLIS) }
    var showAlarmDatePicker by remember { mutableStateOf(false) }
    var customMinutes by remember { mutableStateOf("10") }
    var customHours by remember { mutableStateOf("0") }
    var customSeconds by remember { mutableStateOf("0") }
    var saveTimerPreset by remember { mutableStateOf(false) }
    var editingPreset by remember { mutableStateOf<com.geepscontrol.core.database.TimerPresetEntity?>(null) }
    var presetMenuId by remember { mutableStateOf<Long?>(null) }
    var alarmLabel by remember { mutableStateOf("Wake up") }
    var alarmHour by remember { mutableStateOf("7") }
    var alarmMinute by remember { mutableStateOf("00") }
    var alarmRepeat by remember { mutableStateOf("Once") }
    var alarmInterval by remember { mutableStateOf("1") }
    var alarmUnit by remember { mutableStateOf(AlarmRepeatUnit.DAY) }
    var alarmWeekdays by remember { mutableStateOf(setOf<Int>()) }
    var alarmProfileId by remember { mutableStateOf<Long?>(null) }
    var alarmSound by remember { mutableStateOf(true) }
    var alarmSoundId by remember { mutableStateOf("standard_steel") }
    var alarmLoopSound by remember { mutableStateOf(true) }
    var alarmVibrationMode by remember { mutableStateOf("STANDARD") }
    var alarmVibrationDelay by remember { mutableStateOf(0) }
    var alarmStartVolume by remember { mutableStateOf(30) }
    var alarmMaxVolume by remember { mutableStateOf(100) }
    var alarmVolumeRamp by remember { mutableStateOf(45) }
    var alarmSnoozeMinutes by remember { mutableStateOf("8") }
    var alarmMaximumSnoozes by remember { mutableStateOf("0") }
    var alarmDismissalMethod by remember { mutableStateOf("STANDARD") }
    var alarmDismissalDifficulty by remember { mutableStateOf("NORMAL") }
    fun applyProfile(profile: AlarmProfileEntity) {
        alarmProfileId = profile.id
        alarmSound = profile.soundEnabled
        alarmSoundId = profile.soundId ?: "standard_steel"
        alarmLoopSound = profile.loopSound
        alarmVibrationMode = if (profile.vibrationEnabled) profile.vibrationMode else "OFF"
        alarmVibrationDelay = profile.vibrationStartDelaySeconds
        alarmStartVolume = profile.startingVolumePercent
        alarmMaxVolume = profile.maximumVolumePercent
        alarmVolumeRamp = profile.volumeRampSeconds
        alarmSnoozeMinutes = profile.snoozeDurationMinutes.toString()
        alarmMaximumSnoozes = profile.maximumSnoozes.toString()
        alarmDismissalMethod = profile.dismissalMethod
        alarmDismissalDifficulty = profile.dismissalDifficulty
    }
    fun openAlarmEditor(alarm: StandaloneAlarmEntity?) {
        editingAlarm = alarm
        val rule = AlarmRepeatRule.decode(alarm?.recurrenceRule)
        alarmLabel = alarm?.label ?: "Wake up"
        alarmDate = alarm?.scheduledAt?.let { dayKey(it) * DAY_MILLIS } ?: dayKey(currentTimeMillis()) * DAY_MILLIS
        alarmHour = alarm?.scheduledAt?.let { (((it % DAY_MILLIS) + DAY_MILLIS) % DAY_MILLIS / 3_600_000L).toString() } ?: "7"
        alarmMinute = alarm?.scheduledAt?.let { ((((it % DAY_MILLIS) + DAY_MILLIS) % DAY_MILLIS / 60_000L) % 60).toString().padStart(2, '0') } ?: "00"
        alarmInterval = rule?.interval?.toString() ?: "1"
        alarmUnit = rule?.unit ?: AlarmRepeatUnit.DAY
        alarmWeekdays = rule?.weekdays.orEmpty()
        val profile = alarm?.profileId?.let { id -> profiles.firstOrNull { it.id == id } } ?: profiles.firstOrNull { it.isDefault } ?: profiles.firstOrNull { it.builtInKey == "standard" }
        if (profile != null) applyProfile(profile)
        alarmProfileId = alarm?.profileId ?: profile?.id
        alarmSound = alarm?.sound ?: alarmSound
        alarmSoundId = alarm?.soundId ?: alarmSoundId
        alarmLoopSound = alarm?.loopSound ?: alarmLoopSound
        alarmVibrationMode = alarm?.vibrationMode ?: alarmVibrationMode
        alarmVibrationDelay = alarm?.vibrationStartDelaySeconds ?: alarmVibrationDelay
        alarmStartVolume = alarm?.startingVolumePercent ?: alarmStartVolume
        alarmMaxVolume = alarm?.maximumVolumePercent ?: alarmMaxVolume
        alarmVolumeRamp = alarm?.volumeRampSeconds ?: alarmVolumeRamp
        alarmSnoozeMinutes = (alarm?.snoozeMinutes ?: alarmSnoozeMinutes.toIntOrNull() ?: 8).toString()
        alarmMaximumSnoozes = (alarm?.maximumSnoozes ?: alarmMaximumSnoozes.toIntOrNull() ?: 0).toString()
        alarmDismissalMethod = alarm?.dismissalMethod ?: alarmDismissalMethod
        alarmDismissalDifficulty = alarm?.dismissalDifficulty ?: alarmDismissalDifficulty
        alarmRepeat = when { rule == null -> "Once"; rule.unit == AlarmRepeatUnit.MONTH && rule.interval == 1 -> "Monthly"; rule.unit == AlarmRepeatUnit.WEEK && rule.interval == 1 -> "Weekly"; rule.unit == AlarmRepeatUnit.WEEK && rule.interval == 2 -> "Bi-weekly"; else -> "Custom" }
        showAlarmEditor = true
    }
    LaunchedEffect(timerCreationRequest) { if (timerCreationRequest > 0) showCustomTimer = true }
    LaunchedEffect(alarmCreationRequest) { if (alarmCreationRequest > 0) openAlarmEditor(null) }
    LaunchedEffect(active?.state) {
        while (active?.state == TimerState.RUNNING) {
            tick = currentTimeMillis()
            delay(10)
        }
    }
    LaunchedEffect(standaloneTimers.any { it.state == "RUNNING" }) {
        while (standaloneTimers.any { it.state == "RUNNING" }) {
            tick = currentTimeMillis()
            delay(250)
        }
    }
    val runningStandalone = standaloneTimers.filter { it.state == "RUNNING" }
    val inactiveStandalone = standaloneTimers.filter { it.state != "RUNNING" }
    val enabledStandaloneAlarms = standaloneAlarms.filter { it.enabled }
    val disabledStandaloneAlarms = standaloneAlarms.filterNot { it.enabled }
    val recentHistory = history.filter { it.endedAt >= dayKey(currentTimeMillis()) * DAY_MILLIS }
    val quickTimerMinutes = setOf(5L, 10L, 15L, 30L, 45L, 60L)
    Box(modifier = Modifier.fillMaxSize().padding(padding)) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(top = 16.dp, bottom = 88.dp)) {
        item { Text("Timers & Alarms", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Timers", "Alarms").forEach { value ->
                    OutlinedButton(onClick = { timerTab = value }, modifier = Modifier.weight(1f), colors = ButtonDefaults.outlinedButtonColors(containerColor = if (timerTab == value) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)) { Text(value) }
                }
            }
        }
        if (timerTab == "Timers") item {
            Text("Quick timers", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            listOf(5, 10, 15, 30, 45, 60).chunked(3).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    row.forEach { minutes -> Button(onClick = { onStartStandaloneTimer("$minutes-minute timer", minutes * 60_000L) }, modifier = Modifier.weight(1f)) { Text("$minutes min") } }
                }
            }
            OutlinedButton(onClick = { editingPreset = null; timerName = ""; showCustomTimer = true }, modifier = Modifier.fillMaxWidth()) { Text("Custom") }
        }
        if (timerTab == "Timers" && presets.any { it.isFavorite }) item {
            Text("Favorite presets", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            presets.filter { it.isFavorite }.sortedBy { it.manualOrder }.take(8).forEach { preset ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = { onStartStandaloneTimer(preset.name, preset.durationSeconds * 1000L) }, modifier = Modifier.weight(1f)) { Text("${preset.name} · ${formatDuration(preset.durationSeconds * 1000L)}") }
                    Box { TextButton(onClick = { presetMenuId = preset.id }) { Text("⋮") }; DropdownMenu(expanded = presetMenuId == preset.id, onDismissRequest = { presetMenuId = null }) {
                        DropdownMenuItem(text = { Text("Edit") }, onClick = { editingPreset = preset; timerName = preset.name; customHours = (preset.durationSeconds / 3600).toString(); customMinutes = ((preset.durationSeconds / 60) % 60).toString(); customSeconds = (preset.durationSeconds % 60).toString(); saveTimerPreset = preset.isFavorite; presetMenuId = null; showCustomTimer = true })
                        DropdownMenuItem(text = { Text("Unpin") }, onClick = { onSavePreset(preset.copy(isFavorite = false, updatedAt = currentTimeMillis())); presetMenuId = null })
                        DropdownMenuItem(text = { Text("Move up") }, onClick = { onSavePreset(preset.copy(manualOrder = (preset.manualOrder - 1).coerceAtLeast(0), updatedAt = currentTimeMillis())); presetMenuId = null })
                        DropdownMenuItem(text = { Text("Move down") }, onClick = { onSavePreset(preset.copy(manualOrder = preset.manualOrder + 1, updatedAt = currentTimeMillis())); presetMenuId = null })
                        DropdownMenuItem(text = { Text("Duplicate") }, onClick = { onSavePreset(preset.copy(id = 0, name = "${preset.name} copy", manualOrder = presets.size.toLong(), createdAt = currentTimeMillis(), updatedAt = currentTimeMillis())); presetMenuId = null })
                        DropdownMenuItem(text = { Text("Delete") }, onClick = { onDeletePreset(preset); presetMenuId = null })
                    } }
                }
            }
        }
        if (timerTab == "Timers" && recentHistory.isNotEmpty()) item {
            Text("Ran today", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            recentHistory.take(5).forEach { recent ->
                val durationMinutes = recent.originalDurationMillis / 60_000L
                val isGenericQuickTimer = durationMinutes in quickTimerMinutes && recent.name == "$durationMinutes-minute timer"
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${recent.name.ifBlank { formatDuration(recent.originalDurationMillis) }} · ${recent.result.lowercase()} · ${formatAlarmTime(recent.endedAt)}", modifier = Modifier.weight(1f))
                    if (!isGenericQuickTimer) TextButton(onClick = { onStartStandaloneTimer(recent.name, recent.originalDurationMillis) }) { Text("Start again") }
                    if (!isGenericQuickTimer && presets.none { it.durationSeconds * 1000L == recent.originalDurationMillis && it.name == recent.name }) TextButton(onClick = { onSavePreset(com.geepscontrol.core.database.TimerPresetEntity(name = recent.name.ifBlank { formatDuration(recent.originalDurationMillis) }, durationSeconds = recent.originalDurationMillis / 1000L, isFavorite = true, manualOrder = presets.size.toLong(), createdAt = currentTimeMillis(), updatedAt = currentTimeMillis())) }) { Text("Save") }
                }
            }
        }
        if (timerTab == "Alarms") item {
            Text("Alarm controls", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            OutlinedButton(onClick = { showProfileManager = true }, modifier = Modifier.fillMaxWidth()) { Text("Manage alarm profiles") }
        }
        if ((timerTab == "Timers" && runningStandalone.isNotEmpty()) || (timerTab == "Alarms" && enabledStandaloneAlarms.isNotEmpty())) item { Text("Active", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        if (timerTab == "Timers") items(runningStandalone, key = { "running-timer-${it.id}" }) { timer ->
            val elapsed = (tick - timer.startedAt).coerceAtLeast(0)
            val remaining = (timer.durationMillis - elapsed).coerceAtLeast(0)
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(timer.name.ifBlank { "Timer" }, fontWeight = FontWeight.SemiBold)
                        Text("${formatDuration(remaining)} remaining")
                    }
                    OutlinedButton(onClick = { onToggleStandaloneTimer(timer) }) { Text("Disable") }
                    TextButton(onClick = { onDeleteStandaloneTimer(timer) }) { Text("\uD83D\uDDD1") }
                }
            }
        }
        if (timerTab == "Alarms") items(enabledStandaloneAlarms, key = { "enabled-alarm-${it.id}" }) { alarm ->
            Card(Modifier.fillMaxWidth().clickable { openAlarmEditor(alarm) }, colors = CardDefaults.cardColors(containerColor = activeAlarmColor(alarm.scheduledAt, tick))) {
                Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(alarm.label, fontWeight = FontWeight.SemiBold)
                        Text(formatAlarmTime(alarm.scheduledAt) + alarm.recurrenceRule?.let { " · repeating" }.orEmpty())
                    }
                    OutlinedButton(onClick = { onToggleStandaloneAlarm(alarm) }) { Text("Disable") }
                    TextButton(onClick = { onDeleteStandaloneAlarm(alarm) }) { Text("\uD83D\uDDD1") }
                }
            }
        }
        if (timerTab == "Timers") item {
            if (active != null) {
                val elapsed = TimerEngine.elapsed(active, tick)
                val remaining = TimerEngine.remaining(active, tick)
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(active.mode.displayName(), fontWeight = FontWeight.Bold)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                if (remaining == null) formatDurationPrecise(elapsed) else if (remaining >= 0) formatDuration(remaining) + " remaining" else formatDuration(-remaining) + " overtime",
                                style = MaterialTheme.typography.headlineMedium,
                            )
                            Column(horizontalAlignment = Alignment.End) {
                                if (active.state == TimerState.RUNNING) Button(onClick = { onSaveTimer(TimerEngine.pause(active, currentTimeMillis())) }) { Text("Pause") }
                                else Button(onClick = { onSaveTimer(TimerEngine.resume(active, currentTimeMillis())) }) { Text("Resume") }
                                if (active.mode == TimerMode.STOPWATCH) {
                                    OutlinedButton(enabled = active.state == TimerState.RUNNING, onClick = { onLap(elapsed) }) { Text("Lap") }
                                } else {
                                    OutlinedButton(onClick = { onSaveTimer(TimerEngine.addTime(active, 5 * 60_000L)) }) { Text("+5 min") }
                                }
                                Button(onClick = { onStop(active, currentTimeMillis(), "") }) { Text("Finish") }
                            }
                        }
                        if (active.mode == TimerMode.STOPWATCH && stopwatchLaps.isNotEmpty()) {
                            Text("Laps", fontWeight = FontWeight.SemiBold)
                            stopwatchLaps.forEachIndexed { index, cumulative ->
                                val previous = stopwatchLaps.getOrNull(index - 1) ?: 0L
                                Text("Lap ${index + 1}: ${formatDurationPrecise(cumulative - previous)} · Total ${formatDurationPrecise(cumulative)}")
                            }
                        }
                    }
                }
            }
        }
        if ((timerTab == "Timers" && inactiveStandalone.isNotEmpty()) || (timerTab == "Alarms" && disabledStandaloneAlarms.isNotEmpty())) item { Text("Disabled", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        if (timerTab == "Timers") items(inactiveStandalone, key = { "inactive-timer-${it.id}" }) { timer ->
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(timer.name.ifBlank { "Timer" }, fontWeight = FontWeight.SemiBold)
                        Text("${formatDuration(timer.durationMillis)} timer · ${timer.state.lowercase()}")
                    }
                    OutlinedButton(onClick = { onToggleStandaloneTimer(timer) }) { Text("Enable") }
                    TextButton(onClick = { onDeleteStandaloneTimer(timer) }) { Text("\uD83D\uDDD1") }
                }
            }
        }
        if (timerTab == "Alarms") items(disabledStandaloneAlarms, key = { "disabled-alarm-${it.id}" }) { alarm ->
            Card(Modifier.fillMaxWidth().clickable { openAlarmEditor(alarm) }) {
                Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(alarm.label, fontWeight = FontWeight.SemiBold)
                        Text(formatAlarmTime(alarm.scheduledAt) + alarm.recurrenceRule?.let { " · repeating" }.orEmpty())
                    }
                    OutlinedButton(onClick = { onToggleStandaloneAlarm(alarm) }) { Text("Enable") }
                    TextButton(onClick = { onDeleteStandaloneAlarm(alarm) }) { Text("\uD83D\uDDD1") }
                }
            }
        }
    }
        Surface(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(), tonalElevation = 6.dp, shadowElevation = 8.dp) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(
                    enabled = active == null,
                    onClick = {
                        val started = currentTimeMillis()
                        onSaveTimer(ActiveTimer(taskId = selectedTaskId, mode = TimerMode.STOPWATCH, state = TimerState.RUNNING, startedAt = started))
                    },
                    modifier = Modifier.weight(1f),
                ) { Text("Stopwatch") }
                Button(onClick = { timerName = ""; showCustomTimer = true }, modifier = Modifier.weight(1f)) { Text("Add Timer") }
                Button(onClick = { openAlarmEditor(null) }, modifier = Modifier.weight(1f)) { Text("Add Alarm") }
            }
        }
    }
    if (showCustomTimer) {
        AlertDialog(
            onDismissRequest = { showCustomTimer = false },
            title = { Text("Add timer") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(timerName, { timerName = it }, label = { Text("Timer name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(customHours, { customHours = it.filter(Char::isDigit) }, label = { Text("Hours") }, modifier = Modifier.weight(1f), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        OutlinedTextField(customMinutes, { customMinutes = it.filter(Char::isDigit) }, label = { Text("Minutes") }, modifier = Modifier.weight(1f), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        OutlinedTextField(customSeconds, { customSeconds = it.filter(Char::isDigit) }, label = { Text("Seconds") }, modifier = Modifier.weight(1f), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(saveTimerPreset, { saveTimerPreset = it }); Text("Save as favorite preset") }
                }
            },
            confirmButton = {
                val customDuration = (customHours.toLongOrNull() ?: 0) * 3_600_000L + (customMinutes.toLongOrNull() ?: 0) * 60_000L + (customSeconds.toLongOrNull() ?: 0) * 1000L
                Button(enabled = customDuration > 0, onClick = {
                    val duration = customDuration.coerceAtLeast(1000L)
                    onStartStandaloneTimer(timerName.trim(), duration)
                    if (saveTimerPreset || editingPreset != null) {
                        val now = currentTimeMillis()
                        onSavePreset((editingPreset ?: com.geepscontrol.core.database.TimerPresetEntity(name = "", durationSeconds = duration / 1000L, createdAt = now, updatedAt = now)).copy(name = timerName.trim().ifBlank { formatDuration(duration) }, durationSeconds = duration / 1000L, isFavorite = saveTimerPreset, manualOrder = editingPreset?.manualOrder ?: presets.size.toLong(), updatedAt = now))
                    }
                    editingPreset = null
                    showCustomTimer = false
                }) { Text("Start") }
            },
            dismissButton = { TextButton(onClick = { showCustomTimer = false }) { Text("Cancel") } },
        )
    }
    if (showTimerName) {
        AlertDialog(
            onDismissRequest = { showTimerName = false },
            title = { Text("Name timer") },
            text = { OutlinedTextField(timerName, { timerName = it }, label = { Text("What is this timer for?") }, modifier = Modifier.fillMaxWidth(), singleLine = true) },
            confirmButton = {
                Button(onClick = {
                    onStartStandaloneTimer(timerName.trim(), pendingTimerDuration)
                    showTimerName = false
                }) { Text("Start") }
            },
            dismissButton = { TextButton(onClick = { showTimerName = false }) { Text("Cancel") } },
        )
    }
    if (showAlarmEditor) {
        AlertDialog(
            onDismissRequest = { showAlarmEditor = false },
            title = { Text(if (editingAlarm == null) "New alarm" else "Edit alarm") },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { OutlinedTextField(alarmLabel, { alarmLabel = it }, label = { Text("Alarm name") }, modifier = Modifier.fillMaxWidth()) }
                    if (profiles.isNotEmpty()) item {
                        OutlinedButton(onClick = { showProfilePicker = true }, modifier = Modifier.fillMaxWidth()) {
                            Text("Use existing profile${alarmProfileId?.let { id -> profiles.firstOrNull { it.id == id }?.name }?.let { ": $it" }.orEmpty()}")
                        }
                    }
                    item { OutlinedButton(onClick = { showAlarmDatePicker = true }, modifier = Modifier.fillMaxWidth()) { Text(formatDate(alarmDate)) } }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(alarmHour, { alarmHour = it.filter(Char::isDigit).take(2) }, label = { Text("Hour (0–23)") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                            OutlinedTextField(alarmMinute, { alarmMinute = it.filter(Char::isDigit).take(2) }, label = { Text("Minute") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        }
                    }
                    item { SelectorField("Repeat", listOf("Once", "Weekly", "Bi-weekly", "Monthly", "Custom"), alarmRepeat, { it }, { alarmRepeat = it }, Modifier.fillMaxWidth()) }
                    item { Text("Alarm behavior", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold) }
                    item { Row(verticalAlignment = Alignment.CenterVertically) { Switch(alarmSound, { alarmSound = it }); Text("Sound", modifier = Modifier.padding(start = 8.dp)) } }
                    if (alarmSound) {
                        item { SelectorField("Sound", bundledAlarmSounds, bundledAlarmSounds.firstOrNull { it.id == alarmSoundId } ?: bundledAlarmSounds.first(), { it.name }, { alarmSoundId = it.id }, Modifier.fillMaxWidth()) }
                        item { Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { OutlinedButton(onClick = { platformActions.previewAlarmSound(alarmSoundId) }, modifier = Modifier.weight(1f)) { Text("Preview") }; OutlinedButton(onClick = platformActions::stopAlarmSoundPreview, modifier = Modifier.weight(1f)) { Text("Stop") } } }
                        item { Row(verticalAlignment = Alignment.CenterVertically) { Switch(alarmLoopSound, { alarmLoopSound = it }); Text("Repeat sound until acknowledged", modifier = Modifier.padding(start = 8.dp)) } }
                        item { Text("Starting volume: $alarmStartVolume%"); Slider(alarmStartVolume.toFloat(), { alarmStartVolume = it.toInt().coerceAtMost(alarmMaxVolume) }, valueRange = 0f..100f, steps = 9) }
                        item { Text("Maximum volume: $alarmMaxVolume%"); Slider(alarmMaxVolume.toFloat(), { alarmMaxVolume = it.toInt().coerceAtLeast(alarmStartVolume) }, valueRange = 0f..100f, steps = 9) }
                        item { Text("Volume ramp: ${alarmVolumeRamp}s"); Slider(alarmVolumeRamp.toFloat(), { alarmVolumeRamp = it.toInt() }, valueRange = 0f..300f, steps = 9) }
                    }
                    item { SelectorField("Vibration", listOf("OFF", "LIGHT", "STANDARD", "STRONG", "ESCALATING", "CONTINUOUS"), alarmVibrationMode, { it.lowercase().replaceFirstChar(Char::uppercase) }, { alarmVibrationMode = it }, Modifier.fillMaxWidth()) }
                    if (alarmVibrationMode != "OFF") item { Text("Vibration delay: ${alarmVibrationDelay}s"); Slider(alarmVibrationDelay.toFloat(), { alarmVibrationDelay = it.toInt() }, valueRange = 0f..120f, steps = 11) }
                    item { Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { OutlinedTextField(alarmSnoozeMinutes, { alarmSnoozeMinutes = it.filter(Char::isDigit) }, label = { Text("Snooze min") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)); OutlinedTextField(alarmMaximumSnoozes, { alarmMaximumSnoozes = it.filter(Char::isDigit) }, label = { Text("Max (0 unlimited)") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)) } }
                    item { SelectorField("End condition", listOf("STANDARD", "NUMBER", "MATH", "MEMORY", "TRACE", "SORT", "SCAN", "NFC", "HOLD"), alarmDismissalMethod, { it.lowercase().replaceFirstChar(Char::uppercase) }, { alarmDismissalMethod = it }, Modifier.fillMaxWidth()) }
                    if (alarmDismissalMethod != "STANDARD") item { SelectorField("Difficulty", listOf("EASY", "NORMAL", "HARD"), alarmDismissalDifficulty, { it.lowercase().replaceFirstChar(Char::uppercase) }, { alarmDismissalDifficulty = it }, Modifier.fillMaxWidth()) }
                    if (alarmRepeat in setOf("Weekly", "Bi-weekly") || (alarmRepeat == "Custom" && alarmUnit == AlarmRepeatUnit.WEEK)) {
                        item { Text("Days of the week", fontWeight = FontWeight.SemiBold) }
                        listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").chunked(3).forEachIndexed { rowIndex, names ->
                            item {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    names.forEachIndexed { index, name ->
                                        val day = rowIndex * 3 + index
                                        OutlinedButton(onClick = { alarmWeekdays = if (day in alarmWeekdays) alarmWeekdays - day else alarmWeekdays + day }, modifier = Modifier.weight(1f), colors = ButtonDefaults.outlinedButtonColors(containerColor = if (day in alarmWeekdays) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)) { Text(name) }
                                    }
                                    repeat(3 - names.size) { Box(Modifier.weight(1f)) }
                                }
                            }
                        }
                    }
                    if (alarmRepeat == "Custom") {
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(alarmInterval, { alarmInterval = it.filter(Char::isDigit) }, label = { Text("Every") }, modifier = Modifier.weight(.6f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                                SelectorField("", AlarmRepeatUnit.entries, alarmUnit, { it.name.lowercase() + "s" }, { alarmUnit = it }, Modifier.weight(1f))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(enabled = alarmLabel.isNotBlank(), onClick = {
                    val interval = (alarmInterval.toIntOrNull() ?: 1).coerceAtLeast(1)
                    val rule = when (alarmRepeat) {
                        "Weekly" -> AlarmRepeatRule(1, AlarmRepeatUnit.WEEK, alarmWeekdays)
                        "Bi-weekly" -> AlarmRepeatRule(2, AlarmRepeatUnit.WEEK, alarmWeekdays)
                        "Monthly" -> AlarmRepeatRule(1, AlarmRepeatUnit.MONTH)
                        "Custom" -> AlarmRepeatRule(interval, alarmUnit, alarmWeekdays.takeIf { alarmUnit == AlarmRepeatUnit.WEEK }.orEmpty())
                        else -> null
                    }
                    val now = currentTimeMillis()
                    val scheduled = alarmDate + (alarmHour.toIntOrNull() ?: 7).coerceIn(0, 23) * 3_600_000L + (alarmMinute.toIntOrNull() ?: 0).coerceIn(0, 59) * 60_000L
                    val previous = editingAlarm
                    onSaveStandaloneAlarm(
                        StandaloneAlarmEntity(
                            id = previous?.id ?: 0,
                            label = alarmLabel.trim(),
                            scheduledAt = scheduled,
                            recurrenceRule = rule?.encode(),
                            enabled = previous?.enabled ?: true,
                            sound = alarmSound,
                            vibration = alarmVibrationMode != "OFF",
                            snoozeMinutes = (alarmSnoozeMinutes.toIntOrNull() ?: 8).coerceAtLeast(1),
                            profileId = null,
                            snoozeCount = previous?.snoozeCount ?: 0,
                            soundId = alarmSoundId,
                            loopSound = alarmLoopSound,
                            vibrationMode = alarmVibrationMode,
                            vibrationStartDelaySeconds = alarmVibrationDelay,
                            startingVolumePercent = alarmStartVolume,
                            maximumVolumePercent = alarmMaxVolume,
                            volumeRampSeconds = alarmVolumeRamp,
                            maximumSnoozes = (alarmMaximumSnoozes.toIntOrNull() ?: 0).coerceAtLeast(0),
                            dismissalMethod = alarmDismissalMethod,
                            dismissalDifficulty = alarmDismissalDifficulty,
                            localHour = (alarmHour.toIntOrNull() ?: 7).coerceIn(0, 23),
                            localMinute = (alarmMinute.toIntOrNull() ?: 0).coerceIn(0, 59),
                            localEpochDay = dayKey(alarmDate),
                            associatedType = previous?.associatedType,
                            associatedId = previous?.associatedId,
                            lastTriggeredAt = previous?.lastTriggeredAt,
                            lastDismissedAt = previous?.lastDismissedAt,
                            createdAt = previous?.createdAt ?: now,
                            updatedAt = now,
                        ),
                    )
                    platformActions.stopAlarmSoundPreview()
                    showAlarmEditor = false
                }) { Text("Save alarm") }
            },
            dismissButton = { TextButton(onClick = { showAlarmEditor = false }) { Text("Cancel") } },
        )
    }
    if (showAlarmDatePicker) {
        val picker = rememberDatePickerState(initialSelectedDateMillis = alarmDate)
        DatePickerDialog(onDismissRequest = { showAlarmDatePicker = false }, confirmButton = { TextButton(onClick = { alarmDate = picker.selectedDateMillis ?: alarmDate; showAlarmDatePicker = false }) { Text("Set date") } }, dismissButton = { TextButton(onClick = { showAlarmDatePicker = false }) { Text("Cancel") } }) { DatePicker(picker) }
    }
    if (showProfilePicker) {
        AlertDialog(
            onDismissRequest = { showProfilePicker = false },
            title = { Text("Use existing profile") },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(profiles.filterNot { it.builtInKey?.startsWith("legacy_") == true }, key = { "profile-choice-${it.id}" }) { profile ->
                        OutlinedButton(
                            onClick = { applyProfile(profile); showProfilePicker = false },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(Modifier.fillMaxWidth()) {
                                Text(profile.name, fontWeight = FontWeight.SemiBold)
                                Text("${profile.dismissalMethod.lowercase()} · ${profile.snoozeDurationMinutes} min snooze", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showProfilePicker = false }) { Text("Cancel") } },
        )
    }
    if (showProfileManager) {
        AlertDialog(
            onDismissRequest = { showProfileManager = false },
            title = { Text("Alarm profiles") },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(profiles.filterNot { it.builtInKey?.startsWith("legacy_") == true }, key = { "managed-profile-${it.id}" }) { profile ->
                        Card(Modifier.fillMaxWidth()) {
                            Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(profile.name + if (profile.isDefault) " · Default" else "", fontWeight = FontWeight.SemiBold)
                                    Text("${profile.dismissalMethod.lowercase()} · ${profile.snoozeDurationMinutes} min snooze", style = MaterialTheme.typography.bodySmall)
                                }
                                TextButton(onClick = {
                                    profileBeingEdited = if (profile.builtInKey == null) profile else profile.copy(id = 0, name = "${profile.name} copy", builtInKey = null, createdAt = currentTimeMillis(), updatedAt = currentTimeMillis())
                                }) { Text(if (profile.builtInKey == null) "Edit" else "Copy") }
                                if (!profile.isDefault) TextButton(onClick = { onSetDefaultProfile(profile) }) { Text("Set default") }
                            }
                        }
                    }
                    item {
                        OutlinedButton(onClick = { val now = currentTimeMillis(); profileBeingEdited = AlarmProfileEntity(name = "Custom", soundId = "standard_steel", createdAt = now, updatedAt = now) }, modifier = Modifier.fillMaxWidth()) { Text("New profile") }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showProfileManager = false }) { Text("Done") } },
        )
    }
    profileBeingEdited?.let { profile ->
        AlarmProfileEditorDialog(
            profile,
            platformActions,
            onDismiss = { platformActions.stopAlarmSoundPreview(); profileBeingEdited = null },
            onSave = { onSaveProfile(it.copy(updatedAt = currentTimeMillis())); platformActions.stopAlarmSoundPreview(); profileBeingEdited = null },
            onDelete = if (profile.id > 0 && profile.builtInKey == null) ({ onDeleteProfile(profile); profileBeingEdited = null }) else null,
        )
    }
}

private fun formatDuration(millis: Long): String {
    val seconds = millis.coerceAtLeast(0) / 1000
    return (seconds / 60).toString().padStart(2, '0') + ":" + (seconds % 60).toString().padStart(2, '0')
}

@Composable
private fun UnifiedTodayScreen(
    timeline: List<TimelineItem>,
    tasks: List<Task>,
    plan: List<DailyPlanItem>,
    projects: List<Project>,
    onEdit: (Task) -> Unit,
    onCompleteTask: (Task) -> Unit,
    onSavePlan: (DailyPlanItem) -> Unit,
    onRemove: (Long) -> Unit,
    onOpenTimeline: (TimelineItem) -> Unit,
    onDoneTimeline: (TimelineItem) -> Unit,
    padding: PaddingValues,
) {
    var hideFinished by remember { mutableStateOf(false) }
    val now = currentTimeMillis()
    val todayStart = dayKey(now) * DAY_MILLIS
    val todayEnd = todayStart + DAY_MILLIS
    val visible = timeline.filter { (it.startAt == null || it.startAt in todayStart until todayEnd || (it.actionable && it.startAt < todayStart)) && (!hideFinished || it.status !in setOf("COMPLETED", "TAKEN", "DISMISSED", "SKIPPED", "CANCELLED")) }
    val nowItems = visible.filter { it.type == com.geepscontrol.core.domain.TimelineItemType.TIMER || (it.startAt != null && it.startAt >= todayStart && it.startAt <= now && (it.endAt == null || it.endAt >= now)) }
    val upcoming = visible.filter { it.startAt in (now + 1) until todayEnd }
    val earlier = visible.filter { !it.actionable && it.startAt != null && it.startAt < now && it !in nowItems }
    val anytime = visible.filter { it.startAt == null || it.allDay }
    val overdue = visible.filter { it.type != TimelineItemType.TIMER && it.actionable && it.startAt != null && it.startAt < todayStart && it.status in setOf("PENDING", "DUE", "SCHEDULED", "OPEN") }
    val completed = visible.filter { it.status in setOf("COMPLETED", "TAKEN", "DISMISSED", "SKIPPED", "CANCELLED") }
    val conflicts = com.geepscontrol.core.domain.UnifiedTimelineService::class.let { visible.filter { it.endAt != null && it.startAt != null }.flatMapIndexed { index, first -> visible.drop(index + 1).filter { second -> second.endAt != null && second.startAt != null && first.startAt!! < second.endAt!! && second.startAt < first.endAt!! }.map { first to it } } }
    val plannedIds = plan.mapTo(mutableSetOf()) { it.taskId }
    val openTasks = tasks.filter { it.status !in setOf(TaskStatus.COMPLETED, TaskStatus.CANCELLED, TaskStatus.ARCHIVED) }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
    ) {
        item { Text("Today timeline", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(hideFinished, { hideFinished = it })
                Text("Hide completed or confirmed", modifier = Modifier.padding(start = 8.dp))
            }
        }
        if (conflicts.isNotEmpty()) item { Text("${conflicts.size} overlapping scheduled item(s)", color = MaterialTheme.colorScheme.error) }
        if (visible.isEmpty()) item { Text("No matching life items today. Use Add to create one.") }

        fun androidx.compose.foundation.lazy.LazyListScope.timelineSection(
            title: String,
            rows: List<TimelineItem>,
        ) {
            if (rows.isNotEmpty()) item { Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            items(rows.distinctBy { it.type to it.id }, key = { "timeline-$title-${it.type}-${it.id}" }) { row ->
                val isOverdue = row.actionable && row.startAt != null && row.startAt < todayStart && row.status !in setOf("COMPLETED", "TAKEN", "DISMISSED", "SKIPPED", "CANCELLED")
                Card(Modifier.fillMaxWidth().clickable { onOpenTimeline(row) }) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text(row.title, fontWeight = FontWeight.SemiBold)
                            Text("${row.type.name.lowercase().replace('_', ' ')} · ${row.status.lowercase()}${row.subtitle.takeIf { it.isNotBlank() }?.let { " · $it" }.orEmpty()}${row.timezone?.let { " · $it" }.orEmpty()}")
                            if (isOverdue) Text("Overdue!", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                        }
                        if (isOverdue) Button(onClick = { onDoneTimeline(row) }) { Text("Done") }
                    }
                }
            }
        }
        timelineSection("Now", nowItems)
        timelineSection("Upcoming", upcoming)
        timelineSection("Earlier today", earlier)
        timelineSection("Anytime today", anytime)
        timelineSection("Overdue action items", overdue)
        timelineSection("Completed or confirmed", completed)
        item { Text("Task plan", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp)) }
        if (plan.isEmpty()) item { Text("Choose tasks below to add them to today.") }
        items(plan, key = { "planned-${it.taskId}" }) { item ->
            tasks.firstOrNull { it.id == item.taskId }?.let { task ->
                Card(Modifier.fillMaxWidth().clickable { onEdit(task) }) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val isOverdue = task.dueAt != null && task.dueAt < now && task.status !in setOf(TaskStatus.COMPLETED, TaskStatus.CANCELLED, TaskStatus.ARCHIVED)
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Column(Modifier.weight(1f)) {
                                Text(task.title, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable { onEdit(task) })
                                if (isOverdue) Text("Overdue!", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                            }
                            if (isOverdue) Button(onClick = { onCompleteTask(task) }) { Text("Done") }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            (1..0).forEach { rank ->
                                OutlinedButton(onClick = { onSavePlan(item.copy(topRank = if (item.topRank == rank) null else rank)) }) { Text(if (item.topRank == rank) "Top $rank ✓" else "Top $rank") }
                            }
                            TextButton(onClick = { onRemove(task.id) }) { Text("Remove from today") }
                        }
                    }
                }
            }
        }
        item { Text("Add to today", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp)) }
        items(openTasks.filterNot { it.id in plannedIds }, key = { "available-${it.id}" }) { task ->
            OutlinedButton(
                onClick = { onSavePlan(DailyPlanItem(task.id, dayKey(currentTimeMillis()))) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("+ ${task.title}${projects.firstOrNull { it.id == task.projectId }?.let { " · ${it.name}" }.orEmpty()}") }
        }
    }
}

@Composable
private fun UpcomingScreen(
    timeline: List<TimelineItem>,
    now: Long,
    padding: PaddingValues,
) {
    var filter by remember { mutableStateOf<com.geepscontrol.core.domain.TimelineItemType?>(null) }
    val tomorrowStart = (dayKey(now) + 1) * DAY_MILLIS
    val weekEnd = tomorrowStart + 7 * DAY_MILLIS
    val filtered = timeline.filter { filter == null || it.type == filter }
    val tomorrow = filtered.filter { it.startAt in tomorrowStart until tomorrowStart + DAY_MILLIS }
    val nextSeven = filtered.filter { it.startAt in tomorrowStart + DAY_MILLIS until weekEnd }
    val later = filtered.filter { it.startAt != null && it.startAt >= weekEnd }
    val unscheduled = filtered.filter { it.startAt == null }
    LazyColumn(
        Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
    ) {
        item { Text("Upcoming", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(null, com.geepscontrol.core.domain.TimelineItemType.TASK, com.geepscontrol.core.domain.TimelineItemType.REMINDER, com.geepscontrol.core.domain.TimelineItemType.EVENT).forEach { value ->
                        OutlinedButton(onClick = { filter = value }) { Text(if (value == null) "All" else value.name.lowercase().replaceFirstChar { it.uppercase() }) }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(com.geepscontrol.core.domain.TimelineItemType.ROUTINE, com.geepscontrol.core.domain.TimelineItemType.MEDICATION, com.geepscontrol.core.domain.TimelineItemType.ALARM, com.geepscontrol.core.domain.TimelineItemType.ITINERARY).forEach { value ->
                        OutlinedButton(onClick = { filter = value }) { Text(value.name.lowercase().replaceFirstChar { it.uppercase() }) }
                    }
                }
            }
        }

        fun androidx.compose.foundation.lazy.LazyListScope.section(
            title: String,
            rows: List<TimelineItem>,
        ) {
            item { Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            if (rows.isEmpty()) item { Text("Nothing here", style = MaterialTheme.typography.bodySmall) }
            items(rows, key = { "upcoming-$title-${it.type}-${it.id}" }) { row ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(row.title, fontWeight = FontWeight.SemiBold)
                        Text("${row.type.name.lowercase()} · ${row.status.lowercase()}${row.timezone?.let { " · $it" }.orEmpty()}")
                    }
                }
            }
        }
        section("Tomorrow", tomorrow)
        section("Next seven days", nextSeven)
        section("Later", later)
        section("Unscheduled", unscheduled)
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun LifeItemsScreen(
    reminders: List<ReminderEntity>,
    routines: List<RoutineEntity>,
    routineOccurrences: List<RoutineOccurrenceEntity>,
    medications: List<MedicationEntity>,
    medicationOccurrences: List<MedicationOccurrenceEntity>,
    events: List<LifeEventEntity>,
    alarms: List<StandaloneAlarmEntity>,
    timers: List<StandaloneTimerEntity>,
    presets: List<com.geepscontrol.core.database.TimerPresetEntity>,
    initialType: String,
    showAddInitially: Boolean,
    onOpenMedication: (Long) -> Unit,
    repository: LifeItemRepository,
    alarmScheduler: AlarmScheduler,
    padding: PaddingValues,
) {
    var title by remember { mutableStateOf("") }
    var type by remember(initialType) { mutableStateOf(initialType) }
    var showAdd by remember(initialType, showAddInitially) { mutableStateOf(showAddInitially) }
    var durationMinutes by remember { mutableStateOf("10") }
    var details by remember { mutableStateOf("") }
    var timezone by remember { mutableStateOf("America/New_York") }
    var recurring by remember { mutableStateOf(false) }
    val initialScheduleAt = remember { currentTimeMillis() + 60 * 60_000L }
    var scheduledDate by remember { mutableStateOf(dayKey(initialScheduleAt) * DAY_MILLIS) }
    var scheduleHour by remember { mutableStateOf(((initialScheduleAt % DAY_MILLIS) / 3_600_000L).toString()) }
    var scheduleMinute by remember { mutableStateOf((((initialScheduleAt % DAY_MILLIS) / 60_000L) % 60).toString().padStart(2, '0')) }
    var showScheduleDatePicker by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val types = listOf("Reminder", "Routine", "Medication", "Event", "Alarm", "Timer")
    LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(vertical = 16.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { showAdd = false }, modifier = Modifier.weight(1f), colors = ButtonDefaults.outlinedButtonColors(containerColor = if (!showAdd) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)) { Text("Saved items") }
                OutlinedButton(onClick = { showAdd = true }, modifier = Modifier.weight(1f), colors = ButtonDefaults.outlinedButtonColors(containerColor = if (showAdd) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)) { Text("Add new") }
            }
        }
        if (showAdd) {
        item { Text("Quick add", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
        item { Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { types.take(3).forEach { value -> OutlinedButton(onClick = { type = value }) { Text(if (type == value) "$value ✓" else value) } } } }
        item { Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { types.drop(3).forEach { value -> OutlinedButton(onClick = { type = value }) { Text(if (type == value) "$value ✓" else value) } } } }
        item { OutlinedTextField(title, { title = it }, label = { Text("$type name") }, modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(details, { details = it }, label = { Text(if (type == "Medication") "Dosage / instructions" else "Description / notes") }, modifier = Modifier.fillMaxWidth()) }
        if (type != "Timer") {
            item { OutlinedButton(onClick = { showScheduleDatePicker = true }, modifier = Modifier.fillMaxWidth()) { Text("Date: ${formatDate(scheduledDate)}") } }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(scheduleHour, { scheduleHour = it.filter(Char::isDigit).take(2) }, label = { Text("Hour (0–23)") }, modifier = Modifier.weight(1f), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    OutlinedTextField(scheduleMinute, { scheduleMinute = it.filter(Char::isDigit).take(2) }, label = { Text("Minute") }, modifier = Modifier.weight(1f), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                }
            }
        }
        if (type in listOf("Timer", "Event")) item { OutlinedTextField(durationMinutes, { durationMinutes = it.filter(Char::isDigit) }, label = { Text("Duration minutes") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)) }
        if (type == "Event") item { OutlinedTextField(timezone, { timezone = it }, label = { Text("IANA time zone") }, modifier = Modifier.fillMaxWidth()) }
        if (type in listOf("Reminder", "Medication", "Event", "Alarm")) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(recurring, { recurring = it })
                    Text("Repeat daily", modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
        if (type == "Routine") item { Text("Repeats daily at the selected time", style = MaterialTheme.typography.bodySmall) }
        if (type == "Medication") item { Text("Life Minder provides reminders and personal records only. Do not rely on it as the sole safeguard for time-critical medication.", style = MaterialTheme.typography.bodySmall) }
        item {
            Button(onClick = {
                if (title.isNotBlank()) {
                    scope.launch {
                        val now = currentTimeMillis()
                        val scheduledAt = scheduledDate + (scheduleHour.toIntOrNull() ?: 9).coerceIn(0, 23) * 3_600_000L + (scheduleMinute.toIntOrNull() ?: 0).coerceIn(0, 59) * 60_000L
                        val rule = if (recurring) "1:DAY:FIXED" else null
                        when (type) {
                            "Reminder" -> {
                                val id = repository.save(ReminderEntity(title = title, description = details, scheduledAt = scheduledAt, recurrenceRule = rule, createdAt = now, updatedAt = now))
                                alarmScheduler.scheduleReminder(1_000_000 + id, scheduledAt, title, NotificationCategory.TASK_REMINDER)
                            }
                            "Routine" -> {
                                val preferredMinutes = ((scheduledAt - dayKey(scheduledAt) * DAY_MILLIS) / 60_000).toInt()
                                val id = repository.save(RoutineEntity(title = title, description = details, recurrenceRule = "1:DAY:FIXED", preferredTimeMinutes = preferredMinutes, startDate = dayKey(scheduledAt) * DAY_MILLIS, createdAt = now, updatedAt = now))
                                repository.save(RoutineOccurrenceEntity(routineId = id, scheduledAt = scheduledAt, createdAt = now, updatedAt = now))
                            }
                            "Medication" -> {
                                val minutes = ((scheduledAt - dayKey(scheduledAt) * DAY_MILLIS) / 60_000).toInt()
                                val id = repository.save(MedicationEntity(name = title, dosageText = details, instructions = details, scheduleType = if (recurring) "DAILY" else "ONCE", recurrenceRule = rule, timesOfDay = minutes.toString(), startDate = dayKey(scheduledAt) * DAY_MILLIS, createdAt = now, updatedAt = now))
                                repository.save(MedicationOccurrenceEntity(medicationId = id, scheduledAt = scheduledAt, createdAt = now, updatedAt = now))
                                alarmScheduler.scheduleReminder(2_000_000 + id, scheduledAt, title, NotificationCategory.TASK_REMINDER)
                            }
                            "Event" -> repository.save(LifeEventEntity(title = title, description = details, startAt = scheduledAt, endAt = scheduledAt + (durationMinutes.toLongOrNull() ?: 60L).coerceAtLeast(1) * 60_000L, timezone = timezone.ifBlank { "UTC" }, recurrenceRule = rule, createdAt = now, updatedAt = now))
                            "Alarm" -> {
                                val id = repository.save(StandaloneAlarmEntity(label = title, scheduledAt = scheduledAt, recurrenceRule = rule, createdAt = now, updatedAt = now))
                                alarmScheduler.schedule(AlarmDefinition(id = 3_000_000 + id, triggerAt = scheduledAt, label = title))
                            }
                            "Timer" -> {
                                val duration = (durationMinutes.toLongOrNull() ?: 10L).coerceAtLeast(1) * 60_000L
                                val presetId =
                                    repository.save(
                                        com.geepscontrol.core.database
                                            .TimerPresetEntity(name = title, durationSeconds = duration / 1000, notes = details, createdAt = now, updatedAt = now),
                                    )
                                val id = repository.save(StandaloneTimerEntity(presetId = presetId, name = title, startedAt = now, resumedAt = now, durationMillis = duration, updatedAt = now))
                                alarmScheduler.scheduleTimer(ActiveTimer(id = 4_000_000 + id, mode = TimerMode.COUNTDOWN, state = TimerState.RUNNING, startedAt = now, intendedDurationMillis = duration))
                            }
                        }
                        title = ""
                        showAdd = false
                    }
                }
            }, modifier = Modifier.fillMaxWidth()) { Text("Save $type") }
        }
        if (presets.isNotEmpty()) item { Text("Timer presets", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        items(presets, key = { "preset-${it.id}" }) { preset ->
            OutlinedButton(onClick = {
                scope.launch {
                    val started = currentTimeMillis()
                    val id = repository.save(StandaloneTimerEntity(presetId = preset.id, name = preset.name, startedAt = started, resumedAt = started, durationMillis = preset.durationSeconds * 1000, sound = preset.sound, vibration = preset.vibration, continueOvertime = preset.continueOvertime, autoRepeat = preset.autoRepeat, updatedAt = started))
                    alarmScheduler.scheduleTimer(ActiveTimer(id = 4_000_000 + id, mode = TimerMode.COUNTDOWN, state = TimerState.RUNNING, startedAt = started, intendedDurationMillis = preset.durationSeconds * 1000))
                }
            }, modifier = Modifier.fillMaxWidth()) { Text("Start ${preset.name} · ${preset.durationSeconds / 60} min") }
        }
        }
        if (!showAdd) {
        item { Text("Saved life items", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        items(reminders, key = { "reminder-${it.id}" }) { value ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text(value.title, fontWeight = FontWeight.SemiBold)
                    Text("Reminder · ${value.status.lowercase()} · ${formatAlarmTime(value.scheduledAt)}")
                    Row {
                        TextButton(onClick = {
                            scope.launch {
                                repository.reminderStatus(value.id, "COMPLETED", currentTimeMillis())
                                alarmScheduler.cancelReminder(1_000_000 + value.id)
                            }
                        }) { Text("Done") }
                        TextButton(onClick = { scope.launch { repository.snoozeReminder(value.id, currentTimeMillis() + 10 * 60_000, currentTimeMillis()) } }) { Text("Snooze") }
                        TextButton(onClick = { scope.launch { repository.reminderStatus(value.id, "DISMISSED", currentTimeMillis()) } }) { Text("Dismiss") }
                    }
                }
            }
        }
        items(routines, key = { "routine-${it.id}" }) { value ->
            val history = routineOccurrences.filter { it.routineId == value.id && it.status != "PENDING" }
            val completedCount = history.count { it.status == "COMPLETED" }
            val completionRate = if (history.isEmpty()) 0 else completedCount * 100 / history.size
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text(value.title, fontWeight = FontWeight.SemiBold)
                    Text("Routine · ${if (value.active) "active" else "paused"} · $completedCount completed · $completionRate% rate")
                    routineOccurrences.firstOrNull { it.routineId == value.id && it.status == "PENDING" }?.let { Text("Next: ${formatAlarmTime(it.scheduledAt)}", style = MaterialTheme.typography.bodySmall) }
                    Row {
                        TextButton(onClick = { scope.launch { routineOccurrences.firstOrNull { it.routineId == value.id && it.status == "PENDING" }?.let { repository.save(it.copy(status = "COMPLETED", recordedAt = currentTimeMillis(), updatedAt = currentTimeMillis())) } } }) { Text("Done") }
                        TextButton(onClick = { scope.launch { routineOccurrences.firstOrNull { it.routineId == value.id && it.status == "PENDING" }?.let { repository.save(it.copy(status = "SKIPPED", recordedAt = currentTimeMillis(), updatedAt = currentTimeMillis())) } } }) { Text("Skip") }
                        TextButton(onClick = { scope.launch { repository.setRoutineActive(value.id, !value.active, currentTimeMillis()) } }) { Text(if (value.active) "Pause" else "Resume") }
                    }
                }
            }
        }
        items(medications, key = { "medication-${it.id}" }) { value ->
            Card(Modifier.fillMaxWidth().clickable { onOpenMedication(value.id) }) {
                Column(Modifier.padding(12.dp)) {
                    Text(value.name, fontWeight = FontWeight.SemiBold)
                    Text("Medication · ${if (value.active) "active" else "paused"}${value.quantityRemaining?.let { " · $it remaining" }.orEmpty()}")
                    medicationOccurrences.firstOrNull { it.medicationId == value.id && it.status in setOf("DUE", "SNOOZED") }?.let { Text("Next: ${formatAlarmTime(it.snoozedUntil ?: it.scheduledAt)}", style = MaterialTheme.typography.bodySmall) }
                    Row {
                        TextButton(onClick = {
                            scope.launch {
                                medicationOccurrences.firstOrNull { it.medicationId == value.id && it.status in setOf("DUE", "SNOOZED") }?.let {
                                    repository.save(it.copy(status = "TAKEN", recordedAt = currentTimeMillis(), dosageTakenText = value.dosageText, updatedAt = currentTimeMillis()))
                                    repository.save(
                                        value.copy(
                                            quantityRemaining =
                                                com.geepscontrol.core.domain.LifeScheduleEngine
                                                    .refillAfterDose(value.quantityRemaining, value.quantityPerDose),
                                            updatedAt = currentTimeMillis(),
                                        ),
                                    )
                                }
                            }
                        }) { Text("Taken") }
                        TextButton(onClick = { scope.launch { medicationOccurrences.firstOrNull { it.medicationId == value.id && it.status in setOf("DUE", "SNOOZED") }?.let { repository.save(it.copy(status = "SKIPPED", recordedAt = currentTimeMillis(), updatedAt = currentTimeMillis())) } } }) { Text("Skip") }
                        TextButton(onClick = {
                            scope.launch {
                                medicationOccurrences.firstOrNull { it.medicationId == value.id && it.status == "DUE" }?.let {
                                    val until = currentTimeMillis() + 10 * 60_000
                                    repository.save(it.copy(status = "SNOOZED", snoozedUntil = until, updatedAt = currentTimeMillis()))
                                    alarmScheduler.scheduleReminder(2_000_000 + it.id, until, value.name, NotificationCategory.TASK_REMINDER)
                                }
                            }
                        }) { Text("Snooze") }
                        TextButton(onClick = { scope.launch { repository.setMedicationActive(value.id, !value.active, currentTimeMillis()) } }) { Text(if (value.active) "Pause" else "Resume") }
                    }
                    medicationOccurrences.firstOrNull { it.medicationId == value.id && it.status in setOf("TAKEN", "SKIPPED") }?.let { latest -> TextButton(onClick = { scope.launch { repository.save(latest.copy(status = "DUE", recordedAt = null, dosageTakenText = "", updatedAt = currentTimeMillis())) } }) { Text("Correct latest entry") } }
                }
            }
        }
        items(events, key = { "event-${it.id}" }) { value ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text(value.title, fontWeight = FontWeight.SemiBold)
                    Text("Event · ${value.status.lowercase()} · ${formatAlarmTime(value.startAt)} · ${value.timezone}")
                }
            }
        }
        items(alarms, key = { "life-alarm-${it.id}" }) { value ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text(value.label, fontWeight = FontWeight.SemiBold)
                    Text("Alarm · ${if (value.enabled) "enabled" else "disabled"} · ${formatAlarmTime(value.scheduledAt)}")
                    TextButton(onClick = {
                        scope.launch {
                            repository.setAlarmEnabled(value.id, !value.enabled, currentTimeMillis())
                            if (value.enabled) alarmScheduler.cancel(3_000_000 + value.id) else alarmScheduler.schedule(AlarmDefinition(id = 3_000_000 + value.id, triggerAt = value.scheduledAt, label = value.label))
                        }
                    }) { Text(if (value.enabled) "Turn off" else "Enable") }
                }
            }
        }
        items(timers, key = { "standalone-timer-${it.id}" }) { value ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text(value.name, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Timer · ${value.state.lowercase()} · ${com.geepscontrol.core.domain.LifeScheduleEngine
                            .timerRemaining(value, currentTimeMillis()) / 1000}s",
                    )
                    Row {
                        TextButton(onClick = {
                            scope.launch {
                                val updated =
                                    value.copy(
                                        state = if (value.state == "RUNNING") "PAUSED" else "RUNNING",
                                        accumulatedMillis =
                                            com.geepscontrol.core.domain.LifeScheduleEngine
                                                .timerElapsed(value, currentTimeMillis()),
                                        resumedAt = currentTimeMillis(),
                                        updatedAt = currentTimeMillis(),
                                    )
                                repository.save(updated)
                                if (updated.state == "RUNNING") alarmScheduler.scheduleTimer(ActiveTimer(id = 4_000_000 + updated.id, mode = TimerMode.COUNTDOWN, state = TimerState.RUNNING, startedAt = updated.startedAt, resumedAt = updated.resumedAt, accumulatedMillis = updated.accumulatedMillis, intendedDurationMillis = updated.durationMillis, updatedAt = updated.updatedAt))
                            }
                        }) { Text(if (value.state == "RUNNING") "Pause" else "Resume") }
                        TextButton(onClick = { scope.launch { repository.save(value.copy(durationMillis = value.durationMillis + 60_000, updatedAt = currentTimeMillis())) } }) { Text("+1m") }
                        TextButton(onClick = { scope.launch { repository.save(value.copy(durationMillis = (value.durationMillis - 60_000).coerceAtLeast(60_000), updatedAt = currentTimeMillis())) } }) { Text("-1m") }
                        TextButton(onClick = {
                            scope.launch {
                                val ended = currentTimeMillis()
                                val elapsed =
                                    com.geepscontrol.core.domain.LifeScheduleEngine
                                        .timerElapsed(value, ended)
                                repository.addHistory(
                                    com.geepscontrol.core.database
                                        .TimerHistoryEntity(presetId = value.presetId, name = value.name, startedAt = value.startedAt, endedAt = ended, originalDurationMillis = value.durationMillis, finalDurationMillis = value.durationMillis, result = if (elapsed >= value.durationMillis) "COMPLETED" else "CANCELLED", overtimeMillis = (elapsed - value.durationMillis).coerceAtLeast(0)),
                                )
                                repository.removeTimer(value.id)
                            }
                        }) { Text("Dismiss") }
                    }
                }
            }
        }
        }
    }
    if (showScheduleDatePicker) {
        val picker = rememberDatePickerState(initialSelectedDateMillis = scheduledDate)
        DatePickerDialog(
            onDismissRequest = { showScheduleDatePicker = false },
            confirmButton = { TextButton(onClick = { scheduledDate = picker.selectedDateMillis ?: scheduledDate; showScheduleDatePicker = false }) { Text("Set date") } },
            dismissButton = { TextButton(onClick = { showScheduleDatePicker = false }) { Text("Cancel") } },
        ) { DatePicker(picker) }
    }
    LaunchedEffect(routines, medications, routineOccurrences, medicationOccurrences) {
        val generatedAt = currentTimeMillis()
        val dayStart = dayKey(generatedAt) * DAY_MILLIS
        routines.filter { it.active && it.startDate <= dayStart && (it.endDate == null || it.endDate >= dayStart) }.forEach { routine ->
            if (routineOccurrences.none { it.routineId == routine.id && it.scheduledAt in dayStart until dayStart + DAY_MILLIS }) {
                repository.save(RoutineOccurrenceEntity(routineId = routine.id, scheduledAt = dayStart + (routine.preferredTimeMinutes ?: 9 * 60) * 60_000L, createdAt = generatedAt, updatedAt = generatedAt))
            }
        }
        medications.filter { it.active && !it.asNeeded && (it.scheduleType != "ONCE" || it.recurrenceRule != null) && it.startDate <= dayStart && (it.endDate == null || it.endDate >= dayStart) }.forEach { medication ->
            val minutes =
                medication.timesOfDay
                    .split(',')
                    .mapNotNull { it.trim().toIntOrNull() }
                    .ifEmpty { listOf(9 * 60) }
            com.geepscontrol.core.domain.LifeScheduleEngine.medicationTimes(dayStart, minutes).forEach { scheduled ->
                if (medicationOccurrences.none { it.medicationId == medication.id && it.scheduledAt == scheduled }) repository.save(MedicationOccurrenceEntity(medicationId = medication.id, scheduledAt = scheduled, createdAt = generatedAt, updatedAt = generatedAt))
            }
        }
    }
    LaunchedEffect(reminders, alarms, timers) {
        val schedulingNow = currentTimeMillis()
        reminders.filter { it.status in setOf("SCHEDULED", "SNOOZED") && it.scheduledAt > schedulingNow }.forEach { alarmScheduler.scheduleReminder(1_000_000 + it.id, it.scheduledAt, it.title, NotificationCategory.TASK_REMINDER) }
        alarms.filter { it.enabled && it.scheduledAt > schedulingNow }.forEach { alarmScheduler.schedule(AlarmDefinition(id = 3_000_000 + it.id, triggerAt = it.scheduledAt, label = it.label, vibration = it.vibration, sound = it.sound)) }
        timers.filter { it.state == "RUNNING" }.forEach { timer -> alarmScheduler.scheduleTimer(ActiveTimer(id = 4_000_000 + timer.id, mode = TimerMode.COUNTDOWN, state = TimerState.RUNNING, startedAt = timer.startedAt, resumedAt = timer.resumedAt, accumulatedMillis = timer.accumulatedMillis, intendedDurationMillis = timer.durationMillis, updatedAt = timer.updatedAt)) }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun TripsScreen(
    trips: List<TripEntity>,
    entries: List<ItineraryEntryEntity>,
    references: List<com.geepscontrol.core.database.TripReferenceEntity>,
    tripTasks: List<com.geepscontrol.core.database.TripTaskEntity>,
    transportation: List<com.geepscontrol.core.database.TransportationDetailsEntity>,
    lodging: List<com.geepscontrol.core.database.LodgingDetailsEntity>,
    tasks: List<Task>,
    repository: LifeItemRepository,
    alarmScheduler: AlarmScheduler,
    platformActions: PlatformActions,
    initialTripId: Long?,
    entryRequest: Int,
    onAddTask: (Long) -> Unit,
    padding: PaddingValues,
) {
    var title by remember { mutableStateOf("") }
    var selectedTrip by remember(initialTripId) { mutableStateOf(initialTripId) }
    var detailsExpanded by remember(initialTripId) { mutableStateOf(false) }
    var mode by remember { mutableStateOf("Entry") }
    var editingEntryId by remember { mutableStateOf<Long?>(null) }
    var entryType by remember { mutableStateOf("GENERAL") }
    var details by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var provider by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var website by remember { mutableStateOf("") }
    var timezone by remember { mutableStateOf("America/New_York") }
    var startDate by remember { mutableStateOf(dayKey(currentTimeMillis()) * DAY_MILLIS) }
    var endDate by remember { mutableStateOf(dayKey(currentTimeMillis()) * DAY_MILLIS) }
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }
    var sensitive by remember { mutableStateOf(false) }
    var tripSummary by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val now = currentTimeMillis()
    LaunchedEffect(initialTripId, trips) {
        trips.firstOrNull { it.id == initialTripId }?.let { trip ->
            startDate = trip.startDate
            endDate = trip.endDate
            timezone = trip.destinationTimezones.ifBlank { trip.homeTimezone }
            tripSummary = trip.description
        }
    }
    LaunchedEffect(entryRequest) {
        if (entryRequest > 0 && selectedTrip != null) {
            detailsExpanded = true
            mode = "Entry"
            entryType = "GENERAL"
        }
    }
    LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(vertical = 16.dp)) {
        item { Text("Trips and itineraries", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
        selectedTrip?.let { tripId ->
            trips.firstOrNull { it.id == tripId }?.let { trip ->
                item {
                    Text(trip.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(listOf("${formatDate(trip.startDate)} – ${formatDate(trip.endDate)}", trip.description.ifBlank { trip.destinationTimezones }).filter { it.isNotBlank() }.joinToString(" · "), style = MaterialTheme.typography.bodySmall)
                }
            }
            val linkedTasks = tasks.filter { task -> tripTasks.any { it.tripId == tripId && it.taskId == task.id } }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Trip tasks", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(onClick = { detailsExpanded = true; mode = "Entry"; editingEntryId = null; entryType = "GENERAL"; title = ""; details = ""; address = "" }) { Text("Add place") }
                        Button(onClick = { onAddTask(tripId) }) { Text("Add task") }
                    }
                }
            }
            if (linkedTasks.isEmpty()) item { Text("No tasks linked to this trip yet.") }
            items(linkedTasks, key = { "trip-main-task-${it.id}" }) { task ->
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = priorityColor(task.priority).copy(alpha = 0.16f))) {
                    Column(Modifier.padding(12.dp)) {
                        Text(task.title, fontWeight = FontWeight.SemiBold)
                        Text("${task.status.displayName()} · ${task.priority.displayName()}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            val tripPlaces = entries.filter { it.tripId == tripId && it.status !in setOf("COMPLETED", "CANCELLED") && (it.endAt == null || it.endAt >= now) }
            item { Text("Places & itinerary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            if (tripPlaces.isEmpty()) item { Text("No places added yet.") }
            items(tripPlaces, key = { "trip-main-place-${it.id}" }) { place ->
                Card(
                    Modifier.fillMaxWidth().clickable {
                        editingEntryId = place.id
                        detailsExpanded = true
                        mode = "Entry"
                        entryType = place.type
                        title = place.title
                        details = place.description
                        address = place.address
                        confirmation = place.confirmationNumber
                        provider = place.provider
                        timezone = place.timezone
                        startDate = place.startAt ?: now
                        endDate = place.endAt ?: startDate
                    },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    ),
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(place.title, fontWeight = FontWeight.SemiBold)
                        Text(listOf(place.type.lowercase(), place.address, place.startAt?.let(::formatDate)).filter { !it.isNullOrBlank() }.joinToString(" · "), style = MaterialTheme.typography.bodySmall)
                        Row {
                            if (place.address.isNotBlank()) TextButton(onClick = { platformActions.openMap(place.address) }) { Text("Open map") }
                            TextButton(onClick = { scope.launch { repository.save(place.copy(status = "COMPLETED", updatedAt = currentTimeMillis())) } }) { Text("Complete") }
                        }
                    }
                }
            }
            item {
                OutlinedButton(onClick = { detailsExpanded = !detailsExpanded }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (detailsExpanded) "▼ Hide trip details & itinerary" else "▶ Show trip details & itinerary")
                }
            }
        }
        if (selectedTrip == null || detailsExpanded) {
        selectedTrip?.let { tripId ->
            item { Text("Trip details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { showStartPicker = true }, modifier = Modifier.weight(1f)) { Text("Start: ${formatDate(startDate)}") }
                    OutlinedButton(onClick = { showEndPicker = true }, modifier = Modifier.weight(1f)) { Text("End: ${formatDate(endDate)}") }
                }
            }
            item { OutlinedTextField(timezone, { timezone = it }, label = { Text("Destination time zone") }, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(tripSummary, { tripSummary = it }, label = { Text("Destination / trip summary") }, modifier = Modifier.fillMaxWidth()) }
            item {
                OutlinedButton(onClick = {
                    trips.firstOrNull { it.id == tripId }?.let { trip ->
                        scope.launch { repository.save(trip.copy(description = tripSummary.trim(), startDate = startDate, endDate = endDate.coerceAtLeast(startDate), destinationTimezones = timezone.trim(), updatedAt = currentTimeMillis())) }
                    }
                }, modifier = Modifier.fillMaxWidth()) { Text("Save trip details") }
            }
        }
        if (selectedTrip != null) item { Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { listOf("Entry", "Reference").forEach { value -> OutlinedButton(onClick = { mode = value }) { Text(if (mode == value) "$value ✓" else value) } } } }
        if (selectedTrip != null && mode == "Entry") item {
            SelectorField("Location type", listOf("GENERAL", "HOTEL", "ATTRACTION", "RESTAURANT", "EVENT", "FLIGHT", "TRAIN", "TRANSFER"), entryType, { it.lowercase().replaceFirstChar { char -> char.uppercase() } }, { entryType = it }, Modifier.fillMaxWidth())
        }
        item { OutlinedTextField(title, { title = it }, label = { Text(if (selectedTrip == null) "Trip name" else "$mode title") }, modifier = Modifier.fillMaxWidth()) }
        if (selectedTrip == null) {
            item { OutlinedTextField(address, { address = it }, label = { Text("Destination or main location") }, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(details, { details = it }, label = { Text("Trip summary / notes") }, modifier = Modifier.fillMaxWidth()) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { showStartPicker = true }, modifier = Modifier.weight(1f)) { Text("Start: ${formatDate(startDate)}") }
                    OutlinedButton(onClick = { showEndPicker = true }, modifier = Modifier.weight(1f)) { Text("End: ${formatDate(endDate)}") }
                }
            }
            item { OutlinedTextField(timezone, { timezone = it }, label = { Text("Destination time zone") }, modifier = Modifier.fillMaxWidth()) }
        }
        if (selectedTrip != null) {
            item { OutlinedTextField(details, { details = it }, label = { Text("Description / notes") }, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(address, { address = it }, label = { Text("Address or location") }, modifier = Modifier.fillMaxWidth()) }
            if (mode == "Entry") {
                item { OutlinedTextField(confirmation, { confirmation = it }, label = { Text("Confirmation / booking reference") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(provider, { provider = it }, label = { Text("Carrier, property, or provider") }, modifier = Modifier.fillMaxWidth()) }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { showStartPicker = true }, modifier = Modifier.weight(1f)) { Text("Start: ${formatDate(startDate)}") }
                        OutlinedButton(onClick = { showEndPicker = true }, modifier = Modifier.weight(1f)) { Text("End: ${formatDate(endDate)}") }
                    }
                }
                item { OutlinedTextField(timezone, { timezone = it }, label = { Text("IANA time zone") }, modifier = Modifier.fillMaxWidth()) }
            } else {
                item { OutlinedTextField(phone, { phone = it }, label = { Text("Phone") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(website, { website = it }, label = { Text("Website") }, modifier = Modifier.fillMaxWidth()) }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(sensitive, { sensitive = it })
                        Text("Sensitive", modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        }
        item {
            Button(onClick = {
                if (title.isNotBlank()) {
                    scope.launch {
                        if (selectedTrip == null) {
                            repository.save(TripEntity(name = title, description = listOf(address, details).filter { it.isNotBlank() }.joinToString(" · "), startDate = startDate, endDate = endDate.coerceAtLeast(startDate), homeTimezone = timezone.ifBlank { "UTC" }, destinationTimezones = timezone.ifBlank { "UTC" }, createdAt = now, updatedAt = now))
                        } else if (mode == "Reference") {
                            repository.save(
                                com.geepscontrol.core.database
                                    .TripReferenceEntity(tripId = selectedTrip!!, category = "GENERAL", title = title, content = details, address = address, phone = phone, website = website, sensitive = sensitive, createdAt = now, updatedAt = now),
                            )
                        } else {
                            val start = startDate
                            val end = endDate.coerceAtLeast(startDate)
                            val previous = editingEntryId?.let { id -> entries.firstOrNull { it.id == id } }
                            val id = repository.save(ItineraryEntryEntity(id = editingEntryId ?: 0, tripId = selectedTrip!!, type = entryType, title = title, description = details, startAt = start, endAt = end, timezone = timezone.ifBlank { "UTC" }, address = address, confirmationNumber = confirmation, bookingReference = confirmation, provider = provider, notes = details, createdAt = previous?.createdAt ?: now, updatedAt = now))
                            if (entryType in setOf("FLIGHT", "TRAIN", "BUS", "FERRY", "TRANSFER")) {
                                repository.save(
                                    com.geepscontrol.core.database
                                        .TransportationDetailsEntity(itineraryEntryId = id, carrier = provider, serviceNumber = confirmation, ticketReference = confirmation),
                                )
                            }
                            if (entryType == "HOTEL") {
                                repository.save(
                                    com.geepscontrol.core.database
                                        .LodgingDetailsEntity(itineraryEntryId = id, propertyName = provider.ifBlank { title }, address = address, confirmationNumber = confirmation),
                                )
                            }
                        }
                        title = ""
                        details = ""
                        address = ""
                        confirmation = ""
                        editingEntryId = null
                    }
                }
            }, modifier = Modifier.fillMaxWidth()) { Text(if (selectedTrip == null) "Create trip" else "Add ${mode.lowercase()}") }
        }
        }
        if (selectedTrip == null) trips.forEach { trip ->
            val tripEntries = entries.filter { it.tripId == trip.id }
            val conflicts =
                com.geepscontrol.core.domain.TripService
                    .conflicts(tripEntries)
            item {
                Card(Modifier.fillMaxWidth().clickable {
                    selectedTrip = if (selectedTrip == trip.id) null else trip.id
                    detailsExpanded = false
                    startDate = trip.startDate
                    endDate = trip.endDate
                    timezone = trip.destinationTimezones.ifBlank { trip.homeTimezone }
                    tripSummary = trip.description
                }) {
                    Column(Modifier.padding(12.dp)) {
                        Text(trip.name, fontWeight = FontWeight.Bold)
                        Text("${formatDate(trip.startDate)} – ${formatDate(trip.endDate)} · ${trip.status.lowercase()} · ${tripEntries.size} locations")
                        if (trip.description.isNotBlank()) Text(trip.description, style = MaterialTheme.typography.bodySmall)
                        if (conflicts.isNotEmpty()) Text("${conflicts.size} schedule conflict warning(s)", color = MaterialTheme.colorScheme.error)
                        val missing = tripEntries.count { it.status == "TENTATIVE" || it.address.isBlank() || (it.type in setOf("FLIGHT", "TRAIN", "HOTEL") && it.confirmationNumber.isBlank()) }
                        if (missing > 0) Text("$missing tentative or missing-detail item(s)")
                        Text(if (selectedTrip == trip.id) "Selected — tasks are shown above" else "Tap to open trip")
                        Row {
                            TextButton(onClick = {
                                scope.launch {
                                    val copyId = repository.save(trip.copy(id = 0, name = "${trip.name} copy", createdAt = now, updatedAt = now))
                                    tripEntries.forEach {
                                        repository.save(
                                            com.geepscontrol.core.domain.TripService
                                                .duplicate(it, copyId, 0, now),
                                        )
                                    }
                                }
                            }) { Text("Duplicate trip") }
                        }
                    }
                }
            }
            if (selectedTrip == trip.id && detailsExpanded) {
                item { Text("Itinerary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
                items(tripEntries, key = { "trip-entry-${it.id}" }) { entry ->
                    Card(Modifier.fillMaxWidth().padding(start = 16.dp)) {
                        Column(Modifier.padding(12.dp)) {
                            Text(entry.title, fontWeight = FontWeight.SemiBold)
                            Text("${entry.type.lowercase()} · ${entry.status.lowercase()} · ${entry.timezone}")
                            if (entry.address.isNotBlank()) Text(entry.address)
                            if (entry.confirmationNumber.isNotBlank()) Text("Confirmation: ${entry.confirmationNumber}")
                            transportation.firstOrNull { it.itineraryEntryId == entry.id }?.let { Text("${it.carrier} ${it.serviceNumber} · ${it.seat.ifBlank { "seat TBD" }}") }
                            lodging.firstOrNull { it.itineraryEntryId == entry.id }?.let { Text("${it.propertyName} · ${it.confirmationNumber}") }
                            Row {
                                if (entry.address.isNotBlank()) TextButton(onClick = { platformActions.openMap(entry.address) }) { Text("Map") }
                                if (entry.confirmationNumber.isNotBlank()) TextButton(onClick = { platformActions.copy("Confirmation", entry.confirmationNumber) }) { Text("Copy") }
                                TextButton(onClick = {
                                    scope.launch {
                                        repository.save(
                                            com.geepscontrol.core.domain.TripService
                                                .duplicate(entry, now = currentTimeMillis()),
                                        )
                                    }
                                }) { Text("Duplicate") }
                                TextButton(onClick = { scope.launch { repository.moveItinerary(entry.id, entry.startAt?.plus(DAY_MILLIS), entry.endAt?.plus(DAY_MILLIS), currentTimeMillis()) } }) { Text("+1 day") }
                            }
                            Row {
                                TextButton(onClick = {
                                    entry.startAt?.let { start ->
                                        val at =
                                            com.geepscontrol.core.domain.TripService
                                                .reminderAt(start, 30)
                                        alarmScheduler.scheduleReminder(5_000_000 + entry.id, at, "${trip.name}: ${entry.title}", NotificationCategory.TASK_REMINDER)
                                    }
                                }) { Text("Remind 30m") }
                                TextButton(onClick = { scope.launch { repository.save(entry.copy(status = "COMPLETED", updatedAt = currentTimeMillis())) } }) { Text("Complete") }
                                TextButton(onClick = { scope.launch { repository.save(entry.copy(status = "CANCELLED", updatedAt = currentTimeMillis())) } }) { Text("Cancel") }
                            }
                        }
                    }
                }
                item { Text("References", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
                items(references.filter { it.tripId == trip.id }, key = { "reference-${it.id}" }) { reference ->
                    Card(Modifier.fillMaxWidth().padding(start = 16.dp)) {
                        Column(Modifier.padding(12.dp)) {
                            Text("${if (reference.sensitive) "🔒 " else ""}${reference.title}", fontWeight = FontWeight.SemiBold)
                            Text(reference.content)
                            Row {
                                if (reference.address.isNotBlank()) TextButton(onClick = { platformActions.openMap(reference.address) }) { Text("Map") }
                                if (reference.phone.isNotBlank()) TextButton(onClick = { platformActions.call(reference.phone) }) { Text("Call") }
                                if (reference.website.isNotBlank()) TextButton(onClick = { platformActions.openWebsite(reference.website) }) { Text("Website") }
                                TextButton(onClick = { platformActions.copy(reference.title, reference.content) }) { Text("Copy") }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showStartPicker) {
        val picker = rememberDatePickerState(initialSelectedDateMillis = startDate)
        DatePickerDialog(onDismissRequest = { showStartPicker = false }, confirmButton = {
            TextButton(onClick = {
                startDate = picker.selectedDateMillis ?: startDate
                if (endDate < startDate) endDate = startDate
                showStartPicker = false
            }) { Text("Use date") }
        }, dismissButton = { TextButton(onClick = { showStartPicker = false }) { Text("Cancel") } }) { DatePicker(picker) }
    }
    if (showEndPicker) {
        val picker = rememberDatePickerState(initialSelectedDateMillis = endDate)
        DatePickerDialog(onDismissRequest = { showEndPicker = false }, confirmButton = {
            TextButton(onClick = {
                endDate = (picker.selectedDateMillis ?: endDate).coerceAtLeast(startDate)
                showEndPicker = false
            }) { Text("Use date") }
        }, dismissButton = { TextButton(onClick = { showEndPicker = false }) { Text("Cancel") } }) { DatePicker(picker) }
    }
}

private fun formatDurationPrecise(millis: Long): String = formatDuration(millis) + "." + (millis.coerceAtLeast(0) % 1000).toString().padStart(3, '0')

private fun nextAlarmAt(now: Long, hour: Int, minute: Int, weekdays: Set<Int>): Long {
    val day = dayKey(now)
    var candidate = day * DAY_MILLIS + hour * 3_600_000L + minute * 60_000L
    if (candidate <= now) candidate += DAY_MILLIS
    if (weekdays.isEmpty()) return candidate
    repeat(14) {
        val weekday = (((dayKey(candidate) + 4) % 7 + 7) % 7).toInt()
        if (weekday in weekdays) return candidate
        candidate += DAY_MILLIS
    }
    return candidate
}

private fun formatAlarmTime(epochMillis: Long): String {
    val minutes = ((epochMillis % DAY_MILLIS + DAY_MILLIS) % DAY_MILLIS) / 60_000L
    return formatDate(epochMillis) + " " + (minutes / 60).toString().padStart(2, '0') + ":" + (minutes % 60).toString().padStart(2, '0')
}

private fun activeAlarmColor(
    triggerAt: Long,
    now: Long,
): Color {
    val fractionRemaining = ((triggerAt - now).coerceAtLeast(0L).toFloat() / (24 * 60 * 60_000f)).coerceIn(0f, 1f)
    return if (fractionRemaining >= 0.5f) {
        val amount = (fractionRemaining - 0.5f) * 2f
        Color(
            red = 1f - 0.73f * amount,
            green = 0.76f - 0.13f * amount,
            blue = 0.03f + 0.26f * amount,
            alpha = 0.28f,
        )
    } else {
        val amount = fractionRemaining * 2f
        Color(red = 0.9f + 0.1f * amount, green = 0.22f + 0.54f * amount, blue = 0.21f - 0.18f * amount, alpha = 0.28f)
    }
}

@Composable
private fun DoNowScreen(
    tasks: List<Task>,
    plan: List<DailyPlanItem>,
    now: Long,
    onEdit: (Task) -> Unit,
    onComplete: (Task) -> Unit,
    onStart: (Task) -> Unit,
    onFeedback: (Long, RecommendationAction) -> Unit,
    onSavePlan: (DailyPlanItem) -> Unit,
    padding: PaddingValues,
) {
    var minutes by remember { mutableStateOf(15) }
    var energy by remember { mutableStateOf(EnergyRequirement.MEDIUM) }
    LaunchedEffect(Unit) { minutes = 15; energy = EnergyRequirement.MEDIUM }
    val rankedTasks = RecommendationService.rank(tasks, plan, RecommendationContext(now, availableMinutes = null, energy = energy))
    val eligibleMatches =
        RecommendationService
            .rank(tasks, plan, RecommendationContext(now, availableMinutes = minutes, energy = energy))
            .filter { it.task.energyRequirement.ordinal <= energy.ordinal }
    val matchingTasks = eligibleMatches.filter { it.task.estimatedMinutes != null } + eligibleMatches.filter { it.task.estimatedMinutes == null }
    val matchingTaskIds = matchingTasks.mapTo(mutableSetOf()) { it.task.id }
    val otherTasks = rankedTasks.filterNot { it.task.id in matchingTaskIds }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("What fits right now?", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Choose how much time you have and the effort you can comfortably give.")
                Text("$minutes minutes available", style = MaterialTheme.typography.labelLarge)
                Slider(
                    value = minutes.toFloat(),
                    onValueChange = { minutes = ((it / 5f).toInt() * 5).coerceIn(5, 120) },
                    valueRange = 5f..120f,
                    steps = 22,
                )
                EffortSlider(energy, { energy = it })
            }
        }
        fun androidx.compose.foundation.lazy.LazyListScope.taskSection(title: String, rows: List<com.geepscontrol.core.domain.Recommendation>, matches: Boolean) {
            item { Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp)) }
            if (rows.isEmpty()) item { Text(if (matches) "No tasks currently match both your time and energy." else "No other eligible tasks.", style = MaterialTheme.typography.bodySmall) }
            items(rows.take(10), key = { "$title-${it.task.id}" }) { recommendation ->
                val overdueColor = MaterialTheme.colorScheme.error
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Text(recommendation.task.title, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { onEdit(recommendation.task) })
                        if (!matches) {
                            val mismatches = buildList {
                                recommendation.task.estimatedMinutes?.takeIf { it > minutes }?.let { add("needs about $it minutes") }
                                if (recommendation.task.energyRequirement.ordinal > energy.ordinal) add("needs ${recommendation.task.energyRequirement.displayName().lowercase()} energy")
                            }
                            Text("Does not match current settings${mismatches.takeIf { it.isNotEmpty() }?.joinToString(prefix = ": ").orEmpty()}", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                        }
                        Text(buildAnnotatedString { append("Score ${recommendation.score}"); recommendation.reasons.forEach { reason -> append(" · "); if (reason.equals("overdue", true)) withStyle(SpanStyle(color = overdueColor, fontWeight = FontWeight.Bold)) { append("overdue!") } else append(reason) } }, style = MaterialTheme.typography.bodySmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Button(onClick = { onFeedback(recommendation.task.id, RecommendationAction.STARTED); onStart(recommendation.task) }) { Text("Start") }
                            OutlinedButton(onClick = { onComplete(recommendation.task) }) { Text("Done") }
                            TextButton(onClick = { onFeedback(recommendation.task.id, RecommendationAction.DEFERRED); onSavePlan((plan.firstOrNull { it.taskId == recommendation.task.id } ?: DailyPlanItem(recommendation.task.id, dayKey(now))).copy(deferredUntil = now + 60 * 60 * 1000L)) }) { Text("Later") }
                            TextButton(onClick = { onFeedback(recommendation.task.id, RecommendationAction.HIDDEN); onSavePlan((plan.firstOrNull { it.taskId == recommendation.task.id } ?: DailyPlanItem(recommendation.task.id, dayKey(now))).copy(hidden = true)) }) { Text("Hide") }
                        }
                    }
                }
            }
        }
        taskSection("Matching Tasks", matchingTasks, true)
        taskSection("Other Tasks", otherTasks, false)
    }
}

@Composable
private fun DestinationPlaceholder(
    destination: AppDestination,
    onNavigate: (AppDestination) -> Unit,
    padding: PaddingValues,
) {
    Box(Modifier.fillMaxSize().padding(padding).padding(20.dp), contentAlignment = Alignment.Center) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(destination.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("This destination is scheduled for a later milestone.")
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppDestination.entries.filterNot { it.primary }.forEach { item ->
                        Button(modifier = Modifier.fillMaxWidth(), onClick = { onNavigate(item) }) { Text(item.title) }
                    }
                }
            }
        }
    }
}

private fun Enum<*>.displayName(): String =
    if (this == TaskStatus.INBOX) {
        "Unassigned"
    } else {
        name.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }
    }

private fun dueLabel(dueAt: Long): String {
    val days = (dueAt - currentTimeMillis()) / DAY_MILLIS
    return when {
        days < 0 -> "overdue"
        days == 0L -> "due today"
        days == 1L -> "due tomorrow"
        else -> "due in $days days"
    }
}

private fun formatDate(epochMillis: Long): String {
    val z = epochMillis / DAY_MILLIS + 719_468
    val era = z / 146_097
    val dayOfEra = z - era * 146_097
    val yearOfEra = (dayOfEra - dayOfEra / 1_460 + dayOfEra / 36_524 - dayOfEra / 146_096) / 365
    var year = yearOfEra + era * 400
    val dayOfYear = dayOfEra - (365 * yearOfEra + yearOfEra / 4 - yearOfEra / 100)
    val monthPrime = (5 * dayOfYear + 2) / 153
    val day = dayOfYear - (153 * monthPrime + 2) / 5 + 1
    val month = monthPrime + if (monthPrime < 10) 3 else -9
    year += if (month <= 2) 1 else 0
    return "$year-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"
}

private const val DAY_MILLIS = 86_400_000L
