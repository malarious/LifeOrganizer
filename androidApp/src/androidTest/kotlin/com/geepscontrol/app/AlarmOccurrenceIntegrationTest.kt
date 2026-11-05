package com.geepscontrol.app

import android.app.NotificationManager
import androidx.test.platform.app.InstrumentationRegistry
import com.geepscontrol.core.database.StandaloneAlarmEntity
import com.geepscontrol.core.database.buildDatabase
import com.geepscontrol.core.domain.AlarmOccurrenceAction
import com.geepscontrol.core.domain.AlarmOccurrenceCoordinator
import com.geepscontrol.core.domain.AlarmOccurrenceEngine
import com.geepscontrol.core.domain.AlarmRepeatRule
import com.geepscontrol.core.domain.AlarmRepeatUnit
import com.geepscontrol.core.repository.LifeItemRepository
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.TimeZone

class AlarmOccurrenceIntegrationTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val platform = AndroidPlatformActions(context)
    private val day = 86_400_000L

    @Test fun dailyAndWeekdayAlarmsKeepLocalTimeAcrossDaylightSaving() {
        val original = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"))
            val zone = ZoneId.systemDefault()
            val before = ZonedDateTime.of(2026, 3, 7, 7, 0, 0, 0, zone)
            val next = platform.nextAlarmOccurrence(AlarmRepeatRule(1, AlarmRepeatUnit.DAY), before.toInstant().toEpochMilli())
            assertEquals(
                7,
                java.time.Instant
                    .ofEpochMilli(next)
                    .atZone(zone)
                    .hour,
            )
            assertEquals(23 * 3_600_000L, next - before.toInstant().toEpochMilli())
            val friday = ZonedDateTime.of(2026, 3, 6, 23, 0, 0, 0, zone)
            val monday = platform.nextAlarmOccurrence(AlarmRepeatRule(1, AlarmRepeatUnit.WEEK, setOf(1)), friday.toInstant().toEpochMilli())
            assertEquals(
                1,
                java.time.Instant
                    .ofEpochMilli(monday)
                    .atZone(zone)
                    .dayOfWeek.value,
            )
            assertEquals(
                23,
                java.time.Instant
                    .ofEpochMilli(monday)
                    .atZone(zone)
                    .hour,
            )
        } finally {
            TimeZone.setDefault(original)
        }
    }

    @Test fun monthlyAlarmUsesCalendarMonth() {
        val start = ZonedDateTime.of(2026, 1, 15, 7, 0, 0, 0, ZoneId.systemDefault())
        val next = platform.nextAlarmOccurrence(AlarmRepeatRule(1, AlarmRepeatUnit.MONTH), start.toInstant().toEpochMilli())
        assertEquals(
            2,
            java.time.Instant
                .ofEpochMilli(next)
                .atZone(ZoneId.systemDefault())
                .monthValue,
        )
        assertEquals(
            15,
            java.time.Instant
                .ofEpochMilli(next)
                .atZone(ZoneId.systemDefault())
                .dayOfMonth,
        )
    }

    @Test fun pullDownActionsDelayRepeatedlySkipOnlyOneAndIgnoreOldButtons() =
        runBlocking {
            instrumentation.uiAutomation.executeShellCommand("pm grant ${context.packageName} android.permission.POST_NOTIFICATIONS").close()
            val database = buildDatabase(context)
            val repository = LifeItemRepository(database.lifeItemDao())
            val scheduler = AndroidAlarmScheduler(context)
            val now = System.currentTimeMillis()
            val due = now + 30 * 60_000L
            val first = repository.save(StandaloneAlarmEntity(label = "Notification test A", scheduledAt = due, recurrenceRule = AlarmRepeatRule(1, AlarmRepeatUnit.DAY).encode(), sound = false, vibration = false, createdAt = now, updatedAt = now))
            val second = repository.save(StandaloneAlarmEntity(label = "Notification test B", scheduledAt = due + 60_000L, createdAt = now, updatedAt = now))
            val manager = context.getSystemService(NotificationManager::class.java)

            fun notification(id: Long) = manager.activeNotifications.firstOrNull { it.tag == "upcoming_alarm" && it.id == (3_000_000L + id).toInt() }?.notification

            suspend fun awaitState(
                id: Long,
                predicate: (StandaloneAlarmEntity) -> Boolean,
            ): StandaloneAlarmEntity =
                withTimeout(10_000) {
                    var alarm = repository.getAlarm(id)!!
                    while (!predicate(alarm)) {
                        delay(50)
                        alarm = repository.getAlarm(id)!!
                    }
                    alarm
                }

            suspend fun refreshedSkip(previous: android.app.PendingIntent): android.app.PendingIntent =
                withTimeout(10_000) {
                    var current = notification(first)?.actions?.get(0)?.actionIntent
                    while (current == null || current == previous) {
                        delay(50)
                        current = notification(first)?.actions?.get(0)?.actionIntent
                    }
                    current
                }
            try {
                UpcomingAlarmNotifications.show(context, repository.getAlarm(first)!!, due)
                UpcomingAlarmNotifications.show(context, repository.getAlarm(second)!!, due + 60_000L)
                assertNotNull(notification(first))
                assertNotNull(notification(second))
                val staleSkip = notification(first)!!.actions[0].actionIntent
                notification(first)!!.actions[1].actionIntent.send()
                awaitState(first) { it.scheduledAt == due + 5 * 60_000L }
                val firstDelaySkip = refreshedSkip(staleSkip)
                notification(first)!!.actions[1].actionIntent.send()
                val delayed = awaitState(first) { it.scheduledAt == due + 10 * 60_000L }
                assertEquals(due, delayed.nextRegularAt)
                val currentSkip = refreshedSkip(firstDelaySkip)
                // A stale pre-delay action must not skip the updated occurrence.
                staleSkip.send()
                delay(200)
                assertEquals(delayed.scheduledAt, repository.getAlarm(first)!!.scheduledAt)
                currentSkip.send()
                val skipped = awaitState(first) { it.lastSkippedAt != null }
                assertTrue(skipped.enabled)
                assertEquals(platform.nextAlarmOccurrence(AlarmRepeatRule(1, AlarmRepeatUnit.DAY), due), skipped.scheduledAt)
                assertEquals(due + 60_000L, repository.getAlarm(second)!!.scheduledAt)
                assertTrue(repository.getAlarm(second)!!.enabled)
            } finally {
                listOf(first, second).forEach {
                    scheduler.cancel(3_000_000L + it)
                    repository.removeAlarm(it)
                }
                database.close()
            }
        }

    @Test fun acknowledgingSnoozePersistsAndUpdatesOtherDatabaseObservers() =
        runBlocking {
            val name = "alarm-occurrence-integration.db"
            val database = buildDatabase(context, name)
            val other = buildDatabase(context, name)
            val repository = LifeItemRepository(database.lifeItemDao())
            val now = System.currentTimeMillis()
            val id = repository.save(StandaloneAlarmEntity(label = "Snoozed", scheduledAt = now + 5 * 60_000L, nextRegularAt = now + day, recurrenceRule = AlarmRepeatRule(1, AlarmRepeatUnit.DAY).encode(), snoozeCount = 2, lastTriggeredAt = now - 60_000L, createdAt = now, updatedAt = now))
            try {
                val observer =
                    async(start = CoroutineStart.UNDISPATCHED) {
                        withTimeout(10_000) { other.lifeItemDao().observeStandaloneAlarms().first { it.singleOrNull()?.lastDismissedAt == now } }
                    }
                val done = AlarmOccurrenceCoordinator(repository, RecordingScheduler(), platform).perform(id, AlarmOccurrenceAction.ACKNOWLEDGE, now)!!
                assertEquals(now + day, done.scheduledAt)
                assertTrue(done.enabled)
                assertFalse(AlarmOccurrenceEngine.needsAcknowledgment(done))
                assertEquals(now, observer.await().single().lastDismissedAt)
            } finally {
                database.close()
                other.close()
                context.deleteDatabase(name)
            }
        }

    @Test fun oldSnoozeRecordsRecoverTheirRegularLocalTime() =
        runBlocking {
            val name = "legacy-snooze-integration.db"
            val database = buildDatabase(context, name)
            val repository = LifeItemRepository(database.lifeItemDao())
            val morning =
                ZonedDateTime
                    .now()
                    .withHour(7)
                    .withMinute(0)
                    .withSecond(0)
                    .withNano(0)
            val fired = morning.toInstant().toEpochMilli()
            val tomorrow = morning.plusDays(1).toInstant().toEpochMilli()
            val id = repository.save(StandaloneAlarmEntity(label = "Legacy snooze", scheduledAt = fired + 10 * 60_000L, recurrenceRule = "1:DAY:FIXED", snoozeCount = 1, localHour = 7, localMinute = 0, localEpochDay = morning.plusDays(1).toLocalDate().toEpochDay(), lastTriggeredAt = fired, createdAt = fired, updatedAt = fired))
            try {
                val done = AlarmOccurrenceCoordinator(repository, RecordingScheduler(), platform).perform(id, AlarmOccurrenceAction.ACKNOWLEDGE, fired + 60_000L)!!
                assertEquals(tomorrow, done.scheduledAt)
                assertTrue(done.enabled)
                assertFalse(AlarmOccurrenceEngine.needsAcknowledgment(done))
            } finally {
                database.close()
                context.deleteDatabase(name)
            }
        }

    @Test fun staleActionsAndExhaustedSnoozeLimitsDoNotChangeTheAlarm() =
        runBlocking {
            val name = "alarm-guard-integration.db"
            val database = buildDatabase(context, name)
            val repository = LifeItemRepository(database.lifeItemDao())
            val now = System.currentTimeMillis()
            val alarm = StandaloneAlarmEntity(label = "Limited", scheduledAt = now + 5 * 60_000L, recurrenceRule = AlarmRepeatRule(1, AlarmRepeatUnit.DAY).encode(), snoozeCount = 2, maximumSnoozes = 2, lastTriggeredAt = now - 60_000L, createdAt = now, updatedAt = now)
            val id = repository.save(alarm)
            val scheduler = RecordingScheduler()
            val coordinator = AlarmOccurrenceCoordinator(repository, scheduler, platform)
            try {
                assertNull(coordinator.perform(id, AlarmOccurrenceAction.DELAY, now, alarm.scheduledAt - 1))
                assertNull(coordinator.perform(id, AlarmOccurrenceAction.SNOOZE, now))
                assertEquals(alarm.copy(id = id), repository.getAlarm(id))
                assertTrue(scheduler.scheduled.isEmpty())
            } finally {
                database.close()
                context.deleteDatabase(name)
            }
        }

    private class RecordingScheduler : com.geepscontrol.core.domain.AlarmScheduler {
        val scheduled = mutableListOf<com.geepscontrol.core.domain.AlarmDefinition>()

        override fun schedule(alarm: com.geepscontrol.core.domain.AlarmDefinition) {
            scheduled += alarm
        }

        override fun cancel(alarmId: Long) {}

        override fun scheduleTimer(timer: com.geepscontrol.core.domain.ActiveTimer) {}

        override fun cancelTimer(timerId: Long?) {}

        override fun scheduleReminder(
            id: Long,
            triggerAt: Long,
            label: String,
            category: com.geepscontrol.core.domain.NotificationCategory,
        ) {}

        override fun cancelReminder(id: Long) {}
    }
}
