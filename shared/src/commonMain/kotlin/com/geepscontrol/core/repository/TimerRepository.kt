package com.geepscontrol.core.repository

import com.geepscontrol.core.database.ActiveTimerEntity
import com.geepscontrol.core.database.AlarmEntity
import com.geepscontrol.core.database.TimerDao
import com.geepscontrol.core.database.WorkSessionEntity
import com.geepscontrol.core.domain.ActiveTimer
import com.geepscontrol.core.domain.AlarmDefinition
import com.geepscontrol.core.domain.WorkSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface TimerRepository {
    fun observeActive(): Flow<ActiveTimer?>

    fun observeSessions(): Flow<List<WorkSession>>

    fun observeAlarms(): Flow<List<AlarmDefinition>>

    suspend fun saveActive(timer: ActiveTimer)

    suspend fun clearActive()

    suspend fun addSession(session: WorkSession): Long

    suspend fun addAlarm(alarm: AlarmDefinition): Long

    suspend fun saveAlarm(alarm: AlarmDefinition)

    suspend fun getAlarm(id: Long): AlarmDefinition?

    suspend fun disableAlarm(id: Long)

    suspend fun disableAlarmsForTask(taskId: Long)
}

class RoomTimerRepository(
    private val dao: TimerDao,
) : TimerRepository {
    override fun observeActive() = dao.observeActive().map { it?.toDomain() }

    override fun observeSessions() = dao.observeSessions().map { rows -> rows.map { it.toDomain() } }

    override fun observeAlarms() = dao.observeEnabledAlarms().map { rows -> rows.map { it.toDomain() } }

    override suspend fun saveActive(timer: ActiveTimer) = dao.putActive(timer.toEntity())

    override suspend fun clearActive() = dao.clearActive()

    override suspend fun addSession(session: WorkSession) = dao.addSession(session.toEntity())

    override suspend fun addAlarm(alarm: AlarmDefinition) = dao.addAlarm(alarm.toEntity())

    override suspend fun saveAlarm(alarm: AlarmDefinition) = dao.putAlarm(alarm.toEntity())

    override suspend fun getAlarm(id: Long) = dao.getAlarm(id)?.toDomain()

    override suspend fun disableAlarm(id: Long) = dao.disableAlarm(id)

    override suspend fun disableAlarmsForTask(taskId: Long) = dao.disableForTask(taskId)
}

private fun ActiveTimer.toEntity() = ActiveTimerEntity(id, taskId, mode.name, state.name, startedAt, resumedAt, accumulatedMillis, intendedDurationMillis, updatedAt)

private fun ActiveTimerEntity.toDomain() = ActiveTimer(id, taskId, enumValueOf(mode), enumValueOf(state), startedAt, resumedAt, accumulatedMillis, intendedDurationMillis, updatedAt)

private fun WorkSession.toEntity() = WorkSessionEntity(id, taskId, projectId, startedAt, endedAt, pausedMillis, activeMillis, plannedMillis, mode.name, completion.name, notes, accomplishment, interruptions, focusRating)

private fun WorkSessionEntity.toDomain() = WorkSession(id, taskId, projectId, startedAt, endedAt, pausedMillis, activeMillis, plannedMillis, enumValueOf(mode), enumValueOf(completion), notes, accomplishment, interruptions, focusRating)

private fun AlarmDefinition.toEntity() = AlarmEntity(id, taskId, projectId, triggerAt, label, vibration, sound, enabled)

private fun AlarmEntity.toDomain() = AlarmDefinition(id, taskId, projectId, triggerAt, label, vibration, sound, enabled)
