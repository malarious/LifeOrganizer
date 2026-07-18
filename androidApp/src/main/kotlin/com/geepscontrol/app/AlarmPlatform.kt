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
import com.geepscontrol.core.domain.AlarmRepeatEngine
import com.geepscontrol.core.domain.AlarmRepeatRule
import com.geepscontrol.core.domain.AlarmScheduler
import com.geepscontrol.core.domain.NotificationCategory
import com.geepscontrol.core.domain.NotificationPolicy
import com.geepscontrol.core.repository.LifeItemRepository
import com.geepscontrol.core.repository.RoomTimerRepository
import com.geepscontrol.core.settings.AndroidAppSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

class AndroidAlarmScheduler(
    private val context: Context,
) : AlarmScheduler {
    private val manager = context.getSystemService(AlarmManager::class.java)

    override fun schedule(alarm: AlarmDefinition) {
        val operation = pending(context, AlarmReceiver.ACTION_FIRE, alarm.id)
        if (Build.VERSION.SDK_INT < 31 || manager.canScheduleExactAlarms()) {
            manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarm.triggerAt, operation)
        } else {
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarm.triggerAt, operation)
        }
    }

    override fun cancel(alarmId: Long) {
        manager.cancel(pending(context, AlarmReceiver.ACTION_FIRE, alarmId))
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
            try {
                val id = intent.getLongExtra(EXTRA_ID, 0)
                val repository = RoomTimerRepository(buildDatabase(context).timerDao())
                val lifeRepository = LifeItemRepository(buildDatabase(context).lifeItemDao())
                when (intent.action) {
                    ACTION_FIRE -> {
                        val standalone = lifeRepository.getAlarm(id - STANDALONE_ALARM_OFFSET)?.takeIf { it.enabled }
                        val profile = standalone?.profileId?.let { lifeRepository.getAlarmProfile(it) }
                        val alarm = repository.getAlarm(id)?.takeIf { it.enabled } ?: standalone?.let { AlarmDefinition(id = id, triggerAt = it.scheduledAt, label = it.label, vibration = it.vibration, sound = it.sound) }
                        alarm?.let { showAlarmScreen(context, it.id, it.label, sound = profile?.soundEnabled ?: it.sound, vibration = profile?.vibrationEnabled ?: it.vibration, soundId = profile?.soundId, profileName = profile?.name, loopSound = profile?.loopSound ?: true, vibrationMode = profile?.vibrationMode ?: "STANDARD", vibrationDelaySeconds = profile?.vibrationStartDelaySeconds ?: 0, startingVolumePercent = profile?.startingVolumePercent ?: 100, maximumVolumePercent = profile?.maximumVolumePercent ?: 100, volumeRampSeconds = profile?.volumeRampSeconds ?: 0, dismissalMethod = profile?.dismissalMethod ?: "STANDARD", dismissalDifficulty = profile?.dismissalDifficulty ?: "NORMAL", snoozeAvailable = profile?.let { com.geepscontrol.core.domain.AlarmExperience.canSnooze(standalone?.snoozeCount ?: 0, it.maximumSnoozes) } ?: true) }
                        standalone?.takeIf { it.recurrenceRule != null }?.let { value ->
                            val next = AlarmRepeatRule.decode(value.recurrenceRule)?.let { AlarmRepeatEngine.nextFuture(it, value.scheduledAt, System.currentTimeMillis()) } ?: value.scheduledAt + 86_400_000L
                            lifeRepository.save(value.copy(scheduledAt = next, lastTriggeredAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis()))
                            AndroidAlarmScheduler(context).schedule(AlarmDefinition(id = id, triggerAt = next, label = value.label, vibration = value.vibration, sound = value.sound))
                        }
                    }
                    ACTION_SNOOZE ->
                        (
                            repository.getAlarm(id)?.let { alarm ->
                                val snoozed = alarm.copy(triggerAt = System.currentTimeMillis() + 10 * 60_000L, enabled = true)
                                repository.saveAlarm(snoozed)
                                AndroidAlarmScheduler(context).schedule(snoozed)
                                NotificationManagerCompat.from(context).cancel(id.toInt())
                                true
                            } ?: false
                        ).also { handled ->
                            if (!handled) {
                                lifeRepository.getAlarm(id - STANDALONE_ALARM_OFFSET)?.let { alarm ->
                                    val now = System.currentTimeMillis()
                                    val profile = alarm.profileId?.let { lifeRepository.getAlarmProfile(it) }
                                    if (profile == null || com.geepscontrol.core.domain.AlarmExperience.canSnooze(alarm.snoozeCount, profile.maximumSnoozes)) {
                                        val minutes = profile?.snoozeDurationMinutes ?: alarm.snoozeMinutes
                                        lifeRepository.snoozeAlarm(alarm.id, now + minutes * 60_000L, now)
                                        AndroidAlarmScheduler(context).schedule(AlarmDefinition(id = id, triggerAt = now + minutes * 60_000L, label = alarm.label, vibration = profile?.vibrationEnabled ?: alarm.vibration, sound = profile?.soundEnabled ?: alarm.sound))
                                        NotificationManagerCompat.from(context).cancel(id.toInt())
                                    }
                                }
                            }
                        }
                    ACTION_DISMISS -> {
                        if (id >= STANDALONE_ALARM_OFFSET) {
                            val alarm = lifeRepository.getAlarm(id - STANDALONE_ALARM_OFFSET)
                            if (alarm?.recurrenceRule == null) {
                                lifeRepository.setAlarmEnabled(id - STANDALONE_ALARM_OFFSET, false, System.currentTimeMillis())
                                AndroidAlarmScheduler(context).cancel(id)
                            } else {
                                val now = System.currentTimeMillis()
                                val rule = AlarmRepeatRule.decode(alarm.recurrenceRule)
                                // ACTION_FIRE normally advances the record first. Keeping an already-future
                                // date makes dismiss safe even when the two broadcasts finish out of order.
                                val next = rule?.let { AlarmRepeatEngine.nextFuture(it, alarm.scheduledAt, now) } ?: alarm.scheduledAt
                                lifeRepository.save(alarm.copy(scheduledAt = next, snoozeCount = 0, lastDismissedAt = now, updatedAt = now))
                                AndroidAlarmScheduler(context).cancel(id)
                                AndroidAlarmScheduler(context).schedule(AlarmDefinition(id = id, triggerAt = next, label = alarm.label, vibration = alarm.vibration, sound = alarm.sound))
                            }
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
                    Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED ->
                        AndroidAlarmScheduler(context).let { scheduler ->
                            repository.observeAlarms().first().forEach(scheduler::schedule)
                            repository.observeActive().first()?.let(scheduler::scheduleTimer)
                            lifeRepository
                                .alarms()
                                .first()
                                .filter { it.enabled && it.scheduledAt > System.currentTimeMillis() }
                                .forEach { scheduler.schedule(AlarmDefinition(id = STANDALONE_ALARM_OFFSET + it.id, triggerAt = it.scheduledAt, label = it.label, vibration = it.vibration, sound = it.sound)) }
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
        soundId: String? = if (isTimer) "timer_hit" else null,
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
        context.startActivity(
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
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        )
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
) = PendingIntent.getBroadcast(context, (id % Int.MAX_VALUE).toInt(), Intent(context, AlarmReceiver::class.java).setAction(action).putExtra(AlarmReceiver.EXTRA_ID, id), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

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
