package com.geepscontrol.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.geepscontrol.core.database.AlarmProfileEntity
import com.geepscontrol.core.domain.PlatformActions
import com.geepscontrol.core.domain.bundledAlarmSounds

@Composable
internal fun AlarmProfileEditorDialog(original: AlarmProfileEntity, platformActions: PlatformActions, onDismiss: () -> Unit, onSave: (AlarmProfileEntity) -> Unit, onDelete: (() -> Unit)?) {
    var name by remember(original.id) { mutableStateOf(original.name) }; var soundEnabled by remember(original.id) { mutableStateOf(original.soundEnabled) }; var soundId by remember(original.id) { mutableStateOf(original.soundId ?: "standard_steel") }; var loopSound by remember(original.id) { mutableStateOf(original.loopSound) }; var vibrationMode by remember(original.id) { mutableStateOf(original.vibrationMode) }; var vibrationDelay by remember(original.id) { mutableStateOf(original.vibrationStartDelaySeconds) }; var startingVolume by remember(original.id) { mutableStateOf(original.startingVolumePercent) }; var maximumVolume by remember(original.id) { mutableStateOf(original.maximumVolumePercent) }; var ramp by remember(original.id) { mutableStateOf(original.volumeRampSeconds) }; var snooze by remember(original.id) { mutableStateOf(original.snoozeDurationMinutes.toString()) }; var maxSnoozes by remember(original.id) { mutableStateOf(original.maximumSnoozes.toString()) }; var method by remember(original.id) { mutableStateOf(original.dismissalMethod) }; var difficulty by remember(original.id) { mutableStateOf(original.dismissalDifficulty) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Alarm profile") }, text = { LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { OutlinedTextField(name, { name = it }, label = { Text("Profile name") }, modifier = Modifier.fillMaxWidth()) }; item { Row(verticalAlignment = Alignment.CenterVertically) { Switch(soundEnabled, { soundEnabled = it }); Text("Sound", modifier = Modifier.padding(start = 8.dp)) } }; item { SelectorField("Sound", bundledAlarmSounds, bundledAlarmSounds.firstOrNull { it.id == soundId } ?: bundledAlarmSounds.first(), { "${it.name} · ${it.category}" }, { soundId = it.id }, Modifier.fillMaxWidth()) }; item { Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { OutlinedButton({ platformActions.previewAlarmSound(soundId) }, Modifier.weight(1f)) { Text("Preview") }; OutlinedButton(platformActions::stopAlarmSoundPreview, Modifier.weight(1f)) { Text("Stop") } } }; item { SelectorField("Vibration", listOf("OFF", "LIGHT", "STANDARD", "STRONG", "ESCALATING", "CONTINUOUS"), vibrationMode, { it.lowercase().replaceFirstChar(Char::uppercase) }, { vibrationMode = it }, Modifier.fillMaxWidth()) }; item { Row(verticalAlignment = Alignment.CenterVertically) { Switch(loopSound, { loopSound = it }); Text("Repeat sound until acknowledged", Modifier.padding(start = 8.dp)) } }; if (vibrationMode != "OFF") item { Text("Vibration delay: ${vibrationDelay}s"); Slider(vibrationDelay.toFloat(), { vibrationDelay = it.toInt() }, valueRange = 0f..120f, steps = 11) }; item { Text("Starting volume: $startingVolume%"); Slider(startingVolume.toFloat(), { startingVolume = it.toInt().coerceAtMost(maximumVolume) }, valueRange = 0f..100f, steps = 9) }; item { Text("Maximum volume: $maximumVolume%"); Slider(maximumVolume.toFloat(), { maximumVolume = it.toInt().coerceAtLeast(startingVolume) }, valueRange = 0f..100f, steps = 9) }; item { Text("Volume ramp: ${ramp}s"); Slider(ramp.toFloat(), { ramp = it.toInt() }, valueRange = 0f..300f, steps = 9) }; item { Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { OutlinedTextField(snooze, { snooze = it.filter(Char::isDigit) }, label = { Text("Snooze min") }, modifier = Modifier.weight(1f)); OutlinedTextField(maxSnoozes, { maxSnoozes = it.filter(Char::isDigit) }, label = { Text("Max (0 unlimited)") }, modifier = Modifier.weight(1f)) } }; item { SelectorField("Dismiss", listOf("STANDARD", "HOLD", "NUMBER", "MATH", "MEMORY", "TRACE", "SORT", "SCAN", "NFC"), method, { it.lowercase().replaceFirstChar(Char::uppercase) }, { method = it }, Modifier.fillMaxWidth()) }; if (method != "STANDARD") item { SelectorField("Difficulty", listOf("EASY", "NORMAL", "HARD"), difficulty, { it.lowercase().replaceFirstChar(Char::uppercase) }, { difficulty = it }, Modifier.fillMaxWidth()) }; onDelete?.let { action -> item { TextButton(action) { Text("Delete profile", color = MaterialTheme.colorScheme.error) } } }
    } }, confirmButton = { Button(onClick = { onSave(original.copy(name = name.trim(), soundId = soundId, soundEnabled = soundEnabled, loopSound = loopSound, vibrationMode = vibrationMode, vibrationEnabled = vibrationMode != "OFF", vibrationStartDelaySeconds = vibrationDelay, startingVolumePercent = startingVolume, maximumVolumePercent = maximumVolume, volumeRampSeconds = ramp, snoozeDurationMinutes = (snooze.toIntOrNull() ?: 8).coerceAtLeast(1), maximumSnoozes = (maxSnoozes.toIntOrNull() ?: 0).coerceAtLeast(0), dismissalMethod = method, dismissalDifficulty = difficulty, challengeRequiredForDismiss = method != "STANDARD")) }, enabled = name.isNotBlank()) { Text("Save") } }, dismissButton = { TextButton(onDismiss) { Text("Cancel") } })
}
