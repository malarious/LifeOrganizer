package com.geepscontrol.core.domain

import com.geepscontrol.core.repository.LifeItemRepository
import com.geepscontrol.core.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class UnifiedTimelineService(
    private val tasks: TaskRepository,
    private val life: LifeItemRepository,
) {
    fun observeItems(now: Long): Flow<List<TimelineItem>> =
        combine(
            tasks.observeTasks(),
            life.reminders(),
            life.routines(),
            life.routineOccurrences(),
            life.medications(),
            life.medicationOccurrences(),
            life.events(),
            life.alarms(),
            life.timers(),
            life.trips(),
            life.itineraryEntries(),
        ) { values ->
            @Suppress("UNCHECKED_CAST")
            val taskRows = values[0] as List<Task>
            val reminders = values[1] as List<com.geepscontrol.core.database.ReminderEntity>
            val routines = (values[2] as List<com.geepscontrol.core.database.RoutineEntity>).associateBy { it.id }
            val routineOccurrences = values[3] as List<com.geepscontrol.core.database.RoutineOccurrenceEntity>
            val medications = (values[4] as List<com.geepscontrol.core.database.MedicationEntity>).associateBy { it.id }
            val medicationOccurrences = values[5] as List<com.geepscontrol.core.database.MedicationOccurrenceEntity>
            val events = values[6] as List<com.geepscontrol.core.database.LifeEventEntity>
            val alarms = values[7] as List<com.geepscontrol.core.database.StandaloneAlarmEntity>
            val timers = values[8] as List<com.geepscontrol.core.database.StandaloneTimerEntity>
            val trips = (values[9] as List<com.geepscontrol.core.database.TripEntity>).associateBy { it.id }
            val itinerary = values[10] as List<com.geepscontrol.core.database.ItineraryEntryEntity>
            buildList {
                taskRows.filter { it.dueAt != null || it.status == TaskStatus.COMPLETED }.forEach { add(TimelineItem(it.id, TimelineItemType.TASK, it.title, it.dueAt, actionable = true, status = it.status.name, subtitle = it.description)) }
                addAll(reminders.map { it.timeline() })
                addAll(routineOccurrences.map { it.timeline(routines[it.routineId]?.title ?: "Routine") })
                addAll(medicationOccurrences.map { it.timeline(medications[it.medicationId]?.name ?: "Medication") })
                addAll(events.map { it.timeline() })
                addAll(alarms.filter { it.enabled }.map { it.timeline() })
                addAll(timers.filter { it.state == "RUNNING" }.map { it.timeline(now) })
                addAll(itinerary.map { it.timeline(trips[it.tripId]?.name ?: "Trip") })
            }.let(TimelineOrdering::sort)
        }

    fun forRange(
        items: List<TimelineItem>,
        start: Long,
        endExclusive: Long,
    ) = items.filter { it.startAt == null || it.startAt in start until endExclusive }

    fun overdue(
        items: List<TimelineItem>,
        now: Long,
    ) = items.filter { it.actionable && it.startAt != null && it.startAt < now && it.status in setOf("PENDING", "DUE", "SCHEDULED", "OPEN") }

    fun conflicts(items: List<TimelineItem>) = items.filter { it.endAt != null }.flatMapIndexed { index, a -> items.drop(index + 1).filter { b -> b.endAt != null && a.startAt != null && b.startAt != null && a.startAt < b.endAt!! && b.startAt < a.endAt!! }.map { a to it } }
}

object TimelineOrdering {
    fun sort(items: List<TimelineItem>): List<TimelineItem> =
        items.sortedWith(
            compareBy<TimelineItem> { it.startAt == null }
                .thenBy { !it.allDay }
                .thenBy { it.startAt }
                .thenBy { it.type.ordinal }
                .thenBy { it.title },
        )
}
