package org.maaya.app.data.repository

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.maaya.app.ble.BleConnectionState
import org.maaya.app.ble.BleGattManager
import org.maaya.app.data.local.dao.HealthMetricDao
import org.maaya.app.data.local.entity.HealthMetricEntity
import org.maaya.app.domain.model.BleDeviceItem
import org.maaya.app.domain.model.HealthMetric

class BleHealthRepository(
    private val gattManager: BleGattManager,
    private val healthMetricDao: HealthMetricDao,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    val connectionState: Flow<BleConnectionState> = gattManager.connectionState
    val scannedDevices: Flow<List<BleDeviceItem>> = gattManager.scannedDevices

    val recentMetrics: Flow<List<HealthMetric>> = healthMetricDao.getRecentMetricsFlow().map { list ->
        list.map { it.toDomain() }
    }

    private val _anomalyTriggers = MutableSharedFlow<HealthMetric>(extraBufferCapacity = 16)
    val anomalyTriggers: SharedFlow<HealthMetric> = _anomalyTriggers.asSharedFlow()

    val hardwareSosTriggers: SharedFlow<String> = gattManager.hardwareSosTriggers

    private val rollingHrWindow = ArrayDeque<Int>(15)

    init {
        // Collect health metrics from GATT manager, persist them to Room, and evaluate stress anomalies
        scope.launch {
            gattManager.healthEvents.collect { metric ->
                val isAnomaly = evaluateAnomaly(metric.heartRateBpm)
                val finalMetric = metric.copy(isAnomaly = isAnomaly)

                healthMetricDao.insertMetric(finalMetric.toEntity())

                if (isAnomaly) {
                    _anomalyTriggers.emit(finalMetric)
                }
            }
        }
    }

    private fun evaluateAnomaly(bpm: Int): Boolean {
        if (bpm <= 0) return false
        rollingHrWindow.addLast(bpm)
        if (rollingHrWindow.size > 15) {
            rollingHrWindow.removeFirst()
        }

        val baseline = rollingHrWindow.average()
        // Anomaly: Heart rate spikes above 145 bpm or exceeds baseline by > 50 bpm suddenly
        return bpm >= 145 || (rollingHrWindow.size >= 5 && (bpm - baseline) >= 50.0)
    }

    fun startScan(): Result<Unit> = gattManager.startScan()
    fun stopScan() = gattManager.stopScan()
    fun connectDevice(address: String): Result<Unit> = gattManager.connect(address)
    fun disconnect() = gattManager.disconnect()
    fun simulateMetrics(bpm: Int, isAnomaly: Boolean = false) = gattManager.simulateEmulatedWatchMetrics(bpm, isAnomaly)

    suspend fun triggerPeriodicWatchMeasurement() {
        if (gattManager.isConnected()) {
            gattManager.requestPeriodicGattUpdate()
        } else {
            // Emulate periodic background resting vitals sampling (e.g. 70-76 bpm)
            val current = healthMetricDao.getLatestMetric()
            val baseBpm = if (current != null && current.heartRateBpm in 60..100) current.heartRateBpm else 72
            val sampledBpm = (baseBpm + (-2..2).random()).coerceIn(60, 95)
            val metric = HealthMetric(
                heartRateBpm = sampledBpm,
                sensorContactDetected = true,
                bloodOxygenSpO2 = 98.5f,
                watchBatteryPercentage = current?.watchBatteryPercentage ?: 85,
                timestamp = System.currentTimeMillis(),
                isAnomaly = false
            )
            healthMetricDao.insertMetric(metric.toEntity())
        }
    }

    private fun HealthMetricEntity.toDomain() = HealthMetric(
        id = id,
        heartRateBpm = heartRateBpm,
        sensorContactDetected = sensorContactDetected,
        bloodOxygenSpO2 = bloodOxygenSpO2,
        watchBatteryPercentage = watchBatteryPercentage,
        timestamp = timestamp,
        isAnomaly = isAnomaly
    )

    private fun HealthMetric.toEntity() = HealthMetricEntity(
        id = id,
        heartRateBpm = heartRateBpm,
        sensorContactDetected = sensorContactDetected,
        bloodOxygenSpO2 = bloodOxygenSpO2,
        watchBatteryPercentage = watchBatteryPercentage,
        timestamp = timestamp,
        isAnomaly = isAnomaly
    )
}
