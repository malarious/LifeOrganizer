package com.geepscontrol.core.data

import com.geepscontrol.core.database.ItineraryEntryEntity
import com.geepscontrol.core.database.AlarmProfileEntity
import com.geepscontrol.core.database.StandaloneAlarmEntity
import com.geepscontrol.core.database.TripEntity
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

class DataPortabilityTest {
    private val json = Json { encodeDefaults = true }

    @Test fun `valid export round trips`() {
        val bundle = ExportBundle(exportedAt = 1, projects = listOf(ProjectDto(1, "Home", "", "", "ACTIVE", "NORMAL", 0, 0)), tasks = listOf(TaskDto(2, 1, "Repair", "", "READY", "NORMAL", 30, "MEDIUM", null, null, null, null, 0, 0, null, 0, emptyList(), emptyList(), emptyList())), contexts = emptyList(), tags = emptyList(), sessions = emptyList(), reviews = emptyList(), alarms = emptyList())
        assertEquals(bundle, decodeAndValidateExport(json.encodeToString(bundle), json))
    }

    @Test fun `invalid reference is rejected before import`() {
        val bundle = ExportBundle(exportedAt = 1, projects = emptyList(), tasks = listOf(TaskDto(2, 99, "Broken", "", "READY", "NORMAL", null, "MEDIUM", null, null, null, null, 0, 0, null, 0, emptyList(), emptyList(), emptyList())), contexts = emptyList(), tags = emptyList(), sessions = emptyList(), reviews = emptyList(), alarms = emptyList())
        assertFails { decodeAndValidateExport(json.encodeToString(bundle), json) }
    }

    @Test fun `invalid enum is rejected during preflight`() {
        val bundle = ExportBundle(exportedAt = 1, projects = listOf(ProjectDto(1, "Home", "", "", "NOT_A_STATUS", "NORMAL", 0, 0)), tasks = emptyList(), contexts = emptyList(), tags = emptyList(), sessions = emptyList(), reviews = emptyList(), alarms = emptyList())
        assertFails { decodeAndValidateExport(json.encodeToString(bundle), json) }
    }

    @Test fun `trip relationships validate before import`() {
        val trip = TripEntity(5, "Japan", startDate = 0, endDate = 100, homeTimezone = "America/Detroit", createdAt = 0, updatedAt = 0)
        val valid = ExportBundle(exportedAt = 1, projects = emptyList(), tasks = emptyList(), contexts = emptyList(), tags = emptyList(), sessions = emptyList(), reviews = emptyList(), alarms = emptyList(), trips = listOf(trip), itineraryEntries = listOf(ItineraryEntryEntity(7, 5, title = "Flight", timezone = "Asia/Tokyo", createdAt = 0, updatedAt = 0)))
        assertEquals(1, decodeAndValidateExport(json.encodeToString(valid), json).itineraryEntries.size)
        assertFails { decodeAndValidateExport(json.encodeToString(valid.copy(itineraryEntries = valid.itineraryEntries.map { it.copy(tripId = 99) })), json) }
    }

    @Test fun `alarm profiles and challenge settings round trip with references`() {
        val profile = AlarmProfileEntity(id = 4, name = "Wake", dismissalMethod = "NUMBER", challengeRequiredForDismiss = true, createdAt = 0, updatedAt = 0)
        val alarm = StandaloneAlarmEntity(id = 8, label = "Up", scheduledAt = 100, profileId = 4, createdAt = 0, updatedAt = 0)
        val bundle = ExportBundle(exportedAt = 1, projects = emptyList(), tasks = emptyList(), contexts = emptyList(), tags = emptyList(), sessions = emptyList(), reviews = emptyList(), alarms = emptyList(), alarmProfiles = listOf(profile), standaloneAlarms = listOf(alarm))
        assertEquals("NUMBER", decodeAndValidateExport(json.encodeToString(bundle), json).alarmProfiles.single().dismissalMethod)
        assertFails { decodeAndValidateExport(json.encodeToString(bundle.copy(alarmProfiles = emptyList())), json) }
    }
}
