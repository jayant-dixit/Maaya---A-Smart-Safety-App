package org.maaya.app.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.maaya.app.data.local.dao.SafeRideDao
import org.maaya.app.data.local.entity.SafeRideEntity
import org.maaya.app.data.remote.MaayaApiService
import org.maaya.app.data.remote.dto.SafeRideCheckInRequest
import org.maaya.app.data.remote.dto.SafeRideStartRequest
import org.maaya.app.domain.model.SafeRideSession
import java.util.UUID

class SafeRideRepository(
    private val safeRideDao: SafeRideDao,
    private val apiService: MaayaApiService
) {
    val activeRideFlow: Flow<SafeRideSession?> = safeRideDao.getActiveRideFlow().map { it?.toDomain() }
    val allRidesFlow: Flow<List<SafeRideSession>> = safeRideDao.getAllRidesFlow().map { list -> list.map { it.toDomain() } }

    suspend fun getActiveRide(): SafeRideSession? {
        return safeRideDao.getActiveRide()?.toDomain()
    }

    suspend fun startRide(
        vehiclePlate: String,
        driverName: String,
        destinationName: String,
        destinationLatitude: Double?,
        destinationLongitude: Double?,
        expectedArrivalMinutes: Int,
        checkInIntervalMinutes: Int,
        cabPhotoUri: String? = null,
        cabModel: String? = null
    ): SafeRideSession {
        val now = System.currentTimeMillis()
        val rideId = UUID.randomUUID().toString()
        val entity = SafeRideEntity(
            id = rideId,
            vehiclePlate = vehiclePlate,
            driverName = driverName,
            destinationName = destinationName,
            destinationLatitude = destinationLatitude,
            destinationLongitude = destinationLongitude,
            expectedArrivalMinutes = expectedArrivalMinutes,
            checkInIntervalMinutes = checkInIntervalMinutes,
            startedAt = now,
            lastCheckInAt = now,
            isCompleted = false,
            isEscalatedToSos = false,
            cabPhotoUri = cabPhotoUri,
            cabModel = cabModel
        )
        safeRideDao.insertOrUpdateRide(entity)

        try {
            apiService.startSafeRide(
                SafeRideStartRequest(
                    rideId = rideId,
                    vehiclePlate = vehiclePlate,
                    driverName = driverName,
                    destinationName = destinationName,
                    destinationLatitude = destinationLatitude,
                    destinationLongitude = destinationLongitude,
                    expectedArrivalMinutes = expectedArrivalMinutes,
                    checkInIntervalMinutes = checkInIntervalMinutes
                )
            )
        } catch (_: Exception) {}

        return entity.toDomain()
    }

    suspend fun recordCheckIn(rideId: String, lat: Double, lon: Double) {
        val active = safeRideDao.getActiveRide() ?: return
        if (active.id == rideId) {
            val updated = active.copy(lastCheckInAt = System.currentTimeMillis())
            safeRideDao.insertOrUpdateRide(updated)

            try {
                apiService.checkInSafeRide(
                    SafeRideCheckInRequest(
                        rideId = rideId,
                        latitude = lat,
                        longitude = lon
                    )
                )
            } catch (_: Exception) {}
        }
    }

    suspend fun completeRide(rideId: String) {
        safeRideDao.markRideCompleted(rideId)
        try {
            apiService.endSafeRide(mapOf("rideId" to rideId))
        } catch (_: Exception) {}
    }

    private fun SafeRideEntity.toDomain() = SafeRideSession(
        id = id,
        vehiclePlate = vehiclePlate,
        driverName = driverName,
        destinationName = destinationName,
        destinationLatitude = destinationLatitude,
        destinationLongitude = destinationLongitude,
        expectedArrivalMinutes = expectedArrivalMinutes,
        checkInIntervalMinutes = checkInIntervalMinutes,
        startedAt = startedAt,
        lastCheckInAt = lastCheckInAt,
        isCompleted = isCompleted,
        isEscalatedToSos = isEscalatedToSos,
        cabPhotoUri = cabPhotoUri,
        cabModel = cabModel
    )
}
