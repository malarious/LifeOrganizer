package com.geepscontrol.core.domain

import kotlin.math.floor
import kotlin.test.Test
import kotlin.test.assertEquals

class PlatformActionsTest {
    @Test
    fun `local epoch day follows device date across utc boundary`() {
        val japanActions =
            object : PlatformActions {
                override fun localDateEpochMillis(epochMillis: Long): Long =
                    floor((epochMillis + 9 * HOUR_MILLIS).toDouble() / DAY_MILLIS).toLong() * DAY_MILLIS

                override fun openMap(address: String) = false
                override fun openWebsite(url: String) = false
                override fun call(phone: String) = false
                override fun copy(label: String, value: String) = Unit
                override fun previewAlarmSound(soundId: String) = false
                override fun stopAlarmSoundPreview() = Unit
                override fun deviceCalendars() = emptyList<DeviceCalendar>()
                override fun deviceCalendarEvents(startAt: Long, endAt: Long, calendarIds: Set<Long>) = emptyList<DeviceCalendarEvent>()
                override fun launchTestAlarm(dismissalMethod: String, difficulty: String) = false
                override fun canScheduleExactAlarms() = true
            }

        assertEquals(0, japanActions.localEpochDay(-HOUR_MILLIS))
        assertEquals(-1, japanActions.localEpochDay(-10 * HOUR_MILLIS))
    }

    private companion object {
        const val HOUR_MILLIS = 3_600_000L
        const val DAY_MILLIS = 86_400_000L
    }
}
