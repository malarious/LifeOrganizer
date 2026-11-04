package com.geepscontrol.core.domain

import com.geepscontrol.core.database.StandaloneAlarmEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AlarmOccurrenceEngineTest {
    private val day = 86_400_000L
    private val minute = 60_000L

    private fun alarm(repeating: Boolean = true) =
        StandaloneAlarmEntity(
            id = 1,
            label = "Wake up",
            scheduledAt = day,
            recurrenceRule = if (repeating) AlarmRepeatRule(1, AlarmRepeatUnit.DAY).encode() else null,
            createdAt = 0,
            updatedAt = 0,
        )

    @Test fun `skip before daily alarm advances once and keeps repeat enabled`() {
        val skipped = AlarmOccurrenceEngine.apply(alarm(), AlarmOccurrenceAction.SKIP, day - minute)
        assertEquals(2 * day, skipped.scheduledAt)
        assertTrue(skipped.enabled)
        assertEquals(day - minute, skipped.lastSkippedAt)
        assertEquals(null, skipped.lastDismissedAt)
        val fired = AlarmOccurrenceEngine.apply(skipped, AlarmOccurrenceAction.FIRE, 2 * day)
        assertEquals(3 * day, fired.scheduledAt)
    }

    @Test fun `multiple five minute delays preserve tomorrows regular time`() {
        val first = AlarmOccurrenceEngine.apply(alarm(), AlarmOccurrenceAction.DELAY, day - minute)
        val second = AlarmOccurrenceEngine.apply(first, AlarmOccurrenceAction.DELAY, day - minute)
        assertEquals(day + 10 * minute, second.scheduledAt)
        assertEquals(day, second.nextRegularAt)
        val fired = AlarmOccurrenceEngine.apply(second, AlarmOccurrenceAction.FIRE, second.scheduledAt)
        assertEquals(2 * day, fired.scheduledAt)
    }

    @Test fun `acknowledging snooze cancels it while keeping next daily occurrence`() {
        val fired = AlarmOccurrenceEngine.apply(alarm(), AlarmOccurrenceAction.FIRE, day)
        val snoozed = AlarmOccurrenceEngine.apply(fired, AlarmOccurrenceAction.SNOOZE, day + minute)
        assertTrue(AlarmOccurrenceEngine.isSnoozed(snoozed))
        assertEquals(2 * day, snoozed.nextRegularAt)
        val done = AlarmOccurrenceEngine.apply(snoozed, AlarmOccurrenceAction.ACKNOWLEDGE, day + 2 * minute)
        assertEquals(2 * day, done.scheduledAt)
        assertTrue(done.enabled)
        assertEquals(0, done.snoozeCount)
        assertFalse(AlarmOccurrenceEngine.needsAcknowledgment(done))
    }

    @Test fun `repeated snooze rings retain normal repeat time and snooze count`() {
        var state = AlarmOccurrenceEngine.apply(alarm(), AlarmOccurrenceAction.FIRE, day)
        repeat(3) {
            state = AlarmOccurrenceEngine.apply(state, AlarmOccurrenceAction.SNOOZE, day + it * minute)
            state = AlarmOccurrenceEngine.apply(state, AlarmOccurrenceAction.FIRE, state.scheduledAt)
        }
        assertEquals(2 * day, state.scheduledAt)
        assertEquals(3, state.snoozeCount)
        assertFalse(AlarmOccurrenceEngine.isSnoozed(state))
        val nextDay = AlarmOccurrenceEngine.apply(state, AlarmOccurrenceAction.FIRE, 2 * day)
        assertEquals(0, nextDay.snoozeCount)
    }

    @Test fun `stop snooze through skip action acknowledges without skipping tomorrow`() {
        val fired = AlarmOccurrenceEngine.apply(alarm(), AlarmOccurrenceAction.FIRE, day)
        val snoozed = AlarmOccurrenceEngine.apply(fired, AlarmOccurrenceAction.SNOOZE, day)
        val stopped = AlarmOccurrenceEngine.apply(snoozed, AlarmOccurrenceAction.SKIP, day + minute)
        assertEquals(2 * day, stopped.scheduledAt)
        assertFalse(AlarmOccurrenceEngine.needsAcknowledgment(stopped))
    }

    @Test fun `one shot skip or acknowledgment turns it off`() {
        assertFalse(AlarmOccurrenceEngine.apply(alarm(false), AlarmOccurrenceAction.SKIP, day - minute).enabled)
        val fired = AlarmOccurrenceEngine.apply(alarm(false), AlarmOccurrenceAction.FIRE, day)
        val snoozed = AlarmOccurrenceEngine.apply(fired, AlarmOccurrenceAction.SNOOZE, day)
        val done = AlarmOccurrenceEngine.apply(snoozed, AlarmOccurrenceAction.ACKNOWLEDGE, day + minute)
        assertFalse(done.enabled)
        assertFalse(AlarmOccurrenceEngine.needsAcknowledgment(done))
    }

    @Test fun `acknowledging an old disabled alarm does not enable it`() {
        val done = AlarmOccurrenceEngine.apply(alarm().copy(enabled = false, lastTriggeredAt = day), AlarmOccurrenceAction.ACKNOWLEDGE, 3 * day)
        assertFalse(done.enabled)
        assertFalse(AlarmOccurrenceEngine.needsAcknowledgment(done))
    }

    @Test fun `disabled alarms and future occurrences cannot fire early`() {
        val disabled = alarm().copy(enabled = false)
        assertEquals(disabled, AlarmOccurrenceEngine.apply(disabled, AlarmOccurrenceAction.FIRE, day))
        assertEquals(alarm(), AlarmOccurrenceEngine.apply(alarm(), AlarmOccurrenceAction.FIRE, day - minute))
    }

    @Test fun `delay and skip stale due alarm leave it unchanged`() {
        assertEquals(alarm(), AlarmOccurrenceEngine.apply(alarm(), AlarmOccurrenceAction.SKIP, day))
        assertEquals(alarm(), AlarmOccurrenceEngine.apply(alarm(), AlarmOccurrenceAction.DELAY, day))
    }

    @Test fun `re enabling a repeat catches up but past one shot requires editing`() {
        val repeating = AlarmOccurrenceEngine.apply(alarm().copy(enabled = false), AlarmOccurrenceAction.ENABLE, 3 * day)
        assertEquals(4 * day, repeating.scheduledAt)
        val once = alarm(false).copy(enabled = false)
        assertEquals(once, AlarmOccurrenceEngine.apply(once, AlarmOccurrenceAction.ENABLE, 3 * day))
    }

    @Test fun `disable during snooze retains the normal time for re enabling`() {
        val snoozed = AlarmOccurrenceEngine.apply(AlarmOccurrenceEngine.apply(alarm(), AlarmOccurrenceAction.FIRE, day), AlarmOccurrenceAction.SNOOZE, day)
        val off = AlarmOccurrenceEngine.apply(snoozed, AlarmOccurrenceAction.DISABLE, day + minute)
        assertFalse(off.enabled)
        assertEquals(2 * day, off.scheduledAt)
        assertEquals(2 * day, AlarmOccurrenceEngine.apply(off, AlarmOccurrenceAction.ENABLE, day + minute).scheduledAt)
    }
}
