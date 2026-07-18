package com.geepscontrol.core.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class AlarmRepeatEngineTest {
    @Test
    fun `hourly and custom day intervals advance correctly`() {
        assertEquals(6 * 3_600_000L, AlarmRepeatEngine.next(AlarmRepeatRule(6, AlarmRepeatUnit.HOUR), 0))
        assertEquals(3 * 86_400_000L, AlarmRepeatEngine.next(AlarmRepeatRule(3, AlarmRepeatUnit.DAY), 0))
    }

    @Test
    fun `alarm recurrence round trips weekday selections`() {
        val rule = AlarmRepeatRule(2, AlarmRepeatUnit.WEEK, setOf(1, 3, 5))
        assertEquals(rule, AlarmRepeatRule.decode(rule.encode()))
    }

    @Test
    fun `next future catches up stale alarms but preserves an occurrence already advanced on fire`() {
        val rule = AlarmRepeatRule(1, AlarmRepeatUnit.DAY)
        val day = 86_400_000L

        assertEquals(3 * day, AlarmRepeatEngine.nextFuture(rule, day, 2 * day))
        assertEquals(3 * day, AlarmRepeatEngine.nextFuture(rule, 3 * day, 2 * day))
    }
}
