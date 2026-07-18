package com.geepscontrol.core.domain

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NotificationPolicyTest {
    @Test fun `overnight quiet hours wrap midnight`() {
        assertTrue(NotificationPolicy.isQuietHour(23, true, 22, 7))
        assertTrue(NotificationPolicy.isQuietHour(3, true, 22, 7))
        assertFalse(NotificationPolicy.isQuietHour(12, true, 22, 7))
    }

    @Test fun `disabled quiet hours never suppress`() {
        assertFalse(NotificationPolicy.isQuietHour(23, false, 22, 7))
    }
}
