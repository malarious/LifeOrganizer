package com.geepscontrol.core.domain

enum class ProjectStatus { ACTIVE, WAITING, PARKED, COMPLETED, ARCHIVED }

enum class TaskStatus { INBOX, READY, SCHEDULED, IN_PROGRESS, WAITING, BLOCKED, COMPLETED, CANCELLED, ARCHIVED }

enum class Priority { CRITICAL, HIGH, NORMAL, LOW, SOMEDAY }

enum class EnergyRequirement { VERY_LOW, LOW, MEDIUM, HIGH, DEEP_FOCUS }

data class Project(
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val desiredOutcome: String = "",
    val status: ProjectStatus = ProjectStatus.ACTIVE,
    val priority: Priority = Priority.NORMAL,
    val colorArgb: Long = 0xff6750a4,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
)

data class Task(
    val id: Long = 0,
    val projectId: Long? = null,
    val title: String,
    val description: String = "",
    val status: TaskStatus = TaskStatus.INBOX,
    val priority: Priority = Priority.NORMAL,
    val estimatedMinutes: Int? = null,
    val energyRequirement: EnergyRequirement = EnergyRequirement.MEDIUM,
    val dueAt: Long? = null,
    val reminderAt: Long? = null,
    val recurrenceRule: String? = null,
    val recurrenceParentId: Long? = null,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val completedAt: Long? = null,
    val manualOrder: Long = 0,
    val contextIds: Set<Long> = emptySet(),
    val tagIds: Set<Long> = emptySet(),
    val dependencyIds: Set<Long> = emptySet(),
    val isBlocked: Boolean = false,
    val blockingTaskTitles: List<String> = emptyList(),
)

data class ProjectSummary(
    val project: Project,
    val openTaskCount: Int,
    val completedTaskCount: Int,
)

data class TaskContext(
    val id: Long = 0,
    val name: String,
    val isSystemDefault: Boolean = false,
)

data class TaskTag(
    val id: Long = 0,
    val name: String,
)
