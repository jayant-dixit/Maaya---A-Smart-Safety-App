package org.maaya.app.domain.engine

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BatteryMonitor(
    private val context: Context,
    private val onLowBatteryThresholdReached: (level: Int) -> Unit
) {
    private var isRegistered = false
    private var hasAlertedForCurrentDischarge = false

    var thresholdPercentage: Int = 15

    private val _currentBatteryLevel = MutableStateFlow(getCurrentBatteryLevel())
    val currentBatteryLevel: StateFlow<Int> = _currentBatteryLevel.asStateFlow()

    private val _isCharging = MutableStateFlow(checkIsCharging())
    val isCharging: StateFlow<Boolean> = _isCharging.asStateFlow()

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_BATTERY_CHANGED) {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL

                _isCharging.value = charging

                if (level >= 0 && scale > 0) {
                    val batteryPct = (level * 100) / scale
                    _currentBatteryLevel.value = batteryPct

                    if (charging) {
                        hasAlertedForCurrentDischarge = false
                        return
                    }

                    if (batteryPct <= thresholdPercentage && !hasAlertedForCurrentDischarge) {
                        hasAlertedForCurrentDischarge = true
                        onLowBatteryThresholdReached(batteryPct)
                    }
                }
            }
        }
    }

    fun start() {
        if (!isRegistered) {
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            context.registerReceiver(receiver, filter)
            isRegistered = true
        }
    }

    fun stop() {
        if (isRegistered) {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: Exception) {}
            isRegistered = false
        }
    }

    fun getCurrentBatteryLevel(): Int {
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { filter ->
            context.registerReceiver(null, filter)
        }
        val level: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        return if (level >= 0 && scale > 0) (level * 100) / scale else 100
    }

    fun checkIsCharging(): Boolean {
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { filter ->
            context.registerReceiver(null, filter)
        }
        val status: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        return status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
    }
}
