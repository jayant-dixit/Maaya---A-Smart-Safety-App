package org.maaya.app.ui.screens.sos

import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Base64
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import org.maaya.app.R
import org.maaya.app.data.repository.ContactRepository
import org.maaya.app.domain.engine.EmergencySosEngine
import org.maaya.app.domain.engine.SosEngineState
import org.maaya.app.media.EmergencyMediaProvider
import org.maaya.app.ui.theme.*
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ActiveSosScreen(
    engine: EmergencySosEngine,
    mediaProvider: EmergencyMediaProvider,
    contactRepository: ContactRepository? = null,
    onResolved: () -> Unit
) {
    val engineState by engine.engineState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val contactsList by (contactRepository?.contacts ?: kotlinx.coroutines.flow.flowOf(emptyList()))
        .collectAsState(initial = emptyList())

    var showPinDialog by remember { mutableStateOf(false) }
    var enteredPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }
    var isFrontCamera by remember { mutableStateOf(true) }

    // Intercept back button to prevent accidental exit of emergency mode
    BackHandler(enabled = true) {
        if (engineState is SosEngineState.Countdown) {
            engine.cancelCountdown()
            onResolved()
        } else {
            pinError = null
            enteredPin = ""
            showPinDialog = true
        }
    }

    var hasBeenActive by remember { mutableStateOf(false) }

    LaunchedEffect(engineState) {
        if (engineState is SosEngineState.Active || engineState is SosEngineState.Countdown) {
            hasBeenActive = true
        } else if (hasBeenActive && engineState is SosEngineState.Idle) {
            onResolved()
        }
    }

    val isCountdown = engineState is SosEngineState.Countdown
    val countdownRemaining = (engineState as? SosEngineState.Countdown)?.remainingSeconds ?: 4

    // Infinite animation transition driving waves, radar rotation, and glow
    val infiniteTransition = rememberInfiniteTransition(label = "SosRadarAnimation")

    val waveProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "waveProgress"
    )

    val radarRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 14000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radarRotation"
    )

    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowPulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFFFFFF),
                        Color(0xFFFFF7F9),
                        Color(0xFFFDECEF),
                        Color(0xFFFDE2E8)
                    )
                )
            )
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // ==========================================
            // SECTION A: EMERGENCY HEADER (Stationary at Top)
            // ==========================================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 16.dp, bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isCountdown) "Requesting Emergency Help..." else "Emergency Help Requested",
                    fontWeight = FontWeight.Black,
                    fontSize = 22.sp,
                    color = Color(0xFF0F172A),
                    textAlign = TextAlign.Center,
                    letterSpacing = (-0.3).sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = if (isCountdown) {
                        "Please stay calm. Help is being requested. Your emergency contacts and nearby rescue services have been alerted."
                    } else {
                        "Your distress telemetry and live location have been broadcast. Help is on the way."
                    },
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF475569),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }

            // ==========================================
            // SECTION B: CENTRAL SOS ANIMATION & CONTACTS (Centered)
            // ==========================================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                // Layer 1: Animated Radar Waves, Concentric Dotted Rings & Particles
                SosRadarCanvas(
                    waveProgress = waveProgress,
                    radarRotation = radarRotation,
                    glowPulse = glowPulse,
                    modifier = Modifier.fillMaxSize()
                )

                // Layer 2: Central Enlarged 3D Coral-Red Dome with Thick White Ring
                CentralSosCircle(
                    displayNumber = if (isCountdown) String.format("%02d", countdownRemaining) else "SOS",
                    glowPulse = glowPulse
                )

                // Layer 3: ONLY Actual Contacts from Contact List with Initial Badge
                val contactOffsets = listOf(
                    Offset(-118f, -92f), // Top-Left
                    Offset(118f, -88f),  // Top-Right
                    Offset(-118f, 92f),  // Bottom-Left
                    Offset(118f, 96f)    // Bottom-Right
                )

                contactsList.take(4).forEachIndexed { index, contact ->
                    val offset = contactOffsets[index]
                    RadarContactAvatar(
                        name = contact.name,
                        index = index,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .offset(x = offset.x.dp, y = offset.y.dp)
                    )
                }
            }

            // ==========================================
            // SECTION C & D: FIXED BOTTOM CONTROLS (Camera & Safe Button)
            // ==========================================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Collapsible Live Camera Dropdown Card
                LiveCameraDropdownCard(
                    isFrontCamera = isFrontCamera,
                    onSwitchCamera = {
                        isFrontCamera = !isFrontCamera
                        mediaProvider.switchCamera()
                    },
                    onFrameCaptured = { base64 ->
                        val activeIncident = (engineState as? SosEngineState.Active)?.incident
                        mediaProvider.broadcastLiveFrame(base64, activeIncident?.id)
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // "I'm safe" Action Button with Centered Icons and Text
                ImSafeButton(
                    onClick = {
                        if (isCountdown) {
                            engine.cancelCountdown()
                            onResolved()
                        } else {
                            pinError = null
                            enteredPin = ""
                            showPinDialog = true
                        }
                    }
                )
            }
        }
    }

    // ==========================================
    // SAFETY PIN CONFIRMATION DIALOG
    // ==========================================
    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = {
                Text(
                    text = "Enter PIN to Confirm Safety",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
            },
            text = {
                Column {
                    Text(
                        text = "Safety PIN is required to deactivate emergency broadcast and inform responders you are safe.",
                        fontSize = 13.sp,
                        color = Color(0xFF475569)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "(Default PIN: 1234)",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B),
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = enteredPin,
                        onValueChange = { if (it.length <= 6) enteredPin = it },
                        label = { Text("Safety PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    if (pinError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = pinError!!,
                            color = CrimsonRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            val result = engine.resolveEmergency(enteredPin, "User marked safe with PIN")
                            if (result.isSuccess) {
                                showPinDialog = false
                                onResolved()
                            } else {
                                pinError = result.exceptionOrNull()?.message ?: "Incorrect PIN. Default is 1234."
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C882)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Confirm Safe", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false }) {
                    Text("Cancel", color = Color(0xFF64748B))
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = Color.White
        )
    }
}

/**
 * Animated Canvas drawing expanding wave ripples, concentric dotted radar rings,
 * floating pink light particles, and wide diffused ambient red/pink glow.
 */
@Composable
fun SosRadarCanvas(
    waveProgress: Float,
    radarRotation: Float,
    glowPulse: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val center = Offset(centerX, centerY)

        val baseRadius = 90.dp.toPx()
        val maxWaveRadius = 220.dp.toPx()

        // 1. Soft Ambient Center Glow (Spread wide red/pink shade across canvas)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFF3B5C).copy(alpha = 0.32f * glowPulse.coerceIn(0.8f, 1.15f)),
                    Color(0xFFFF5E7E).copy(alpha = 0.22f * glowPulse.coerceIn(0.8f, 1.15f)),
                    Color(0xFFFF8DA1).copy(alpha = 0.12f),
                    Color(0xFFFFCCD5).copy(alpha = 0.05f),
                    Color.Transparent
                ),
                center = center,
                radius = 230.dp.toPx()
            ),
            radius = 230.dp.toPx(),
            center = center
        )

        // 2. Expanding SOS Wave Rings (4 staggered rings)
        for (i in 0..3) {
            val ringPhase = (waveProgress + i * 0.25f) % 1f
            val currentRadius = baseRadius + ringPhase * (maxWaveRadius - baseRadius)
            val ringAlpha = (1f - ringPhase) * 0.38f
            val fillAlpha = (1f - ringPhase) * 0.04f

            // Faint expanding fill
            drawCircle(
                color = Color(0xFFFF5277).copy(alpha = fillAlpha),
                radius = currentRadius,
                center = center
            )

            // Expanding stroke
            drawCircle(
                color = Color(0xFFFF4D6D).copy(alpha = ringAlpha),
                radius = currentRadius,
                center = center,
                style = Stroke(width = 1.8.dp.toPx())
            )
        }

        // 3. Concentric Dotted Radar Rings (Centered on SOS Button)
        val dottedRadii = listOf(112.dp.toPx(), 152.dp.toPx(), 194.dp.toPx())
        val rotationPhase = (radarRotation * 2f)

        for (radius in dottedRadii) {
            drawCircle(
                color = Color(0xFFFF758F).copy(alpha = 0.45f),
                radius = radius,
                center = center,
                style = Stroke(
                    width = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 9.dp.toPx()), rotationPhase)
                )
            )
        }

        // 4. Floating Light Particles around radar rings
        val particleAngles = listOf(
            0.4f, 1.1f, 1.8f, 2.5f, 3.2f, 3.9f, 4.6f, 5.3f, 6.0f,
            0.8f, 2.1f, 3.6f, 4.9f
        )
        val particleRadii = listOf(
            114.dp.toPx(), 152.dp.toPx(), 194.dp.toPx(), 130.dp.toPx(),
            175.dp.toPx(), 108.dp.toPx(), 148.dp.toPx(), 190.dp.toPx(),
            120.dp.toPx(), 160.dp.toPx(), 186.dp.toPx(), 138.dp.toPx(), 172.dp.toPx()
        )
        val particleSizes = listOf(3.dp, 4.dp, 2.5.dp, 3.5.dp, 4.5.dp, 3.dp, 4.dp, 2.5.dp, 3.dp, 3.5.dp, 4.dp, 3.dp, 3.5.dp)

        for (idx in particleAngles.indices) {
            val angle = particleAngles[idx] + (radarRotation * (Math.PI.toFloat() / 180f) * 0.15f)
            val pRadius = particleRadii[idx]
            val px = centerX + pRadius * cos(angle)
            val py = centerY + pRadius * sin(angle)
            val pColor = if (idx % 2 == 0) Color(0xFFFF3B5C) else Color(0xFFFFFFFF)
            val pAlpha = if (idx % 2 == 0) 0.65f else 0.85f

            drawCircle(
                color = pColor.copy(alpha = pAlpha * glowPulse.coerceIn(0.7f, 1f)),
                radius = particleSizes[idx].toPx(),
                center = Offset(px, py)
            )
        }
    }
}

