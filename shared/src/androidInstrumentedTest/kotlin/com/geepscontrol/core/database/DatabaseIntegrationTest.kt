package com.geepscontrol.core.database

import android.content.Context
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.geepscontrol.core.domain.Project
import com.geepscontrol.core.domain.RecurrenceRule
import com.geepscontrol.core.domain.RecurrenceUnit
import com.geepscontrol.core.domain.ReviewRecord
import com.geepscontrol.core.domain.ReviewType
import com.geepscontrol.core.domain.Task
import com.geepscontrol.core.domain.TaskContext
import com.geepscontrol.core.domain.TaskStatus
import com.geepscontrol.core.repository.RoomContextRepository
import com.geepscontrol.core.repository.RoomProjectRepository
import com.geepscontrol.core.repository.RoomReviewRepository
import com.geepscontrol.core.repository.RoomTaskRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseIntegrationTest {
    @get:Rule
    val migrationHelper =
        MigrationTestHelper(
            instrumentation = InstrumentationRegistry.getInstrumentation(),
            databaseClass = GeepsDatabase::class.java,
        )

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private var database: GeepsDatabase? = null

    @After
    fun closeDatabase() {
        database?.close()
        context.deleteDatabase(TEST_DATABASE)
    }

    @Test
    fun migratesVersionOneToLatestWithoutDestructiveFallback() =
        runBlocking {
            migrationHelper.createDatabase(TEST_DATABASE, 1).close()

            database = buildDatabase(context, TEST_DATABASE)

            assertTrue(
                database!!
                    .projectDao()
                    .observeActive()
                    .first()
                    .isEmpty(),
            )
            assertTrue(
                database!!
                    .timerDao()
                    .observeSessions()
                    .first()
                    .isEmpty(),
            )
            assertTrue(
                database!!
                    .lifeItemDao()
                    .observeReminders()
                    .first()
                    .isEmpty(),
            )
            assertTrue(
                database!!
                    .lifeItemDao()
                    .observeTrips()
                    .first()
                    .isEmpty(),
            )
        }

    @Test
    fun migratesPreMilestoneSevenDatabaseThroughBothNewSchemas() =
        runBlocking {
            migrationHelper.createDatabase(TEST_DATABASE, 6).apply {
                execSQL("INSERT INTO projects (name, description, desiredOutcome, status, priority, createdAt, updatedAt) VALUES ('Preserved', '', '', 'ACTIVE', 'MEDIUM', 1, 1)")
                close()
            }
            database = buildDatabase(context, TEST_DATABASE)
            assertEquals(
                "Preserved",
                database!!
                    .projectDao()
                    .observeActive()
                    .first()
                    .single()
                    .name,
            )
            assertEquals(
                0xff6750a4,
                database!!
                    .projectDao()
                    .observeActive()
                    .first()
                    .single()
                    .colorArgb,
            )
            assertTrue(
                database!!
                    .lifeItemDao()
                    .observeRoutines()
                    .first()
                    .isEmpty(),
            )
            assertTrue(
                database!!
                    .lifeItemDao()
                    .observeItineraryEntries()
                    .first()
                    .isEmpty(),
            )
        }

    @Test
    fun migratesReusableChecklistDataFromVersionThirteen() =
        runBlocking {
            migrationHelper.createDatabase(TEST_DATABASE, 13).apply {
                execSQL("INSERT INTO checklists (id, name, createdAt, updatedAt) VALUES (11, 'Packing', 1, 1)")
                execSQL("INSERT INTO checklist_items (id, checklistId, text, checked, sortOrder, createdAt, updatedAt) VALUES (21, 11, 'Passport', 1, 0, 1, 1)")
                close()
            }

            database = buildDatabase(context, TEST_DATABASE)

            val checklist = database!!.lifeItemDao().observeChecklists().first().single()
            val item = database!!.lifeItemDao().observeChecklistItems().first().single()
            assertEquals("Packing", checklist.name)
            assertEquals(null, checklist.tripId)
            assertEquals(null, checklist.sourceChecklistId)
            assertEquals("Passport", item.text)
            assertTrue(item.checked)
        }

    @Test
    fun migratesAlarmsFromVersionFourteenAndPreservesHistory() = runBlocking {
        migrationHelper.createDatabase(TEST_DATABASE, 14).apply {
            execSQL("INSERT INTO standalone_alarms (id, label, scheduledAt, enabled, sound, vibration, snoozeMinutes, snoozeCount, lastTriggeredAt, createdAt, updatedAt) VALUES (7, 'Wake up', 1000, 1, 1, 1, 8, 2, 500, 1, 500)")
            close()
        }
        database = buildDatabase(context, TEST_DATABASE)
        val alarm = database!!.lifeItemDao().getStandaloneAlarm(7)!!
        assertEquals("Wake up", alarm.label)
        assertEquals(2, alarm.snoozeCount)
        assertEquals(500L, alarm.lastTriggeredAt)
        assertEquals(null, alarm.nextRegularAt)
        assertEquals(null, alarm.lastSkippedAt)
    }

    @Test
    fun repositoriesPersistRelationshipsAndBlockedState() =
        runBlocking {
            database = buildDatabase(context, TEST_DATABASE)
            val projects = RoomProjectRepository(database!!.projectDao())
            val tasks = RoomTaskRepository(database!!.taskDao())
            val contexts = RoomContextRepository(database!!.contextDao())
            val projectId = projects.save(Project(name = "Test project"))
            val contextId = contexts.save(TaskContext(name = "Computer"))
            val prerequisiteId = tasks.save(Task(title = "Prerequisite", projectId = projectId, status = TaskStatus.READY))
            val dependentId =
                tasks.save(
                    Task(
                        title = "Dependent",
                        projectId = projectId,
                        status = TaskStatus.READY,
                        contextIds = setOf(contextId),
                        dependencyIds = setOf(prerequisiteId),
                    ),
                )

            val dependent = tasks.observeTasks().first().first { it.id == dependentId }

            assertEquals(setOf(contextId), dependent.contextIds)
            assertTrue(dependent.isBlocked)
            assertEquals(listOf("Prerequisite"), dependent.blockingTaskTitles)

            val prerequisite = tasks.get(prerequisiteId)!!
            tasks.save(prerequisite.copy(dependencyIds = setOf(dependentId)))
            assertTrue(tasks.get(prerequisiteId)!!.dependencyIds.isEmpty())
        }

    @Test
    fun completingRecurringTaskPreservesHistoryAndCreatesNextOccurrence() =
        runBlocking {
            database = buildDatabase(context, TEST_DATABASE)
            val tasks = RoomTaskRepository(database!!.taskDao())
            val originalId = tasks.save(Task(title = "Repeat me", status = TaskStatus.READY, recurrenceRule = RecurrenceRule(unit = RecurrenceUnit.DAY).encode()))

            tasks.complete(originalId)

            val occurrences = tasks.observeTasks().first().filter { it.title == "Repeat me" }
            assertEquals(2, occurrences.size)
            assertTrue(occurrences.any { it.id == originalId && it.status == TaskStatus.COMPLETED })
            assertTrue(occurrences.any { it.id != originalId && it.status == TaskStatus.READY && it.recurrenceParentId == originalId })
        }

    @Test
    fun reviewRecordsPersistThroughRepository() =
        runBlocking {
            database = buildDatabase(context, TEST_DATABASE)
            val reviews = RoomReviewRepository(database!!.reviewDao())
            reviews.save(ReviewRecord(type = ReviewType.DAILY, periodStart = 0, periodEnd = 100, wentWell = "Focused", completedAt = 100))
            assertEquals(
                "Focused",
                reviews
                    .observeReviews()
                    .first()
                    .single()
                    .wentWell,
            )
        }

    private companion object {
        const val TEST_DATABASE = "geeps-control-integration-test.db"
    }
}
