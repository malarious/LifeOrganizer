package com.geepscontrol.core.repository

import com.geepscontrol.core.database.ContextDao
import com.geepscontrol.core.database.ContextEntity
import com.geepscontrol.core.database.ProjectDao
import com.geepscontrol.core.database.ProjectEntity
import com.geepscontrol.core.database.TagDao
import com.geepscontrol.core.database.TagEntity
import com.geepscontrol.core.database.TaskDao
import com.geepscontrol.core.database.TaskEntity
import com.geepscontrol.core.database.TaskWithMetadata
import com.geepscontrol.core.domain.EnergyRequirement
import com.geepscontrol.core.domain.Priority
import com.geepscontrol.core.domain.Project
import com.geepscontrol.core.domain.ProjectStatus
import com.geepscontrol.core.domain.RecurrenceEngine
import com.geepscontrol.core.domain.RecurrenceRule
import com.geepscontrol.core.domain.Task
import com.geepscontrol.core.domain.TaskContext
import com.geepscontrol.core.domain.TaskStatus
import com.geepscontrol.core.domain.TaskTag
import com.geepscontrol.core.domain.currentTimeMillis
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class RoomProjectRepository(
    private val dao: ProjectDao,
) : ProjectRepository {
    override fun observeProjects() = dao.observeActive().map { rows -> rows.map(ProjectEntity::toDomain) }

    override suspend fun get(id: Long) = dao.get(id)?.toDomain()

    override suspend fun save(project: Project): Long {
        val now = currentTimeMillis()
        val entity = project.copy(createdAt = project.createdAt.takeIf { it > 0 } ?: now, updatedAt = now).toEntity()
        return if (entity.id == 0L) {
            dao.insert(entity)
        } else {
            dao.update(entity)
            entity.id
        }
    }

    override suspend fun delete(project: Project) = dao.delete(project.toEntity())
}

class RoomTaskRepository(
    private val dao: TaskDao,
) : TaskRepository {
    override fun observeInbox() = dao.observeInbox().map(::mapTasks)

    override fun observeTasks() = dao.observeAllOpenAndCompleted().map(::mapTasks)

    override fun observeForProject(projectId: Long) = dao.observeForProject(projectId).map(::mapTasks)

    override suspend fun get(id: Long): Task? {
        val rows = dao.observeAllOpenAndCompleted().first()
        val byId = rows.associateBy { it.task.id }
        return byId[id]?.toDomain(byId)
    }

    override suspend fun save(task: Task): Long {
        val now = currentTimeMillis()
        val entity = task.copy(createdAt = task.createdAt.takeIf { it > 0 } ?: now, updatedAt = now).toEntity()
        val id =
            if (entity.id == 0L) {
                dao.insert(entity)
            } else {
                dao.update(entity)
                entity.id
            }
        val graph =
            dao.observeAllOpenAndCompleted().first().associate { row ->
                row.task.id to row.dependencies.mapTo(mutableSetOf()) { it.dependsOnTaskId }
            }
        val safeDependencies =
            task.dependencyIds
                .filterNot { dependencyId ->
                    dependencyId == id || reaches(dependencyId, id, graph)
                }.toSet()
        dao.replaceMetadata(id, task.contextIds, task.tagIds, safeDependencies)
        return id
    }

    override suspend fun complete(id: Long): Task? {
        val previous = get(id) ?: return null
        val completedAt = currentTimeMillis()
        save(previous.copy(status = TaskStatus.COMPLETED, completedAt = completedAt))
        RecurrenceRule.decode(previous.recurrenceRule)?.let { rule ->
            val nextAt = RecurrenceEngine.nextTimestamp(rule, previous.dueAt, completedAt)
            save(previous.copy(id = 0, status = TaskStatus.READY, dueAt = nextAt, reminderAt = previous.reminderAt?.let { nextAt - ((previous.dueAt ?: nextAt) - it) }, recurrenceParentId = previous.recurrenceParentId ?: previous.id, createdAt = 0, updatedAt = 0, completedAt = null))
        }
        return previous
    }

    override suspend fun restore(task: Task) {
        dao.upsert(task.toEntity())
        dao.replaceMetadata(task.id, task.contextIds, task.tagIds, task.dependencyIds)
    }

    override suspend fun delete(task: Task) = dao.delete(task.toEntity())

    override suspend fun isBlocked(id: Long) = dao.incompleteDependencyCount(id) > 0
}

