package org.maaya.app.data.repository

import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.maaya.app.data.local.dao.IncidentDao
import org.maaya.app.data.local.entity.IncidentEntity
import org.maaya.app.data.remote.MaayaApiService
import org.maaya.app.data.remote.dto.CreateIncidentRequest
import org.maaya.app.data.remote.dto.LocationUpdateRequest
import org.maaya.app.data.remote.dto.ResolveIncidentRequest
import org.maaya.app.domain.model.EmergencyIncident
import org.maaya.app.domain.model.SosStatus
import org.maaya.app.domain.model.SosTriggerType

class IncidentRepository(
    private val incidentDao: IncidentDao,
    private val apiService: MaayaApiService
) {
    val incidents: Flow<List<EmergencyIncident>> = incidentDao.getAllIncidentsFlow().map { entities ->
        entities.map { it.toDomain() }
    }

    suspend fun getActiveIncident(): EmergencyIncident? {
        return incidentDao.getLatestIncidentByStatus(SosStatus.ACTIVE)?.toDomain()
    }

    suspend fun createAndPersistIncidentLocally(
        id: String,
        triggerType: SosTriggerType,
        latitude: Double,
        longitude: Double,
        accuracy: Float,
        notes: String? = null
    ): EmergencyIncident {
        val now = System.currentTimeMillis()
        val localIncident = IncidentEntity(
            id = id,
            startedAt = now,
            triggerType = triggerType,
            status = SosStatus.ACTIVE,
            initialLatitude = latitude,
            initialLongitude = longitude,
            currentLatitude = latitude,
            currentLongitude = longitude,
            accuracyMeters = accuracy,
            isSmsDispatched = false,
            isMediaStreamActive = false,
            notes = notes,
            isSyncedWithServer = false
        )
        // CRITICAL REQUIREMENT: Persist locally BEFORE network operations
        incidentDao.insertIncident(localIncident)
        return localIncident.toDomain()
    }

    suspend fun syncIncidentWithServer(
        incidentId: String,
        triggerType: SosTriggerType,
        latitude: Double,
        longitude: Double,
        accuracy: Float,
        heartRate: Int? = null,
        battery: Int? = null
    ): Result<EmergencyIncident> {
        return try {
            val response = apiService.createIncident(
                CreateIncidentRequest(
                    id = incidentId,
                    triggerType = triggerType.name,
                    latitude = latitude,
                    longitude = longitude,
                    accuracy = accuracy,
                    heartRateBpm = heartRate,
                    batteryLevel = battery
                )
            )

            if (response.isSuccessful && response.body() != null) {
                val data = response.body()!!
                val existing = incidentDao.getIncidentById(incidentId)
                if (existing != null) {
                    val updated = existing.copy(
                        dashboardAccessToken = data.dashboardAccessToken,
                        mediaSessionId = data.incidentId,
                        isSyncedWithServer = true
                    )
                    incidentDao.updateIncident(updated)
                    Result.success(updated.toDomain())
                } else {
                    Result.failure(IllegalStateException("Local incident not found for ID $incidentId"))
                }
            } else {
                Log.w("IncidentRepo", "Backend sync returned error code: ${response.code()}")
                Result.failure(RuntimeException("Server returned ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("IncidentRepo", "Failed to sync incident with server (device offline or unreachable)", e)
            Result.failure(e)
        }
    }

    suspend fun updateLocation(
        incidentId: String,
        latitude: Double,
        longitude: Double,
        accuracy: Float,
        speed: Float? = null,
        heartRate: Int? = null,
        battery: Int? = null
    ) {
        val local = incidentDao.getIncidentById(incidentId) ?: return
        incidentDao.updateIncident(
            local.copy(
                currentLatitude = latitude,
                currentLongitude = longitude,
                accuracyMeters = accuracy
            )
        )

        // Remote transmission is non-blocking with error catching
        try {
            apiService.updateLocation(
                LocationUpdateRequest(
                    incidentId = incidentId,
                    latitude = latitude,
                    longitude = longitude,
                    accuracy = accuracy,
                    speed = speed,
                    batteryPercentage = battery,
                    heartRateBpm = heartRate
                )
            )
        } catch (e: Exception) {
            Log.w("IncidentRepo", "Non-fatal: could not transmit live coordinate update", e)
        }
    }

    suspend fun resolveIncident(
        incidentId: String,
        pin: String,
        reason: String
    ): Result<Unit> {
        val local = incidentDao.getIncidentById(incidentId)
        if (local != null) {
            val resolved = local.copy(
                status = SosStatus.RESOLVED,
                resolvedAt = System.currentTimeMillis()
            )
            incidentDao.updateIncident(resolved)
        }

        try {
            apiService.resolveIncident(
                ResolveIncidentRequest(
                    incidentId = incidentId,
                    safetyPin = pin,
                    resolutionReason = reason
                )
            )
        } catch (e: Exception) {
            Log.w("IncidentRepo", "Non-fatal: server resolution failed offline", e)
        }

        return Result.success(Unit)
    }

    suspend fun updateMediaStreamingState(incidentId: String, isActive: Boolean, sessionId: String?) {
        val local = incidentDao.getIncidentById(incidentId) ?: return
        incidentDao.updateIncident(
            local.copy(
                isMediaStreamActive = isActive,
                mediaSessionId = sessionId ?: local.mediaSessionId
            )
        )
    }

    suspend fun updateSmsDispatched(incidentId: String, isDispatched: Boolean) {
        val local = incidentDao.getIncidentById(incidentId) ?: return
        incidentDao.updateIncident(local.copy(isSmsDispatched = isDispatched))
    }

    private fun IncidentEntity.toDomain() = EmergencyIncident(
        id = id,
        startedAt = startedAt,
        resolvedAt = resolvedAt,
        triggerType = triggerType,
        status = status,
        initialLatitude = initialLatitude,
        initialLongitude = initialLongitude,
        currentLatitude = currentLatitude,
        currentLongitude = currentLongitude,
        accuracyMeters = accuracyMeters,
        isSmsDispatched = isSmsDispatched,
        isMediaStreamActive = isMediaStreamActive,
        mediaSessionId = mediaSessionId,
        dashboardAccessToken = dashboardAccessToken,
        notes = notes
    )
}
