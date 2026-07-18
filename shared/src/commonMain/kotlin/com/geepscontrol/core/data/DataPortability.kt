package com.geepscontrol.core.data

import com.geepscontrol.core.database.ItineraryEntryEntity
import com.geepscontrol.core.database.AlarmProfileEntity
import com.geepscontrol.core.database.LifeEventEntity
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
import com.geepscontrol.core.domain.AlarmDefinition
import com.geepscontrol.core.domain.EnergyRequirement
import com.geepscontrol.core.domain.Priority
import com.geepscontrol.core.domain.Project
import com.geepscontrol.core.domain.ProjectStatus
import com.geepscontrol.core.domain.ReviewRecord
import com.geepscontrol.core.domain.ReviewType
import com.geepscontrol.core.domain.SessionCompletion
import com.geepscontrol.core.domain.Task
import com.geepscontrol.core.domain.TaskContext
import com.geepscontrol.core.domain.TaskStatus
import com.geepscontrol.core.domain.TaskTag
import com.geepscontrol.core.domain.TimerMode
import com.geepscontrol.core.domain.WorkSession
import com.geepscontrol.core.repository.ContextRepository
import com.geepscontrol.core.repository.LifeItemRepository
import com.geepscontrol.core.repository.ProjectRepository
import com.geepscontrol.core.repository.ReviewRepository
import com.geepscontrol.core.repository.TagRepository
import com.geepscontrol.core.repository.TaskRepository
import com.geepscontrol.core.repository.TimerRepository
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class ExportBundle(
    val format: String = "life-minder",
    val version: Int = 4,
    val exportedAt: Long,
    val projects: List<ProjectDto>,
    val tasks: List<TaskDto>,
    val contexts: List<ContextDto>,
    val tags: List<TagDto>,
    val sessions: List<SessionDto>,
    val reviews: List<ReviewDto>,
    val alarms: List<AlarmDto>,
    val reminders: List<ReminderEntity> = emptyList(),
    val routines: List<RoutineEntity> = emptyList(),
    val routineOccurrences: List<RoutineOccurrenceEntity> = emptyList(),
    val medications: List<MedicationEntity> = emptyList(),
    val medicationOccurrences: List<MedicationOccurrenceEntity> = emptyList(),
    val events: List<LifeEventEntity> = emptyList(),
    val standaloneAlarms: List<StandaloneAlarmEntity> = emptyList(),
    val timerPresets: List<TimerPresetEntity> = emptyList(),
    val standaloneTimers: List<StandaloneTimerEntity> = emptyList(),
    val timerHistory: List<TimerHistoryEntity> = emptyList(),
    val alarmProfiles: List<AlarmProfileEntity> = emptyList(),
    val trips: List<TripEntity> = emptyList(),
    val itineraryEntries: List<ItineraryEntryEntity> = emptyList(),
    val transportationDetails: List<TransportationDetailsEntity> = emptyList(),
    val lodgingDetails: List<LodgingDetailsEntity> = emptyList(),
    val tripReferences: List<TripReferenceEntity> = emptyList(),
    val tripTasks: List<TripTaskEntity> = emptyList(),
)

@Serializable data class ProjectDto(
    val id: Long,
    val name: String,
    val description: String,
    val outcome: String,
    val status: String,
    val priority: String,
    val createdAt: Long,
    val updatedAt: Long,
    val colorArgb: Long = 0xff6750a4,
)

@Serializable data class TaskDto(
    val id: Long,
    val projectId: Long?,
    val title: String,
    val description: String,
    val status: String,
    val priority: String,
    val estimatedMinutes: Int?,
    val energy: String,
    val dueAt: Long?,
    val reminderAt: Long?,
    val recurrenceRule: String?,
    val recurrenceParentId: Long?,
    val createdAt: Long,
    val updatedAt: Long,
    val completedAt: Long?,
    val manualOrder: Long,
    val contextIds: List<Long>,
    val tagIds: List<Long>,
    val dependencyIds: List<Long>,
)

