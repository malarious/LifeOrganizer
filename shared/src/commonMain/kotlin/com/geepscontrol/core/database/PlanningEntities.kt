package com.geepscontrol.core.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(
    tableName = "daily_plan_items",
    primaryKeys = ["dayKey", "taskId"],
    foreignKeys = [ForeignKey(entity = TaskEntity::class, parentColumns = ["id"], childColumns = ["taskId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("taskId")],
)
data class DailyPlanItemEntity(
    val dayKey: Long,
    val taskId: Long,
    val topRank: Int?,
    val deferredUntil: Long?,
    val hidden: Boolean,
)

@Entity(tableName = "recommendation_feedback", indices = [Index("taskId"), Index("createdAt")])
data class RecommendationFeedbackEntity(
    @androidx.room.PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: Long,
    val action: String,
    val createdAt: Long,
)

@Dao
interface PlanningDao {
    @Query("SELECT * FROM daily_plan_items WHERE dayKey = :dayKey ORDER BY CASE WHEN topRank IS NULL THEN 1 ELSE 0 END, topRank, taskId")
    fun observeDay(dayKey: Long): Flow<List<DailyPlanItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(item: DailyPlanItemEntity)

    @Query("DELETE FROM daily_plan_items WHERE dayKey = :dayKey AND taskId = :taskId")
    suspend fun remove(
        dayKey: Long,
        taskId: Long,
    )

    @Insert
    suspend fun addFeedback(feedback: RecommendationFeedbackEntity)

    @Query("SELECT COUNT(*) FROM recommendation_feedback WHERE action = 'DEFERRED' AND createdAt >= :from AND createdAt < :endExclusive")
    fun observeDeferralCount(
        from: Long,
        endExclusive: Long,
    ): Flow<Int>
}
