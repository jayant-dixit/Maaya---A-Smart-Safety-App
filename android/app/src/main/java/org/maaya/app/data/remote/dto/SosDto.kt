package org.maaya.app.data.remote.dto

data class CreateIncidentRequest(
    val id: String? = null,
    val incidentId: String? = null,
    val triggerType: String,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val heartRateBpm: Int? = null,
    val batteryLevel: Int? = null,
    val notes: String? = null
)

data class IncidentResponse(
    val incidentId: String,
    val dashboardAccessToken: String,
    val dashboardViewerUrl: String,
    val streamToken: String,
    val status: String,
    val createdAt: Long
)

data class LocationUpdateRequest(
    val incidentId: String,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val speed: Float? = null,
    val altitude: Double? = null,
    val batteryPercentage: Int? = null,
    val heartRateBpm: Int? = null
)

data class ResolveIncidentRequest(
    val incidentId: String,
    val safetyPin: String,
    val resolutionReason: String
)

data class SafeRideStartRequest(
    val rideId: String,
    val vehiclePlate: String,
    val driverName: String,
    val destinationName: String,
    val destinationLatitude: Double?,
    val destinationLongitude: Double?,
    val expectedArrivalMinutes: Int,
    val checkInIntervalMinutes: Int
)

data class SafeRideCheckInRequest(
    val rideId: String,
    val latitude: Double,
    val longitude: Double
)

data class GenericApiResponse(
    val success: Boolean,
    val message: String
)
