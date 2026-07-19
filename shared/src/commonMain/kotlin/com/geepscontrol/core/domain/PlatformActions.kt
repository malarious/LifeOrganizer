package com.geepscontrol.core.domain

data class DeviceCalendar(val id: Long, val name: String, val accountName: String)
data class DeviceCalendarEvent(val id: Long, val calendarId: Long, val title: String, val startAt: Long, val endAt: Long, val allDay: Boolean, val location: String = "")

interface PlatformActions {
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
}