/**
 * Central SOS 3D Dome Button displaying countdown e.g. "04"
 * with outer translucent halo, thick white bevel ring, and coral-red dome.
 * Increased in size for visual prominence.
 */
@Composable
fun CentralSosCircle(
    displayNumber: String,
    glowPulse: Float,
    modifier: Modifier = Modifier
) {
    // Outer subtle translucent halo ring
    Box(
        modifier = modifier
            .size(208.dp)
            .border(1.5.dp, Color(0x33FF6B88), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        // White Bevel Ring with Deep Shadow
        Box(
            modifier = Modifier
                .size(176.dp)
                .shadow(
                    elevation = 16.dp,
                    shape = CircleShape,
                    spotColor = Color(0x66FF3B5C)
                )
                .background(Color.White, CircleShape)
                .border(6.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            // Coral-Red Gradient Dome
            Box(
                modifier = Modifier
                    .size(146.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFF6584),
                                Color(0xFFFF3358),
                                Color(0xFFE51D44)
                            ),
                            center = Offset(73f, 50f)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = displayNumber,
                    fontSize = if (displayNumber.length > 2) 34.sp else 54.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    letterSpacing = (-1.5).sp
                )
            }
        }
    }
}

/**
 * Emergency Contact Avatar widget with circular solid background displaying
 * the first letter of their name in the center, white border, coral status dot,
 * and name below.
 */
