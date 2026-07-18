package com.geepscontrol.core.domain

enum class TimerMode { STOPWATCH, COUNTDOWN, POMODORO, FOCUS }

enum class TimerState { RUNNING, PAUSED, FINISHED, CANCELLED }

enum class SessionCompletion { COMPLETED, INTERRUPTED, ABANDONED }

data class ActiveTimer(
    val id: Long = 1,
    val taskId: Long? = null,
    val mode: TimerMode,
    val state: TimerState,
    val startedAt: Long,
    val resumedAt: Long = startedAt,
    val accumulatedMillis: Long = 0,
    val intendedDurationMillis: Long? = null,
    val updatedAt: Long = startedAt,
)

data class WorkSession(
    val id: Long = 0,
    val taskId: Long? = null,
    val projectId: Long? = null,
    val startedAt: Long,
    val endedAt: Long,
    val pausedMillis: Long,
    val activeMillis: Long,
    val plannedMillis: Long? = null,
    val mode: TimerMode,
    val completion: SessionCompletion = SessionCompletion.COMPLETED,
    val notes: String = "",
    val accomplishment: String = "",
    val interruptions: Int = 0,
    val focusRating: Int? = null,
)

data class AlarmDefinition(
    val id: Long = 0,
    val taskId: Long? = null,
    val projectId: Long? = null,
    val triggerAt: Long,
    val label: String,
    val vibration: Boolean = true,
    val sound: Boolean = true,
    val enabled: Boolean = true,
)

object TimerEngine {
    fun elapsed(
        timer: ActiveTimer,
        now: Long,
    ): Long = timer.accumulatedMillis + if (timer.state == TimerState.RUNNING) (now - timer.resumedAt).coerceAtLeast(0) else 0

    fun remaining(
        timer: ActiveTimer,
        now: Long,
    ): Long? = timer.intendedDurationMillis?.minus(elapsed(timer, now))

    fun pause(
        timer: ActiveTimer,
        now: Long,
    ) = timer.copy(state = TimerState.PAUSED, accumulatedMillis = elapsed(timer, now), updatedAt = now)

    fun resume(
        timer: ActiveTimer,
        now: Long,
    ) = timer.copy(state = TimerState.RUNNING, resumedAt = now, updatedAt = now)

    fun addTime(
        timer: ActiveTimer,
        millis: Long,
    ) = timer.copy(intendedDurationMillis = (timer.intendedDurationMillis ?: 0) + millis)
}

interface AlarmScheduler {
    fun schedule(alarm: AlarmDefinition)

    fun cancel(alarmId: Long)

    fun scheduleTimer(timer: ActiveTimer)

    fun cancelTimer(timerId: Long? = null)

    fun scheduleReminder(
        id: Long,
        triggerAt: Long,
        label: String,
        category: NotificationCategory,
    )

    fun cancelReminder(id: Long)
}

enum class NotificationCategory { TASK_REMINDER, DUE_STATUS, DAILY_PLANNING, EVENING_REVIEW, WEEKLY_REVIEW }