class RoomContextRepository(
    private val dao: ContextDao,
) : ContextRepository {
    override fun observeContexts() = dao.observeAll().map { rows -> rows.map { TaskContext(it.id, it.name, it.isSystemDefault) } }

    override suspend fun save(context: TaskContext): Long {
        val row = ContextEntity(context.id, context.name.trim(), context.isSystemDefault)
        return if (row.id == 0L) {
            dao.insert(row)
        } else {
            dao.update(row)
            row.id
        }
    }

    override suspend fun delete(context: TaskContext) {
        if (!context.isSystemDefault) dao.delete(ContextEntity(context.id, context.name, false))
    }
}

class RoomTagRepository(
    private val dao: TagDao,
) : TagRepository {
    override fun observeTags() = dao.observeAll().map { rows -> rows.map { TaskTag(it.id, it.name) } }

    override suspend fun save(tag: TaskTag): Long {
        val row = TagEntity(tag.id, tag.name.trim())
        return if (row.id == 0L) {
            dao.insert(row)
        } else {
            dao.update(row)
            row.id
        }
    }

    override suspend fun delete(tag: TaskTag) = dao.delete(TagEntity(tag.id, tag.name))
}

private fun mapTasks(rows: List<TaskWithMetadata>): List<Task> {
    val byId = rows.associateBy { it.task.id }
    return rows.map { it.toDomain(byId) }
}

private fun reaches(
    start: Long,
    target: Long,
    graph: Map<Long, Set<Long>>,
    visited: MutableSet<Long> = mutableSetOf(),
): Boolean {
    if (start == target) return true
    if (!visited.add(start)) return false
    return graph[start].orEmpty().any { reaches(it, target, graph, visited) }
}

private fun TaskWithMetadata.toDomain(all: Map<Long, TaskWithMetadata>): Task {
    val blocking =
        dependencies
            .mapNotNull { dependency -> all[dependency.dependsOnTaskId]?.task }
            .filter { it.status != TaskStatus.COMPLETED.name }
    return task.toDomain().copy(
        contextIds = contexts.mapTo(mutableSetOf()) { it.id },
        tagIds = tags.mapTo(mutableSetOf()) { it.id },
        dependencyIds = dependencies.mapTo(mutableSetOf()) { it.dependsOnTaskId },
        isBlocked = blocking.isNotEmpty(),
        blockingTaskTitles = blocking.map { it.title },
    )
}

private fun ProjectEntity.toDomain() =
    Project(
        id = id,
        name = name,
        description = description,
        desiredOutcome = desiredOutcome,
        status = enumValueOrDefault(status, ProjectStatus.ACTIVE),
        priority = enumValueOrDefault(priority, Priority.NORMAL),
        colorArgb = colorArgb,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

private fun Project.toEntity() =
    ProjectEntity(
        id = id,
        name = name,
        description = description,
        desiredOutcome = desiredOutcome,
        status = status.name,
        priority = priority.name,
        colorArgb = colorArgb,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

private fun TaskEntity.toDomain() =
    Task(
        id = id,
        projectId = projectId,
        title = title,
        description = description,
        status = enumValueOrDefault(status, TaskStatus.INBOX),
        priority = enumValueOrDefault(priority, Priority.NORMAL),
        estimatedMinutes = estimatedMinutes,
        energyRequirement = enumValueOrDefault(energyRequirement, EnergyRequirement.MEDIUM),
        dueAt = dueAt,
        reminderAt = reminderAt,
        recurrenceRule = recurrenceRule,
        recurrenceParentId = recurrenceParentId,
        createdAt = createdAt,
        updatedAt = updatedAt,
        completedAt = completedAt,
        manualOrder = manualOrder,
    )

private fun Task.toEntity() =
    TaskEntity(
        id = id,
        projectId = projectId,
        title = title,
        description = description,
        status = status.name,
        priority = priority.name,
        estimatedMinutes = estimatedMinutes,
        energyRequirement = energyRequirement.name,
        dueAt = dueAt,
        reminderAt = reminderAt,
        recurrenceRule = recurrenceRule,
        recurrenceParentId = recurrenceParentId,
        createdAt = createdAt,
        updatedAt = updatedAt,
        completedAt = completedAt,
        manualOrder = manualOrder,
    )

private inline fun <reified T : Enum<T>> enumValueOrDefault(
    value: String,
    default: T,
): T = enumValues<T>().firstOrNull { it.name == value } ?: default