@Composable
fun RadarContactAvatar(
    name: String,
    index: Int,
    modifier: Modifier = Modifier
) {
    val initial = name.trim().firstOrNull()?.uppercase() ?: "?"

    // Curated solid palette: Royal Blue, Violet, Jade, Terracotta
    val avatarColors = listOf(
        Color(0xFF2563EB), // Royal Blue
        Color(0xFF7C3AED), // Violet
        Color(0xFF0D9488), // Jade Teal
        Color(0xFFEA580C)  // Terracotta Orange
    )
    val bgColor = avatarColors[index % avatarColors.size]

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            Surface(
                modifier = Modifier
                    .size(52.dp)
                    .shadow(elevation = 6.dp, shape = CircleShape, spotColor = Color(0x330F172A)),
                shape = CircleShape,
                color = bgColor,
                border = BorderStroke(2.5.dp, Color.White)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initial,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Small coral status dot with white border
            Box(
                modifier = Modifier
                    .offset(x = 1.dp, y = 1.dp)
                    .size(13.dp)
                    .background(Color(0xFFFF3B5C), CircleShape)
                    .border(2.dp, Color.White, CircleShape)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = name,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0F172A),
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.widthIn(max = 84.dp)
        )
    }
}

/**
 * Collapsible Live Camera Card with subtle pink border,
 * live camera preview feed, and flip camera toggle.
 */