@Serializable data class ContextDto(
    val id: Long,
    val name: String,
    val system: Boolean,
)

@Serializable data class TagDto(
    val id: Long,
    val name: String,
)

@Serializable data class SessionDto(
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

@Serializable data class ReviewDto(
    val type: String,
    val periodStart: Long,
    val periodEnd: Long,
    val wentWell: String,
    val friction: String,
    val nextPriority: String,
    val notes: String,
    val completedAt: Long,
)

@Serializable data class AlarmDto(
    val taskId: Long?,
    val projectId: Long?,
    val triggerAt: Long,
    val label: String,
    val vibration: Boolean,
    val sound: Boolean,
    val enabled: Boolean,
)

data class ImportResult(
    val projects: Int,
    val tasks: Int,
    val sessions: Int,
)

@Serializable
data class TripPackage(
    val format: String = "life-minder-trip",
    val version: Int = 1,
    val exportedAt: Long,
    val trip: TripEntity,
    val entries: List<ItineraryEntryEntity>,
    val transportation: List<TransportationDetailsEntity>,
    val lodging: List<LodgingDetailsEntity>,
    val references: List<TripReferenceEntity>,
    val associatedTaskIds: List<Long>,
)

interface DataFileGateway {
    suspend fun writeExport(
        fileName: String,
        mimeType: String,
        content: String,
    ): String

    suspend fun readImport(fileName: String): String?

    suspend fun backupDatabase(): String

    suspend fun restoreDatabase(): String

    suspend fun deleteAllLocalData(): String
}

class DataPortabilityService(
    private val projects: ProjectRepository,
    private val tasks: TaskRepository,
    private val contexts: ContextRepository,
    private val tags: TagRepository,
    private val timers: TimerRepository,
    private val reviews: ReviewRepository,
    private val life: LifeItemRepository? = null,
    private val json: Json =
        Json {
            prettyPrint = true
            ignoreUnknownKeys = true
            encodeDefaults = true
        },
) {
    suspend fun exportTripJson(
        tripId: Long,
        now: Long,
        fullBackup: Boolean = false,
    ): String {
        val repository = requireNotNull(life) { "Trip storage is unavailable" }
        val trip = repository.trips().first().firstOrNull { it.id == tripId } ?: error("Trip not found")
        val entries = repository.itineraryEntries().first().filter { it.tripId == tripId }
        val entryIds = entries.mapTo(mutableSetOf()) { it.id }
        return json.encodeToString(
            TripPackage(
                exportedAt = now,
                trip = trip,
                entries = entries,
                transportation = repository.transportationDetails().first().filter { it.itineraryEntryId in entryIds },
                lodging =
                    repository
                        .lodgingDetails()
                        .first()
                        .filter { it.itineraryEntryId in entryIds }
                        .map { if (fullBackup) it else it.copy(accessCode = "") },
                references =
                    repository
                        .tripReferences()
                        .first()
                        .filter { it.tripId == tripId }
                        .map { if (fullBackup || !it.sensitive) it else it.copy(content = "") },
                associatedTaskIds =
                    repository
                        .tripTasks()
                        .first()
                        .filter { it.tripId == tripId }
                        .map { it.taskId },
            ),
        )
    }

    suspend fun importTripAsNew(content: String): Long {
        val repository = requireNotNull(life) { "Trip storage is unavailable" }
        val value = json.decodeFromString<TripPackage>(content)
        require(value.format == "life-minder-trip" && value.version == 1 && value.trip.name.isNotBlank()) { "Unsupported Life Minder trip package" }
        require(value.entries.all { it.tripId == value.trip.id }) { "Trip package contains an unrelated entry" }
        val oldEntryIds = value.entries.mapTo(mutableSetOf()) { it.id }
        require(value.transportation.all { it.itineraryEntryId in oldEntryIds } && value.lodging.all { it.itineraryEntryId in oldEntryIds } && value.references.all { it.tripId == value.trip.id }) { "Trip package contains a missing relationship" }
        val tripId = repository.save(value.trip.copy(id = 0, name = "${value.trip.name} (imported)"))
        val entryMap = mutableMapOf<Long, Long>()
        value.entries.forEach { entry -> entryMap[entry.id] = repository.save(entry.copy(id = 0, tripId = tripId)) }
        value.transportation.forEach { row -> entryMap[row.itineraryEntryId]?.let { repository.save(row.copy(itineraryEntryId = it)) } }
        value.lodging.forEach { row -> entryMap[row.itineraryEntryId]?.let { repository.save(row.copy(itineraryEntryId = it)) } }
        value.references.forEach { repository.save(it.copy(id = 0, tripId = tripId)) }
        value.associatedTaskIds.forEach { taskId -> if (tasks.get(taskId) != null) repository.save(TripTaskEntity(tripId, taskId)) }
        return tripId
    }

    suspend fun exportJson(
        now: Long,
        fullBackup: Boolean = false,
    ): String {
        val portableLife = life
        val bundle =
            ExportBundle(
                exportedAt = now,
                projects = projects.observeProjects().first().map { ProjectDto(it.id, it.name, it.description, it.desiredOutcome, it.status.name, it.priority.name, it.createdAt, it.updatedAt, it.colorArgb) },
                tasks = tasks.observeTasks().first().map { TaskDto(it.id, it.projectId, it.title, it.description, it.status.name, it.priority.name, it.estimatedMinutes, it.energyRequirement.name, it.dueAt, it.reminderAt, it.recurrenceRule, it.recurrenceParentId, it.createdAt, it.updatedAt, it.completedAt, it.manualOrder, it.contextIds.toList(), it.tagIds.toList(), it.dependencyIds.toList()) },
                contexts = contexts.observeContexts().first().map { ContextDto(it.id, it.name, it.isSystemDefault) },
                tags = tags.observeTags().first().map { TagDto(it.id, it.name) },
                sessions = timers.observeSessions().first().map { SessionDto(it.taskId, it.projectId, it.startedAt, it.endedAt, it.pausedMillis, it.activeMillis, it.plannedMillis, it.mode.name, it.completion.name, it.notes, it.accomplishment, it.interruptions, it.focusRating) },
                reviews = reviews.observeReviews().first().map { ReviewDto(it.type.name, it.periodStart, it.periodEnd, it.wentWell, it.friction, it.nextPriority, it.notes, it.completedAt) },
                alarms = timers.observeAlarms().first().map { AlarmDto(it.taskId, it.projectId, it.triggerAt, it.label, it.vibration, it.sound, it.enabled) },
                reminders = portableLife?.reminders()?.first().orEmpty(),
                routines = portableLife?.routines()?.first().orEmpty(),
                routineOccurrences = portableLife?.routineOccurrences()?.first().orEmpty(),
                medications = portableLife?.medications()?.first().orEmpty(),
                medicationOccurrences = portableLife?.medicationOccurrences()?.first().orEmpty(),
                events = portableLife?.events()?.first().orEmpty(),
                standaloneAlarms = portableLife?.alarms()?.first().orEmpty(),
                timerPresets = portableLife?.presets()?.first().orEmpty(),
                standaloneTimers = portableLife?.timers()?.first().orEmpty(),
                timerHistory = portableLife?.timerHistory()?.first().orEmpty(),
                alarmProfiles = portableLife?.alarmProfiles()?.first().orEmpty(),
                trips = portableLife?.trips()?.first().orEmpty(),
                itineraryEntries = portableLife?.itineraryEntries()?.first().orEmpty(),
                transportationDetails = portableLife?.transportationDetails()?.first().orEmpty(),
                lodgingDetails =
                    portableLife
                        ?.lodgingDetails()
                        ?.first()
                        .orEmpty()
                        .map { if (fullBackup) it else it.copy(accessCode = "") },
                tripReferences =
                    portableLife
                        ?.tripReferences()
                        ?.first()
                        .orEmpty()
                        .map { if (fullBackup || !it.sensitive) it else it.copy(content = "") },
                tripTasks = portableLife?.tripTasks()?.first().orEmpty(),
            )
        return json.encodeToString(bundle)
    }

    fun validate(content: String): ExportBundle = decodeAndValidateExport(content, json)

    suspend fun importValidated(content: String): ImportResult {
        val bundle = validate(content)
        val projectMap = mutableMapOf<Long, Long>()
        bundle.projects.forEach { row -> projectMap[row.id] = projects.save(Project(name = row.name, description = row.description, desiredOutcome = row.outcome, status = enumValueOf(row.status), priority = enumValueOf(row.priority), colorArgb = row.colorArgb)) }
        val contextMap = mutableMapOf<Long, Long>()
        val existingContexts = contexts.observeContexts().first().associateBy { it.name.lowercase() }
        bundle.contexts.forEach { row -> contextMap[row.id] = existingContexts[row.name.lowercase()]?.id ?: contexts.save(TaskContext(name = row.name, isSystemDefault = row.system)) }
        val tagMap = mutableMapOf<Long, Long>()
        val existingTags = tags.observeTags().first().associateBy { it.name.lowercase() }
        bundle.tags.forEach { row -> tagMap[row.id] = existingTags[row.name.lowercase()]?.id ?: tags.save(TaskTag(name = row.name)) }
        val taskMap = mutableMapOf<Long, Long>()
        bundle.tasks.forEach { row -> taskMap[row.id] = tasks.save(row.toTask(projectMap, contextMap, tagMap, emptyMap())) }
        bundle.tasks.forEach { row -> tasks.save(row.toTask(projectMap, contextMap, tagMap, taskMap).copy(id = taskMap.getValue(row.id))) }
        bundle.sessions.forEach { row -> timers.addSession(WorkSession(taskId = row.taskId?.let(taskMap::get), projectId = row.projectId?.let(projectMap::get), startedAt = row.startedAt, endedAt = row.endedAt, pausedMillis = row.pausedMillis, activeMillis = row.activeMillis, plannedMillis = row.plannedMillis, mode = enumValueOf(row.mode), completion = enumValueOf(row.completion), notes = row.notes, accomplishment = row.accomplishment, interruptions = row.interruptions, focusRating = row.focusRating)) }
        bundle.reviews.forEach { row -> reviews.save(ReviewRecord(type = enumValueOf(row.type), periodStart = row.periodStart, periodEnd = row.periodEnd, wentWell = row.wentWell, friction = row.friction, nextPriority = row.nextPriority, notes = row.notes, completedAt = row.completedAt)) }
        bundle.alarms.forEach { row -> timers.addAlarm(AlarmDefinition(taskId = row.taskId?.let(taskMap::get), projectId = row.projectId?.let(projectMap::get), triggerAt = row.triggerAt, label = row.label, vibration = row.vibration, sound = row.sound, enabled = row.enabled)) }
        life?.let { repo ->
            val routineMap = mutableMapOf<Long, Long>()
            bundle.routines.forEach { row -> routineMap[row.id] = repo.save(row.copy(id = 0)) }
            bundle.routineOccurrences.forEach { row -> routineMap[row.routineId]?.let { repo.save(row.copy(id = 0, routineId = it)) } }
            val medicationMap = mutableMapOf<Long, Long>()
            bundle.medications.forEach { row -> medicationMap[row.id] = repo.save(row.copy(id = 0)) }
            bundle.medicationOccurrences.forEach { row -> medicationMap[row.medicationId]?.let { repo.save(row.copy(id = 0, medicationId = it)) } }
            bundle.reminders.forEach { row -> repo.save(row.copy(id = 0, taskId = row.taskId?.let(taskMap::get))) }
            bundle.events.forEach { repo.save(it.copy(id = 0)) }
            val profileMap = mutableMapOf<Long, Long>()
            bundle.alarmProfiles.forEach { row -> profileMap[row.id] = repo.save(row.copy(id = 0, builtInKey = null)) }
            bundle.standaloneAlarms.forEach { row -> repo.save(row.copy(id = 0, profileId = row.profileId?.let(profileMap::get))) }
            val presetMap = mutableMapOf<Long, Long>()
            bundle.timerPresets.forEach { row -> presetMap[row.id] = repo.save(row.copy(id = 0)) }
            bundle.standaloneTimers.forEach { row -> repo.save(row.copy(id = 0, presetId = row.presetId?.let(presetMap::get))) }
            bundle.timerHistory.forEach { row -> repo.addHistory(row.copy(id = 0, presetId = row.presetId?.let(presetMap::get))) }
            val tripMap = mutableMapOf<Long, Long>()
            bundle.trips.forEach { row -> tripMap[row.id] = repo.save(row.copy(id = 0)) }
            val entryMap = mutableMapOf<Long, Long>()
            bundle.itineraryEntries.forEach { row -> tripMap[row.tripId]?.let { entryMap[row.id] = repo.save(row.copy(id = 0, tripId = it)) } }
            bundle.transportationDetails.forEach { row -> entryMap[row.itineraryEntryId]?.let { repo.save(row.copy(itineraryEntryId = it)) } }
            bundle.lodgingDetails.forEach { row -> entryMap[row.itineraryEntryId]?.let { repo.save(row.copy(itineraryEntryId = it)) } }
            bundle.tripReferences.forEach { row -> tripMap[row.tripId]?.let { repo.save(row.copy(id = 0, tripId = it)) } }
            bundle.tripTasks.forEach { row -> tripMap[row.tripId]?.let { trip -> taskMap[row.taskId]?.let { task -> repo.save(TripTaskEntity(trip, task)) } } }
        }
        return ImportResult(bundle.projects.size, bundle.tasks.size, bundle.sessions.size)
    }

    suspend fun exportCsv(): String {
        fun cell(value: Any?) = "\"${value?.toString().orEmpty().replace("\"", "\"\"")}\""
        return buildString {
            appendLine("id,title,project_id,status,priority,estimated_minutes,due_at,completed_at")
            tasks.observeTasks().first().forEach { appendLine(listOf(it.id, it.title, it.projectId, it.status, it.priority, it.estimatedMinutes, it.dueAt, it.completedAt).joinToString(",", transform = ::cell)) }
        }
    }
}

