package com.geepscontrol.core.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RecommendationServiceTest {
    @Test
    fun `blocked completed and oversized tasks are excluded`() {
        val tasks =
            listOf(
                Task(id = 1, title = "Fits", status = TaskStatus.READY, estimatedMinutes = 20),
                Task(id = 2, title = "Blocked", status = TaskStatus.READY, isBlocked = true),
                Task(id = 3, title = "Done", status = TaskStatus.COMPLETED),
                Task(id = 4, title = "Too long", status = TaskStatus.READY, estimatedMinutes = 90),
            )
        val result = RecommendationService.rank(tasks, emptyList(), RecommendationContext(now = 0, availableMinutes = 30))
        assertEquals(listOf(1L), result.map { it.task.id })
    }

    @Test
    fun `top three and overdue work receive clear ranking reasons`() {
        val now = RecommendationService.DAY_MILLIS * 10
        val normal = Task(id = 1, title = "Normal", status = TaskStatus.READY)
        val urgent = Task(id = 2, title = "Urgent", status = TaskStatus.READY, dueAt = now - 1)
        val result = RecommendationService.rank(listOf(normal, urgent), listOf(DailyPlanItem(urgent.id, dayKey(now), topRank = 1)), RecommendationContext(now))
        assertEquals(urgent.id, result.first().task.id)
        assertTrue(result.first().reasons.contains("overdue"))
        assertTrue(result.first().reasons.contains("Top 3 #1"))
    }
}