@Composable
fun LiveCameraDropdownCard(
    isFrontCamera: Boolean,
    onSwitchCamera: () -> Unit,
    onFrameCaptured: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "chevronRotation"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 6.dp, shape = RoundedCornerShape(22.dp), spotColor = Color(0x1F0F172A)),
        shape = RoundedCornerShape(22.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFFCE7EC))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(Color(0xFFFFEEF1), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = "Camera",
                            tint = Color(0xFFFF3B5C),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "View live camera",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color(0xFF0F172A)
                    )
                }

                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = Color(0xFF0F172A),
                    modifier = Modifier
                        .size(24.dp)
                        .rotate(chevronRotation)
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
                exit = shrinkVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 14.dp, end = 14.dp, bottom = 14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(190.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        CameraLivePreviewView(
                            isFrontCamera = isFrontCamera,
                            onFrameCaptured = onFrameCaptured
                        )

                        // Top-left badge: Camera active
                        Row(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(10.dp)
                                .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(Color(0xFFFF3B5C), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Camera active",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        // Top-right button: Flip camera
                        Surface(
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.65f),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(10.dp)
                                .clip(CircleShape)
                                .clickable(onClick = onSwitchCamera)
                        ) {
                            Box(
                                modifier = Modifier.padding(7.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FlipCameraAndroid,
                                    contentDescription = "Flip camera",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Large green rounded pill button for "I'm safe" anchored near bottom.
 * Features centered check-shield icon and vertically balanced typography.
 */
@Composable
fun ImSafeButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(32.dp),
                spotColor = Color(0x6600C882)
            ),
        shape = RoundedCornerShape(32.dp),
        color = Color(0xFF00C882)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color(0xFF00D289),
                            Color(0xFF00C882),
                            Color(0xFF00B374)
                        )
                    )
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    // White shield with green checkmark inside
                    Box(
                        modifier = Modifier.size(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color(0xFF00C882),
                            modifier = Modifier
                                .size(11.dp)
                                .offset(y = 0.5.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "I'm safe",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = Color.White,
                        letterSpacing = (-0.2).sp
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "Only tap when you're safe.",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.92f),
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Real device CameraX Live Preview integrated with Compose.
 */
@Composable
fun CameraLivePreviewView(
    isFrontCamera: Boolean,
    onFrameCaptured: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    var lastCapturedTimestamp by remember { mutableLongStateOf(0L) }

    DisposableEffect(Unit) {
        onDispose {
            try {
                cameraExecutor.shutdown()
            } catch (_: Exception) {}
        }
    }

    val hasCameraPermission = ContextCompat.checkSelfPermission(
        context,
        android.Manifest.permission.CAMERA
    ) == android.content.pm.PackageManager.PERMISSION_GRANTED

    if (!hasCameraPermission) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Camera preview active once permission granted",
                color = TextSecondary,
                fontSize = 12.sp
            )
        }
        return
    }

    AndroidView(
        factory = { ctx ->
            PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            }
        },
        modifier = Modifier.fillMaxSize(),
        update = { previewView ->
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }

                    val requestedSelector = if (isFrontCamera) {
                        CameraSelector.DEFAULT_FRONT_CAMERA
                    } else {
                        CameraSelector.DEFAULT_BACK_CAMERA
                    }

                    val cameraSelector = if (cameraProvider.hasCamera(requestedSelector)) {
                        requestedSelector
                    } else if (cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) {
                        CameraSelector.DEFAULT_BACK_CAMERA
                    } else if (cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
                        CameraSelector.DEFAULT_FRONT_CAMERA
                    } else {
                        null
                    }

                    if (cameraSelector == null) {
                        Log.w("CameraPreview", "No available camera found on this device")
                        return@addListener
                    }

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                        .build()

                    imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                        val now = System.currentTimeMillis()
                        if (now - lastCapturedTimestamp >= 280L) {
                            lastCapturedTimestamp = now
                            try {
                                val bitmap = imageProxy.toBitmap()
                                val rotation = imageProxy.imageInfo.rotationDegrees
                                val matrix = Matrix().apply {
                                    if (rotation != 0) postRotate(rotation.toFloat())
                                    if (isFrontCamera) postScale(-1f, 1f, bitmap.width / 2f, bitmap.height / 2f)
                                }
                                val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                                val scaled = Bitmap.createScaledBitmap(rotated, 480, 360, true)
                                val baos = ByteArrayOutputStream()
                                scaled.compress(Bitmap.CompressFormat.JPEG, 70, baos)
                                val base64 = "data:image/jpeg;base64," + Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
                                onFrameCaptured(base64)
                            } catch (t: Throwable) {
                                Log.w("CameraPreview", "Frame encode error handled", t)
                            }
                        }
                        try {
                            imageProxy.close()
                        } catch (_: Exception) {}
                    }

                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis
                    )
                } catch (t: Throwable) {
                    Log.e("CameraPreview", "Camera preview bind error handled", t)
                }
            }, ContextCompat.getMainExecutor(context))
        }
    )
}
