package org.maaya.app.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.maaya.app.ble.BleConnectionState
import org.maaya.app.data.local.SecurePreferences
import org.maaya.app.data.repository.BleHealthRepository
import org.maaya.app.data.repository.ContactRepository
import org.maaya.app.domain.engine.BatteryMonitor
import org.maaya.app.domain.engine.EmergencySosEngine
import org.maaya.app.domain.engine.SosEngineState
import org.maaya.app.domain.model.SosTriggerType
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import org.maaya.app.ui.components.*
import org.maaya.app.ui.theme.*

@Composable
fun DashboardScreen(
    engine: EmergencySosEngine,
    bleRepository: BleHealthRepository,
    contactRepository: ContactRepository,
    batteryMonitor: BatteryMonitor,
    securePreferences: SecurePreferences,
    onNavigateToContacts: () -> Unit,
    onNavigateToSmartwatch: () -> Unit,
    onNavigateToSafeRide: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToActiveSos: () -> Unit
) {
    val context = LocalContext.current
    val engineState by engine.engineState.collectAsState()
    val bleState by bleRepository.connectionState.collectAsState(initial = BleConnectionState.Disconnected)
    val recentMetrics by bleRepository.recentMetrics.collectAsState(initial = emptyList())
    val contacts by contactRepository.contacts.collectAsState(initial = emptyList())

    val latestMetric = recentMetrics.firstOrNull()
    val isWatchConnected = bleState is BleConnectionState.Connected || bleState is BleConnectionState.Monitoring

    val requiredPermissions = remember {
        val list = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.SEND_SMS
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            list.add(Manifest.permission.BLUETOOTH_SCAN)
            list.add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        list.toTypedArray()
    }

    var hasMissingPermissions by remember {
        mutableStateOf(
            requiredPermissions.any {
                ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasMissingPermissions = results.values.any { !it }
    }

    LaunchedEffect(Unit) {
        if (hasMissingPermissions) {
            permissionLauncher.launch(requiredPermissions)
        }
    }

    LaunchedEffect(engineState) {
        if (engineState is SosEngineState.Active) {
            onNavigateToActiveSos()
        }
    }

    val isCountdown = engineState is SosEngineState.Countdown
    val countdownRemaining = (engineState as? SosEngineState.Countdown)?.remainingSeconds ?: 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkNavyBackground)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Missing Permissions Action Banner
        if (hasMissingPermissions) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .clickable { permissionLauncher.launch(requiredPermissions) },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = WarningAmber.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Permissions Required", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 13.sp)
                        Text("Location, Camera & Microphone are needed for live tracking & audio/video. Tap to grant.", color = TextSecondary, fontSize = 11.sp)
                    }
                    Button(
                        onClick = { permissionLauncher.launch(requiredPermissions) },
                        colors = ButtonDefaults.buttonColors(containerColor = WarningAmber),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("Grant", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CrimsonRed.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = CrimsonRed,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Maaya Shield",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = if (securePreferences.userName.isNotBlank()) "Hi, ${securePreferences.userName}" else "Protection Active",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }

            IconButton(onClick = onNavigateToSettings) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // System Sensors & Hardware Badges
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ConnectionStatusBadge(
                label = "GPS Ready",
                isActive = true,
                activeIcon = Icons.Default.LocationOn,
                modifier = Modifier.weight(1f)
            )
            ConnectionStatusBadge(
                label = if (isWatchConnected) "Watch Linked" else "No Watch",
                isActive = isWatchConnected,
                activeIcon = Icons.Default.Watch,
                modifier = Modifier.weight(1f)
            )
            ConnectionStatusBadge(
                label = if (securePreferences.isShakeDetectionEnabled) "Shake Ready" else "Shake Off",
                isActive = securePreferences.isShakeDetectionEnabled,
                activeIcon = Icons.Default.Vibration,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Central SOS Button
        SosEmergencyButton(
            isCountdown = isCountdown,
            countdownRemaining = countdownRemaining,
            onClick = {
                if (isCountdown) {
                    engine.cancelCountdown()
                } else {
                    engine.triggerEmergency(SosTriggerType.MANUAL_IN_APP)
                }
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = if (isCountdown) "Countdown in progress! Tap button to abort." else "Shake phone 3 times or press watch button for emergency",
            fontSize = 12.sp,
            color = if (isCountdown) WarningAmber else TextMuted,
            fontWeight = if (isCountdown) FontWeight.Bold else FontWeight.Normal
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Health Vitals Card
        HealthVitalsCard(
            heartRateBpm = latestMetric?.heartRateBpm ?: 0,
            sensorContact = latestMetric?.sensorContactDetected ?: false,
            batteryPercentage = latestMetric?.watchBatteryPercentage,
            spO2Percentage = latestMetric?.bloodOxygenSpO2,
            isWatchConnected = isWatchConnected
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Quick Navigation Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DashboardActionCard(
                title = "Safe Ride",
                subtitle = "Journey escort",
                icon = Icons.Default.DirectionsCar,
                accentColor = AccentPurple,
                modifier = Modifier.weight(1f),
                onClick = onNavigateToSafeRide
            )
            DashboardActionCard(
                title = "Contacts",
                subtitle = "${contacts.size} configured",
                icon = Icons.Default.Group,
                accentColor = SafeGreen,
                modifier = Modifier.weight(1f),
                onClick = onNavigateToContacts
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DashboardActionCard(
                title = "Wearable BLE",
                subtitle = if (isWatchConnected) "Streaming" else "Pair device",
                icon = Icons.Default.Bluetooth,
                accentColor = Color(0xFF38BDF8),
                modifier = Modifier.weight(1f),
                onClick = onNavigateToSmartwatch
            )
            DashboardActionCard(
                title = "Incident Log",
                subtitle = "Audit history",
                icon = Icons.Default.History,
                accentColor = WarningAmber,
                modifier = Modifier.weight(1f),
                onClick = onNavigateToHistory
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun DashboardActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurfaceDark),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.linearGradient(listOf(BorderSubtle, CardSurfaceElevated))
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = TextSecondary
            )
        }
    }
}
