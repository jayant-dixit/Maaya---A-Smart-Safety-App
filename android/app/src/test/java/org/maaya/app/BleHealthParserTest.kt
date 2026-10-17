package org.maaya.app

import org.junit.Assert.*
import org.junit.Test
import org.maaya.app.ble.BleHealthParser

class BleHealthParserTest {

    @Test
    fun testParseHeartRate_8BitFormat_SensorContactDetected() {
        // Flags: 0b00000110 (Bit 0 = 0 -> 8-bit BPM, Bit 1 = 1 -> contact detected, Bit 2 = 1 -> contact supported)
        // BPM: 78 (0x4E)
        val payload = byteArrayOf(0x06.toByte(), 0x4E.toByte())
        val result = BleHealthParser.parseHeartRate(payload)

        assertNotNull(result)
        assertEquals(78, result?.bpm)
        assertTrue(result?.sensorContactDetected == true)
        assertTrue(result?.rrIntervalsMs?.isEmpty() == true)
    }

    @Test
    fun testParseHeartRate_16BitFormat_WithRRIntervals() {
        // Flags: 0b00010001 (Bit 0 = 1 -> 16-bit BPM, Bit 4 = 1 -> RR intervals present)
        // BPM: 280 (0x0118 -> low byte 0x18, high byte 0x01)
        // RR: 1024 raw units = 1000 ms (0x0400 -> low byte 0x00, high byte 0x04)
        val payload = byteArrayOf(
            0x11.toByte(),
            0x18.toByte(), 0x01.toByte(),
            0x00.toByte(), 0x04.toByte()
        )
        val result = BleHealthParser.parseHeartRate(payload)

        assertNotNull(result)
        assertEquals(280, result?.bpm)
        assertEquals(1, result?.rrIntervalsMs?.size)
        assertEquals(1000, result?.rrIntervalsMs?.first())
    }

    @Test
    fun testParseBatteryLevel_ValidRange() {
        val payload85 = byteArrayOf(85.toByte())
        assertEquals(85, BleHealthParser.parseBatteryLevel(payload85))

        val payloadZero = byteArrayOf(0.toByte())
        assertEquals(0, BleHealthParser.parseBatteryLevel(payloadZero))

        val payloadMax = byteArrayOf(100.toByte())
        assertEquals(100, BleHealthParser.parseBatteryLevel(payloadMax))
    }

    @Test
    fun testParseHeartRate_EmptyData_ReturnsNull() {
        val result = BleHealthParser.parseHeartRate(byteArrayOf())
        assertNull(result)
    }
}
