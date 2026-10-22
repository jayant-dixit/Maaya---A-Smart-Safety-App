package org.maaya.app.ui.screens.saferide

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.maaya.app.R
import org.maaya.app.data.repository.SafeRideRepository
import org.maaya.app.domain.engine.EmergencySosEngine
import org.maaya.app.domain.engine.LocationTracker
import org.maaya.app.domain.engine.SafeRideMonitor
import org.maaya.app.domain.model.SafeRideSession
import org.maaya.app.domain.model.SosTriggerType
import org.maaya.app.ui.theme.*
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

/**
 * Maaya Cab Escort Screen
 *
 * Implements:
 * 1. Screen One: Cab Escort Setup (Journey details, Cab photo capture & upload, animated start pulse button)
 * 2. Screen Two: Active Cab Escort (Live monitoring HUD, countdown, animated circular expanding wave Check-In, Emergency SOS)
 */
@Composable
fun SafeRideScreen(
    safeRideRepository: SafeRideRepository,
    locationTracker: LocationTracker,
    safeRideMonitor: SafeRideMonitor,
    engine: EmergencySosEngine,
    onBack: () -> Unit
) {
    val activeRide by safeRideRepository.activeRideFlow.collectAsState(initial = null)
    val allRides by safeRideRepository.allRidesFlow.collectAsState(initial = emptyList())
    val rideProgress by safeRideMonitor.progress.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // Setup Form State initialized with realistic defaults matching UI specs
    var vehiclePlate by remember { mutableStateOf("UP16 AB 1234") }
    var driverName by remember { mutableStateOf("Ramesh Kumar") }
    var destinationName by remember { mutableStateOf("Sector 62, Noida, UP") }
    var etaText by remember { mutableStateOf("30 mins") }
    var checkInIntervalMinutes by remember { mutableIntStateOf(10) }
    var cabPhotoUri by remember { mutableStateOf<String?>(null) }
    var cabModel by remember { mutableStateOf("White Swift Dzire") }

    // Dialog & Sheet States
    var showHistoryDialog by remember { mutableStateOf(false) }
    var showIntervalPicker by remember { mutableStateOf(false) }
    var showEndRideConfirmation by remember { mutableStateOf(false) }
    var showSosConfirmation by remember { mutableStateOf(false) }

    // Check-in success animation state
    var isCheckInSuccess by remember { mutableStateOf(false) }

    // Setup Image Launchers: Camera capture & Gallery upload
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        bitmap?.let { bmp ->
            try {
                val file = File(context.cacheDir, "cab_camera_${System.currentTimeMillis()}.jpg")
                FileOutputStream(file).use { out ->
                    bmp.compress(Bitmap.CompressFormat.JPEG, 90, out)
                }
                cabPhotoUri = file.absolutePath
                Toast.makeText(context, "Cab photo captured!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Could not save photo: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            cabPhotoUri = it.toString()
            Toast.makeText(context, "Cab image uploaded successfully!", Toast.LENGTH_SHORT).show()
        }
    }

    val isRideActive = activeRide != null && !activeRide!!.isCompleted

    Scaffold(
        containerColor = SkyBlueBackground
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (!isRideActive) {
                // ==========================================
                // SCREEN ONE: CAB ESCORT SETUP
                // ==========================================
                CabEscortSetupContent(
                    vehiclePlate = vehiclePlate,
                    onVehiclePlateChange = { vehiclePlate = it },
                    driverName = driverName,
                    onDriverNameChange = { driverName = it },
                    destinationName = destinationName,
                    onDestinationNameChange = { destinationName = it },
                    etaText = etaText,
                    onEtaTextChange = { etaText = it },
                    checkInIntervalMinutes = checkInIntervalMinutes,
                    onIntervalClick = { showIntervalPicker = true },
                    cabPhotoUri = cabPhotoUri,
                    onRemovePhoto = { cabPhotoUri = null },
                    onTakePhotoClick = { cameraLauncher.launch(null) },
                    onUploadPhotoClick = { galleryLauncher.launch("image/*") },
                    onStartEscortClick = {
                        if (vehiclePlate.isBlank() || destinationName.isBlank()) {
                            Toast.makeText(context, "Please enter vehicle plate and destination", Toast.LENGTH_SHORT).show()
                        } else {
                            val parsedEta = etaText.filter { it.isDigit() }.toIntOrNull() ?: 30
                            coroutineScope.launch {
                                safeRideRepository.startRide(
                                    vehiclePlate = vehiclePlate.trim().uppercase(),
                                    driverName = driverName.trim().ifBlank { "Driver" },
                                    destinationName = destinationName.trim(),
                                    destinationLatitude = null,
                                    destinationLongitude = null,
                                    expectedArrivalMinutes = parsedEta,
                                    checkInIntervalMinutes = checkInIntervalMinutes,
                                    cabPhotoUri = cabPhotoUri,
                                    cabModel = cabModel
                                )
                                Toast.makeText(context, "Cab Escort initiated. Monitoring ride...", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onBack = onBack,
                    onHistoryClick = { showHistoryDialog = true }
                )
            } else {
                // ==========================================
                // SCREEN TWO: ACTIVE CAB ESCORT
                // ==========================================
                val currentRide = activeRide!!
                CabEscortActiveContent(
                    ride = currentRide,
                    secondsUntilCheckIn = rideProgress.secondsUntilCheckIn,
                    isCheckInDue = rideProgress.isCheckInDue,
                    isCheckInSuccess = isCheckInSuccess,
                    onCheckInClick = {
                        coroutineScope.launch {
                            val loc = locationTracker.getLastKnownLocation()
                            safeRideRepository.recordCheckIn(
                                currentRide.id,
                                loc?.latitude ?: 0.0,
                                loc?.longitude ?: 0.0
                            )
                            isCheckInSuccess = true
                            delay(2500)
                            isCheckInSuccess = false
                        }
                    },
                    onEndRideClick = { showEndRideConfirmation = true },
                    onSosClick = { showSosConfirmation = true },
                    onBack = onBack
                )
            }
        }
    }

    // Interval Picker Dialog
    if (showIntervalPicker) {
        IntervalPickerDialog(
            currentInterval = checkInIntervalMinutes,
            onSelect = {
                checkInIntervalMinutes = it
                showIntervalPicker = false
            },
            onDismiss = { showIntervalPicker = false }
        )
    }

    // Ride History Dialog
    if (showHistoryDialog) {
        RideHistoryDialog(
            rides = allRides,
            onDismiss = { showHistoryDialog = false }
        )
    }

    // End Ride Confirmation Dialog
    if (showEndRideConfirmation && activeRide != null) {
        AlertDialog(
            onDismissRequest = { showEndRideConfirmation = false },
            title = { Text("End Cab Escort?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Are you sure you want to end this escort session? Real-time check-in prompts and emergency monitoring will conclude.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showEndRideConfirmation = false
                        coroutineScope.launch {
                            safeRideRepository.completeRide(activeRide!!.id)
                            Toast.makeText(context, "Cab Escort session ended safely.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkCrimson)
                ) {
                    Text("End Ride", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showEndRideConfirmation = false }) {
                    Text("Cancel", color = TextPrimary)
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = SurfaceWhite
        )
    }

    // Emergency SOS Confirmation Dialog
    if (showSosConfirmation) {
        AlertDialog(
            onDismissRequest = { showSosConfirmation = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = CrimsonRed)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Escalate to Emergency SOS?", fontWeight = FontWeight.Bold, color = CrimsonRed)
                }
            },
            text = {
                Text(
                    "This will immediately trigger an Emergency SOS, dispatching your live GPS tracking link and alerts to all trusted contacts.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSosConfirmation = false
                        engine.triggerEmergency(SosTriggerType.SAFE_RIDE_OVERDUE, bypassCountdown = true)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonRed)
                ) {
                    Text("Trigger SOS Now", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showSosConfirmation = false }) {
                    Text("Cancel", color = TextPrimary)
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = SurfaceWhite
        )
    }
}

// =========================================================================
// SCREEN 1 CONTENT: SETUP
// =========================================================================

@Composable
private fun CabEscortSetupContent(
    vehiclePlate: String,
    onVehiclePlateChange: (String) -> Unit,
    driverName: String,
    onDriverNameChange: (String) -> Unit,
    destinationName: String,
    onDestinationNameChange: (String) -> Unit,
    etaText: String,
    onEtaTextChange: (String) -> Unit,
    checkInIntervalMinutes: Int,
    onIntervalClick: () -> Unit,
    cabPhotoUri: String?,
    onRemovePhoto: () -> Unit,
    onTakePhotoClick: () -> Unit,
    onUploadPhotoClick: () -> Unit,
    onStartEscortClick: () -> Unit,
    onBack: () -> Unit,
    onHistoryClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circular Back Button
            Surface(
                onClick = onBack,
                shape = CircleShape,
                color = SurfaceWhite,
                shadowElevation = 2.dp,
                border = BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Cab Escort",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Stay safe. We'll keep track of your ride.",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            }

            // Ride History Icon Button
            Surface(
                onClick = onHistoryClick,
                shape = CircleShape,
                color = SurfaceWhite,
                shadowElevation = 2.dp,
                border = BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "Ride History",
                        tint = TextPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Card 1: Journey Details Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(elevation = 3.dp, shape = RoundedCornerShape(24.dp), spotColor = CardShadowLight),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
            border = BorderStroke(1.dp, BorderSubtle)
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                // 1. Vehicle Number
                SetupInputFieldRow(
                    icon = Icons.Default.DirectionsCar,
                    iconBgColor = Color(0xFFE0F2FE),
                    iconColor = Color(0xFF0284C7),
                    label = "Vehicle Number",
                    value = vehiclePlate,
                    onValueChange = onVehiclePlateChange,
                    placeholder = "e.g. UP16 AB 1234"
                )

                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

                // 2. Driver Name
                SetupInputFieldRow(
                    icon = Icons.Default.Person,
                    iconBgColor = Color(0xFFF3E8FF),
                    iconColor = Color(0xFF9333EA),
                    label = "Driver Name",
                    value = driverName,
                    onValueChange = onDriverNameChange,
                    placeholder = "e.g. Ramesh Kumar"
                )

                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

                // 3. Destination
                SetupInputFieldRow(
                    icon = Icons.Default.Place,
                    iconBgColor = Color(0xFFFEE2E2),
                    iconColor = Color(0xFFEF4444),
                    label = "Destination",
                    value = destinationName,
                    onValueChange = onDestinationNameChange,
                    placeholder = "Sector 62, Noida, UP"
                )

                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

                // 4. ETA
                SetupInputFieldRow(
                    icon = Icons.Default.Schedule,
                    iconBgColor = Color(0xFFE0F2FE),
                    iconColor = Color(0xFF0284C7),
                    label = "Estimated Time of Arrival (ETA)",
                    value = etaText,
                    onValueChange = onEtaTextChange,
                    placeholder = "30 mins"
                )

                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

                // 5. Check-in Interval
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onIntervalClick)
                        .padding(vertical = 12.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(Color(0xFFDCFCE7), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = SafeGreen,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Check-in Interval",
                                fontSize = 11.sp,
                                color = TextMuted,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                        Text(
                            text = "$checkInIntervalMinutes minutes",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Select Interval",
                        tint = TextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Text(
                    text = "You'll receive a check-in notification at this interval to confirm you are safe. If you don't respond, we'll alert your emergency contacts.",
                    fontSize = 11.5.sp,
                    color = TextSecondary,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 2.dp, bottom = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Card 2: Cab Photo Card (Supports both Take Photo & Upload Image)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(elevation = 3.dp, shape = RoundedCornerShape(24.dp), spotColor = CardShadowLight),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
            border = BorderStroke(1.dp, BorderSubtle)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(Color(0xFFEDE9FE), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = null,
                            tint = AccentPurple,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Cab Photo",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Take a clear photo of the cab",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }

                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Photos & Actions Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Photo Preview (either captured/uploaded or default sample)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(108.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFFF1F5F9))
                    ) {
                        CabImageThumbnail(
                            photoUri = cabPhotoUri,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Close / remove button on top right
                        Surface(
                            onClick = onRemovePhoto,
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.55f),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp)
                                .size(22.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }

                    // Action Buttons Column: "Take Photo" & "Upload Image"
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Take Photo Button
                        DashedActionButton(
                            icon = Icons.Default.PhotoCamera,
                            label = "Take Photo",
                            onClick = onTakePhotoClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        )

                        // Upload Photo Button (Requested feature!)
                        DashedActionButton(
                            icon = Icons.Default.CloudUpload,
                            label = "Upload Image",
                            onClick = onUploadPhotoClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Center Pulsing "Start Cab Escort" Button
        Box(
            modifier = Modifier
                .size(240.dp)
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            // Animated concentric wave rings around Start button
            StartEscortConcentricRings()

            // Large Circular Purple Button
            Surface(
                onClick = onStartEscortClick,
                shape = CircleShape,
                color = Color.Transparent,
                shadowElevation = 12.dp,
                modifier = Modifier.size(136.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                listOf(Color(0xFF7C3AED), Color(0xFF5B21B6))
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsCar,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(34.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Start",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Cab Escort",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Bottom Safety Information Card
        SafetyInformationCard(
            text = "Your ride will be monitored and your live location will be shared with your emergency contacts during the trip."
        )

        Spacer(modifier = Modifier.height(90.dp))
    }
}

// =========================================================================
// SCREEN 2 CONTENT: ACTIVE CAB ESCORT
// =========================================================================

@Composable
private fun CabEscortActiveContent(
    ride: SafeRideSession,
    secondsUntilCheckIn: Int,
    isCheckInDue: Boolean,
    isCheckInSuccess: Boolean,
    onCheckInClick: () -> Unit,
    onEndRideClick: () -> Unit,
    onSosClick: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Active Ride Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                onClick = onBack,
                shape = CircleShape,
                color = SurfaceWhite,
                shadowElevation = 2.dp,
                border = BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Cab Escort Active",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "We're monitoring your ride in real time.",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            }

            // Red-tinted "End Ride" Button
            Surface(
                onClick = onEndRideClick,
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFFEE2E2),
                modifier = Modifier.height(38.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(DarkCrimson, RoundedCornerShape(2.dp))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "End Ride",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkCrimson
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Large Deep Purple Gradient Ride Details Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(elevation = 8.dp, shape = RoundedCornerShape(24.dp), spotColor = Color(0x332E1065)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            listOf(Color(0xFF382972), Color(0xFF1F1845))
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .padding(18.dp)
            ) {
                Column {
                    // Top Row: "Tracking Live" pill & "Emergency SOS" button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Tracking Live Pill Badge
                        TrackingLiveBadge()

                        // Bright Red Emergency SOS Button
                        Surface(
                            onClick = onSosClick,
                            shape = RoundedCornerShape(20.dp),
                            color = Color.Transparent,
                            modifier = Modifier.height(42.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(
                                        brush = Brush.horizontalGradient(
                                            listOf(Color(0xFFFF3366), Color(0xFFDC2626))
                                        ),
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                    .padding(horizontal = 14.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Security,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(
                                            text = "Emergency",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            lineHeight = 12.sp
                                        )
                                        Text(
                                            text = "SOS",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color.White,
                                            lineHeight = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Vehicle Row (Cab Photo thumbnail + Plate & Model)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 82.dp, height = 54.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF2E2259))
                        ) {
                            CabImageThumbnail(
                                photoUri = ride.cabPhotoUri,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Text(
                                text = ride.vehiclePlate.ifBlank { "UP16 AB 1234" },
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = ride.cabModel ?: "White Swift Dzire",
                                fontSize = 13.sp,
                                color = Color(0xFFD8B4FE)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Driver Row (Avatar with checkmark, Name, Rating)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(46.dp)) {
                            Image(
                                painter = painterResource(id = R.drawable.driver_ramesh),
                                contentDescription = "Driver Avatar",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                            )
                            // Small green verified checkmark badge
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .align(Alignment.BottomEnd)
                                    .background(SafeGreen, CircleShape)
                                    .border(1.5.dp, Color(0xFF1F1845), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(10.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = ride.driverName.ifBlank { "Ramesh Kumar" },
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Driver",
                                fontSize = 12.sp,
                                color = Color(0xFFC4B5FD)
                            )
                        }

                        // Rating
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFFBBF24),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "4.8",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "(256 rides)",
                                fontSize = 12.sp,
                                color = Color(0xFFC4B5FD)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    HorizontalDivider(color = Color(0xFF4C3D82), thickness = 0.8.dp)

                    Spacer(modifier = Modifier.height(14.dp))

                    // Bottom 3 Metrics Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // 1. Destination
                        Column(modifier = Modifier.weight(1.3f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Place,
                                    contentDescription = null,
                                    tint = Color(0xFFA78BFA),
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("Destination", fontSize = 10.5.sp, color = Color(0xFFA78BFA))
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = ride.destinationName.ifBlank { "Sector 62, Noida, UP" },
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // 2. ETA
                        Column(modifier = Modifier.weight(0.9f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = Color(0xFFA78BFA),
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("ETA", fontSize = 10.5.sp, color = Color(0xFFA78BFA))
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${ride.expectedArrivalMinutes} mins",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }

                        // 3. Check-in Interval
                        Column(modifier = Modifier.weight(1.1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFFA78BFA),
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("Check-in Interval", fontSize = 10.5.sp, color = Color(0xFFA78BFA))
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${ride.checkInIntervalMinutes} minutes",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // Live Overdue Notice if applicable
        if (isCheckInDue) {
            Spacer(modifier = Modifier.height(14.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CrimsonRed.copy(alpha = 0.15f)),
                border = BorderStroke(1.dp, CrimsonRed.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = CrimsonRed, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Check-in is due now! Tap 'Check In' below to confirm you are safe.",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CrimsonRed
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(30.dp))

        // Center Animated Check In Control
        val countdownMinutes = secondsUntilCheckIn / 60
        val countdownSecs = secondsUntilCheckIn % 60
        val countdownText = if (isCheckInDue) "DUE NOW!" else "%02d:%02d".format(countdownMinutes, countdownSecs)

        Box(
            modifier = Modifier
                .size(260.dp)
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            // Continuously expanding circular rings & breathing radial glow
            ActiveCheckInExpandingRings(isCheckInDue = isCheckInDue)

            // Animated Button Background Color
            val buttonGradient = if (isCheckInSuccess) {
                Brush.verticalGradient(listOf(SafeGreen, SafeGreenDark))
            } else if (isCheckInDue) {
                Brush.verticalGradient(listOf(Color(0xFFE11D48), Color(0xFFBE123C)))
            } else {
                Brush.verticalGradient(listOf(Color(0xFF7C3AED), Color(0xFF5B21B6)))
            }

            Surface(
                onClick = onCheckInClick,
                shape = CircleShape,
                color = Color.Transparent,
                shadowElevation = 14.dp,
                modifier = Modifier.size(150.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(brush = buttonGradient, shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (isCheckInSuccess) Icons.Default.Check else Icons.Default.Place,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isCheckInSuccess) "Checked In!" else "Check In",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isCheckInSuccess) {
                                "Confirmed safe"
                            } else {
                                "Next check in\nin $countdownText"
                            },
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 13.sp,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        // Safety Information Card at bottom
        SafetyInformationCard(
            text = "Your ride is being monitored and your live location is shared with your emergency contacts."
        )

        Spacer(modifier = Modifier.height(90.dp))
    }
}

// =========================================================================
// REUSABLE COMPONENTS & ANIMATIONS
// =========================================================================

/**
 * Setup Screen Input Field Row with circular icon, label, text field, and clear button
 */
@Composable
private fun SetupInputFieldRow(
    icon: ImageVector,
    iconBgColor: Color,
    iconColor: Color,
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(iconBgColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 11.sp,
                color = TextMuted,
                fontWeight = FontWeight.Medium
            )
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = TextStyle(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { innerTextField ->
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            fontSize = 15.sp,
                            color = TextMuted
                        )
                    }
                    innerTextField()
                }
            )
        }

        if (value.isNotEmpty()) {
            IconButton(
                onClick = { onValueChange("") },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Cancel,
                    contentDescription = "Clear",
                    tint = TextMuted.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * Dashed Action Button for Camera & Uploading
 */
@Composable
private fun DashedActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFF8FAFC),
        modifier = modifier.drawBehind {
            val stroke = Stroke(
                width = 2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)
            )
            drawRoundRect(
                color = Color(0xFF93C5FD),
                style = stroke,
                cornerRadius = CornerRadius(14.dp.toPx())
            )
        }
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = PrimarySkyBlue,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = PrimarySkyBlue
            )
        }
    }
}

/**
 * Renders the Cab Image Thumbnail (either from Uri, file, or default sample)
 */
@Composable
private fun CabImageThumbnail(
    photoUri: String?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var loadedBitmap by remember(photoUri) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(photoUri) {
        if (!photoUri.isNullOrBlank()) {
            try {
                if (photoUri.startsWith("content://")) {
                    val uri = Uri.parse(photoUri)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        val source = ImageDecoder.createSource(context.contentResolver, uri)
                        loadedBitmap = ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                            decoder.isMutableRequired = true
                        }
                    } else {
                        @Suppress("DEPRECATION")
                        loadedBitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                    }
                } else {
                    loadedBitmap = BitmapFactory.decodeFile(photoUri)
                }
            } catch (_: Exception) {
                loadedBitmap = null
            }
        } else {
            loadedBitmap = null
        }
    }

    if (loadedBitmap != null) {
        Image(
            bitmap = loadedBitmap!!.asImageBitmap(),
            contentDescription = "Cab Photo",
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    } else {
        // High quality fallback sample cab photo
        Image(
            painter = painterResource(id = R.drawable.sample_cab),
            contentDescription = "Cab Photo Preview",
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    }
}

/**
 * Pulsing Live Tracking Badge
 */
@Composable
private fun TrackingLiveBadge() {
    val infiniteTransition = rememberInfiniteTransition(label = "trackingPulse")
    val dotAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotAlpha"
    )

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = SurfaceWhite,
        modifier = Modifier.height(34.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .background(SafeGreen.copy(alpha = dotAlpha), CircleShape)
            )
            Spacer(modifier = Modifier.width(7.dp))
            Text(
                text = "Tracking Live",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = SafeGreenDark
            )
        }
    }
}

/**
 * Idle Concentric Waves for Setup Screen
 */
@Composable
private fun StartEscortConcentricRings() {
    val infiniteTransition = rememberInfiniteTransition(label = "setupIdlePulse")
    val ringScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ringScale"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height / 2f)

        // Radial glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF8B5CF6).copy(alpha = 0.15f),
                    Color(0xFF8B5CF6).copy(alpha = 0.05f),
                    Color.Transparent
                ),
                center = center,
                radius = size.width / 2f
            ),
            radius = size.width / 2f,
            center = center
        )

        // Outer smooth ring
        drawCircle(
            color = Color(0xFF8B5CF6).copy(alpha = 0.12f),
            radius = (size.width / 2f - 10f) * ringScale,
            center = center,
            style = Stroke(width = 1.8.dp.toPx())
        )

        // Dashed circle
        val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
        drawCircle(
            color = Color(0xFF8B5CF6).copy(alpha = 0.28f),
            radius = (size.width / 2f - 30f) * ringScale,
            center = center,
            style = Stroke(width = 1.5.dp.toPx(), pathEffect = dashEffect)
        )

        // Inner soft ring
        drawCircle(
            color = Color(0xFF8B5CF6).copy(alpha = 0.22f),
            radius = (size.width / 2f - 48f),
            center = center,
            style = Stroke(width = 2.dp.toPx())
        )
    }
}

/**
 * Continuous Expanding Waves for Active Check In Control
 */
@Composable
private fun ActiveCheckInExpandingRings(isCheckInDue: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "expandingWaves")

    val wave1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave1"
    )

    val wave2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, delayMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave2"
    )

    val wave3 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, delayMillis = 1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave3"
    )

    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(14000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotationAngle"
    )

    val ringColor = if (isCheckInDue) CrimsonRed else Color(0xFF8B5CF6)

    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val baseRadius = 78.dp.toPx()
        val maxExpand = size.width / 2f - baseRadius

        // Radial glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    ringColor.copy(alpha = 0.22f),
                    ringColor.copy(alpha = 0.08f),
                    Color.Transparent
                ),
                center = center,
                radius = size.width / 2f
            ),
            radius = size.width / 2f,
            center = center
        )

        // Expanding Wave 1
        drawCircle(
            color = ringColor.copy(alpha = (1f - wave1) * 0.35f),
            radius = baseRadius + maxExpand * wave1,
            center = center,
            style = Stroke(width = 2.dp.toPx())
        )

        // Expanding Wave 2
        drawCircle(
            color = ringColor.copy(alpha = (1f - wave2) * 0.35f),
            radius = baseRadius + maxExpand * wave2,
            center = center,
            style = Stroke(width = 2.dp.toPx())
        )

        // Expanding Wave 3
        drawCircle(
            color = ringColor.copy(alpha = (1f - wave3) * 0.35f),
            radius = baseRadius + maxExpand * wave3,
            center = center,
            style = Stroke(width = 2.dp.toPx())
        )

        // Dashed circle orbit
        val dashEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)
        drawCircle(
            color = ringColor.copy(alpha = 0.3f),
            radius = baseRadius + 24.dp.toPx(),
            center = center,
            style = Stroke(width = 1.5.dp.toPx(), pathEffect = dashEffect)
        )

        // Rotating orbit particles
        val orbitRadius = baseRadius + 24.dp.toPx()
        val particleAngles = listOf(0.0, 90.0, 180.0, 270.0)
        particleAngles.forEach { baseAngle ->
            val angleRad = Math.toRadians(baseAngle + rotationAngle)
            val px = center.x + orbitRadius * cos(angleRad).toFloat()
            val py = center.y + orbitRadius * sin(angleRad).toFloat()
            drawCircle(
                color = ringColor.copy(alpha = 0.7f),
                radius = 3.5.dp.toPx(),
                center = Offset(px, py)
            )
        }
    }
}

/**
 * Bottom Safety Information Card
 */
@Composable
private fun SafetyInformationCard(text: String) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFEFF4FE),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(Color(0xFFEDE9FE), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = Color(0xFF6366F1),
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = text,
                fontSize = 11.5.sp,
                color = TextSecondary,
                lineHeight = 16.sp
            )
        }
    }
}

/**
 * Interval Selection Dialog
 */
@Composable
private fun IntervalPickerDialog(
    currentInterval: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val intervals = listOf(5, 10, 15, 20)
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = SurfaceWhite,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Select Check-in Interval",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "How frequently should Maaya verify that you are safe during the trip?",
                    fontSize = 12.5.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
                )

                intervals.forEach { minutes ->
                    val isSelected = minutes == currentInterval
                    Surface(
                        onClick = { onSelect(minutes) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) AccentPurpleSoft else Color(0xFFF8FAFC),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) AccentPurple else BorderSubtle
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "$minutes minutes",
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) AccentPurple else TextPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = AccentPurple,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Close", color = TextPrimary)
                }
            }
        }
    }
}