fun decodeAndValidateExport(
    content: String,
    json: Json = Json { ignoreUnknownKeys = true },
): ExportBundle {
    val bundle = json.decodeFromString<ExportBundle>(content)
    require(bundle.format == "life-minder" && bundle.version in 1..4) { "Unsupported Life Minder export" }
    require(bundle.projects.all { it.name.isNotBlank() } && bundle.tasks.all { it.title.isNotBlank() }) { "Export contains blank required names" }
    require(
        bundle.projects
            .map { it.id }
            .distinct()
            .size == bundle.projects.size && bundle.tasks
            .map { it.id }
            .distinct()
            .size == bundle.tasks.size,
    ) { "Export contains duplicate IDs" }
    val projectIds = bundle.projects.mapTo(mutableSetOf()) { it.id }
    val taskIds = bundle.tasks.mapTo(mutableSetOf()) { it.id }
    val contextIds = bundle.contexts.mapTo(mutableSetOf()) { it.id }
    val tagIds = bundle.tags.mapTo(mutableSetOf()) { it.id }
    require(bundle.tasks.all { it.projectId == null || it.projectId in projectIds }) { "Task references a missing project" }
    require(bundle.tasks.all { row -> row.contextIds.all { it in contextIds } && row.tagIds.all { it in tagIds } && row.dependencyIds.all { it in taskIds } && (row.recurrenceParentId == null || row.recurrenceParentId in taskIds) }) { "Task contains a missing relationship" }
    require(bundle.sessions.all { (it.taskId == null || it.taskId in taskIds) && (it.projectId == null || it.projectId in projectIds) }) { "Session contains a missing relationship" }
    require(bundle.alarms.all { (it.taskId == null || it.taskId in taskIds) && (it.projectId == null || it.projectId in projectIds) }) { "Alarm contains a missing relationship" }
    val routineIds = bundle.routines.mapTo(mutableSetOf()) { it.id }
    require(bundle.routineOccurrences.all { it.routineId in routineIds }) { "Routine occurrence references a missing routine" }
    val medicationIds = bundle.medications.mapTo(mutableSetOf()) { it.id }
    require(bundle.medicationOccurrences.all { it.medicationId in medicationIds }) { "Medication occurrence references a missing medication" }
    val profileIds = bundle.alarmProfiles.mapTo(mutableSetOf()) { it.id }
    require(bundle.standaloneAlarms.all { it.profileId == null || it.profileId in profileIds }) { "Alarm references a missing profile" }
    val tripIds = bundle.trips.mapTo(mutableSetOf()) { it.id }
    require(bundle.itineraryEntries.all { it.tripId in tripIds } && bundle.tripReferences.all { it.tripId in tripIds } && bundle.tripTasks.all { it.tripId in tripIds && it.taskId in taskIds }) { "Trip data contains a missing relationship" }
    val entryIds = bundle.itineraryEntries.mapTo(mutableSetOf()) { it.id }
    require(bundle.transportationDetails.all { it.itineraryEntryId in entryIds } && bundle.lodgingDetails.all { it.itineraryEntryId in entryIds }) { "Itinerary details reference a missing entry" }
    require(
        bundle.contexts
            .map { it.name.lowercase() }
            .distinct()
            .size == bundle.contexts.size && bundle.tags
            .map { it.name.lowercase() }
            .distinct()
            .size == bundle.tags.size,
    ) { "Export contains duplicate context or tag names" }
    bundle.projects.forEach {
        enumValue<ProjectStatus>(it.status)
        enumValue<Priority>(it.priority)
    }
    bundle.tasks.forEach {
        enumValue<TaskStatus>(it.status)
        enumValue<Priority>(it.priority)
        enumValue<EnergyRequirement>(it.energy)
    }
    bundle.sessions.forEach {
        enumValue<TimerMode>(it.mode)
        enumValue<SessionCompletion>(it.completion)
    }
    bundle.reviews.forEach { enumValue<ReviewType>(it.type) }
    return bundle
}

private inline fun <reified T : Enum<T>> enumValue(value: String): T = enumValues<T>().firstOrNull { it.name == value } ?: error("Unknown ${T::class.simpleName}: $value")

private fun TaskDto.toTask(
    projects: Map<Long, Long>,
    contexts: Map<Long, Long>,
    tags: Map<Long, Long>,
    tasks: Map<Long, Long>,
) = Task(projectId = projectId?.let(projects::get), title = title, description = description, status = enumValueOf(status), priority = enumValueOf(priority), estimatedMinutes = estimatedMinutes, energyRequirement = enumValueOf(energy), dueAt = dueAt, reminderAt = reminderAt, recurrenceRule = recurrenceRule, recurrenceParentId = recurrenceParentId?.let(tasks::get), createdAt = createdAt, updatedAt = updatedAt, completedAt = completedAt, manualOrder = manualOrder, contextIds = contextIds.mapNotNull(contexts::get).toSet(), tagIds = tagIds.mapNotNull(tags::get).toSet(), dependencyIds = dependencyIds.mapNotNull(tasks::get).toSet())
