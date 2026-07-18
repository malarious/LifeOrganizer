package com.geepscontrol.core.database

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "projects", indices = [Index("status"), Index("updatedAt")])
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String,
    val desiredOutcome: String,
    val status: String,
    val priority: String,
    @ColumnInfo(defaultValue = "4284960932") val colorArgb: Long = 0xff6750a4,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("projectId"), Index("status"), Index("priority"), Index("dueAt"), Index("updatedAt")],
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long?,
    val title: String,
    val description: String,
    val status: String,
    val priority: String,
    val estimatedMinutes: Int?,
    val energyRequirement: String,
    val dueAt: Long?,
    val reminderAt: Long?,
    val recurrenceRule: String?,
    val recurrenceParentId: Long?,
    val createdAt: Long,
    val updatedAt: Long,
    val completedAt: Long?,
    val manualOrder: Long,
)

@Entity(tableName = "contexts", indices = [Index(value = ["name"], unique = true)])
data class ContextEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val isSystemDefault: Boolean,
)

@Entity(
    tableName = "task_contexts",
    primaryKeys = ["taskId", "contextId"],
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ContextEntity::class,
            parentColumns = ["id"],
            childColumns = ["contextId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("contextId")],
)
data class TaskContextEntity(
    val taskId: Long,
    val contextId: Long,
)

@Entity(tableName = "tags", indices = [Index(value = ["name"], unique = true)])
data class TagEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
)

@Entity(
    tableName = "task_tags",
    primaryKeys = ["taskId", "tagId"],
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("tagId")],
)
data class TaskTagEntity(
    val taskId: Long,
    val tagId: Long,
)

@Entity(
    tableName = "task_dependencies",
    primaryKeys = ["taskId", "dependsOnTaskId"],
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["dependsOnTaskId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("dependsOnTaskId")],
)
data class TaskDependencyEntity(
    val taskId: Long,
    val dependsOnTaskId: Long,
    val dependencyType: String = "FINISH_TO_START",
)

data class TaskWithMetadata(
    @Embedded val task: TaskEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(TaskContextEntity::class, parentColumn = "taskId", entityColumn = "contextId"),
    )
    val contexts: List<ContextEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(TaskTagEntity::class, parentColumn = "taskId", entityColumn = "tagId"),
    )
    val tags: List<TagEntity>,
    @Relation(parentColumn = "id", entityColumn = "taskId")
    val dependencies: List<TaskDependencyEntity>,
)
