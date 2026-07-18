package com.geepscontrol.core.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "trips", indices = [Index("startDate"), Index("endDate")])
@Serializable
data class TripEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val startDate: Long,
    val endDate: Long,
    val homeTimezone: String,
    val destinationTimezones: String = "",
    val status: String = "PLANNING",
    val coverColor: Long = 0xff3f51b5,
    val notes: String = "",
    val createdAt: Long,
    val updatedAt: Long,
    val archivedAt: Long? = null,
)

@Entity(tableName = "itinerary_entries", foreignKeys = [ForeignKey(entity = TripEntity::class, parentColumns = ["id"], childColumns = ["tripId"], onDelete = ForeignKey.CASCADE)], indices = [Index("tripId"), Index("startAt")])
@Serializable
data class ItineraryEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tripId: Long,
    val type: String = "GENERAL",
    val title: String,
    val description: String = "",
    val startAt: Long? = null,
    val endAt: Long? = null,
    val allDay: Boolean = false,
    val timezone: String,
    val departureLocation: String = "",
    val arrivalLocation: String = "",
    val address: String = "",
    val confirmationNumber: String = "",
    val bookingReference: String = "",
    val provider: String = "",
    val contactName: String = "",
    val contactPhone: String = "",
    val contactEmail: String = "",
    val website: String = "",
    val notes: String = "",
    val status: String = "PLANNED",
    val sortOrder: Long = 0,
    val leaveByAt: Long? = null,
    val reminderOffsets: String = "",
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(tableName = "transportation_details", primaryKeys = ["itineraryEntryId"], foreignKeys = [ForeignKey(entity = ItineraryEntryEntity::class, parentColumns = ["id"], childColumns = ["itineraryEntryId"], onDelete = ForeignKey.CASCADE)])
@Serializable
data class TransportationDetailsEntity(
    val itineraryEntryId: Long,
    val carrier: String = "",
    val serviceNumber: String = "",
    val departureTerminal: String = "",
    val departureGate: String = "",
    val arrivalTerminal: String = "",
    val arrivalGate: String = "",
    val seat: String = "",
    val platform: String = "",
    val coach: String = "",
    val boardingAt: Long? = null,
    val checkInAt: Long? = null,
    val baggageNotes: String = "",
    val ticketReference: String = "",
)

@Entity(tableName = "lodging_details", primaryKeys = ["itineraryEntryId"], foreignKeys = [ForeignKey(entity = ItineraryEntryEntity::class, parentColumns = ["id"], childColumns = ["itineraryEntryId"], onDelete = ForeignKey.CASCADE)])
@Serializable
data class LodgingDetailsEntity(
    val itineraryEntryId: Long,
    val propertyName: String = "",
    val checkInAt: Long? = null,
    val checkOutAt: Long? = null,
    val address: String = "",
    val roomInformation: String = "",
    val confirmationNumber: String = "",
    val contactPhone: String = "",
    val checkInInstructions: String = "",
    val luggageStorageNotes: String = "",
    val accessCode: String = "",
    val cancellationNotes: String = "",
)

@Entity(tableName = "trip_references", foreignKeys = [ForeignKey(entity = TripEntity::class, parentColumns = ["id"], childColumns = ["tripId"], onDelete = ForeignKey.CASCADE)], indices = [Index("tripId")])
@Serializable
data class TripReferenceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tripId: Long,
    val category: String = "GENERAL",
    val title: String,
    val content: String = "",
    val address: String = "",
    val phone: String = "",
    val website: String = "",
    val sensitive: Boolean = false,
    val sortOrder: Long = 0,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(tableName = "trip_tasks", primaryKeys = ["tripId", "taskId"], foreignKeys = [ForeignKey(entity = TripEntity::class, parentColumns = ["id"], childColumns = ["tripId"], onDelete = ForeignKey.CASCADE), ForeignKey(entity = TaskEntity::class, parentColumns = ["id"], childColumns = ["taskId"], onDelete = ForeignKey.CASCADE)], indices = [Index("taskId")])
@Serializable
data class TripTaskEntity(
    val tripId: Long,
    val taskId: Long,
)
