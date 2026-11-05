package com.geepscontrol.app

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.geepscontrol.core.database.buildDatabase
import com.geepscontrol.core.domain.AlarmDefinition
import com.geepscontrol.core.domain.AlarmOccurrenceAction
import com.geepscontrol.core.domain.AlarmOccurrenceCoordinator
import com.geepscontrol.core.domain.AlarmScheduler
import com.geepscontrol.core.domain.NotificationCategory
import com.geepscontrol.core.domain.NotificationPolicy
import com.geepscontrol.core.repository.LifeItemRepository
import com.geepscontrol.core.repository.RoomTimerRepository
import com.geepscontrol.core.settings.AndroidAppSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

class AndroidAlarmScheduler(
    private val context: Context,
) : AlarmScheduler {
    private val manager = context.getSystemService(AlarmManager::class.java)

    override fun schedule(alarm: AlarmDefinition) {
        if (!alarm.enabled) { cancel(alarm.id); return }
        val operation = pending(context, AlarmReceiver.ACTION_FIRE, alarm.id, alarm.triggerAt)
        if (Build.VERSION.SDK_INT < 31 || manager.canScheduleExactAlarms()) {
            val showAlarm = PendingIntent.getActivity(
                context,
                (alarm.id % Int.MAX_VALUE).toInt(),
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            manager.setAlarmClock(AlarmManager.AlarmClockInfo(alarm.triggerAt, showAlarm), operation)
        } else {
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarm.triggerAt, operation)
        }
        UpcomingAlarmNotifications.schedule(context, alarm)
    }

    override fun cancel(alarmId: Long) {
        manager.cancel(pending(context, AlarmReceiver.ACTION_FIRE, alarmId))
        UpcomingAlarmNotifications.cancel(context, alarmId)
        context.sendBroadcast(Intent(ACTION_STOP_ALERT).setPackage(context.packageName).putExtra(AlarmReceiver.EXTRA_ID, alarmId))
        NotificationManagerCompat.from(context).cancel(alarmId.toInt())
    }

    override fun scheduleTimer(timer: com.geepscontrol.core.domain.ActiveTimer) {
        val remaining =
            com.geepscontrol.core.domain.TimerEngine
                .remaining(timer, System.currentTimeMillis()) ?: return
        if (timer.state != com.geepscontrol.core.domain.TimerState.RUNNING) return
        val operation = pending(context, AlarmReceiver.ACTION_TIMER_COMPLETE, timer.id)
        val trigger = System.currentTimeMillis() + remaining.coerceAtLeast(0)
        if (Build.VERSION.SDK_INT < 31 || manager.canScheduleExactAlarms()) {
            manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, operation)
        } else {
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, operation)
        }
    }

    override fun cancelTimer(timerId: Long?) {
        val id = timerId ?: TIMER_ID
        manager.cancel(pending(context, AlarmReceiver.ACTION_TIMER_COMPLETE, id))
        NotificationManagerCompat.from(context).cancel((TIMER_NOTIFICATION_ID + id % 10_000).toInt())
        NotificationManagerCompat.from(context).cancel(id.toInt())
    }

    override fun scheduleReminder(
        id: Long,
        triggerAt: Long,
        label: String,
        category: NotificationCategory,
    ) {
        val operation = reminderPending(context, id, label, category)
        if (Build.VERSION.SDK_INT < 31 || manager.canScheduleExactAlarms()) {
            manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, operation)
        } else {
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, operation)
        }
    }

    override fun cancelReminder(id: Long) {
        manager.cancel(reminderPending(context, id, "", NotificationCategory.TASK_REMINDER))
        NotificationManagerCompat.from(context).cancel(reminderNotificationId(id))
    }

    companion object {
        const val ACTION_STOP_ALERT = "com.geepscontrol.STOP_ALERT"
        const val TIMER_ID = -9_001L
        const val TIMER_NOTIFICATION_ID = 90_001
    }
}

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            val database = buildDatabase(context)
            try {
                val id = intent.getLongExtra(EXTRA_ID, 0)
                val repository = RoomTimerRepository(database.timerDao())
                val lifeRepository = LifeItemRepository(database.lifeItemDao())
                val coordinator = AlarmOccurrenceCoordinator(lifeRepository, AndroidAlarmScheduler(context), AndroidPlatformActions(context))
                val expectedAt = intent.takeIf { it.hasExtra(UpcomingAlarmNotifications.EXTRA_SCHEDULED_AT) }?.getLongExtra(UpcomingAlarmNotifications.EXTRA_SCHEDULED_AT, 0)
                when (intent.action) {
                    UpcomingAlarmNotifications.ACTION_SHOW -> {
                        lifeRepository.getAlarm(id - STANDALONE_ALARM_OFFSET)?.let { UpcomingAlarmNotifications.show(context, it, expectedAt ?: it.scheduledAt) }
                    }
                    UpcomingAlarmNotifications.ACTION_SKIP, UpcomingAlarmNotifications.ACTION_DELAY -> {
                        coordinator.perform(id - STANDALONE_ALARM_OFFSET, if (intent.action == UpcomingAlarmNotifications.ACTION_SKIP) AlarmOccurrenceAction.SKIP else AlarmOccurrenceAction.DELAY, System.currentTimeMillis(), expectedAt)
                    }
                    ACTION_FIRE -> {
                        val standalone = if (id in STANDALONE_ALARM_OFFSET until 4_000_000L) {
                            coordinator.perform(id - STANDALONE_ALARM_OFFSET, AlarmOccurrenceAction.FIRE, System.currentTimeMillis(), expectedAt)
                                ?: return@launch
                        } else null
                        if (standalone?.recurrenceRule == null) UpcomingAlarmNotifications.cancel(context, id)
                        val profile = standalone?.profileId?.let { lifeRepository.getAlarmProfile(it) }
                        val alarm = repository.getAlarm(id)?.takeIf { it.enabled } ?: standalone?.let { AlarmDefinition(id = id, triggerAt = it.scheduledAt, label = it.label, vibration = it.vibration, sound = it.sound) }
                        alarm?.let { showAlarmScreen(context, it.id, it.label, sound = it.sound, vibration = it.vibration, soundId = standalone?.soundId ?: profile?.soundId, profileName = profile?.name, loopSound = standalone?.loopSound ?: profile?.loopSound ?: true, vibrationMode = standalone?.vibrationMode ?: profile?.vibrationMode ?: "STANDARD", vibrationDelaySeconds = standalone?.vibrationStartDelaySeconds ?: profile?.vibrationStartDelaySeconds ?: 0, startingVolumePercent = standalone?.startingVolumePercent ?: profile?.startingVolumePercent ?: 100, maximumVolumePercent = standalone?.maximumVolumePercent ?: profile?.maximumVolumePercent ?: 100, volumeRampSeconds = standalone?.volumeRampSeconds ?: profile?.volumeRampSeconds ?: 0, dismissalMethod = standalone?.dismissalMethod ?: profile?.dismissalMethod ?: "STANDARD", dismissalDifficulty = standalone?.dismissalDifficulty ?: profile?.dismissalDifficulty ?: "NORMAL", snoozeAvailable = com.geepscontrol.core.domain.AlarmExperience.canSnooze(standalone?.snoozeCount ?: 0, standalone?.maximumSnoozes ?: profile?.maximumSnoozes ?: 0)) }
                    }
                    ACTION_SNOOZE -> {
                        if (id in STANDALONE_ALARM_OFFSET until 4_000_000L) {
                            coordinator.perform(id - STANDALONE_ALARM_OFFSET, AlarmOccurrenceAction.SNOOZE, System.currentTimeMillis())
                        } else repository.getAlarm(id)?.let { alarm ->
                            val snoozed = alarm.copy(triggerAt = System.currentTimeMillis() + 10 * 60_000L, enabled = true)
                            repository.saveAlarm(snoozed)
                            AndroidAlarmScheduler(context).cancel(id)
                            AndroidAlarmScheduler(context).schedule(snoozed)
                        }
                    }
                    ACTION_DISMISS -> {
                        if (id in STANDALONE_ALARM_OFFSET until 4_000_000L) {
                            coordinator.perform(id - STANDALONE_ALARM_OFFSET, AlarmOccurrenceAction.ACKNOWLEDGE, System.currentTimeMillis())
                        } else {
                            repository.disableAlarm(id)
                            AndroidAlarmScheduler(context).cancel(id)
                        }
                    }
                    ACTION_TIMER_COMPLETE -> {
                        val standaloneId = id - 4_000_000L
                        val standalone = standaloneId.takeIf { it > 0 }?.let { lifeRepository.getTimer(it) }
                        standalone?.let {
                            val endedAt = System.currentTimeMillis()
                            lifeRepository.addHistory(com.geepscontrol.core.database.TimerHistoryEntity(presetId = it.presetId, name = it.name, startedAt = it.startedAt, endedAt = endedAt, originalDurationMillis = it.durationMillis, finalDurationMillis = (endedAt - it.startedAt).coerceAtLeast(0), result = "COMPLETED"))
                            lifeRepository.removeTimer(it.id)
                        }
                        showAlarmScreen(context, id, standalone?.name?.takeIf { it.isNotBlank() } ?: "Timer complete", isTimer = true, sound = standalone?.sound ?: true, vibration = standalone?.vibration ?: true)
                    }
                    ACTION_TIMER_DISMISS -> {
                        if (id == AndroidAlarmScheduler.TIMER_ID) repository.clearActive()
                        AndroidAlarmScheduler(context).cancelTimer(id)
                    }
                    ACTION_REMINDER -> {
                        showReminder(context, id, intent.getStringExtra(EXTRA_LABEL).orEmpty(), intent.getStringExtra(EXTRA_CATEGORY))
                        if (id in REMINDER_OFFSET until MEDICATION_OFFSET) {
                            lifeRepository.getReminder(id - REMINDER_OFFSET)?.takeIf { it.recurrenceRule != null }?.let { value ->
                                val next = value.scheduledAt + 86_400_000L
                                lifeRepository.save(value.copy(scheduledAt = next, status = "SCHEDULED", updatedAt = System.currentTimeMillis()))
                                AndroidAlarmScheduler(context).scheduleReminder(id, next, value.title, NotificationCategory.TASK_REMINDER)
                            }
                        }
                        if (id in MEDICATION_OFFSET until STANDALONE_ALARM_OFFSET) {
                            lifeRepository.getMedication(id - MEDICATION_OFFSET)?.takeIf { it.active && it.recurrenceRule != null }?.let { value ->
                                val next = System.currentTimeMillis() + 86_400_000L
                                lifeRepository.save(
                                    com.geepscontrol.core.database
                                        .MedicationOccurrenceEntity(medicationId = value.id, scheduledAt = next, createdAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis()),
                                )
                                AndroidAlarmScheduler(context).scheduleReminder(id, next, value.name, NotificationCategory.TASK_REMINDER)
                            }
                        }
                        if (id < 0) scheduleRoutineReminders(context, AndroidAlarmScheduler(context))
                    }
                    Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED, Intent.ACTION_TIMEZONE_CHANGED, Intent.ACTION_TIME_CHANGED, AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED ->
                        AndroidAlarmScheduler(context).let { scheduler ->
                            repository.observeAlarms().first().forEach(scheduler::schedule)
                            repository.observeActive().first()?.let(scheduler::scheduleTimer)
                            lifeRepository.alarms().first().filter { it.enabled }.forEach { alarm ->
                                val snoozed = com.geepscontrol.core.domain.AlarmOccurrenceEngine.isSnoozed(alarm)
                                if (alarm.recurrenceRule == null && alarm.lastTriggeredAt != null && !snoozed) return@forEach
                                val hasOverride = snoozed || alarm.nextRegularAt != null
                                val clockChanged = intent.action in setOf(Intent.ACTION_TIMEZONE_CHANGED, Intent.ACTION_TIME_CHANGED)
                                val adjusted = if (clockChanged && !hasOverride) localWallClockTrigger(alarm) else alarm.scheduledAt
                                val trigger = adjusted.coerceAtLeast(System.currentTimeMillis() + 500L)
                                val regular = alarm.nextRegularAt?.let { if (clockChanged) localWallClockTrigger(alarm.copy(scheduledAt = it)) else it }
                                if (trigger != alarm.scheduledAt || regular != alarm.nextRegularAt) lifeRepository.save(alarm.copy(scheduledAt = trigger, nextRegularAt = regular, updatedAt = System.currentTimeMillis()))
                                scheduler.schedule(AlarmDefinition(id = STANDALONE_ALARM_OFFSET + alarm.id, triggerAt = trigger, label = alarm.label, vibration = alarm.vibration, sound = alarm.sound))
                            }
                            lifeRepository
                                .reminders()
                                .first()
                                .filter { it.status in setOf("SCHEDULED", "SNOOZED") && it.scheduledAt > System.currentTimeMillis() }
                                .forEach { scheduler.scheduleReminder(1_000_000 + it.id, it.scheduledAt, it.title, NotificationCategory.TASK_REMINDER) }
                            lifeRepository
                                .timers()
                                .first()
                                .filter { it.state == "RUNNING" }
                                .forEach { timer ->
                                    scheduler.scheduleTimer(
                                        com.geepscontrol.core.domain
                                            .ActiveTimer(id = 4_000_000 + timer.id, mode = com.geepscontrol.core.domain.TimerMode.COUNTDOWN, state = com.geepscontrol.core.domain.TimerState.RUNNING, startedAt = timer.startedAt, resumedAt = timer.resumedAt, accumulatedMillis = timer.accumulatedMillis, intendedDurationMillis = timer.durationMillis, updatedAt = timer.updatedAt),
                                    )
                                }
                            scheduleRoutineReminders(context, scheduler)
                        }
                }
            } finally {
                database.close()
                pendingResult.finish()
            }
        }
    }

    private fun showAlarmScreen(
        context: Context,
        id: Long,
        label: String,
        isTimer: Boolean = false,
        sound: Boolean = true,
        vibration: Boolean = true,
        soundId: String? = if (isTimer) "happy" else null,
        profileName: String? = null,
        loopSound: Boolean = !isTimer,
        vibrationMode: String = "STANDARD",
        vibrationDelaySeconds: Int = 0,
        startingVolumePercent: Int = 100,
        maximumVolumePercent: Int = 100,
        volumeRampSeconds: Int = 0,
        dismissalMethod: String = "STANDARD",
        dismissalDifficulty: String = "NORMAL",
        snoozeAvailable: Boolean = true,
    ) {
        val alarmIntent =
            Intent(context, AlarmActivity::class.java)
                .putExtra(EXTRA_ID, id)
                .putExtra(EXTRA_LABEL, label)
                .putExtra(EXTRA_IS_TIMER, isTimer)
                .putExtra(EXTRA_SOUND, sound)
                .putExtra(EXTRA_VIBRATION, vibration)
                .putExtra(EXTRA_SOUND_ID, soundId)
                .putExtra(EXTRA_PROFILE_NAME, profileName)
                .putExtra(EXTRA_LOOP_SOUND, loopSound)
                .putExtra(EXTRA_VIBRATION_MODE, vibrationMode)
                .putExtra(EXTRA_VIBRATION_DELAY, vibrationDelaySeconds)
                .putExtra(EXTRA_START_VOLUME, startingVolumePercent)
                .putExtra(EXTRA_MAX_VOLUME, maximumVolumePercent)
                .putExtra(EXTRA_RAMP_SECONDS, volumeRampSeconds)
                .putExtra(EXTRA_DISMISSAL_METHOD, dismissalMethod)
                .putExtra(EXTRA_DISMISSAL_DIFFICULTY, dismissalDifficulty)
                .putExtra(EXTRA_SNOOZE_AVAILABLE, snoozeAvailable)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        createChannels(context)
        val screen = PendingIntent.getActivity(
            context,
            (id % Int.MAX_VALUE).toInt(),
            alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val dismissAction = if (isTimer) ACTION_TIMER_DISMISS else ACTION_DISMISS
        val acknowledge = if (isTimer) pending(context, dismissAction, id) else PendingIntent.getActivity(
            context, (id % Int.MAX_VALUE).toInt(),
            Intent(alarmIntent).setAction("com.geepscontrol.ACKNOWLEDGE_SCREEN").putExtra(AlarmActivity.EXTRA_ACKNOWLEDGE_ON_OPEN, true),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification =
            NotificationCompat.Builder(context, if (isTimer) CHANNEL_TIMERS else CHANNEL_ALARMS)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle(if (isTimer) "Timer complete" else "Life Minder alarm")
                .setContentText(label)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOngoing(true)
                .setAutoCancel(false)
                .setContentIntent(screen)
                .setFullScreenIntent(screen, true)
                .apply {
                    if (!isTimer && snoozeAvailable) addAction(0, "Snooze", pending(context, ACTION_SNOOZE, id))
                    addAction(0, "Acknowledge", acknowledge)
                }
                .build()
        if (Build.VERSION.SDK_INT < 33 || context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat.from(context).notify(id.toInt(), notification)
        } else {
            // Full-screen alarm notifications require notification permission on Android 13+.
            // Preserve the direct launch as a best-effort fallback when that permission was denied.
            runCatching { context.startActivity(alarmIntent) }
        }
    }

    private fun showReminder(
        context: Context,
        id: Long,
        label: String,
        categoryName: String?,
    ) {
        val category = runCatching { NotificationCategory.valueOf(categoryName.orEmpty()) }.getOrDefault(NotificationCategory.TASK_REMINDER)
        val settings = AndroidAppSettings(context)
        val enabled =
            when (category) {
                NotificationCategory.TASK_REMINDER -> settings.taskRemindersEnabled
                NotificationCategory.DUE_STATUS -> settings.dueNotificationsEnabled
                NotificationCategory.DAILY_PLANNING -> settings.planningNotificationsEnabled
                NotificationCategory.EVENING_REVIEW, NotificationCategory.WEEKLY_REVIEW -> settings.reviewNotificationsEnabled
            }
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val quiet = NotificationPolicy.isQuietHour(hour, settings.quietHoursEnabled, settings.quietStartHour, settings.quietEndHour)
        if (!enabled || quiet) return
        createChannels(context)
        val open = PendingIntent.getActivity(context, reminderNotificationId(id), Intent(context, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification =
            NotificationCompat
                .Builder(context, CHANNEL_REMINDERS)
                .setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setContentTitle("Life Minder reminder")
                .setContentText(label)
                .setContentIntent(open)
                .setAutoCancel(true)
                .build()
        if (Build.VERSION.SDK_INT < 33 || context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) NotificationManagerCompat.from(context).notify(reminderNotificationId(id), notification)
    }

    companion object {
        const val EXTRA_ID = "alarm_id"
        const val EXTRA_LABEL = "label"
        const val EXTRA_CATEGORY = "category"
        const val EXTRA_IS_TIMER = "is_timer"
        const val EXTRA_SOUND = "sound"
        const val EXTRA_VIBRATION = "vibration"
        const val EXTRA_SOUND_ID = "sound_id"
        const val EXTRA_PROFILE_NAME = "profile_name"
        const val EXTRA_LOOP_SOUND = "loop_sound"
        const val EXTRA_VIBRATION_MODE = "vibration_mode"
        const val EXTRA_VIBRATION_DELAY = "vibration_delay"
        const val EXTRA_START_VOLUME = "start_volume"
        const val EXTRA_MAX_VOLUME = "max_volume"
        const val EXTRA_RAMP_SECONDS = "ramp_seconds"
        const val EXTRA_DISMISSAL_METHOD = "dismissal_method"
        const val EXTRA_DISMISSAL_DIFFICULTY = "dismissal_difficulty"
        const val EXTRA_SNOOZE_AVAILABLE = "snooze_available"
        const val ACTION_FIRE = "com.geepscontrol.ALARM_FIRE"
        const val ACTION_SNOOZE = "com.geepscontrol.ALARM_SNOOZE"
        const val ACTION_DISMISS = "com.geepscontrol.ALARM_DISMISS"
        const val ACTION_TIMER_COMPLETE = "com.geepscontrol.TIMER_COMPLETE"
        const val ACTION_TIMER_DISMISS = "com.geepscontrol.TIMER_DISMISS"
        const val ACTION_REMINDER = "com.geepscontrol.REMINDER"
        const val CHANNEL_ALARMS = "alarms"
        const val CHANNEL_TIMERS = "timer_completion_audible_v2"
        const val CHANNEL_REMINDERS = "reminders"
        const val STANDALONE_ALARM_OFFSET = 3_000_000L
        const val REMINDER_OFFSET = 1_000_000L
        const val MEDICATION_OFFSET = 2_000_000L
    }
}

private fun pending(
    context: Context,
    action: String,
    id: Long,
    scheduledAt: Long? = null,
) = PendingIntent.getBroadcast(context, (id % Int.MAX_VALUE).toInt(), Intent(context, AlarmReceiver::class.java).setAction(action).putExtra(AlarmReceiver.EXTRA_ID, id).apply { scheduledAt?.let { putExtra(UpcomingAlarmNotifications.EXTRA_SCHEDULED_AT, it) } }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

private fun reminderPending(
    context: Context,
    id: Long,
    label: String,
    category: NotificationCategory,
) = PendingIntent.getBroadcast(
    context,
    reminderNotificationId(id),
    Intent(context, AlarmReceiver::class.java)
        .setAction(AlarmReceiver.ACTION_REMINDER)
        .putExtra(AlarmReceiver.EXTRA_ID, id)
        .putExtra(AlarmReceiver.EXTRA_LABEL, label)
        .putExtra(AlarmReceiver.EXTRA_CATEGORY, category.name),
    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
)

private fun reminderNotificationId(id: Long) = 100_000 + (id % 100_000).toInt()

fun createChannels(context: Context) {
    val manager = context.getSystemService(NotificationManager::class.java)
    val channel =
        NotificationChannel(AlarmReceiver.CHANNEL_ALARMS, "Alarms", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Alarms that require dismissal"
            enableVibration(true)
            setSound(android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI, AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build())
        }
    manager.createNotificationChannel(channel)
    manager.createNotificationChannel(
        NotificationChannel(AlarmReceiver.CHANNEL_TIMERS, "Timer completion", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Audible countdown timer completion"
            enableVibration(true)
            setSound(android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI, AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build())
        },
    )
    manager.createNotificationChannel(NotificationChannel(AlarmReceiver.CHANNEL_REMINDERS, "Reminders", NotificationManager.IMPORTANCE_DEFAULT).apply { description = "Task, planning, and review reminders" })
}

fun scheduleRoutineReminders(
    context: Context,
    scheduler: AndroidAlarmScheduler,
) {
    val settings = AndroidAppSettings(context)

    fun next(
        hour: Int,
        dayOfWeek: Int? = null,
    ): Long {
        val calendar =
            Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                dayOfWeek?.let { set(Calendar.DAY_OF_WEEK, it) }
                if (timeInMillis <= System.currentTimeMillis()) add(if (dayOfWeek == null) Calendar.DAY_OF_YEAR else Calendar.WEEK_OF_YEAR, 1)
            }
        return calendar.timeInMillis
    }
    if (settings.planningNotificationsEnabled) scheduler.scheduleReminder(-101, next(8), "Plan your day", NotificationCategory.DAILY_PLANNING)
    if (settings.reviewNotificationsEnabled) {
        scheduler.scheduleReminder(-102, next(20), "Review your day", NotificationCategory.EVENING_REVIEW)
        scheduler.scheduleReminder(-103, next(18, Calendar.SUNDAY), "Complete your weekly review", NotificationCategory.WEEKLY_REVIEW)
    }
}

private fun localWallClockTrigger(alarm: com.geepscontrol.core.database.StandaloneAlarmEntity): Long {
    val zone = java.time.ZoneId.systemDefault()
    val intendedDate = alarm.localEpochDay?.let(java.time.LocalDate::ofEpochDay)
        ?: java.time.Instant.ofEpochMilli(alarm.scheduledAt).atZone(zone).toLocalDate()
    val time = java.time.LocalTime.of(alarm.localHour ?: java.time.Instant.ofEpochMilli(alarm.scheduledAt).atZone(zone).hour, alarm.localMinute ?: java.time.Instant.ofEpochMilli(alarm.scheduledAt).atZone(zone).minute)
    return java.time.ZonedDateTime.of(intendedDate, time, zone).toInstant().toEpochMilli()
}
