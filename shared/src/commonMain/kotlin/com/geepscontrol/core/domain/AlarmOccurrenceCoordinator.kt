package com.geepscontrol.core.domain

import com.geepscontrol.core.database.StandaloneAlarmEntity
import com.geepscontrol.core.repository.LifeItemRepository

class AlarmOccurrenceCoordinator(
    private val repository: LifeItemRepository,
    private val scheduler: AlarmScheduler,
    private val platform: PlatformActions,
) {
    suspend fun perform(
        id: Long,
        action: AlarmOccurrenceAction,
        now: Long,
        expectedAt: Long? = null,
        onSaved: (StandaloneAlarmEntity) -> Unit = {},
    ): StandaloneAlarmEntity? =
        repository.updateAlarm(id, { current ->
            if (expectedAt != null && current.scheduledAt != expectedAt) return@updateAlarm current
            if (action == AlarmOccurrenceAction.SNOOZE) {
                val profile = current.profileId?.let { repository.getAlarmProfile(it) }
                if (!AlarmExperience.canSnooze(current.snoozeCount, current.maximumSnoozes ?: profile?.maximumSnoozes ?: 0)) return@updateAlarm current
            }
            // Recover the ordinary time for snoozes saved before schema 15.
            val rule = AlarmRepeatRule.decode(current.recurrenceRule)
            val prepared =
                if (rule != null && current.nextRegularAt == null && AlarmOccurrenceEngine.isSnoozed(current) && current.localHour != null && current.localMinute != null) {
                    var regular = platform.localDateTimeToEpoch((current.localEpochDay ?: platform.localEpochDay(current.scheduledAt)) * 86_400_000L, current.localHour, current.localMinute)
                    while (regular <= (current.lastTriggeredAt ?: now)) regular = platform.nextAlarmOccurrence(rule, regular)
                    current.copy(nextRegularAt = regular)
                } else {
                    current
                }
            val updated = AlarmOccurrenceEngine.apply(prepared, action, now, platform::nextAlarmOccurrence)
            if (updated == current) current else updated.copy(localEpochDay = platform.localEpochDay(updated.nextRegularAt ?: updated.scheduledAt))
        }, { updated ->
            val systemId = 3_000_000L + updated.id
            if (action != AlarmOccurrenceAction.FIRE) scheduler.cancel(systemId)
            if (updated.enabled && updated.scheduledAt > now) {
                scheduler.schedule(AlarmDefinition(id = systemId, triggerAt = updated.scheduledAt, label = updated.label, sound = updated.sound, vibration = updated.vibration))
            }
            onSaved(updated)
        })
}