/**
 * Previous Ride History Dialog
 */
@Composable
private fun RideHistoryDialog(
    rides: List<SafeRideSession>,
    onDismiss: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = SurfaceWhite,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.7f)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Cab Escort History",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                if (rides.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.DirectionsCar,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No previous cab escort records yet", color = TextMuted, fontSize = 13.sp)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(rides) { ride ->
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                border = BorderStroke(1.dp, BorderSubtle),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = ride.vehiclePlate,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = TextPrimary
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (ride.isCompleted) SafeGreenSoft else EmergencyRedSoft
                                        ) {
                                            Text(
                                                text = if (ride.isCompleted) "Completed" else "In Progress",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (ride.isCompleted) SafeGreenDark else CrimsonRed,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "To: ${ride.destinationName}",
                                        fontSize = 12.sp,
                                        color = TextSecondary
                                    )
                                    Text(
                                        text = "Driver: ${ride.driverName} • ${dateFormat.format(Date(ride.startedAt))}",
                                        fontSize = 11.sp,
                                        color = TextMuted
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimarySkyBlue),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Done", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// =========================================================================
// COMPOSE PREVIEWS FOR ANDROID STUDIO TOOLING
// =========================================================================

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
fun CabEscortSetupScreenPreview() {
    MaayaTheme {
        CabEscortSetupContent(
            vehiclePlate = "UP16 AB 1234",
            onVehiclePlateChange = {},
            driverName = "Ramesh Kumar",
            onDriverNameChange = {},
            destinationName = "Sector 62, Noida, UP",
            onDestinationNameChange = {},
            etaText = "30 mins",
            onEtaTextChange = {},
            checkInIntervalMinutes = 10,
            onIntervalClick = {},
            cabPhotoUri = null,
            onRemovePhoto = {},
            onTakePhotoClick = {},
            onUploadPhotoClick = {},
            onStartEscortClick = {},
            onBack = {},
            onHistoryClick = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
fun CabEscortActiveScreenPreview() {
    MaayaTheme {
        CabEscortActiveContent(
            ride = SafeRideSession(
                id = "preview-ride-1",
                vehiclePlate = "UP16 AB 1234",
                driverName = "Ramesh Kumar",
                destinationName = "Sector 62, Noida, UP",
                destinationLatitude = 28.628,
                destinationLongitude = 77.375,
                expectedArrivalMinutes = 30,
                checkInIntervalMinutes = 10,
                startedAt = System.currentTimeMillis() - 96_000L,
                lastCheckInAt = System.currentTimeMillis() - 96_000L,
                isCompleted = false,
                isEscalatedToSos = false,
                cabPhotoUri = null,
                cabModel = "White Swift Dzire"
            ),
            secondsUntilCheckIn = 504, // 08:24 countdown!
            isCheckInDue = false,
            isCheckInSuccess = false,
            onCheckInClick = {},
            onEndRideClick = {},
            onSosClick = {},
            onBack = {}
        )
    }
}

