package org.maaya.app.domain.engine

import android.util.Log
import kotlinx.coroutines.*
import org.maaya.app.data.repository.BleHealthRepository
import org.maaya.app.domain.model.HealthMetric

class HeartRateAutoScheduler(
    private val bleHealthRepository: BleHealthRepository,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default),
    var intervalSeconds: Int = 30
) {
    private var schedulerJob: Job? = null
    var isEnabled: Boolean = true

    fun start() {
        if (schedulerJob?.isActive == true) return
        schedulerJob = scope.launch {
            Log.i("HeartRateScheduler", "Started automatic heart rate scheduler (interval: ${intervalSeconds}s)")
            while (isActive) {
                delay(intervalSeconds * 1000L)
                if (isEnabled) {
                    try {
                        triggerMeasurement()
                    } catch (e: Exception) {
                        Log.e("HeartRateScheduler", "Error during scheduled heart rate measurement", e)
                    }
                }
            }
        }
    }

    fun stop() {
        schedulerJob?.cancel()
        schedulerJob = null
    }

    private suspend fun triggerMeasurement() {
        bleHealthRepository.triggerPeriodicWatchMeasurement()
    }
}
