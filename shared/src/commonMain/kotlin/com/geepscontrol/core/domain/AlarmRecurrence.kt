package com.geepscontrol.core.domain

enum class AlarmRepeatUnit { HOUR, DAY, WEEK, MONTH }

data class AlarmRepeatRule(
    val interval: Int = 1,
    val unit: AlarmRepeatUnit,
    val weekdays: Set<Int> = emptySet(),
) {
    fun encode(): String = "ALARM:${interval.coerceAtLeast(1)}:${unit.name}:${weekdays.sorted().joinToString(",")}" 

    companion object {
        fun decode(value: String?): AlarmRepeatRule? {
            val parts = value?.split(':') ?: return null
            if (parts.size != 4 || parts[0] != "ALARM") return null
            return AlarmRepeatRule(
                interval = parts[1].toIntOrNull()?.coerceAtLeast(1) ?: return null,
                unit = AlarmRepeatUnit.entries.firstOrNull { it.name == parts[2] } ?: return null,
                weekdays = parts[3].split(',').mapNotNull(String::toIntOrNull).filter { it in 0..6 }.toSet(),
            )
        }
    }
}

object AlarmRepeatEngine {
    private const val HOUR = 3_600_000L
    private const val DAY = 86_400_000L

    fun next(rule: AlarmRepeatRule, previous: Long): Long {
        if (rule.unit == AlarmRepeatUnit.HOUR) return previous + rule.interval * HOUR
        if (rule.unit == AlarmRepeatUnit.DAY) return previous + rule.interval * DAY
        if (rule.unit == AlarmRepeatUnit.MONTH) return previous + rule.interval * 30L * DAY
        if (rule.weekdays.isEmpty()) return previous + rule.interval * 7L * DAY

        val currentDay = previous / DAY
        val currentWeek = floorDiv(currentDay + 3, 7)
        for (offset in 1..(rule.interval * 7 + 7)) {
            val candidateDay = currentDay + offset
            val weekday = floorMod(candidateDay + 4, 7).toInt()
            val candidateWeek = floorDiv(candidateDay + 3, 7)
            if (weekday in rule.weekdays && (candidateWeek - currentWeek) % rule.interval == 0L) return previous + offset * DAY
        }
        return previous + rule.interval * 7L * DAY
    }

    /** Returns [scheduledAt] when it is already in the future, otherwise catches it up. */
    fun nextFuture(
        rule: AlarmRepeatRule,
        scheduledAt: Long,
        now: Long,
    ): Long {
        var next = scheduledAt
        while (next <= now) next = AlarmRepeatEngine.next(rule, next)
        return next
    }

    private fun floorDiv(value: Long, divisor: Long): Long {
        var result = value / divisor
        if ((value xor divisor) < 0 && result * divisor != value) result--
        return result
    }

    private fun floorMod(value: Long, divisor: Long): Long = value - floorDiv(value, divisor) * divisor
}
