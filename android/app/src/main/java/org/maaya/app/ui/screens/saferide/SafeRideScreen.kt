package org.maaya.app.ui.screens.saferide

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.maaya.app.data.repository.SafeRideRepository
import org.maaya.app.domain.engine.EmergencySosEngine
import org.maaya.app.domain.engine.LocationTracker
import org.maaya.app.domain.engine.SafeRideMonitor
import org.maaya.app.domain.model.SosTriggerType
import org.maaya.app.ui.theme.*

@Composable
fun SafeRideScreen(
    safeRideRepository: SafeRideRepository,
    locationTracker: LocationTracker,
    safeRideMonitor: SafeRideMonitor,
    engine: EmergencySosEngine,
    onBack: () -> Unit
) {
    val activeRide by safeRideRepository.activeRideFlow.collectAsState(initial = null)
    val rideProgress by safeRideMonitor.progress.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var vehiclePlate by remember { mutableStateOf("") }
    var driverName by remember { mutableStateOf("") }
    var destinationName by remember { mutableStateOf("") }
    var etaMinutes by remember { mutableStateOf("25") }
    var checkInInterval by remember { mutableStateOf("5") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Safe Ride Journey Escort", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkNavyBackground)
            )
        },
        containerColor = DarkNavyBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (activeRide != null && !activeRide!!.isCompleted) {
                // Active Ride Session HUD
                val ride = activeRide!!
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = AccentPurple.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "JOURNEY IN PROGRESS",
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                color = TextPrimary
                            )
                            Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = AccentPurple)
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(text = "Destination: ${ride.destinationName}", fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text(text = "Vehicle Plate: ${ride.vehiclePlate}", color = TextSecondary)
                        Text(text = "Driver: ${ride.driverName}", color = TextSecondary)
                        Text(text = "Check-in Interval: Every ${ride.checkInIntervalMinutes} minutes", color = TextSecondary)

                        Spacer(modifier = Modifier.height(16.dp))

                        // Live Monitoring Countdowns
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = CardSurfaceDark)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                val checkInSecs = rideProgress.secondsUntilCheckIn
                                val checkInMin = checkInSecs / 60
                                val checkInSec = checkInSecs % 60

                                val etaSecs = rideProgress.secondsUntilEta
                                val etaMin = etaSecs / 60
                                val etaSec = etaSecs % 60

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Next Check-In Due:", color = TextSecondary, fontSize = 13.sp)
                                    Text(
                                        text = if (rideProgress.isCheckInDue) "OVERDUE!" else "%02d:%02d".format(checkInMin, checkInSec),
                                        fontWeight = FontWeight.Bold,
                                        color = if (rideProgress.isCheckInDue) CrimsonRed else SafeGreen,
                                        fontSize = 14.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Time until Expected Arrival:", color = TextSecondary, fontSize = 13.sp)
                                    Text(
                                        text = if (rideProgress.isEtaExceeded) "EXCEEDED!" else "%02d:%02d".format(etaMin, etaSec),
                                        fontWeight = FontWeight.Bold,
                                        color = if (rideProgress.isEtaExceeded) WarningAmber else TextPrimary,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }

                        if (rideProgress.isCheckInDue) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = CrimsonRed.copy(alpha = 0.25f))
                            ) {
                                Text(
                                    text = "CHECK-IN DUE NOW! Tap 'I'm OK' to confirm you are safe and reset the check-in timer.",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }

                        if (rideProgress.isEtaExceeded) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = WarningAmber.copy(alpha = 0.25f))
                            ) {
                                Text(
                                    text = "JOURNEY HAS EXCEEDED ETA! Please tap 'I'm OK' or 'End Journey' to prevent automated escalation.",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        val loc = locationTracker.getLastKnownLocation()
                                        safeRideRepository.recordCheckIn(
                                            ride.id,
                                            loc?.latitude ?: 0.0,
                                            loc?.longitude ?: 0.0
                                        )
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = SafeGreen)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("I'm OK (Check In)")
                            }

                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        safeRideRepository.completeRide(ride.id)
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = CardSurfaceElevated)
                            ) {
                                Text("End Journey")
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                engine.triggerEmergency(SosTriggerType.SAFE_RIDE_OVERDUE, bypassCountdown = true)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = CrimsonRed)
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Escalate to Emergency SOS")
                        }
                    }
                }
            } else {
                // Start New Journey Form
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardSurfaceDark)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Log Vehicle & Journey Details",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = "Automatically verifies your progress and triggers check-in prompts",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = vehiclePlate,
                            onValueChange = { vehiclePlate = it },
                            label = { Text("Vehicle Registration / Cab Plate") },
                            leadingIcon = { Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = TextMuted) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = driverName,
                            onValueChange = { driverName = it },
                            label = { Text("Driver Name / Cab Service") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = TextMuted) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = destinationName,
                            onValueChange = { destinationName = it },
                            label = { Text("Destination Name / Address") },
                            leadingIcon = { Icon(Icons.Default.Place, contentDescription = null, tint = TextMuted) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = etaMinutes,
                                onValueChange = { etaMinutes = it },
                                label = { Text("ETA (Mins)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = checkInInterval,
                                onValueChange = { checkInInterval = it },
                                label = { Text("Interval (Mins)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = {
                                if (vehiclePlate.isNotBlank() && destinationName.isNotBlank()) {
                                    coroutineScope.launch {
                                        safeRideRepository.startRide(
                                            vehiclePlate = vehiclePlate.trim(),
                                            driverName = driverName.trim(),
                                            destinationName = destinationName.trim(),
                                            destinationLatitude = null,
                                            destinationLongitude = null,
                                            expectedArrivalMinutes = etaMinutes.toIntOrNull() ?: 30,
                                            checkInIntervalMinutes = checkInInterval.toIntOrNull() ?: 5
                                        )
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
                        ) {
                            Text(
                                text = "Start Safe Ride Escort",
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}
