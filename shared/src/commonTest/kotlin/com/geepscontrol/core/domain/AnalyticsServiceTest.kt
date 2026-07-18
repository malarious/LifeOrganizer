package com.geepscontrol.core.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AnalyticsServiceTest {
    @Test fun `summaries and estimate accuracy use period data`() {
        val task = Task(id = 1, projectId = 2, title = "Work", status = TaskStatus.COMPLETED, estimatedMinutes = 30, createdAt = 10, completedAt = 50)
        val session = WorkSession(taskId = 1, projectId = 2, startedAt = 20, endedAt = 80, pausedMillis = 0, activeMillis = 45 * 60_000L, plannedMillis = 30 * 60_000L, mode = TimerMode.COUNTDOWN)
        val result = AnalyticsService.snapshot(0, 100, 100, listOf(task), emptyList(), listOf(session))
        assertEquals(1, result.summary.completedTasks)
        assertEquals(45 * 60_000L, result.summary.focusByProject[2])
        assertEquals(1.5, result.estimateAccuracyRatio)
    }

    @Test fun `stalled and no-next-action insights explain evidence`() {
        val now = 30 * RecommendationService.DAY_MILLIS
        val project = Project(id = 1, name = "Dormant", status = ProjectStatus.ACTIVE, updatedAt = 0)
        val result = AnalyticsService.snapshot(0, now, now, emptyList(), listOf(project), emptyList())
        assertTrue(project in result.stalledProjects)
        assertTrue(project in result.projectsWithoutNextAction)
        assertTrue(result.insights.all { it.explanation.isNotBlank() })
    }

    @Test fun `analytics handles thousands of tasks`() {
        val tasks = (1L..5_000L).map { Task(id = it, projectId = it % 20, title = "Task $it", status = if (it % 2L == 0L) TaskStatus.COMPLETED else TaskStatus.READY, createdAt = it, completedAt = if (it % 2L == 0L) it else null) }
        val result = AnalyticsService.snapshot(0, 10_000, 10_000, tasks, emptyList(), emptyList())
        assertEquals(2_500, result.summary.completedTasks)
    }
}
