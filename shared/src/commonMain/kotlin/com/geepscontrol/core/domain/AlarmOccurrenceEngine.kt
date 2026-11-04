package com.geepscontrol.core.domain

import com.geepscontrol.core.database.StandaloneAlarmEntity

enum class AlarmOccurrenceAction { FIRE, SNOOZE, ACKNOWLEDGE, SKIP, DELAY, DISABLE, ENABLE }

/** Transitions one occurrence while retaining the ordinary repeat schedule. */
object AlarmOccurrenceEngine {
    fun needsAcknowledgment(alarm: StandaloneAlarmEntity): Boolean = alarm.lastTriggeredAt?.let { it > (alarm.lastDismissedAt ?: Long.MIN_VALUE) } == true

    fun isSnoozed(alarm: StandaloneAlarmEntity): Boolean =
        alarm.enabled && alarm.snoozeCount > 0 && needsAcknowledgment(alarm) && alarm.scheduledAt > (alarm.lastTriggeredAt ?: Long.MAX_VALUE) &&
            (alarm.nextRegularAt != null || alarm.recurrenceRule == null || alarm.scheduledAt - alarm.lastTriggeredAt!! <= alarm.snoozeMinutes * 60_000L)

    fun apply(
        alarm: StandaloneAlarmEntity,
        action: AlarmOccurrenceAction,
        now: Long,
        nextOccurrence: (AlarmRepeatRule, Long) -> Long = AlarmRepeatEngine::next,
    ): StandaloneAlarmEntity {
        val rule = AlarmRepeatRule.decode(alarm.recurrenceRule)
        val regularAt = alarm.nextRegularAt ?: alarm.scheduledAt

        fun future(after: Long): Long {
            var next = regularAt
            if (rule != null) while (next <= after) next = nextOccurrence(rule, next)
            return next
        }
        return when (action) {
            AlarmOccurrenceAction.FIRE -> {
                if (!alarm.enabled || alarm.scheduledAt > now) return alarm
                if (rule == null && !isSnoozed(alarm) && (alarm.lastTriggeredAt ?: Long.MIN_VALUE) >= alarm.scheduledAt) return alarm
                alarm.copy(
                    scheduledAt = if (rule == null) alarm.scheduledAt else future(now),
                    nextRegularAt = null,
                    snoozeCount = if (isSnoozed(alarm)) alarm.snoozeCount else 0,
                    lastTriggeredAt = now,
                    updatedAt = now,
                )
            }
            AlarmOccurrenceAction.SNOOZE -> {
                if (!alarm.enabled || !needsAcknowledgment(alarm)) return alarm
                alarm.copy(
                    scheduledAt = now + alarm.snoozeMinutes.coerceAtLeast(1) * 60_000L,
                    nextRegularAt = if (rule == null) null else regularAt,
                    snoozeCount = alarm.snoozeCount + 1,
                    updatedAt = now,
                )
            }
            AlarmOccurrenceAction.ACKNOWLEDGE ->
                alarm.copy(
                    enabled = alarm.enabled && rule != null,
                    scheduledAt = if (rule == null) alarm.scheduledAt else future(now),
                    nextRegularAt = null,
                    snoozeCount = 0,
                    lastDismissedAt = now,
                    updatedAt = now,
                )
            AlarmOccurrenceAction.SKIP -> {
                if (!alarm.enabled || alarm.scheduledAt <= now) return alarm
                // Stop snooze finishes the active occurrence without skipping tomorrow as well.
                if (isSnoozed(alarm)) return apply(alarm, AlarmOccurrenceAction.ACKNOWLEDGE, now, nextOccurrence)
                alarm.copy(
                    enabled = rule != null,
                    scheduledAt = if (rule == null) alarm.scheduledAt else future(maxOf(now, regularAt)),
                    nextRegularAt = null,
                    snoozeCount = 0,
                    lastSkippedAt = now,
                    updatedAt = now,
                )
            }
            AlarmOccurrenceAction.DELAY -> {
                if (!alarm.enabled || alarm.scheduledAt <= now) return alarm
                alarm.copy(scheduledAt = alarm.scheduledAt + 5 * 60_000L, nextRegularAt = if (rule == null) null else regularAt, updatedAt = now)
            }
            AlarmOccurrenceAction.DISABLE -> alarm.copy(enabled = false, scheduledAt = if (rule == null) alarm.scheduledAt else future(now), nextRegularAt = null, snoozeCount = 0, updatedAt = now)
            AlarmOccurrenceAction.ENABLE -> {
                // A past one-shot needs a new time from the editor, rather than an immediate ring.
                if (rule == null && alarm.scheduledAt <= now) return alarm
                alarm.copy(enabled = true, scheduledAt = future(now), nextRegularAt = null, snoozeCount = 0, updatedAt = now)
            }
        }
    }
}
