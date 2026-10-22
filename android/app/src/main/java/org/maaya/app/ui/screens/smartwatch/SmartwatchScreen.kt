package org.maaya.app.ui.screens.smartwatch

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.maaya.app.ble.BleConnectionState
import org.maaya.app.data.repository.BleHealthRepository
import org.maaya.app.domain.engine.EmergencySosEngine
import org.maaya.app.domain.model.SosTriggerType
import org.maaya.app.ui.theme.*

@Composable
fun SmartwatchScreen(
    bleRepository: BleHealthRepository,
    engine: EmergencySosEngine,
    onBack: () -> Unit
) {
    val connectionState by bleRepository.connectionState.collectAsState(initial = BleConnectionState.Disconnected)
    val scannedDevices by bleRepository.scannedDevices.collectAsState(initial = emptyList())
    val recentMetrics by bleRepository.recentMetrics.collectAsState(initial = emptyList())

    val isScanning = connectionState is BleConnectionState.Scanning
    val isConnected = connectionState is BleConnectionState.Connected || connectionState is BleConnectionState.Monitoring

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Smartwatch BLE Integration", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SkyBlueBackground,
                    titleContentColor = TextPrimary,
                    navigationIconContentColor = TextPrimary
                )
            )
        },
        containerColor = SkyBlueBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Connection Status Banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurfaceDark)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Watch,
                                contentDescription = null,
                                tint = if (isConnected) SafeGreen else CrimsonRed
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Status: " + when (connectionState) {
                                        is BleConnectionState.Disconnected -> "Disconnected"
                                        is BleConnectionState.Scanning -> "Scanning for Peripherals..."
                                        is BleConnectionState.Connecting -> "Connecting..."
                                        is BleConnectionState.Connected -> "GATT Connected"
                                        is BleConnectionState.Monitoring -> "Monitoring GATT Vitals"
                                        is BleConnectionState.Error -> "Connection Error"
                                    },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Standard Bluetooth SIG GATT (0x180D, 0x180F)",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        if (isConnected) {
                            TextButton(onClick = { bleRepository.disconnect() }) {
                                Text("Disconnect", color = CrimsonRed, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                if (isScanning) {
                                    bleRepository.stopScan()
                                } else {
                                    bleRepository.startScan()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isScanning) WarningAmber else CrimsonRed
                            )
                        ) {
                            Icon(
                                if (isScanning) Icons.Default.Stop else Icons.Default.Search,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isScanning) "Stop Scan" else "Scan for Watches")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Automatic Periodic Sampling Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurfaceElevated)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Favorite, contentDescription = null, tint = CrimsonRed, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Auto Heart Rate Sampling",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = TextPrimary
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SafeGreen.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "ACTIVE (30s)",
                                color = SafeGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Vitals are queried automatically from your smartwatch in the background without requiring manual intervention. Automatic stress spike (>145 BPM) escalation is armed.",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )

                    val latest = recentMetrics.firstOrNull()
                    if (latest != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Latest Recorded BPM:", fontSize = 12.sp, color = TextMuted)
                            Text(
                                text = "${latest.heartRateBpm} BPM",
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp,
                                color = if (latest.isAnomaly) CrimsonRed else TextPrimary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Test Simulation Hub
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurfaceDark)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Hardware Test & Emulation",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = "Verify stress triggers, heart rate spikes, and watch alerts",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { bleRepository.simulateMetrics(74, isAnomaly = false) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Resting (74 bpm)", fontSize = 11.sp)
                        }
                        Button(
                            onClick = {
                                bleRepository.simulateMetrics(158, isAnomaly = true)
                                engine.triggerEmergency(SosTriggerType.HEART_RATE_ANOMALY)
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = CrimsonRed)
                        ) {
                            Text("Stress Spike (158)", fontSize = 11.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Discovered BLE Devices (${scannedDevices.size})",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (scannedDevices.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isScanning) "Scanning nearby Bluetooth LE devices..." else "Tap 'Scan for Watches' to search for BLE heart-rate monitors",
                        color = TextMuted,
                        fontSize = 13.sp
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(scannedDevices) { device ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { bleRepository.connectDevice(device.address) },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = CardSurfaceDark)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = device.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "MAC: ${device.address} • RSSI: ${device.rssi} dBm",
                                        fontSize = 12.sp,
                                        color = TextSecondary
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = "Connect",
                                    tint = TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
