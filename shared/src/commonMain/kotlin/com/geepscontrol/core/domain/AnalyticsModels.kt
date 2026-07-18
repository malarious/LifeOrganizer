package com.geepscontrol.core.domain

data class ReviewSummary(
    val completedTasks: Int,
    val completedSessions: Int,
    val focusedMillis: Long,
    val plannedMillis: Long,
    val deferredTasks: Int,
    val addedTasks: Int,
    val unfinishedPlannedTasks: Int,
    val focusByProject: Map<Long?, Long>,
    val focusByDay: Map<Long, Long>,
    val completedByProject: Map<Long?, Int>,
)

data class ProductivityInsight(
    val title: String,
    val explanation: String,
)

data class AnalyticsSnapshot(
    val summary: ReviewSummary,
    val stalledProjects: List<Project>,
    val projectsWithoutNextAction: List<Project>,
    val estimateAccuracyRatio: Double?,
    val insights: List<ProductivityInsight>,
)

object AnalyticsService {
    fun snapshot(
        from: Long,
        endExclusive: Long,
        now: Long,
        tasks: List<Task>,
        projects: List<Project>,
        sessions: List<WorkSession>,
        plannedTaskIds: Set<Long> = emptySet(),
        deferredTasks: Int = 0,
    ): AnalyticsSnapshot {
        val periodSessions = sessions.filter { it.startedAt in from until endExclusive }
        val periodTasks = tasks.filter { it.completedAt != null && it.completedAt in from until endExclusive }
        val focusByProject = periodSessions.groupBy { it.projectId }.mapValues { (_, rows) -> rows.sumOf { it.activeMillis } }
        val focusByDay = periodSessions.groupBy { dayKey(it.startedAt) }.mapValues { (_, rows) -> rows.sumOf { it.activeMillis } }
        val completedByProject = periodTasks.groupingBy { it.projectId }.eachCount()
        val stalledThreshold = now - 21 * RecommendationService.DAY_MILLIS
        val stalled = projects.filter { project -> project.status == ProjectStatus.ACTIVE && sessions.filter { it.projectId == project.id }.maxOfOrNull { it.endedAt }?.let { it < stalledThreshold } ?: (project.updatedAt < stalledThreshold) }
        val withoutNext = projects.filter { project -> project.status == ProjectStatus.ACTIVE && tasks.none { it.projectId == project.id && it.status in setOf(TaskStatus.READY, TaskStatus.IN_PROGRESS, TaskStatus.SCHEDULED) && !it.isBlocked } }
        val actualByTask = periodSessions.filter { it.taskId != null }.groupBy { it.taskId!! }.mapValues { it.value.sumOf(WorkSession::activeMillis) }
        val estimatePairs = tasks.mapNotNull { task -> task.estimatedMinutes?.let { estimate -> actualByTask[task.id]?.let { actual -> estimate * 60_000L to actual } } }
        val ratio = estimatePairs.takeIf { it.isNotEmpty() }?.let { pairs -> pairs.sumOf { it.second }.toDouble() / pairs.sumOf { it.first }.coerceAtLeast(1) }
        val summary = ReviewSummary(periodTasks.size, periodSessions.size, periodSessions.sumOf { it.activeMillis }, periodSessions.sumOf { it.plannedMillis ?: 0 }, deferredTasks, tasks.count { it.createdAt in from until endExclusive }, plannedTaskIds.count { id -> tasks.any { it.id == id && it.status != TaskStatus.COMPLETED } }, focusByProject, focusByDay, completedByProject)
        val insights =
            buildList {
                stalled.forEach { add(ProductivityInsight("${it.name} may be stalled", "No recorded work session in at least 21 days.")) }
                withoutNext.forEach { add(ProductivityInsight("${it.name} has no ready next action", "Every open task is absent, waiting, or blocked.")) }
                ratio?.takeIf { it > 1.25 }?.let { add(ProductivityInsight("Tasks are taking longer than estimated", "Recorded sessions used ${(it * 100).toInt()}% of estimated time in this period.")) }
                if (deferredTasks >= 5) add(ProductivityInsight("Several tasks were deferred", "$deferredTasks deferrals were recorded in this period."))
            }
        return AnalyticsSnapshot(summary, stalled, withoutNext, ratio, insights)
    }
}
