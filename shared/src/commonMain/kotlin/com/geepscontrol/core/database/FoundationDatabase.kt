package com.geepscontrol.core.database

import androidx.room.AutoMigration
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "foundation_metadata")
data class FoundationMetadata(
    @PrimaryKey val key: String,
    val value: String,
)

@Dao
interface FoundationMetadataDao {
    @Query("SELECT * FROM foundation_metadata ORDER BY key")
    fun observeAll(): Flow<List<FoundationMetadata>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(entry: FoundationMetadata)
}

@Database(
    entities = [
        FoundationMetadata::class,
        ProjectEntity::class,
        TaskEntity::class,
        ContextEntity::class,
        TaskContextEntity::class,
        TagEntity::class,
        TaskTagEntity::class,
        TaskDependencyEntity::class,
        DailyPlanItemEntity::class,
        RecommendationFeedbackEntity::class,
        ActiveTimerEntity::class,
        WorkSessionEntity::class,
        AlarmEntity::class,
        ReviewEntity::class,
        ReminderEntity::class,
        RoutineEntity::class,
        RoutineOccurrenceEntity::class,
        MedicationEntity::class,
        MedicationOccurrenceEntity::class,
        LifeEventEntity::class,
        StandaloneAlarmEntity::class,
        TimerPresetEntity::class,
        StandaloneTimerEntity::class,
        TimerHistoryEntity::class,
        TripEntity::class,
        ItineraryEntryEntity::class,
        TransportationDetailsEntity::class,
        LodgingDetailsEntity::class,
        TripReferenceEntity::class,
        TripTaskEntity::class,
        ChecklistEntity::class,
        ChecklistItemEntity::class,
        AlarmProfileEntity::class,
    ],
    autoMigrations = [AutoMigration(from = 1, to = 2), AutoMigration(from = 2, to = 3), AutoMigration(from = 3, to = 4), AutoMigration(from = 4, to = 5), AutoMigration(from = 5, to = 6), AutoMigration(from = 6, to = 7), AutoMigration(from = 7, to = 8), AutoMigration(from = 8, to = 9), AutoMigration(from = 9, to = 10), AutoMigration(from = 10, to = 11)],
    version = 11,
    exportSchema = true,
)
abstract class GeepsDatabase : RoomDatabase() {
    abstract fun foundationMetadataDao(): FoundationMetadataDao

    abstract fun projectDao(): ProjectDao

    abstract fun taskDao(): TaskDao

    abstract fun contextDao(): ContextDao

    abstract fun tagDao(): TagDao

    abstract fun planningDao(): PlanningDao

    abstract fun timerDao(): TimerDao

    abstract fun reviewDao(): ReviewDao

    abstract fun lifeItemDao(): LifeItemDao
}

const val DATABASE_NAME = "geeps-control.db"
