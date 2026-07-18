package com.geepscontrol.ui

import com.geepscontrol.core.domain.AppDestination
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AppDestinationTest {
    @Test
    fun `all required destinations exist`() {
        assertEquals(10, AppDestination.entries.size)
        assertEquals("Today", AppDestination.entries.first().title)
    }

    @Test
    fun `phone navigation exposes five primary destinations`() {
        assertTrue(AppDestination.entries.count { it.primary } == 5)
    }
}
