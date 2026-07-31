package com.geepscontrol.core.domain

data class DeviceCalendar(val id: Long, val name: String, val accountName: String)
data class DeviceCalendarEvent(val id: Long, val calendarId: Long, val title: String, val startAt: Long, val endAt: Long, val allDay: Boolean, val location: String = "")

interface PlatformActions {
    fun currentTimezoneId(): String = "UTC"

    fun localMinutesOfDay(epochMillis: Long): Int =
        (((epochMillis % 86_400_000L) + 86_400_000L) % 86_400_000L / 60_000L).toInt()

    fun localDateEpochMillis(epochMillis: Long): Long = epochMillis / 86_400_000L * 86_400_000L

    fun localEpochDay(epochMillis: Long): Long = localDateEpochMillis(epochMillis) / 86_400_000L

    fun localDateTimeToEpoch(
        dateEpochMillis: Long,
        hour: Int,
        minute: Int,
    ): Long = dateEpochMillis / 86_400_000L * 86_400_000L + hour * 3_600_000L + minute * 60_000L

    fun openMap(address: String): Boolean

    fun openWebsite(url: String): Boolean

    fun call(phone: String): Boolean

    fun copy(
        label: String,
        value: String,
    )

    fun previewAlarmSound(soundId: String): Boolean

    fun stopAlarmSoundPreview()

    fun deviceCalendars(): List<DeviceCalendar>

    fun deviceCalendarEvents(startAt: Long, endAt: Long, calendarIds: Set<Long>): List<DeviceCalendarEvent>

    fun launchTestAlarm(dismissalMethod: String, difficulty: String): Boolean

    fun canScheduleExactAlarms(): Boolean
}
