package com.geepscontrol.core.domain

enum class RecurrenceMode { FIXED, AFTER_COMPLETION }

enum class RecurrenceUnit { DAY, WEEK, MONTH }

data class RecurrenceRule(
    val interval: Int = 1,
    val unit: RecurrenceUnit,
    val mode: RecurrenceMode = RecurrenceMode.FIXED,
) {
    fun encode(): String = "${interval.coerceAtLeast(1)}:${unit.name}:${mode.name}"

    companion object {
        fun decode(value: String?): RecurrenceRule? {
            val parts = value?.split(':') ?: return null
            if (parts.size != 3) return null
            return RecurrenceRule(parts[0].toIntOrNull()?.coerceAtLeast(1) ?: return null, enumValues<RecurrenceUnit>().firstOrNull { it.name == parts[1] } ?: return null, enumValues<RecurrenceMode>().firstOrNull { it.name == parts[2] } ?: return null)
        }
    }
}

object RecurrenceEngine {
    fun nextTimestamp(
        rule: RecurrenceRule,
        previousScheduledOrDue: Long?,
        completedAt: Long,
    ): Long {
        val base = if (rule.mode == RecurrenceMode.AFTER_COMPLETION) completedAt else previousScheduledOrDue ?: completedAt
        val days =
            when (rule.unit) {
                RecurrenceUnit.DAY -> rule.interval
                RecurrenceUnit.WEEK -> rule.interval * 7
                RecurrenceUnit.MONTH -> rule.interval * 30
            }
        var next = base + days * RecommendationService.DAY_MILLIS
        if (rule.mode == RecurrenceMode.FIXED) while (next <= completedAt) next += days * RecommendationService.DAY_MILLIS
        return next
    }
}
