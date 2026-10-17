package org.maaya.app.domain.engine

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

class ShakeDetector(
    private val context: Context,
    private val onShakeTriggered: () -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var lastShakeTimestamp: Long = 0
    private var shakeCount: Int = 0
    private var isListening = false

    var sensitivityG: Float = 2.7f

    fun start() {
        if (!isListening && accelerometer != null) {
            isListening = true
            sensorManager?.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun stop() {
        if (isListening) {
            isListening = false
            sensorManager?.unregisterListener(this)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        val x = event.values[0] / SensorManager.GRAVITY_EARTH
        val y = event.values[1] / SensorManager.GRAVITY_EARTH
        val z = event.values[2] / SensorManager.GRAVITY_EARTH

        // Calculate net acceleration vector
        val gForce = sqrt((x * x + y * y + z * z).toDouble()).toFloat()

        if (gForce > sensitivityG) {
            val now = System.currentTimeMillis()
            // Ignore shakes too close together (< 250ms)
            if (lastShakeTimestamp + 250 > now) {
                return
            }

            // Reset shake count if last shake was more than 1.5 seconds ago
            if (lastShakeTimestamp + 1500 < now) {
                shakeCount = 0
            }

            lastShakeTimestamp = now
            shakeCount++

            // Require 3 rapid shakes to prevent accidental triggers while walking/jogging
            if (shakeCount >= 3) {
                shakeCount = 0
                onShakeTriggered()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
