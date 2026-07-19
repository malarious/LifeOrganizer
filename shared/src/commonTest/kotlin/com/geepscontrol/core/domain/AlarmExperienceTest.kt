package com.geepscontrol.core.domain

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AlarmExperienceTest {
    @Test fun `challenge grace grows with difficulty`() {
        assertEquals(5, AlarmExperience.challengeGraceSeconds(AlarmDifficulty.EASY))
        assertEquals(10, AlarmExperience.challengeGraceSeconds(AlarmDifficulty.HARD))
    }

    @Test fun `math challenges carry their answer`() {
        AlarmDifficulty.entries.forEach { difficulty ->
            val (prompt, answer) = AlarmExperience.mathChallenge(difficulty, kotlin.random.Random(7))
            kotlin.test.assertTrue(prompt.isNotBlank())
            kotlin.test.assertTrue(answer >= 0)
        }
    }
    @Test fun `volume ramp interpolates and clamps`() {
        assertEquals(0.2f, AlarmExperience.volumeAt(0, 20, 80, 60))
        assertEquals(0.5f, AlarmExperience.volumeAt(30_000, 20, 80, 60), 0.001f)
        assertEquals(0.8f, AlarmExperience.volumeAt(90_000, 20, 80, 60))
    }

    @Test fun `snooze limit and delay rules are deterministic`() {
        assertTrue(AlarmExperience.canSnooze(2, 3))
        assertFalse(AlarmExperience.canSnooze(3, 3))
        assertFalse(AlarmExperience.vibrationShouldStart(29_999, 30))
        assertTrue(AlarmExperience.vibrationShouldStart(30_000, 30))
    }

    @Test fun `number challenges match difficulty and validate exactly`() {
        val value = AlarmExperience.numberChallenge(AlarmDifficulty.NORMAL, Random(7))
        assertEquals(4, value.length)
        assertTrue(AlarmExperience.validateNumber(value, value))
        assertFalse(AlarmExperience.validateNumber(value, "$value 1"))
    }

    @Test fun `hold and trace completion use explicit thresholds`() {
        assertTrue(AlarmExperience.holdComplete(5_000, 5))
        assertFalse(AlarmExperience.traceComplete(8, 10))
        assertTrue(AlarmExperience.traceComplete(9, 10))
    }
}
