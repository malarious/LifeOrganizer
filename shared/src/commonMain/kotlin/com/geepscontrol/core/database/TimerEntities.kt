package com.geepscontrol.core.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "active_timers")
data class ActiveTimerEntity(
    @PrimaryKey val id: Long,
    val taskId: Long?,
    val mode: String,
    val state: String,
    val startedAt: Long,
    val resumedAt: Long,
    val accumulatedMillis: Long,
    val intendedDurationMillis: Long?,
    val updatedAt: Long,
)

@Entity(tableName = "work_sessions")
data class WorkSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: Long?,
    val projectId: Long?,
    val startedAt: Long,
    val endedAt: Long,
    val pausedMillis: Long,
    val activeMillis: Long,
    val plannedMillis: Long?,
    val mode: String,
    val completion: String,
    val notes: String,
    val accomplishment: String,
    val interruptions: Int,
    val focusRating: Int?,
)

@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: Long?,
    val projectId: Long?,
    val triggerAt: Long,
    val label: String,
    val vibration: Boolean,
    val sound: Boolean,
    val enabled: Boolean,
)

@Dao
interface TimerDao {
    @Query("SELECT * FROM active_timers WHERE id = 1")
    fun observeActive(): Flow<ActiveTimerEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putActive(timer: ActiveTimerEntity)

    @Query("DELETE FROM active_timers")
    suspend fun clearActive()

    @Insert suspend fun addSession(session: WorkSessionEntity): Long

    @Query("SELECT * FROM work_sessions ORDER BY startedAt DESC")
    fun observeSessions(): Flow<List<WorkSessionEntity>>

    @Insert suspend fun addAlarm(alarm: AlarmEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putAlarm(alarm: AlarmEntity)

    @Query("SELECT * FROM alarms WHERE enabled = 1 ORDER BY triggerAt")
    fun observeEnabledAlarms(): Flow<List<AlarmEntity>>

    @Query("SELECT * FROM alarms WHERE id = :id")
    suspend fun getAlarm(id: Long): AlarmEntity?

    @Query("UPDATE alarms SET enabled = 0 WHERE id = :id")
    suspend fun disableAlarm(id: Long)

    @Query("UPDATE alarms SET enabled = 0 WHERE taskId = :taskId")
    suspend fun disableForTask(taskId: Long)
}
