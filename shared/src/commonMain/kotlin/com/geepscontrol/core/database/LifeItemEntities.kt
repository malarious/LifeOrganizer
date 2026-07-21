package com.geepscontrol.core.database

import androidx.room.Dao
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

@Entity(tableName = "reminders", indices = [Index("scheduledAt"), Index("taskId")])
@Serializable
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val scheduledAt: Long,
    val allDay: Boolean = false,
    val recurrenceRule: String? = null,
    val behavior: String = "NOTIFICATION",
    val snoozeMinutes: Int = 10,
    val sound: Boolean = true,
    val vibration: Boolean = true,
    val status: String = "SCHEDULED",
    val notes: String = "",
    val taskId: Long? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val completedAt: Long? = null,
)

@Entity(tableName = "routines")
@Serializable
data class RoutineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val recurrenceRule: String,
    val preferredTimeMinutes: Int? = null,
    val windowStartMinutes: Int? = null,
    val windowEndMinutes: Int? = null,
    val expectedDurationMinutes: Int? = null,
    val trackingMode: String = "COMPLETION_ONLY",
    val active: Boolean = true,
    val startDate: Long,
    val endDate: Long? = null,
    val notes: String = "",
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(tableName = "routine_occurrences", foreignKeys = [ForeignKey(entity = RoutineEntity::class, parentColumns = ["id"], childColumns = ["routineId"], onDelete = ForeignKey.CASCADE)], indices = [Index("routineId"), Index(value = ["routineId", "scheduledAt"], unique = true)])
@Serializable
data class RoutineOccurrenceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val routineId: Long,
    val scheduledAt: Long,
    val status: String = "PENDING",
    val value: Int? = null,
    val note: String = "",
    val recordedAt: Long? = null,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(tableName = "medications")
@Serializable
data class MedicationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val dosageText: String = "",
    val instructions: String = "",
    val scheduleType: String = "DAILY",
    val recurrenceRule: String? = null,
    val timesOfDay: String = "",
    val intervalHours: Int? = null,
    val startDate: Long,
    val endDate: Long? = null,
    val asNeeded: Boolean = false,
    val active: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val refillTracking: Boolean = false,
    val quantityRemaining: Double? = null,
    val quantityPerDose: Double = 1.0,
    val refillThreshold: Double? = null,
    val prescribingNotes: String = "",
    val generalNotes: String = "",
    val createdAt: Long,
    val updatedAt: Long,
    val archivedAt: Long? = null,
)

@Entity(tableName = "medication_occurrences", foreignKeys = [ForeignKey(entity = MedicationEntity::class, parentColumns = ["id"], childColumns = ["medicationId"], onDelete = ForeignKey.CASCADE)], indices = [Index("medicationId"), Index(value = ["medicationId", "scheduledAt"], unique = true)])
@Serializable
data class MedicationOccurrenceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val medicationId: Long,
    val scheduledAt: Long,
    val status: String = "DUE",
    val recordedAt: Long? = null,
    val dosageTakenText: String = "",
    val note: String = "",
    val snoozedUntil: Long? = null,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(tableName = "life_events", indices = [Index("startAt")])
@Serializable
data class LifeEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val startAt: Long,
    val endAt: Long? = null,
    val allDay: Boolean = false,
    val timezone: String,
    val locationName: String = "",
    val address: String = "",
    val recurrenceRule: String? = null,
    val reminderOffsets: String = "",
    val notes: String = "",
    val status: String = "SCHEDULED",
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(tableName = "standalone_alarms", indices = [Index("scheduledAt")])
@Serializable
data class StandaloneAlarmEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    val scheduledAt: Long,
    val recurrenceRule: String? = null,
    val enabled: Boolean = true,
    val sound: Boolean = true,
    val vibration: Boolean = true,
    val snoozeMinutes: Int = 10,
    val profileId: Long? = null,
    @ColumnInfo(defaultValue = "0") val snoozeCount: Int = 0,
    val soundId: String? = null,
    val loopSound: Boolean? = null,
    val vibrationMode: String? = null,
    val vibrationStartDelaySeconds: Int? = null,
    val startingVolumePercent: Int? = null,
    val maximumVolumePercent: Int? = null,
    val volumeRampSeconds: Int? = null,
    val maximumSnoozes: Int? = null,
    val dismissalMethod: String? = null,
    val dismissalDifficulty: String? = null,
    val localHour: Int? = null,
    val localMinute: Int? = null,
    val localEpochDay: Long? = null,
    val associatedType: String? = null,
    val associatedId: Long? = null,
    val lastTriggeredAt: Long? = null,
    val lastDismissedAt: Long? = null,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(tableName = "timer_presets")
@Serializable
data class TimerPresetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val durationSeconds: Long,
    val sound: Boolean = true,
    val vibration: Boolean = true,
    val continueOvertime: Boolean = true,
    val autoRepeat: Boolean = false,
    val category: String = "",
    val notes: String = "",
    val soundId: String? = null,
    @ColumnInfo(defaultValue = "'STANDARD'") val vibrationMode: String = "STANDARD",
    @ColumnInfo(defaultValue = "0") val isFavorite: Boolean = false,
    @ColumnInfo(defaultValue = "0") val manualOrder: Long = 0,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(tableName = "standalone_timers", indices = [Index("state")])
