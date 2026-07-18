package com.geepscontrol.core.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class TimerEngineTest {
    @Test fun `stopwatch derives elapsed from timestamps`() {
        val timer = ActiveTimer(mode = TimerMode.STOPWATCH, state = TimerState.RUNNING, startedAt = 1000, resumedAt = 2000, accumulatedMillis = 500)
        assertEquals(3500, TimerEngine.elapsed(timer, 5000))
    }

    @Test fun `pause freezes and resume restarts timestamp calculation`() {
        val timer = ActiveTimer(mode = TimerMode.COUNTDOWN, state = TimerState.RUNNING, startedAt = 0, intendedDurationMillis = 10_000)
        val paused = TimerEngine.pause(timer, 3000)
        assertEquals(3000, TimerEngine.elapsed(paused, 9000))
        assertEquals(5000, TimerEngine.elapsed(TimerEngine.resume(paused, 10_000), 12_000))
    }

    @Test fun `countdown supports overtime`() {
        val timer = ActiveTimer(mode = TimerMode.POMODORO, state = TimerState.RUNNING, startedAt = 0, intendedDurationMillis = 1000)
        assertEquals(-500, TimerEngine.remaining(timer, 1500))
    }
}
