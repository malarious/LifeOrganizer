package com.geepscontrol.core.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "reviews")
data class ReviewEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val periodStart: Long,
    val periodEnd: Long,
    val wentWell: String,
    val friction: String,
    val nextPriority: String,
    val notes: String,
    val completedAt: Long,
)

@Dao
interface ReviewDao {
    @Query("SELECT * FROM reviews ORDER BY completedAt DESC")
    fun observeAll(): Flow<List<ReviewEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(review: ReviewEntity): Long
}