@Serializable
data class StandaloneTimerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val presetId: Long? = null,
    val name: String,
    val state: String = "RUNNING",
    val startedAt: Long,
    val resumedAt: Long,
    val accumulatedMillis: Long = 0,
    val durationMillis: Long,
    val sound: Boolean = true,
    val vibration: Boolean = true,
    val continueOvertime: Boolean = true,
    val autoRepeat: Boolean = false,
    val completedAt: Long? = null,
    val updatedAt: Long,
)

@Entity(tableName = "alarm_profiles")
@Serializable
data class AlarmProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val soundId: String? = null,
    val soundEnabled: Boolean = true,
    val loopSound: Boolean = true,
    val vibrationMode: String = "STANDARD",
    val vibrationEnabled: Boolean = true,
    val vibrationStartDelaySeconds: Int = 0,
    val startingVolumePercent: Int = 30,
    val maximumVolumePercent: Int = 100,
    val volumeRampSeconds: Int = 45,
    val snoozeDurationMinutes: Int = 8,
    val maximumSnoozes: Int = 0,
    val dismissalMethod: String = "STANDARD",
    val dismissalDifficulty: String = "NORMAL",
    val challengeRequiredForSnooze: Boolean = false,
    val challengeRequiredForDismiss: Boolean = false,
    val persistentUntilAcknowledged: Boolean = true,
    val builtInKey: String? = null,
    @ColumnInfo(defaultValue = "0") val isDefault: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(tableName = "checklists")
