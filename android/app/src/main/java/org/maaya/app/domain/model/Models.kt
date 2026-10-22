package org.maaya.app.domain.model

enum class SosStatus {
    IDLE,
    COUNTDOWN,
    ACTIVE,
    RESOLVED,
    CANCELLED
}

enum class SosTriggerType {
    MANUAL_IN_APP,
    SHAKE_GESTURE,
    SMARTWATCH_BUTTON,
    HEART_RATE_ANOMALY,
    FALL_DETECTED,
    LOW_BATTERY_ESCALATION,
    SAFE_RIDE_OVERDUE
}

data class EmergencyIncident(
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
    val notes: String? = null
)

data class HealthMetric(
    val id: Long = 0,
    val heartRateBpm: Int,
    val sensorContactDetected: Boolean = true,
    val bloodOxygenSpO2: Float? = null,
    val watchBatteryPercentage: Int? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isAnomaly: Boolean = false
)

data class BleDeviceItem(
    val name: String,
    val address: String,
    val rssi: Int,
    val isConnected: Boolean = false,
    val supportedServicesSummary: List<String> = emptyList()
)

data class TrustedContact(
    val id: Long = 0,
    val name: String,
    val phoneNumber: String,
    val relationship: String,
    val isEmergencyPriority: Boolean = true,
    val isSmsNotificationEnabled: Boolean = true
)

data class SafeRideSession(
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
    val isEscalatedToSos: Boolean = false,
    val cabPhotoUri: String? = null,
    val cabModel: String? = null
)
