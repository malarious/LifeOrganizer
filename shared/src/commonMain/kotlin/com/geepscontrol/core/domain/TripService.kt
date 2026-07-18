package com.geepscontrol.core.domain

import com.geepscontrol.core.database.ItineraryEntryEntity
import com.geepscontrol.core.database.TripEntity

object TripService {
    fun dayNumber(
        trip: TripEntity,
        now: Long,
    ): Int? = if (now < trip.startDate || now >= trip.endDate + LifeScheduleEngine.DAY_MILLIS) null else ((now - trip.startDate) / LifeScheduleEngine.DAY_MILLIS).toInt() + 1

    fun daysUntil(
        trip: TripEntity,
        now: Long,
    ): Long = ((trip.startDate - now).coerceAtLeast(0) + LifeScheduleEngine.DAY_MILLIS - 1) / LifeScheduleEngine.DAY_MILLIS

    fun conflicts(entries: List<ItineraryEntryEntity>): List<Pair<ItineraryEntryEntity, ItineraryEntryEntity>> = entries.filter { it.status != "CANCELLED" && it.startAt != null && it.endAt != null }.flatMapIndexed { index, first -> entries.drop(index + 1).filter { second -> second.status != "CANCELLED" && second.startAt != null && second.endAt != null && first.startAt!! < second.endAt!! && second.startAt < first.endAt!! }.map { first to it } }

    fun reminderAt(
        entryStart: Long,
        offsetMinutes: Int,
    ) = entryStart - offsetMinutes.coerceAtLeast(0) * 60_000L

    fun duplicate(
        entry: ItineraryEntryEntity,
        newTripId: Long = entry.tripId,
        shiftMillis: Long = 0,
        now: Long,
    ) = entry.copy(id = 0, tripId = newTripId, startAt = entry.startAt?.plus(shiftMillis), endAt = entry.endAt?.plus(shiftMillis), sortOrder = entry.sortOrder + 1, createdAt = now, updatedAt = now)
}
