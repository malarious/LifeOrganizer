package com.geepscontrol.core.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TaskModelsTest {
    @Test
    fun `quick capture defaults to inbox without requiring metadata`() {
        val task = Task(title = "Capture this")

        assertEquals(TaskStatus.INBOX, task.status)
        assertEquals(Priority.NORMAL, task.priority)
        assertNull(task.projectId)
        assertNull(task.estimatedMinutes)
    }

    @Test
    fun `project starts active`() {
        val project = Project(name = "Useful outcome")

        assertEquals(ProjectStatus.ACTIVE, project.status)
    }
}
