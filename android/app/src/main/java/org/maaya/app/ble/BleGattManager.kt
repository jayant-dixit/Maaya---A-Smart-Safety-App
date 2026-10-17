package org.maaya.app.ble

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.maaya.app.domain.model.BleDeviceItem
import org.maaya.app.domain.model.HealthMetric

sealed class BleConnectionState {
    object Disconnected : BleConnectionState()
    data class Scanning(val discoveredCount: Int) : BleConnectionState()
    data class Connecting(val deviceAddress: String) : BleConnectionState()
    data class Connected(val deviceName: String, val deviceAddress: String) : BleConnectionState()
    data class Monitoring(
        val deviceName: String,
        val deviceAddress: String,
        val hasHeartRate: Boolean,
        val hasBattery: Boolean,
        val hasSpO2: Boolean
    ) : BleConnectionState()
    data class Error(val message: String) : BleConnectionState()
}

class BleGattManager(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? get() = bluetoothManager?.adapter

    private var activeGatt: BluetoothGatt? = null
    private var connectedDeviceItem: BleDeviceItem? = null

    private val _connectionState = MutableStateFlow<BleConnectionState>(BleConnectionState.Disconnected)
    val connectionState: StateFlow<BleConnectionState> = _connectionState.asStateFlow()

    private val _scannedDevices = MutableStateFlow<List<BleDeviceItem>>(emptyList())
    val scannedDevices: StateFlow<List<BleDeviceItem>> = _scannedDevices.asStateFlow()

    private val _healthEvents = MutableSharedFlow<HealthMetric>(extraBufferCapacity = 64)
    val healthEvents: SharedFlow<HealthMetric> = _healthEvents.asSharedFlow()

    private val _hardwareSosTriggers = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val hardwareSosTriggers: SharedFlow<String> = _hardwareSosTriggers.asSharedFlow()

    private var currentHeartRate = 0
    private var currentBattery: Int? = null

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            val device = result?.device ?: return
            val name = try { device.name ?: "BLE Sensor (${device.address.takeLast(5)})" } catch (e: SecurityException) { "BLE Sensor" }
            val item = BleDeviceItem(
                name = name,
                address = device.address,
                rssi = result.rssi,
                isConnected = false
            )
            updateScannedDevice(item)
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            results?.forEach { onScanResult(ScanSettings.CALLBACK_TYPE_ALL_MATCHES, it) }
        }

        override fun onScanFailed(errorCode: Int) {
            _connectionState.value = BleConnectionState.Error("BLE Scan failed with code $errorCode")
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            val deviceAddress = gatt.device.address
            val deviceName = try { gatt.device.name ?: "Smartwatch" } catch (e: SecurityException) { "Smartwatch" }

            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.w("BleGattManager", "Connection state error status: $status")
                disconnect()
                _connectionState.value = BleConnectionState.Error("Connection error: $status")
                return
            }

            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.i("BleGattManager", "GATT Connected to $deviceAddress, discovering services...")
                    _connectionState.value = BleConnectionState.Connected(deviceName, deviceAddress)
                    activeGatt = gatt
                    gatt.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.i("BleGattManager", "GATT Disconnected from $deviceAddress")
                    disconnect()
                    _connectionState.value = BleConnectionState.Disconnected
                }
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                _connectionState.value = BleConnectionState.Error("Failed to discover services: $status")
                return
            }

            var hasHeartRate = false
            var hasBattery = false
            var hasSpO2 = false

            val hrService = gatt.getService(BleStandardUuids.HEART_RATE_SERVICE)
            if (hrService != null) {
                val hrChar = hrService.getCharacteristic(BleStandardUuids.HEART_RATE_MEASUREMENT)
                if (hrChar != null) {
                    enableNotification(gatt, hrChar)
                    hasHeartRate = true
                }
            }

            val battService = gatt.getService(BleStandardUuids.BATTERY_SERVICE)
            if (battService != null) {
                val battChar = battService.getCharacteristic(BleStandardUuids.BATTERY_LEVEL)
                if (battChar != null) {
                    gatt.readCharacteristic(battChar)
                    enableNotification(gatt, battChar)
                    hasBattery = true
                }
            }

            val spo2Service = gatt.getService(BleStandardUuids.PULSE_OXIMETER_SERVICE)
            if (spo2Service != null) {
                hasSpO2 = true
            }

            val deviceName = try { gatt.device.name ?: "Smartwatch" } catch (e: SecurityException) { "Smartwatch" }
            _connectionState.value = BleConnectionState.Monitoring(
                deviceName = deviceName,
                deviceAddress = gatt.device.address,
                hasHeartRate = hasHeartRate,
                hasBattery = hasBattery,
                hasSpO2 = hasSpO2
            )
        }

        @Deprecated("Deprecated in Java for API 33+")
        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            @Suppress("DEPRECATION")
            handleIncomingCharacteristicPayload(characteristic.uuid, characteristic.value)
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            handleIncomingCharacteristicPayload(characteristic.uuid, value)
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                @Suppress("DEPRECATION")
                val value = characteristic.value ?: return
                handleIncomingCharacteristicPayload(characteristic.uuid, value)
            }
        }
    }

    private fun handleIncomingCharacteristicPayload(uuid: java.util.UUID, payload: ByteArray?) {
        if (payload == null || payload.isEmpty()) return

        when (uuid) {
            BleStandardUuids.HEART_RATE_MEASUREMENT -> {
                val parsed = BleHealthParser.parseHeartRate(payload)
                if (parsed != null) {
                    currentHeartRate = parsed.bpm
                    val metric = HealthMetric(
                        heartRateBpm = parsed.bpm,
                        sensorContactDetected = parsed.sensorContactDetected,
                        watchBatteryPercentage = currentBattery,
                        timestamp = System.currentTimeMillis()
                    )
                    _healthEvents.tryEmit(metric)
                }
            }
            BleStandardUuids.BATTERY_LEVEL -> {
                val batt = BleHealthParser.parseBatteryLevel(payload)
                if (batt != null) {
                    currentBattery = batt
                }
            }
            BleStandardUuids.ALERT_LEVEL -> {
                // Immediate alert / button trigger from smartwatch
                val alert = payload[0].toInt()
                if (alert >= 2) {
                    _hardwareSosTriggers.tryEmit("Smartwatch hardware alert button pressed")
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableNotification(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
        gatt.setCharacteristicNotification(characteristic, true)
        val descriptor = characteristic.getDescriptor(BleStandardUuids.CLIENT_CHARACTERISTIC_CONFIG)
        if (descriptor != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                gatt.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
            } else {
                @Suppress("DEPRECATION")
                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                @Suppress("DEPRECATION")
                gatt.writeDescriptor(descriptor)
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun startScan(): Result<Unit> {
        val adapter = bluetoothAdapter ?: return Result.failure(IllegalStateException("Bluetooth adapter not available"))
        if (!adapter.isEnabled) return Result.failure(IllegalStateException("Bluetooth is disabled"))

        val scanner = adapter.bluetoothLeScanner ?: return Result.failure(IllegalStateException("BLE scanner unavailable"))

        _scannedDevices.value = emptyList()
        _connectionState.value = BleConnectionState.Scanning(0)

        val filters = listOf(
            ScanFilter.Builder().setServiceUuid(ParcelUuid(BleStandardUuids.HEART_RATE_SERVICE)).build()
        )
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        try {
            scanner.startScan(null, settings, scanCallback)
            return Result.success(Unit)
        } catch (e: Exception) {
            _connectionState.value = BleConnectionState.Error("Cannot start scan: ${e.message}")
            return Result.failure(e)
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        try {
            bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
            if (_connectionState.value is BleConnectionState.Scanning) {
                _connectionState.value = BleConnectionState.Disconnected
            }
        } catch (_: Exception) {}
    }

    @SuppressLint("MissingPermission")
    fun connect(address: String): Result<Unit> {
        stopScan()
        val adapter = bluetoothAdapter ?: return Result.failure(IllegalStateException("Bluetooth unavailable"))
        val device = adapter.getRemoteDevice(address)

        _connectionState.value = BleConnectionState.Connecting(address)

        activeGatt?.close()
        activeGatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
        } else {
            device.connectGatt(context, false, gattCallback)
        }
        return Result.success(Unit)
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        try {
            activeGatt?.disconnect()
            activeGatt?.close()
        } catch (_: Exception) {}
        activeGatt = null
        _connectionState.value = BleConnectionState.Disconnected
    }

    private fun updateScannedDevice(item: BleDeviceItem) {
        val list = _scannedDevices.value.toMutableList()
        val idx = list.indexOfFirst { it.address == item.address }
        if (idx >= 0) {
            list[idx] = item
        } else {
            list.add(item)
        }
        _scannedDevices.value = list
        if (_connectionState.value is BleConnectionState.Scanning) {
            _connectionState.value = BleConnectionState.Scanning(list.size)
        }
    }

    fun isConnected(): Boolean {
        return activeGatt != null && (_connectionState.value is BleConnectionState.Connected || _connectionState.value is BleConnectionState.Monitoring)
    }

    @SuppressLint("MissingPermission")
    fun requestPeriodicGattUpdate() {
        val gatt = activeGatt ?: return
        try {
            val battService = gatt.getService(BleStandardUuids.BATTERY_SERVICE)
            val battChar = battService?.getCharacteristic(BleStandardUuids.BATTERY_LEVEL)
            if (battChar != null) {
                gatt.readCharacteristic(battChar)
            }

            val hrService = gatt.getService(BleStandardUuids.HEART_RATE_SERVICE)
            val hrChar = hrService?.getCharacteristic(BleStandardUuids.HEART_RATE_MEASUREMENT)
            if (hrChar != null) {
                enableNotification(gatt, hrChar)
            }
        } catch (e: Exception) {
            Log.w("BleGattManager", "Periodic GATT update ping failed", e)
        }
    }

    /**
     * Diagnostic simulator helper for offline demonstration or testing
     */
    fun simulateEmulatedWatchMetrics(bpm: Int, isAnomaly: Boolean = false) {
        val metric = HealthMetric(
            heartRateBpm = bpm,
            sensorContactDetected = true,
            bloodOxygenSpO2 = 98.5f,
            watchBatteryPercentage = 84,
            timestamp = System.currentTimeMillis(),
            isAnomaly = isAnomaly
        )
        _healthEvents.tryEmit(metric)
    }
}
