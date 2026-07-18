package com.geepscontrol.core.repository

import com.geepscontrol.core.database.DailyPlanItemEntity
import com.geepscontrol.core.database.PlanningDao
import com.geepscontrol.core.database.RecommendationFeedbackEntity
import com.geepscontrol.core.domain.DailyPlanItem
import com.geepscontrol.core.domain.RecommendationAction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface PlanningRepository {
    fun observeDay(dayKey: Long): Flow<List<DailyPlanItem>>

    suspend fun save(item: DailyPlanItem)

    suspend fun remove(
        dayKey: Long,
        taskId: Long,
    )

    suspend fun feedback(
        taskId: Long,
        action: RecommendationAction,
        createdAt: Long,
    )

    fun observeDeferralCount(
        from: Long,
        endExclusive: Long,
    ): Flow<Int>
}

class RoomPlanningRepository(
    private val dao: PlanningDao,
) : PlanningRepository {
    override fun observeDay(dayKey: Long) = dao.observeDay(dayKey).map { rows -> rows.map { DailyPlanItem(it.taskId, it.dayKey, it.topRank, it.deferredUntil, it.hidden) } }

    override suspend fun save(item: DailyPlanItem) = dao.put(DailyPlanItemEntity(item.dayKey, item.taskId, item.topRank, item.deferredUntil, item.hidden))

    override suspend fun remove(
        dayKey: Long,
        taskId: Long,
    ) = dao.remove(dayKey, taskId)

    override suspend fun feedback(
        taskId: Long,
        action: RecommendationAction,
        createdAt: Long,
    ) = dao.addFeedback(RecommendationFeedbackEntity(taskId = taskId, action = action.name, createdAt = createdAt))

    override fun observeDeferralCount(
        from: Long,
        endExclusive: Long,
    ) = dao.observeDeferralCount(from, endExclusive)
}
