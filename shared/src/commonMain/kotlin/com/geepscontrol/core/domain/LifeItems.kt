package com.geepscontrol.core.domain

import com.geepscontrol.core.database.LifeEventEntity
import com.geepscontrol.core.database.MedicationOccurrenceEntity
import com.geepscontrol.core.database.ReminderEntity
import com.geepscontrol.core.database.RoutineOccurrenceEntity
import com.geepscontrol.core.database.StandaloneAlarmEntity
import com.geepscontrol.core.database.StandaloneTimerEntity

enum class TimelineItemType { TASK, ROUTINE, MEDICATION, REMINDER, EVENT, ALARM, TIMER, ITINERARY }

enum class TimelineAction { COMPLETE, CONFIRM, SKIP, SNOOZE, DISMISS, CANCEL }

data class TimelineItem(
    val id: Long,
    val type: TimelineItemType,
    val title: String,
    val startAt: Long?,
    val endAt: Long? = null,
    val allDay: Boolean = false,
    val actionable: Boolean,
    val status: String,
    val subtitle: String = "",
    val sourceId: Long = id,
    val timezone: String? = null,
)

object LifeScheduleEngine {
    const val DAY_MILLIS = 86_400_000L

    fun nextOccurrences(
        start: Long,
        endExclusive: Long,
        intervalMillis: Long,
    ): List<Long> {
        require(intervalMillis > 0)
        val values = mutableListOf<Long>()
        var at = start
        while (at < endExclusive) {
            values += at
            at += intervalMillis
        }
        return values
    }

    fun medicationTimes(
        dayStart: Long,
        minutes: List<Int>,
    ) = minutes.distinct().sorted().map { dayStart + it * 60_000L }

    fun refillAfterDose(
        remaining: Double?,
        used: Double,
    ): Double? = remaining?.let { (it - used).coerceAtLeast(0.0) }

    fun timerElapsed(
        timer: StandaloneTimerEntity,
        now: Long,
    ): Long = timer.accumulatedMillis + if (timer.state == "RUNNING") (now - timer.resumedAt).coerceAtLeast(0) else 0

    fun timerRemaining(
        timer: StandaloneTimerEntity,
        now: Long,
    ): Long = timer.durationMillis - timerElapsed(timer, now)

    fun reminderSnooze(
        now: Long,
        minutes: Int,
    ) = now + minutes.coerceAtLeast(1) * 60_000L
}

internal fun ReminderEntity.timeline() = TimelineItem(id, TimelineItemType.REMINDER, title, scheduledAt, allDay = allDay, actionable = true, status = status, subtitle = description)

internal fun RoutineOccurrenceEntity.timeline(title: String) = TimelineItem(id, TimelineItemType.ROUTINE, title, scheduledAt, actionable = true, status = status, sourceId = routineId)

internal fun MedicationOccurrenceEntity.timeline(title: String) = TimelineItem(id, TimelineItemType.MEDICATION, title, snoozedUntil ?: scheduledAt, actionable = true, status = status, subtitle = dosageTakenText, sourceId = medicationId)

internal fun LifeEventEntity.timeline() = TimelineItem(id, TimelineItemType.EVENT, title, startAt, endAt, allDay, false, status, locationName, timezone = timezone)

internal fun StandaloneAlarmEntity.timeline() = TimelineItem(id, TimelineItemType.ALARM, label, scheduledAt, actionable = true, status = if (enabled) "SCHEDULED" else "DISABLED")

internal fun StandaloneTimerEntity.timeline(now: Long) = TimelineItem(id, TimelineItemType.TIMER, name.ifBlank { "Timer" }, startedAt + durationMillis, actionable = false, status = state, subtitle = "${LifeScheduleEngine.timerRemaining(this, now) / 1000}s remaining")

internal fun com.geepscontrol.core.database.ItineraryEntryEntity.timeline(tripName: String) = TimelineItem(id, TimelineItemType.ITINERARY, title, startAt, endAt, allDay, false, status, listOf(tripName, type, departureLocation.ifBlank { address }).filter { it.isNotBlank() }.joinToString(" · "), sourceId = tripId, timezone = timezone)
