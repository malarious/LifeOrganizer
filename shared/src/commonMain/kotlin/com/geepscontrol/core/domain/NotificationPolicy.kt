package com.geepscontrol.core.domain

object NotificationPolicy {
    fun isQuietHour(
        hour: Int,
        enabled: Boolean,
        startHour: Int,
        endHour: Int,
    ): Boolean {
        if (!enabled) return false
        val normalized = hour.mod(24)
        val start = startHour.mod(24)
        val end = endHour.mod(24)
        return if (start > end) normalized >= start || normalized < end else normalized in start until end
    }
}
