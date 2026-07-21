package com.geepscontrol.core.repository

import com.geepscontrol.core.database.ItineraryEntryEntity
import com.geepscontrol.core.database.AlarmProfileEntity
import com.geepscontrol.core.database.ChecklistEntity
import com.geepscontrol.core.database.ChecklistItemEntity
import com.geepscontrol.core.database.LifeEventEntity
import com.geepscontrol.core.database.LifeItemDao
import com.geepscontrol.core.database.LodgingDetailsEntity
import com.geepscontrol.core.database.MedicationEntity
import com.geepscontrol.core.database.MedicationOccurrenceEntity
import com.geepscontrol.core.database.ReminderEntity
import com.geepscontrol.core.database.RoutineEntity
import com.geepscontrol.core.database.RoutineOccurrenceEntity
import com.geepscontrol.core.database.StandaloneAlarmEntity
import com.geepscontrol.core.database.StandaloneTimerEntity
import com.geepscontrol.core.database.TimerHistoryEntity
import com.geepscontrol.core.database.TimerPresetEntity
import com.geepscontrol.core.database.TransportationDetailsEntity
import com.geepscontrol.core.database.TripEntity
import com.geepscontrol.core.database.TripReferenceEntity
import com.geepscontrol.core.database.TripTaskEntity
import kotlinx.coroutines.flow.Flow

class LifeItemRepository(
    private val dao: LifeItemDao,
) {
    fun checklists() = dao.observeChecklists()

    fun checklistItems() = dao.observeChecklistItems()

    suspend fun save(value: ChecklistEntity) = dao.putChecklist(value)

    suspend fun save(value: ChecklistItemEntity) = dao.putChecklistItem(value)

    suspend fun deleteChecklist(id: Long) = dao.deleteChecklist(id)

    fun reminders() = dao.observeReminders()

    fun routines() = dao.observeRoutines()

    fun routineOccurrences() = dao.observeRoutineOccurrences()

    fun medications() = dao.observeMedications()

    fun medicationOccurrences() = dao.observeMedicationOccurrences()

    fun events() = dao.observeEvents()

    fun alarms() = dao.observeStandaloneAlarms()

    fun presets() = dao.observeTimerPresets()

    fun alarmProfiles() = dao.observeAlarmProfiles()

    fun timers() = dao.observeStandaloneTimers()

    fun timerHistory() = dao.observeTimerHistory()

    suspend fun getReminder(id: Long) = dao.getReminder(id)

    suspend fun getMedication(id: Long) = dao.getMedication(id)

    fun trips() = dao.observeTrips()

    fun itineraryEntries() = dao.observeItineraryEntries()

    fun transportationDetails() = dao.observeTransportationDetails()

    fun lodgingDetails() = dao.observeLodgingDetails()

    fun tripReferences() = dao.observeTripReferences()

    fun tripTasks() = dao.observeTripTasks()

    suspend fun save(value: ReminderEntity) = dao.putReminder(value)

    suspend fun save(value: RoutineEntity) = dao.putRoutine(value)

    suspend fun save(value: RoutineOccurrenceEntity) = dao.putRoutineOccurrence(value)

    suspend fun save(value: MedicationEntity) = dao.putMedication(value)

    suspend fun save(value: MedicationOccurrenceEntity) = dao.putMedicationOccurrence(value)

    suspend fun save(value: LifeEventEntity) = dao.putEvent(value)

    suspend fun save(value: StandaloneAlarmEntity) = dao.putStandaloneAlarm(value)

    suspend fun save(value: TimerPresetEntity) = dao.putTimerPreset(value)

    suspend fun save(value: AlarmProfileEntity) = dao.putAlarmProfile(value)

    suspend fun getAlarmProfile(id: Long) = dao.getAlarmProfile(id)

    suspend fun removeAlarmProfile(id: Long) = dao.deleteAlarmProfile(id)

    suspend fun setDefaultAlarmProfile(id: Long) = dao.setDefaultAlarmProfile(id)

    suspend fun removePreset(id: Long) = dao.deleteTimerPreset(id)

    suspend fun save(value: StandaloneTimerEntity) = dao.putStandaloneTimer(value)

    suspend fun getTimer(id: Long) = dao.getStandaloneTimer(id)

    suspend fun addHistory(value: TimerHistoryEntity) = dao.addTimerHistory(value)

    suspend fun removeHistoryBefore(dayStart: Long) = dao.deleteTimerHistoryBefore(dayStart)

    suspend fun save(value: TripEntity) = dao.putTrip(value)

    suspend fun save(value: ItineraryEntryEntity) = dao.putItineraryEntry(value)

    suspend fun save(value: TransportationDetailsEntity) = dao.putTransportationDetails(value)

    suspend fun save(value: LodgingDetailsEntity) = dao.putLodgingDetails(value)

    suspend fun save(value: TripReferenceEntity) = dao.putTripReference(value)

    suspend fun save(value: TripTaskEntity) = dao.putTripTask(value)

    suspend fun unlinkTripTask(tripId: Long, taskId: Long) = dao.deleteTripTask(tripId, taskId)

    suspend fun moveItinerary(
        id: Long,
        startAt: Long?,
        endAt: Long?,
        now: Long,
    ) = dao.moveItineraryEntry(id, startAt, endAt, now)

    suspend fun reminderStatus(
        id: Long,
        status: String,
        now: Long,
    ) = dao.setReminderStatus(id, status, now)

    suspend fun snoozeReminder(
        id: Long,
        until: Long,
        now: Long,
    ) = dao.snoozeReminder(id, until, now)

    suspend fun setRoutineActive(
        id: Long,
        active: Boolean,
        now: Long,
    ) = dao.setRoutineActive(id, active, now)

    suspend fun setMedicationActive(
        id: Long,
        active: Boolean,
        now: Long,
    ) = dao.setMedicationActive(id, active, now)

    suspend fun setAlarmEnabled(
        id: Long,
        enabled: Boolean,
        now: Long,
    ) = dao.setStandaloneAlarmEnabled(id, enabled, now)

    suspend fun getAlarm(id: Long) = dao.getStandaloneAlarm(id)

    suspend fun removeAlarm(id: Long) = dao.deleteStandaloneAlarm(id)

    suspend fun snoozeAlarm(
        id: Long,
        until: Long,
        now: Long,
    ) = dao.snoozeStandaloneAlarm(id, until, now)

    suspend fun removeTimer(id: Long) = dao.deleteStandaloneTimer(id)
}
