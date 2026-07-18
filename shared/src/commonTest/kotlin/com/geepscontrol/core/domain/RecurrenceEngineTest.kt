package com.geepscontrol.core.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RecurrenceEngineTest {
    @Test fun `fixed recurrence catches up beyond completion`() {
        val day = RecommendationService.DAY_MILLIS
        val next = RecurrenceEngine.nextTimestamp(RecurrenceRule(1, RecurrenceUnit.DAY), previousScheduledOrDue = day, completedAt = 3 * day)
        assertTrue(next > 3 * day)
        assertEquals(4 * day, next)
    }

    @Test fun `after completion recurrence anchors to completion`() {
        val day = RecommendationService.DAY_MILLIS
        val next = RecurrenceEngine.nextTimestamp(RecurrenceRule(2, RecurrenceUnit.WEEK, RecurrenceMode.AFTER_COMPLETION), day, 10 * day)
        assertEquals(24 * day, next)
    }

    @Test fun `rule round trips`() {
        val rule = RecurrenceRule(3, RecurrenceUnit.MONTH, RecurrenceMode.AFTER_COMPLETION)
        assertEquals(rule, RecurrenceRule.decode(rule.encode()))
    }
}
