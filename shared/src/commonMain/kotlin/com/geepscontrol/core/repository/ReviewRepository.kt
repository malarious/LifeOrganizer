package com.geepscontrol.core.repository

import com.geepscontrol.core.database.ReviewDao
import com.geepscontrol.core.database.ReviewEntity
import com.geepscontrol.core.domain.ReviewRecord
import kotlinx.coroutines.flow.map

interface ReviewRepository {
    fun observeReviews(): kotlinx.coroutines.flow.Flow<List<ReviewRecord>>

    suspend fun save(review: ReviewRecord): Long
}

class RoomReviewRepository(
    private val dao: ReviewDao,
) : ReviewRepository {
    override fun observeReviews() = dao.observeAll().map { rows -> rows.map { ReviewRecord(it.id, enumValueOf(it.type), it.periodStart, it.periodEnd, it.wentWell, it.friction, it.nextPriority, it.notes, it.completedAt) } }

    override suspend fun save(review: ReviewRecord) = dao.save(ReviewEntity(review.id, review.type.name, review.periodStart, review.periodEnd, review.wentWell, review.friction, review.nextPriority, review.notes, review.completedAt))
}
