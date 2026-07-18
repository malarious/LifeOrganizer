package com.geepscontrol.core.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects WHERE status != 'ARCHIVED' ORDER BY status, priority, name COLLATE NOCASE")
    fun observeActive(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun get(id: Long): ProjectEntity?

    @Insert
    suspend fun insert(project: ProjectEntity): Long

    @Update
    suspend fun update(project: ProjectEntity)

    @Delete
    suspend fun delete(project: ProjectEntity)
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE status = 'INBOX' ORDER BY manualOrder, createdAt DESC")
    @Transaction
    fun observeInbox(): Flow<List<TaskWithMetadata>>

    @Query("SELECT * FROM tasks WHERE status NOT IN ('ARCHIVED', 'CANCELLED') ORDER BY manualOrder, updatedAt DESC")
    @Transaction
    fun observeAllOpenAndCompleted(): Flow<List<TaskWithMetadata>>

    @Query("SELECT * FROM tasks WHERE projectId = :projectId AND status != 'ARCHIVED' ORDER BY manualOrder, createdAt")
    @Transaction
    fun observeForProject(projectId: Long): Flow<List<TaskWithMetadata>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    @Transaction
    suspend fun get(id: Long): TaskWithMetadata?

    @Insert
    suspend fun insert(task: TaskEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(task: TaskEntity): Long

    @Update
    suspend fun update(task: TaskEntity)

    @Delete
    suspend fun delete(task: TaskEntity)

    @Query(
        "SELECT COUNT(*) FROM task_dependencies d JOIN tasks parent ON parent.id = d.dependsOnTaskId WHERE d.taskId = :taskId AND parent.status != 'COMPLETED'",
    )
    suspend fun incompleteDependencyCount(taskId: Long): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addDependency(dependency: TaskDependencyEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addContexts(contexts: List<TaskContextEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addTags(tags: List<TaskTagEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addDependencies(dependencies: List<TaskDependencyEntity>)

    @Query("DELETE FROM task_contexts WHERE taskId = :taskId")
    suspend fun clearContexts(taskId: Long)

    @Query("DELETE FROM task_tags WHERE taskId = :taskId")
    suspend fun clearTags(taskId: Long)

    @Query("DELETE FROM task_dependencies WHERE taskId = :taskId")
    suspend fun clearDependencies(taskId: Long)

    @Transaction
    suspend fun replaceMetadata(
        taskId: Long,
        contextIds: Set<Long>,
        tagIds: Set<Long>,
        dependencyIds: Set<Long>,
    ) {
        clearContexts(taskId)
        clearTags(taskId)
        clearDependencies(taskId)
        addContexts(contextIds.map { TaskContextEntity(taskId, it) })
        addTags(tagIds.map { TaskTagEntity(taskId, it) })
        addDependencies(dependencyIds.filter { it != taskId }.map { TaskDependencyEntity(taskId, it) })
    }
}

@Dao
interface ContextDao {
    @Query("SELECT * FROM contexts ORDER BY isSystemDefault DESC, name COLLATE NOCASE")
    fun observeAll(): Flow<List<ContextEntity>>

    @Insert
    suspend fun insert(context: ContextEntity): Long

    @Update
    suspend fun update(context: ContextEntity)

    @Delete
    suspend fun delete(context: ContextEntity)
}

@Dao
interface TagDao {
    @Query("SELECT * FROM tags ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<TagEntity>>

    @Insert
    suspend fun insert(tag: TagEntity): Long

    @Update
    suspend fun update(tag: TagEntity)

    @Delete
    suspend fun delete(tag: TagEntity)
}
