package org.maaya.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.maaya.app.data.local.SecurePreferences
import org.maaya.app.ui.theme.*

@Composable
fun SettingsScreen(
    securePreferences: SecurePreferences,
    onBack: () -> Unit
) {
    var pin by remember { mutableStateOf(securePreferences.safetyPin) }
    var countdownSecs by remember { mutableStateOf(securePreferences.countdownDurationSeconds.toFloat()) }
    var isShakeEnabled by remember { mutableStateOf(securePreferences.isShakeDetectionEnabled) }
    var shakeSensitivity by remember { mutableStateOf(securePreferences.shakeSensitivityThreshold) }
    var isBatteryAlertEnabled by remember { mutableStateOf(securePreferences.isLowBatteryEscalationEnabled) }
    var batteryThreshold by remember { mutableStateOf(securePreferences.lowBatteryThreshold.toFloat()) }
    var backendUrl by remember { mutableStateOf(securePreferences.backendBaseUrl) }
    var dashboardUrl by remember { mutableStateOf(securePreferences.dashboardBaseUrl) }
    var savedToast by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Security & Settings", fontWeight = FontWeight.Bold) },
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
                .verticalScroll(rememberScrollState())
        ) {
            // Safety PIN Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurfaceDark)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Safety De-escalation PIN",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "Required to cancel false alarms and stop emergency broadcasting",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = pin,
                        onValueChange = { if (it.length <= 6) pin = it },
                        label = { Text("PIN Code") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = TextMuted) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // SOS Countdown Duration
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurfaceDark)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Countdown Window", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 15.sp)
                        Text(text = "${countdownSecs.toInt()} seconds", color = CrimsonRed, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = "Grace period to cancel an accidental SOS press",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Slider(
                        value = countdownSecs,
                        onValueChange = { countdownSecs = it },
                        valueRange = 3f..15f,
                        steps = 11,
                        colors = SliderDefaults.colors(thumbColor = CrimsonRed, activeTrackColor = CrimsonRed)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Shake Trigger Settings
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
                        Text(text = "Multi-Shake Trigger", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 15.sp)
                        Switch(checked = isShakeEnabled, onCheckedChange = { isShakeEnabled = it })
                    }
                    Text(
                        text = "Trigger SOS by shaking device rapidly 3 times",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    if (isShakeEnabled) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(text = "Sensitivity: ${String.format("%.1f", shakeSensitivity)} G", fontSize = 12.sp, color = TextPrimary)
                        Slider(
                            value = shakeSensitivity,
                            onValueChange = { shakeSensitivity = it },
                            valueRange = 2.0f..4.5f,
                            colors = SliderDefaults.colors(thumbColor = AccentPurple, activeTrackColor = AccentPurple)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Low Battery Alerts
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
                        Text(text = "Low Battery SOS Notice", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 15.sp)
                        Switch(checked = isBatteryAlertEnabled, onCheckedChange = { isBatteryAlertEnabled = it })
                    }
                    Text(
                        text = "Alert trusted contacts with last known location when battery is critical",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    if (isBatteryAlertEnabled) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(text = "Threshold: ${batteryThreshold.toInt()}%", fontSize = 12.sp, color = TextPrimary)
                        Slider(
                            value = batteryThreshold,
                            onValueChange = { batteryThreshold = it },
                            valueRange = 5f..25f,
                            steps = 3,
                            colors = SliderDefaults.colors(thumbColor = WarningAmber, activeTrackColor = WarningAmber)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Backend Server Configuration
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurfaceDark)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Cloud & Server Endpoints", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 15.sp)
                    Text(
                        text = "Connect to deployed Render/Vercel cloud or local dev servers",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = backendUrl,
                        onValueChange = { backendUrl = it },
                        label = { Text("Backend API Base URL (Render)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = dashboardUrl,
                        onValueChange = { dashboardUrl = it },
                        label = { Text("Web Dashboard Base URL (Vercel)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                backendUrl = "https://maaya-nrba.onrender.com"
                                dashboardUrl = "https://maaya-henna.vercel.app"
                            },
                            modifier = Modifier.weight(1.2f)
                        ) {
                            Text("Use Render/Vercel", fontSize = 10.sp)
                        }
                        OutlinedButton(
                            onClick = {
                                backendUrl = "http://192.168.29.112:8088"
                                dashboardUrl = "http://192.168.29.112:5173"
                            },
                            modifier = Modifier.weight(0.9f)
                        ) {
                            Text("Local Wi-Fi", fontSize = 10.sp)
                        }
                        OutlinedButton(
                            onClick = {
                                backendUrl = "http://10.0.2.2:8088"
                                dashboardUrl = "http://10.0.2.2:5173"
                            },
                            modifier = Modifier.weight(0.9f)
                        ) {
                            Text("Emulator", fontSize = 10.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    securePreferences.safetyPin = pin
                    securePreferences.countdownDurationSeconds = countdownSecs.toInt()
                    securePreferences.isShakeDetectionEnabled = isShakeEnabled
                    securePreferences.shakeSensitivityThreshold = shakeSensitivity
                    securePreferences.isLowBatteryEscalationEnabled = isBatteryAlertEnabled
                    securePreferences.lowBatteryThreshold = batteryThreshold.toInt()
                    securePreferences.backendBaseUrl = backendUrl.trimEnd('/')
                    securePreferences.dashboardBaseUrl = dashboardUrl.trimEnd('/')
                    savedToast = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CrimsonRed)
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Configurations", fontWeight = FontWeight.Bold)
            }

            if (savedToast) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Configurations saved successfully!",
                    color = SafeGreen,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
