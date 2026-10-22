package org.maaya.app.ui.screens.dashboard

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import org.maaya.app.ble.BleConnectionState
import org.maaya.app.data.local.SecurePreferences
import org.maaya.app.data.repository.BleHealthRepository
import org.maaya.app.data.repository.ContactRepository
import org.maaya.app.domain.engine.BatteryMonitor
import org.maaya.app.domain.engine.EmergencySosEngine
import org.maaya.app.domain.engine.SosEngineState
import org.maaya.app.domain.model.SosTriggerType
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

    val userGreeting = if (securePreferences.userName.isNotBlank()) {
        securePreferences.userName
    } else {
        "Jayant"
    }

    val liveBatteryPct by batteryMonitor.currentBatteryLevel.collectAsState()
    val isChargingState by batteryMonitor.isCharging.collectAsState()

    var runtimeBatteryLevel by remember { mutableStateOf(liveBatteryPct) }
    var runtimeCharging by remember { mutableStateOf(isChargingState) }

    DisposableEffect(context) {
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(ctx: android.content.Context?, intent: android.content.Intent?) {
                if (intent?.action == android.content.Intent.ACTION_BATTERY_CHANGED) {
                    val level = intent.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1)
                    val scale = intent.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1)
                    val status = intent.getIntExtra(android.os.BatteryManager.EXTRA_STATUS, -1)
                    runtimeCharging = status == android.os.BatteryManager.BATTERY_STATUS_CHARGING ||
                            status == android.os.BatteryManager.BATTERY_STATUS_FULL
                    if (level >= 0 && scale > 0) {
                        runtimeBatteryLevel = (level * 100) / scale
                    }
                }
            }
        }
        val filter = android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED)
        val sticky = context.registerReceiver(receiver, filter)
        sticky?.let { intent ->
            val level = intent.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1)
            val status = intent.getIntExtra(android.os.BatteryManager.EXTRA_STATUS, -1)
            runtimeCharging = status == android.os.BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == android.os.BatteryManager.BATTERY_STATUS_FULL
            if (level >= 0 && scale > 0) {
                runtimeBatteryLevel = (level * 100) / scale
            }
        }
        onDispose {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: Exception) {}
        }
    }

    val displayBatteryPct = runtimeBatteryLevel

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        SkyBlueGradientTop,
                        SkyBlueGradientMid,
                        SkyBlueGradientBottom,
                        SurfaceWhite
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.statusBarsPadding())
            Spacer(modifier = Modifier.height(10.dp))

            // ==========================================
            // SECTION A: HEADER & SAFETY STATUS
            // ==========================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Circular Initials Avatar
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .shadow(4.dp, CircleShape, spotColor = CardShadowLight)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(PrimarySkyBlueBright, PrimarySkyBlue)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = userGreeting.take(2).uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        // "Hi, Jayant" Greeting
                        Text(
                            text = "Hi, $userGreeting",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        // "Welcome back!" subtitle
                        Text(
                            text = "Welcome back!",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal,
                            color = TextSecondary
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Smartwatch Icon in White Circular Button with Status Dot
                    WatchStatusHeaderBadge(
                        isConnected = isWatchConnected,
                        onClick = onNavigateToSmartwatch
                    )

                    // Settings Icon in White Circular Button
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .shadow(3.dp, CircleShape, spotColor = CardShadowLight)
                            .clip(CircleShape)
                            .background(SurfaceWhite)
                            .clickable(onClick = onNavigateToSettings),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Color(0xFF334155),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // Subheader: SAFETY STATUS + Everything under control on Left, Watch Toast Pill on Right
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "SAFETY STATUS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                        color = Color(0xFF64748B)
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(SafeGreen)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Everything under control",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                    }
                }

                // Compact Toast-style Pill on Right
                WatchToastPill(
                    isConnected = isWatchConnected,
                    onClick = onNavigateToSmartwatch
                )
            }

            // Missing Permissions Warning Banner (if any)
            if (hasMissingPermissions) {
                Spacer(modifier = Modifier.height(10.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { permissionLauncher.launch(requiredPermissions) },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = WarningAmberSoft),
                    border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(WarningAmber.copy(alpha = 0.4f)))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Permissions Required", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 12.sp)
                            Text("Location, Camera & Microphone are needed for emergency tracking.", color = TextSecondary, fontSize = 11.sp)
                        }
                        Button(
                            onClick = { permissionLauncher.launch(requiredPermissions) },
                            colors = ButtonDefaults.buttonColors(containerColor = WarningAmber),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Grant", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ==========================================
            // SECTION B: CENTRAL SOS CONTROL
            // - Large red circular 3D dome button with white shield icon & red "!"
            // - Concentric radar compass tick ring & thin pink circles
            // - Lateral curved bracket arcs and ripple waves
            // - Text underneath: "Tap to activate sos"
            // - Soft pink glow behind button
            // ==========================================
            CentralSosControl(
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

            Spacer(modifier = Modifier.height(18.dp))

            // ==========================================
            // SECTION C: EMERGENCY SHORTCUTS
            // - Single white card enclosing 3 horizontal pills: Police, Ambulance, Fire
            // - Each contains only an icon badge and a text label
            // - Subtle blue, pink, and orange accents distinguish services
            // - No phone numbers or unnecessary descriptions
            // ==========================================
            EmergencyShortcutsRow()

            Spacer(modifier = Modifier.height(20.dp))

            // ==========================================
            // SECTION D: VITALS AND SYSTEM METRICS
            // - Four cards in a two-column grid
            // - Heart Rate: value and animated red waveform
            // - Blood SpO2: percentage and animated blue liquid graphic
            // - Stress Level: status and animated green semicircular gauge
            // - Mobile Battery: percentage and animated purple battery bars (REALTIME)
            // ==========================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Vitals & System Metrics",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                // Compact "• Live" green badge pill
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFECFDF5)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(SafeGreen)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Live",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SafeGreen
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Row 1: Heart Rate & Blood SpO2
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Card 1: Heart Rate (with animated red ECG pulse waveform)
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(BorderSubtle))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(EmergencyRedSoft),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = null,
                                    tint = CrimsonRed,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(7.dp))
                            Text(
                                text = "Heart Rate",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if ((latestMetric?.heartRateBpm ?: 0) > 0) "${latestMetric?.heartRateBpm}" else "60",
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "bpm",
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                            }

                            // Animated Red Cardiac Waveform Graph
                            WaveSparkline(
                                modifier = Modifier
                                    .width(74.dp)
                                    .height(40.dp),
                                lineColor = CrimsonRed,
                                fillColor = CrimsonRed.copy(alpha = 0.18f),
                                bpm = latestMetric?.heartRateBpm ?: 60
                            )
                        }
                    }
                }

                // Card 2: Blood SpO2 (with animated circular water wave liquid graphic)
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(BorderSubtle))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(SkyBlueLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Air,
                                    contentDescription = null,
                                    tint = PrimarySkyBlue,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(7.dp))
                            Text(
                                text = "Blood SpO\u2082",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (latestMetric?.bloodOxygenSpO2 != null) "${latestMetric.bloodOxygenSpO2?.toInt()}" else "98",
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "% oxygen",
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                            }

                            // Animated Blue Circular Liquid Wave Graphic
                            LiquidWaveCircle(
                                modifier = Modifier.size(46.dp),
                                spO2Percentage = latestMetric?.bloodOxygenSpO2 ?: 98f,
                                waveColor = PrimarySkyBlue,
                                backgroundColor = SkyBlueLight
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Row 2: Stress Level & Mobile Battery (Realtime)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Card 3: Stress Level (with animated green arc gauge)
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(BorderSubtle))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(SafeGreenSoft),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Eco,
                                    contentDescription = null,
                                    tint = SafeGreen,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(7.dp))
                            Text(
                                text = "Stress Level",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (latestMetric?.isAnomaly == true) "Elevated" else "Normal",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "index 22",
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                            }

                            // Animated Green Arc Gauge with pulsing indicator dot
                            ArcGauge(
                                modifier = Modifier.size(48.dp, 28.dp),
                                fraction = if (latestMetric?.isAnomaly == true) 0.78f else 0.22f,
                                activeColor = SafeGreen
                            )
                        }
                    }
                }

                // Card 4: Mobile Battery (Realtime percentage & animated purple vertical bars)
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(BorderSubtle))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(AccentPurpleSoft),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.BatteryChargingFull,
                                    contentDescription = null,
                                    tint = AccentPurple,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(7.dp))
                            Text(
                                text = "Mobile Battery",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "$displayBatteryPct%",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = if (runtimeCharging) "charging" else "charged",
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                            }

                            // Animated Purple Vertical Bars responding in realtime
                            VerticalBarGauge(
                                levelPercentage = displayBatteryPct,
                                isCharging = runtimeCharging,
                                barColor = AccentPurple
                            )
                        }
                    }
                }
            }

            // Bottom spacing to prevent floating bottom nav bar overlap
            Spacer(modifier = Modifier.height(110.dp))
        }
    }
}
