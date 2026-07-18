package com.geepscontrol.core.domain

import com.geepscontrol.core.database.ItineraryEntryEntity
import com.geepscontrol.core.database.StandaloneTimerEntity
import com.geepscontrol.core.database.TripEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LifeScheduleEngineTest {
    @Test fun `multiple timers calculate independently after restart`() {
        val first = StandaloneTimerEntity(1, name = "Pasta", startedAt = 1_000, resumedAt = 1_000, durationMillis = 10_000, updatedAt = 1_000)
        val second = StandaloneTimerEntity(2, name = "Laundry", startedAt = 2_000, resumedAt = 2_000, durationMillis = 30_000, updatedAt = 2_000)
        assertEquals(6_000, LifeScheduleEngine.timerRemaining(first, 5_000))
        assertEquals(27_000, LifeScheduleEngine.timerRemaining(second, 5_000))
    }

    @Test fun `medication interval and refill calculations are stable`() {
        assertEquals(listOf(0L, 8L * 3_600_000, 16L * 3_600_000), LifeScheduleEngine.nextOccurrences(0, 24L * 3_600_000, 8L * 3_600_000))
        assertEquals(8.5, LifeScheduleEngine.refillAfterDose(10.0, 1.5))
    }

    @Test fun `snooze and reminder offsets use requested minutes`() {
        assertEquals(700_000, LifeScheduleEngine.reminderSnooze(100_000, 10))
        assertEquals(2_800_000, TripService.reminderAt(4_600_000, 30))
    }

    @Test fun `trip day and overlap calculations`() {
        val trip = TripEntity(1, "Japan", startDate = 0, endDate = 3 * LifeScheduleEngine.DAY_MILLIS, homeTimezone = "America/Detroit", createdAt = 0, updatedAt = 0)
        assertEquals(2, TripService.dayNumber(trip, LifeScheduleEngine.DAY_MILLIS))
        val a = ItineraryEntryEntity(1, 1, title = "Flight", startAt = 100, endAt = 300, timezone = "America/Detroit", createdAt = 0, updatedAt = 0)
        val b = ItineraryEntryEntity(2, 1, title = "Train", startAt = 250, endAt = 400, timezone = "Asia/Tokyo", createdAt = 0, updatedAt = 0)
        assertTrue(TripService.conflicts(listOf(a, b)).isNotEmpty())
    }

    @Test fun `timeline orders all day and absolute instants consistently across zones`() {
        val timedTokyo = TimelineItem(1, TimelineItemType.ITINERARY, "Tokyo", 2_000, actionable = false, status = "CONFIRMED", timezone = "Asia/Tokyo")
        val timedDetroit = TimelineItem(2, TimelineItemType.EVENT, "Detroit", 1_000, actionable = false, status = "SCHEDULED", timezone = "America/Detroit")
        val allDay = TimelineItem(3, TimelineItemType.EVENT, "Birthday", 1_000, allDay = true, actionable = false, status = "SCHEDULED")
        assertEquals(listOf(3L, 2L, 1L), TimelineOrdering.sort(listOf(timedTokyo, timedDetroit, allDay)).map { it.id })
    }
}
