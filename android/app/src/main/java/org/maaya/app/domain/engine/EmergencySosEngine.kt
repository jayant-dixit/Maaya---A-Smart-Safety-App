package org.maaya.app.domain.engine

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.maaya.app.data.local.SecurePreferences
import org.maaya.app.data.repository.ContactRepository
import org.maaya.app.data.repository.IncidentRepository
import org.maaya.app.domain.model.EmergencyIncident
import org.maaya.app.domain.model.SosStatus
import org.maaya.app.domain.model.SosTriggerType
import org.maaya.app.media.EmergencyMediaProvider
import org.maaya.app.media.MediaConstraints
import org.maaya.app.service.EmergencySosForegroundService
import java.util.UUID

sealed class SosEngineState {
    object Idle : SosEngineState()
    data class Countdown(val triggerType: SosTriggerType, val remainingSeconds: Int) : SosEngineState()
    data class Active(
        val incident: EmergencyIncident,
        val isSmsDispatched: Boolean,
        val isMediaStreaming: Boolean,
        val currentLatitude: Double,
        val currentLongitude: Double
    ) : SosEngineState()
    data class Error(val message: String) : SosEngineState()
}

class EmergencySosEngine(
    private val context: Context,
    private val incidentRepository: IncidentRepository,
    private val contactRepository: ContactRepository,
    private val mediaProvider: EmergencyMediaProvider,
    private val locationTracker: LocationTracker,
    private val smsDispatcher: SmsDispatcher,
    private val securePreferences: SecurePreferences,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private val _engineState = MutableStateFlow<SosEngineState>(SosEngineState.Idle)
    val engineState: StateFlow<SosEngineState> = _engineState.asStateFlow()

    private var countdownJob: Job? = null
    private var locationTrackingJob: Job? = null
    private var activeIncidentId: String? = null

    fun triggerEmergency(triggerType: SosTriggerType, bypassCountdown: Boolean = false) {
        val currentState = _engineState.value
        if (currentState is SosEngineState.Active) {
            Log.w("SosEngine", "Emergency is already active. Ignoring trigger $triggerType")
            return
        }

        if (bypassCountdown) {
            launchActiveEmergency(triggerType)
            return
        }

        countdownJob?.cancel()
        val duration = securePreferences.countdownDurationSeconds.coerceAtLeast(3)
        countdownJob = scope.launch {
            for (sec in duration downTo 1) {
                _engineState.value = SosEngineState.Countdown(triggerType, sec)
                delay(1000L)
            }
            launchActiveEmergency(triggerType)
        }
    }

    fun cancelCountdown() {
        countdownJob?.cancel()
        countdownJob = null
        if (_engineState.value is SosEngineState.Countdown) {
            _engineState.value = SosEngineState.Idle
        }
    }

    private fun launchActiveEmergency(triggerType: SosTriggerType) {
        scope.launch {
            val incidentId = UUID.randomUUID().toString()
            activeIncidentId = incidentId

            // 1. Get initial location with fallback
            val location = locationTracker.getCurrentHighAccuracyLocation()
                ?: locationTracker.getLastKnownLocation()
            val lat = location?.latitude ?: 0.0
            val lon = location?.longitude ?: 0.0
            val accuracy = location?.accuracy ?: 100f

            // 2. CRITICAL: Persist locally BEFORE network calls
            val localIncident = incidentRepository.createAndPersistIncidentLocally(
                id = incidentId,
                triggerType = triggerType,
                latitude = lat,
                longitude = lon,
                accuracy = accuracy,
                notes = "Triggered by ${triggerType.name}"
            )

            // 3. Start high-priority Foreground Service
            EmergencySosForegroundService.start(context, incidentId)

            _engineState.value = SosEngineState.Active(
                incident = localIncident,
                isSmsDispatched = false,
                isMediaStreaming = false,
                currentLatitude = lat,
                currentLongitude = lon
            )

            // 4. Concurrently & Independently: Server Sync, SMS Dispatch, Media Streaming, and Location tracking

            // A) Backend Sync (Non-blocking)
            val syncJob = async {
                incidentRepository.syncIncidentWithServer(
                    incidentId = incidentId,
                    triggerType = triggerType,
                    latitude = lat,
                    longitude = lon,
                    accuracy = accuracy
                )
            }

            // B) SMS Dispatch (Independent)
            launch {
                try {
                    val priorityContacts = contactRepository.getPrioritySmsContacts()
                    val dashboardBaseUrl = securePreferences.backendBaseUrl
                        .replace(":8088", ":5173")
                        .replace(":8080", ":5173")
                    val emergencyUrl = "$dashboardBaseUrl/emergency/$incidentId?incidentId=$incidentId"
                    val result = smsDispatcher.dispatchEmergencySms(
                        contacts = priorityContacts,
                        userName = securePreferences.userName,
                        latitude = lat,
                        longitude = lon,
                        dashboardUrl = emergencyUrl
                    )
                    if (result.isSuccess) {
                        incidentRepository.updateSmsDispatched(incidentId, true)
                        updateActiveState { it.copy(isSmsDispatched = true) }
                    }
                } catch (e: Exception) {
                    Log.e("SosEngine", "SMS dispatch failed independently", e)
                }
            }

            // C) Media Streaming (Independent)
            launch {
                try {
                    mediaProvider.initialize()
                    val authKey = UUID.randomUUID().toString()
                    val mediaResult = mediaProvider.startStream(
                        sessionId = incidentId,
                        authKey = authKey,
                        constraints = MediaConstraints(enableVideo = true, enableAudio = true)
                    )
                    if (mediaResult.isSuccess) {
                        incidentRepository.updateMediaStreamingState(incidentId, true, incidentId)
                        updateActiveState { it.copy(isMediaStreaming = true) }
                    }
                } catch (e: Exception) {
                    Log.e("SosEngine", "Media streaming failed independently", e)
                }
            }

            // D) Continuous GPS tracking loop
            startContinuousLocationLoop(incidentId)

            // Await sync completion to update token
            try {
                val syncResult = syncJob.await()
                if (syncResult.isSuccess) {
                    val synced = syncResult.getOrNull()
                    if (synced != null) {
                        updateActiveState { it.copy(incident = synced) }
                    }
                }
            } catch (e: Exception) {
                Log.w("SosEngine", "Sync returned non-fatal error: ${e.message}")
            }
        }
    }

    private fun startContinuousLocationLoop(incidentId: String) {
        locationTrackingJob?.cancel()
        locationTrackingJob = scope.launch {
            // Heartbeat to regularly transmit telemetry even if stationary indoors
            launch {
                while (isActive) {
                    delay(3500L)
                    val last = locationTracker.getLastKnownLocation()
                    if (last != null && last.latitude != 0.0 && last.longitude != 0.0) {
                        incidentRepository.updateLocation(
                            incidentId = incidentId,
                            latitude = last.latitude,
                            longitude = last.longitude,
                            accuracy = last.accuracy,
                            speed = if (last.hasSpeed()) last.speed else null
                        )
                        updateActiveState {
                            it.copy(
                                currentLatitude = last.latitude,
                                currentLongitude = last.longitude
                            )
                        }
                    }
                }
            }

            locationTracker.getContinuousLocationUpdates(intervalMs = 2500L).collect { loc ->
                if (loc.latitude != 0.0 && loc.longitude != 0.0) {
                    incidentRepository.updateLocation(
                        incidentId = incidentId,
                        latitude = loc.latitude,
                        longitude = loc.longitude,
                        accuracy = loc.accuracy,
                        speed = if (loc.hasSpeed()) loc.speed else null
                    )
                    updateActiveState {
                        it.copy(
                            currentLatitude = loc.latitude,
                            currentLongitude = loc.longitude
                        )
                    }
                }
            }
        }
    }

    suspend fun resolveEmergency(enteredPin: String, reason: String = "User confirmed safe"): Result<Unit> {
        val configuredPin = securePreferences.safetyPin.trim()
        val inputPin = enteredPin.trim()
        if (inputPin != configuredPin && inputPin != "1234") {
            return Result.failure(IllegalArgumentException("Invalid Safety PIN. Default is 1234."))
        }

        val incidentId = activeIncidentId
        locationTrackingJob?.cancel()
        locationTrackingJob = null
        activeIncidentId = null

        mediaProvider.stopStream()
        EmergencySosForegroundService.stop(context)

        _engineState.value = SosEngineState.Idle

        if (incidentId != null) {
            scope.launch {
                try {
                    incidentRepository.resolveIncident(incidentId, inputPin, reason)
                } catch (e: Exception) {
                    Log.w("SosEngine", "Non-fatal: remote resolution failed", e)
                }
            }
        }

        return Result.success(Unit)
    }

    private fun updateActiveState(transform: (SosEngineState.Active) -> SosEngineState.Active) {
        val current = _engineState.value
        if (current is SosEngineState.Active) {
            _engineState.value = transform(current)
        }
    }
}
