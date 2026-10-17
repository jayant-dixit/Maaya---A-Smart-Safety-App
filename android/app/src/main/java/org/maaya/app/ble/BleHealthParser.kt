package org.maaya.app.ble

import java.util.UUID

object BleStandardUuids {
    val HEART_RATE_SERVICE: UUID = UUID.fromString("0000180D-0000-1000-8000-00805F9B34FB")
    val HEART_RATE_MEASUREMENT: UUID = UUID.fromString("00002A37-0000-1000-8000-00805F9B34FB")
    val BODY_SENSOR_LOCATION: UUID = UUID.fromString("00002A38-0000-1000-8000-00805F9B34FB")

    val BATTERY_SERVICE: UUID = UUID.fromString("0000180F-0000-1000-8000-00805F9B34FB")
    val BATTERY_LEVEL: UUID = UUID.fromString("00002A19-0000-1000-8000-00805F9B34FB")

    val PULSE_OXIMETER_SERVICE: UUID = UUID.fromString("00001822-0000-1000-8000-00805F9B34FB")
    val PLX_CONTINUOUS_MEASUREMENT: UUID = UUID.fromString("00002A5F-0000-1000-8000-00805F9B34FB")
    val PLX_SPOT_CHECK: UUID = UUID.fromString("00002A5E-0000-1000-8000-00805F9B34FB")

    val IMMEDIATE_ALERT_SERVICE: UUID = UUID.fromString("00001802-0000-1000-8000-00805F9B34FB")
    val ALERT_LEVEL: UUID = UUID.fromString("00002A06-0000-1000-8000-00805F9B34FB")

    val CLIENT_CHARACTERISTIC_CONFIG: UUID = UUID.fromString("00002902-0000-1000-8000-00805F9B34FB")
}

data class ParsedHeartRate(
    val bpm: Int,
    val sensorContactDetected: Boolean,
    val energyExpendedJoules: Int? = null,
    val rrIntervalsMs: List<Int> = emptyList()
)

object BleHealthParser {

    /**
     * Parses official Bluetooth SIG Heart Rate Measurement payload (GATT 0x2A37).
     * Strictly verifies byte flags:
     * - Bit 0: 0 -> UINT8 bpm, 1 -> UINT16 bpm
     * - Bit 1: Sensor contact feature supported and detected
     */
    fun parseHeartRate(data: ByteArray): ParsedHeartRate? {
        if (data.isEmpty()) return null

        val flags = data[0].toInt()
        val isFormat16Bit = (flags and 0x01) != 0
        val isSensorContactSupported = (flags and 0x04) != 0
        val isSensorContactDetected = if (isSensorContactSupported) {
            (flags and 0x02) != 0
        } else {
            true // default assume on skin if device doesn't report contact bit
        }

        var offset = 1
        val bpm = if (isFormat16Bit) {
            if (data.size < 3) return null
            val b1 = data[offset++].toInt() and 0xFF
            val b2 = data[offset++].toInt() and 0xFF
            (b2 shl 8) or b1
        } else {
            if (data.size < 2) return null
            data[offset++].toInt() and 0xFF
        }

        var energyExpended: Int? = null
        if ((flags and 0x08) != 0 && data.size >= offset + 2) {
            val b1 = data[offset++].toInt() and 0xFF
            val b2 = data[offset++].toInt() and 0xFF
            energyExpended = (b2 shl 8) or b1
        }

        val rrIntervals = mutableListOf<Int>()
        if ((flags and 0x10) != 0) {
            while (offset + 1 < data.size) {
                val b1 = data[offset++].toInt() and 0xFF
                val b2 = data[offset++].toInt() and 0xFF
                val rawValue = (b2 shl 8) or b1
                // 1/1024 second units converted to milliseconds
                val ms = (rawValue * 1000) / 1024
                rrIntervals.add(ms)
            }
        }

        return ParsedHeartRate(
            bpm = bpm,
            sensorContactDetected = isSensorContactDetected,
            energyExpendedJoules = energyExpended,
            rrIntervalsMs = rrIntervals
        )
    }

    /**
     * Parses official Bluetooth SIG Battery Level (GATT 0x2A19).
     * Single UINT8 representation (0 - 100%).
     */
    fun parseBatteryLevel(data: ByteArray): Int? {
        if (data.isEmpty()) return null
        val level = data[0].toInt() and 0xFF
        return level.coerceIn(0, 100)
    }
}
