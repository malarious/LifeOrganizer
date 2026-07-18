package com.geepscontrol.core.domain

data class DailyPlanItem(
    val taskId: Long,
    val dayKey: Long,
    val topRank: Int? = null,
    val deferredUntil: Long? = null,
    val hidden: Boolean = false,
)

enum class RecommendationAction { STARTED, DEFERRED, HIDDEN, NOT_NOW }

data class Recommendation(
    val task: Task,
    val score: Int,
    val reasons: List<String>,
)

data class RecommendationContext(
    val now: Long,
    val availableMinutes: Int? = null,
    val energy: EnergyRequirement? = null,
    val contextIds: Set<Long> = emptySet(),
)

object RecommendationService {
    fun rank(
        tasks: List<Task>,
        plan: List<DailyPlanItem>,
        context: RecommendationContext,
    ): List<Recommendation> {
        val planByTask = plan.associateBy { it.taskId }
        return tasks
            .asSequence()
            .filter { it.status in eligibleStatuses && !it.isBlocked }
            .filter { task -> planByTask[task.id]?.let { !it.hidden && (it.deferredUntil == null || it.deferredUntil <= context.now) } ?: true }
            .filter { task -> context.availableMinutes == null || task.estimatedMinutes == null || task.estimatedMinutes <= context.availableMinutes }
            .filter { task -> context.contextIds.isEmpty() || task.contextIds.isEmpty() || task.contextIds.any(context.contextIds::contains) }
            .map { task -> score(task, planByTask[task.id], context) }
            .sortedWith(compareByDescending<Recommendation> { it.score }.thenBy { it.task.manualOrder }.thenBy { it.task.title.lowercase() })
            .toList()
    }

    private fun score(
        task: Task,
        plan: DailyPlanItem?,
        context: RecommendationContext,
    ): Recommendation {
        var score =
            when (task.priority) {
                Priority.CRITICAL -> 60
                Priority.HIGH -> 40
                Priority.NORMAL -> 20
                Priority.LOW -> 8
                Priority.SOMEDAY -> 0
            }
        val reasons = mutableListOf<String>()
        reasons += "${task.priority.name.lowercase().replace('_', ' ')} priority"
        if (plan != null) {
            score += 35
            reasons += "planned for today"
            plan.topRank?.let {
                score += 40 - (it.coerceIn(1, 3) * 5)
                reasons += "Top 3 #$it"
            }
        }
        task.dueAt?.let { due ->
            val days = ((due - context.now) / DAY_MILLIS).toInt()
            val urgency =
                when {
                    due < context.now -> 70
                    days == 0 -> 55
                    days <= 2 -> 35
                    days <= 7 -> 15
                    else -> 0
                }
            score += urgency
            if (urgency > 0) reasons += (if (due < context.now) "overdue" else "due soon")
        }
        if (context.energy == task.energyRequirement) {
            score += 12
            reasons += "matches your energy"
        }
        if (context.contextIds.isNotEmpty() && task.contextIds.any(context.contextIds::contains)) {
            score += 10
            reasons += "matches your context"
        }
        task.estimatedMinutes?.let {
            if (context.availableMinutes != null) {
                score += 8
                reasons += "fits your available time"
            }
        }
        return Recommendation(task, score, reasons)
    }

    private val eligibleStatuses = setOf(TaskStatus.READY, TaskStatus.SCHEDULED, TaskStatus.IN_PROGRESS)
    const val DAY_MILLIS = 86_400_000L
}

fun dayKey(timestamp: Long): Long = timestamp / RecommendationService.DAY_MILLIS
