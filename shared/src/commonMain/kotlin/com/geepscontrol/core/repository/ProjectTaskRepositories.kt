package com.geepscontrol.core.repository

import com.geepscontrol.core.domain.Project
import com.geepscontrol.core.domain.Task
import com.geepscontrol.core.domain.TaskContext
import com.geepscontrol.core.domain.TaskTag
import kotlinx.coroutines.flow.Flow

interface ProjectRepository {
    fun observeProjects(): Flow<List<Project>>

    suspend fun get(id: Long): Project?

    suspend fun save(project: Project): Long

    suspend fun delete(project: Project)
}

interface TaskRepository {
    fun observeInbox(): Flow<List<Task>>

    fun observeTasks(): Flow<List<Task>>

    fun observeForProject(projectId: Long): Flow<List<Task>>

    suspend fun get(id: Long): Task?

    suspend fun save(task: Task): Long

    suspend fun complete(id: Long): Task?

    suspend fun restore(task: Task)

    suspend fun delete(task: Task)

    suspend fun isBlocked(id: Long): Boolean
}

interface ContextRepository {
    fun observeContexts(): Flow<List<TaskContext>>

    suspend fun save(context: TaskContext): Long

    suspend fun delete(context: TaskContext)
}

interface TagRepository {
    fun observeTags(): Flow<List<TaskTag>>

    suspend fun save(tag: TaskTag): Long

    suspend fun delete(tag: TaskTag)
}
