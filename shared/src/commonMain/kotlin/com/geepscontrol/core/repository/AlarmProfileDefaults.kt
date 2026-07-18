package com.geepscontrol.core.repository

import com.geepscontrol.core.database.AlarmProfileEntity
import kotlinx.coroutines.flow.first

suspend fun ensureDefaultAlarmProfiles(repository: LifeItemRepository, now: Long) {
    val definitions =
        listOf(
            AlarmProfileEntity(name = "Gentle", soundId = "gentle_pizzicato", vibrationMode = "LIGHT", startingVolumePercent = 15, maximumVolumePercent = 70, volumeRampSeconds = 90, snoozeDurationMinutes = 10, builtInKey = "gentle", createdAt = now, updatedAt = now),
            AlarmProfileEntity(name = "Standard", soundId = "standard_steel", vibrationMode = "STANDARD", startingVolumePercent = 30, maximumVolumePercent = 100, volumeRampSeconds = 45, snoozeDurationMinutes = 8, builtInKey = "standard", createdAt = now, updatedAt = now),
            AlarmProfileEntity(name = "Heavy Sleeper", soundId = "retro_alert", vibrationMode = "STRONG", startingVolumePercent = 25, maximumVolumePercent = 100, volumeRampSeconds = 30, snoozeDurationMinutes = 5, maximumSnoozes = 3, dismissalMethod = "NUMBER", dismissalDifficulty = "NORMAL", challengeRequiredForDismiss = true, builtInKey = "heavy_sleeper", createdAt = now, updatedAt = now),
            AlarmProfileEntity(name = "Silent Vibration", soundEnabled = false, soundId = null, vibrationMode = "STRONG", startingVolumePercent = 0, maximumVolumePercent = 0, volumeRampSeconds = 0, snoozeDurationMinutes = 8, builtInKey = "silent_vibration", createdAt = now, updatedAt = now),
            AlarmProfileEntity(name = "Reminder", soundId = "short_hit", loopSound = false, vibrationMode = "LIGHT", maximumVolumePercent = 70, volumeRampSeconds = 0, snoozeDurationMinutes = 10, builtInKey = "reminder", createdAt = now, updatedAt = now),
            AlarmProfileEntity(name = "Legacy Sound Only", soundId = "standard_steel", vibrationEnabled = false, vibrationMode = "OFF", builtInKey = "legacy_sound_only", createdAt = now, updatedAt = now),
            AlarmProfileEntity(name = "Legacy Silent", soundEnabled = false, soundId = null, vibrationEnabled = false, vibrationMode = "OFF", startingVolumePercent = 0, maximumVolumePercent = 0, volumeRampSeconds = 0, builtInKey = "legacy_silent", createdAt = now, updatedAt = now),
        )
    val existingKeys = repository.alarmProfiles().first().mapNotNull { it.builtInKey }.toSet()
    definitions.filterNot { it.builtInKey in existingKeys }.forEach { repository.save(it) }

    val profiles = repository.alarmProfiles().first().associateBy { it.builtInKey }
    repository.alarms().first().filter { it.profileId == null }.forEach { alarm ->
        val key = when {
            alarm.sound && alarm.vibration -> "standard"
            alarm.vibration -> "silent_vibration"
            alarm.sound -> "legacy_sound_only"
            else -> "legacy_silent"
        }
        profiles[key]?.let { repository.save(alarm.copy(profileId = it.id, updatedAt = now)) }
    }
}