data class ChecklistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "checklist_items",
    foreignKeys = [ForeignKey(entity = ChecklistEntity::class, parentColumns = ["id"], childColumns = ["checklistId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("checklistId")],
)
data class ChecklistItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val checklistId: Long,
    val text: String,
    val checked: Boolean = false,
    val sortOrder: Int = 0,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(tableName = "timer_history")
@Serializable
data class TimerHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val presetId: Long? = null,
    val name: String,
    val startedAt: Long,
    val endedAt: Long,
    val originalDurationMillis: Long,
    val finalDurationMillis: Long,
    val result: String,
    val overtimeMillis: Long = 0,
)

@Dao
interface LifeItemDao {
    @Query("SELECT * FROM alarm_profiles ORDER BY CASE WHEN builtInKey IS NULL THEN 1 ELSE 0 END, name")
    fun observeAlarmProfiles(): Flow<List<AlarmProfileEntity>>

    @Query("SELECT * FROM alarm_profiles WHERE id = :id")
    suspend fun getAlarmProfile(id: Long): AlarmProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putAlarmProfile(value: AlarmProfileEntity): Long

    @Query("DELETE FROM alarm_profiles WHERE id = :id AND builtInKey IS NULL")
    suspend fun deleteAlarmProfile(id: Long)

    @Query("UPDATE alarm_profiles SET isDefault = CASE WHEN id = :id THEN 1 ELSE 0 END")
    suspend fun setDefaultAlarmProfile(id: Long)

    @Query("SELECT * FROM checklists ORDER BY updatedAt DESC")
    fun observeChecklists(): Flow<List<ChecklistEntity>>

    @Query("SELECT * FROM checklist_items ORDER BY sortOrder, id")
    fun observeChecklistItems(): Flow<List<ChecklistItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putChecklist(value: ChecklistEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putChecklistItem(value: ChecklistItemEntity): Long

    @Query("DELETE FROM checklists WHERE id = :id")
    suspend fun deleteChecklist(id: Long)

    @Query("SELECT * FROM reminders ORDER BY scheduledAt")
    fun observeReminders(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getReminder(id: Long): ReminderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putReminder(value: ReminderEntity): Long

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteReminder(id: Long)

    @Query("UPDATE reminders SET status = :status, completedAt = :at, updatedAt = :at WHERE id = :id")
    suspend fun setReminderStatus(
        id: Long,
        status: String,
        at: Long,
    )

    @Query("UPDATE reminders SET scheduledAt = :at, status = 'SNOOZED', updatedAt = :now WHERE id = :id")
    suspend fun snoozeReminder(
        id: Long,
        at: Long,
        now: Long,
    )

    @Query("SELECT * FROM routines ORDER BY title")
    fun observeRoutines(): Flow<List<RoutineEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putRoutine(value: RoutineEntity): Long

    @Query("DELETE FROM routines WHERE id = :id")
    suspend fun deleteRoutine(id: Long)

    @Query("UPDATE routines SET active = :active, updatedAt = :now WHERE id = :id")
    suspend fun setRoutineActive(
        id: Long,
        active: Boolean,
        now: Long,
    )

    @Query("SELECT * FROM routine_occurrences ORDER BY scheduledAt DESC")
    fun observeRoutineOccurrences(): Flow<List<RoutineOccurrenceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putRoutineOccurrence(value: RoutineOccurrenceEntity): Long

    @Query("SELECT * FROM medications ORDER BY name")
    fun observeMedications(): Flow<List<MedicationEntity>>

    @Query("SELECT * FROM medications WHERE id = :id")
    suspend fun getMedication(id: Long): MedicationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putMedication(value: MedicationEntity): Long

    @Query("DELETE FROM medications WHERE id = :id")
    suspend fun deleteMedication(id: Long)

    @Query("UPDATE medications SET active = :active, updatedAt = :now WHERE id = :id")
    suspend fun setMedicationActive(
        id: Long,
        active: Boolean,
        now: Long,
    )

    @Query("SELECT * FROM medication_occurrences ORDER BY scheduledAt DESC")
    fun observeMedicationOccurrences(): Flow<List<MedicationOccurrenceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putMedicationOccurrence(value: MedicationOccurrenceEntity): Long

    @Query("SELECT * FROM life_events ORDER BY startAt")
    fun observeEvents(): Flow<List<LifeEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putEvent(value: LifeEventEntity): Long

    @Query("DELETE FROM life_events WHERE id = :id")
    suspend fun deleteEvent(id: Long)

    @Query("SELECT * FROM standalone_alarms ORDER BY scheduledAt")
    fun observeStandaloneAlarms(): Flow<List<StandaloneAlarmEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putStandaloneAlarm(value: StandaloneAlarmEntity): Long

    @Query("SELECT * FROM standalone_alarms WHERE id = :id")
    suspend fun getStandaloneAlarm(id: Long): StandaloneAlarmEntity?

    @Query("UPDATE standalone_alarms SET enabled = :enabled, updatedAt = :now WHERE id = :id")
    suspend fun setStandaloneAlarmEnabled(
        id: Long,
        enabled: Boolean,
        now: Long,
    )

    @Query("DELETE FROM standalone_alarms WHERE id = :id")
    suspend fun deleteStandaloneAlarm(id: Long)

    @Query("UPDATE standalone_alarms SET scheduledAt = :at, lastTriggeredAt = :triggeredAt, snoozeCount = snoozeCount + 1, updatedAt = :triggeredAt WHERE id = :id")
    suspend fun snoozeStandaloneAlarm(
        id: Long,
        at: Long,
        triggeredAt: Long,
    )

    @Query("SELECT * FROM timer_presets ORDER BY name")
    fun observeTimerPresets(): Flow<List<TimerPresetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putTimerPreset(value: TimerPresetEntity): Long

    @Query("DELETE FROM timer_presets WHERE id = :id")
    suspend fun deleteTimerPreset(id: Long)

    @Query("SELECT * FROM standalone_timers ORDER BY startedAt")
    fun observeStandaloneTimers(): Flow<List<StandaloneTimerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putStandaloneTimer(value: StandaloneTimerEntity): Long

    @Query("SELECT * FROM standalone_timers WHERE id = :id")
    suspend fun getStandaloneTimer(id: Long): StandaloneTimerEntity?

    @Query("DELETE FROM standalone_timers WHERE id = :id")
    suspend fun deleteStandaloneTimer(id: Long)

    @Query("SELECT * FROM timer_history ORDER BY endedAt DESC")
    fun observeTimerHistory(): Flow<List<TimerHistoryEntity>>

    @Insert suspend fun addTimerHistory(value: TimerHistoryEntity): Long

    @Query("DELETE FROM timer_history WHERE endedAt < :dayStart")
    suspend fun deleteTimerHistoryBefore(dayStart: Long)

    @Query("SELECT * FROM trips ORDER BY startDate")
    fun observeTrips(): Flow<List<TripEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putTrip(value: TripEntity): Long

    @Query("SELECT * FROM itinerary_entries ORDER BY CASE WHEN startAt IS NULL THEN 1 ELSE 0 END, startAt, sortOrder")
    fun observeItineraryEntries(): Flow<List<ItineraryEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putItineraryEntry(value: ItineraryEntryEntity): Long

    @Query("DELETE FROM itinerary_entries WHERE id = :id")
    suspend fun deleteItineraryEntry(id: Long)

    @Query("UPDATE itinerary_entries SET startAt = :startAt, endAt = :endAt, updatedAt = :now WHERE id = :id")
    suspend fun moveItineraryEntry(
        id: Long,
        startAt: Long?,
        endAt: Long?,
        now: Long,
    )

    @Query("SELECT * FROM transportation_details")
    fun observeTransportationDetails(): Flow<List<TransportationDetailsEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putTransportationDetails(value: TransportationDetailsEntity)

    @Query("SELECT * FROM lodging_details")
    fun observeLodgingDetails(): Flow<List<LodgingDetailsEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putLodgingDetails(value: LodgingDetailsEntity)

    @Query("SELECT * FROM trip_references ORDER BY sortOrder, title")
    fun observeTripReferences(): Flow<List<TripReferenceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putTripReference(value: TripReferenceEntity): Long

    @Query("SELECT * FROM trip_tasks")
    fun observeTripTasks(): Flow<List<TripTaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putTripTask(value: TripTaskEntity)

    @Query("DELETE FROM trip_tasks WHERE tripId = :tripId AND taskId = :taskId")
    suspend fun deleteTripTask(tripId: Long, taskId: Long)
}
