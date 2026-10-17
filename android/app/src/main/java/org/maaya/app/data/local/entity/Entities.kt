package org.maaya.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.maaya.app.domain.model.SosStatus
import org.maaya.app.domain.model.SosTriggerType

@Entity(tableName = "trusted_contacts")
data class ContactEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val phoneNumber: String,
    val relationship: String,
    val isEmergencyPriority: Boolean = true,
    val isSmsNotificationEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "emergency_incidents")
data class IncidentEntity(
    @PrimaryKey
    val id: String,
    val startedAt: Long,
    val resolvedAt: Long? = null,
    val triggerType: SosTriggerType,
    val status: SosStatus,
    val initialLatitude: Double,
    val initialLongitude: Double,
    val currentLatitude: Double,
    val currentLongitude: Double,
    val accuracyMeters: Float,
    val isSmsDispatched: Boolean = false,
    val isMediaStreamActive: Boolean = false,
    val mediaSessionId: String? = null,
    val dashboardAccessToken: String? = null,
    val notes: String? = null,
    val isSyncedWithServer: Boolean = false
)

@Entity(tableName = "health_metrics")
data class HealthMetricEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val heartRateBpm: Int,
    val sensorContactDetected: Boolean = true,
    val bloodOxygenSpO2: Float? = null,
    val watchBatteryPercentage: Int? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isAnomaly: Boolean = false
)

@Entity(tableName = "safe_ride_sessions")
data class SafeRideEntity(
    @PrimaryKey
    val id: String,
    val vehiclePlate: String,
    val driverName: String,
    val destinationName: String,
    val destinationLatitude: Double?,
    val destinationLongitude: Double?,
    val expectedArrivalMinutes: Int,
    val checkInIntervalMinutes: Int,
    val startedAt: Long,
    val lastCheckInAt: Long,
    val isCompleted: Boolean = false,
    val isEscalatedToSos: Boolean = false
)
