package org.maaya.app.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.maaya.app.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.roundToInt
import kotlin.math.PI

/**
 * Radial Tick Gauge with pulsing center icon (inspired by the hero circle in the UI reference)
 */
@Composable
fun RadialTickGauge(
    modifier: Modifier = Modifier,
    scorePercentage: Int = 98,
    centerIcon: ImageVector = Icons.Default.Favorite,
    centerIconTint: Color = CrimsonRed
) {
    val infiniteTransition = rememberInfiniteTransition(label = "radialPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = modifier.size(110.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val outerRadius = size.minDimension / 2f - 4f
            val innerRadius = outerRadius - 14f
            val totalTicks = 36
            val activeTicks = ((scorePercentage / 100f) * totalTicks).toInt()

            for (i in 0 until totalTicks) {
                val angle = Math.toRadians((i * (360.0 / totalTicks) - 90.0))
                val cosA = cos(angle).toFloat()
                val sinA = sin(angle).toFloat()

                val p1 = Offset(center.x + innerRadius * cosA, center.y + innerRadius * sinA)
                val p2 = Offset(center.x + outerRadius * cosA, center.y + outerRadius * sinA)

                val tickColor = if (i <= activeTicks) {
                    PrimarySkyBlue
                } else {
                    PrimarySkyBlue.copy(alpha = 0.22f)
                }

                drawLine(
                    color = tickColor,
                    start = p1,
                    end = p2,
                    strokeWidth = 2.5f,
                    cap = StrokeCap.Round
                )
            }
        }

        // Inner glowing pulsing circle with icon
        Box(
            modifier = Modifier
                .size(54.dp)
                .scale(pulseScale)
                .shadow(elevation = 6.dp, shape = CircleShape, spotColor = centerIconTint.copy(alpha = 0.35f))
                .clip(CircleShape)
                .background(SurfaceWhite),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = centerIcon,
                contentDescription = null,
                tint = centerIconTint,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}

/**
 * Animated Smooth Cardiac Waveform Sparkline (Heart Rate)
 * Animates rhythm pulse dynamically in response to BPM value
 */
@Composable
fun WaveSparkline(
    modifier: Modifier = Modifier,
    lineColor: Color = CrimsonRed,
    fillColor: Color = CrimsonRed.copy(alpha = 0.18f),
    bpm: Int = 60
) {
    val effectiveBpm = bpm.coerceIn(45, 180)
    val cycleDurationMs = (60000 / effectiveBpm).coerceIn(450, 1300)

    val infiniteTransition = rememberInfiniteTransition(label = "cardiacPulse")
    val pulseAmp by infiniteTransition.animateFloat(
        initialValue = 0.90f,
        targetValue = 1.10f,
        animationSpec = infiniteRepeatable(
            animation = tween(cycleDurationMs / 2, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAmp"
    )
    val dotPulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(cycleDurationMs / 2, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotPulse"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val strokePath = Path()
        val numPoints = 50
        val stepX = w / (numPoints - 1)

        val peakX = w * 0.84f
        var peakY = h * 0.28f

        for (i in 0 until numPoints) {
            val x = i * stepX
            val t = (i.toFloat() / (numPoints - 1)).coerceIn(0f, 1f)

            // Smooth undulating wave matching reference:
            // Starts around 0.70h, slight gentle dip at 0.25t, rises up to peak at 0.84t, slopes down to 0.46h
            val baseCurve = when {
                t < 0.30f -> {
                    0.70f + 0.04f * sin((t / 0.30f) * (PI.toFloat() / 2f))
                }
                t < 0.84f -> {
                    val progress = (t - 0.30f) / (0.84f - 0.30f)
                    val smoothRise = (1f - cos(progress * PI.toFloat())) / 2f
                    0.74f - (0.74f - 0.28f) * smoothRise
                }
                else -> {
                    val progress = (t - 0.84f) / (1f - 0.84f)
                    val smoothFall = (1f - cos(progress * (PI.toFloat() / 2f)))
                    0.28f + (0.46f - 0.28f) * smoothFall
                }
            }

            // Apply subtle pulse rhythm around baseline
            val baseline = 0.58f
            val dynamicOffset = (baseCurve - baseline) * pulseAmp
            val y = ((baseline + dynamicOffset) * h).coerceIn(3f, h - 3f)

            if (t >= 0.835f && t <= 0.845f) {
                peakY = y
            }

            if (i == 0) strokePath.moveTo(x, y) else strokePath.lineTo(x, y)
        }

        val fillPath = Path().apply {
            addPath(strokePath)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }

        // Shaded gradient fill underneath
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(fillColor, Color.Transparent),
                startY = peakY - 4f,
                endY = h
            )
        )

        // Wave curve line
        drawPath(
            path = strokePath,
            color = lineColor,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Peak indicator circle with cardiac glow positioned directly at peak
        drawCircle(
            color = lineColor.copy(alpha = 0.30f),
            radius = 6.5.dp.toPx() * dotPulseScale,
            center = Offset(peakX, peakY)
        )
        drawCircle(
            color = SurfaceWhite,
            radius = 4.5.dp.toPx(),
            center = Offset(peakX, peakY)
        )
        drawCircle(
            color = lineColor,
            radius = 3.dp.toPx(),
            center = Offset(peakX, peakY)
        )
    }
}

/**
 * Animated Circular Water Wave Liquid Gauge (Blood SpO2)
 * Sloshing liquid with animated continuous wave motion that rises and falls with SpO2 percentage
 */
@Composable
fun LiquidWaveCircle(
    modifier: Modifier = Modifier,
    spO2Percentage: Float = 98f,
    waveColor: Color = PrimarySkyBlue,
    backgroundColor: Color = SkyBlueLight
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waterWave")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavePhase"
    )

    val animatedPercentage by animateFloatAsState(
        targetValue = spO2Percentage.coerceIn(50f, 100f),
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "spO2Level"
    )

    Canvas(modifier = modifier) {
        val r = size.minDimension / 2f
        val center = Offset(size.width / 2f, size.height / 2f)

        val circlePath = Path().apply {
            addOval(androidx.compose.ui.geometry.Rect(center.x - r, center.y - r, center.x + r, center.y + r))
        }

        // Background Circle
        drawCircle(
            color = backgroundColor,
            radius = r,
            center = center
        )

        // Scaled to undulate gracefully across the middle of the circle matching the mockup
        val fillFraction = (0.38f + (animatedPercentage / 100f) * 0.20f).coerceIn(0.35f, 0.65f)
        val waterLevelY = (center.y + r) - (2 * r * fillFraction)

        clipPath(circlePath) {
            // Secondary back translucent wave
            val backWavePath = Path().apply {
                moveTo(center.x - r, center.y + r)
                lineTo(center.x - r, waterLevelY)
                val wavePoints = 28
                val stepX = (2 * r) / wavePoints
                for (i in 0..wavePoints) {
                    val x = center.x - r + i * stepX
                    val waveY = waterLevelY + 4f * sin((i.toFloat() / wavePoints * 2 * PI.toFloat()) + wavePhase + 1.2f)
                    lineTo(x, waveY)
                }
                lineTo(center.x + r, center.y + r)
                close()
            }
            drawPath(
                path = backWavePath,
                color = waveColor.copy(alpha = 0.35f)
            )

            // Primary front crisp wave
            val frontWavePath = Path().apply {
                moveTo(center.x - r, center.y + r)
                lineTo(center.x - r, waterLevelY)
                val wavePoints = 28
                val stepX = (2 * r) / wavePoints
                for (i in 0..wavePoints) {
                    val x = center.x - r + i * stepX
                    val waveY = waterLevelY + 5.5f * sin((i.toFloat() / wavePoints * 2 * PI.toFloat()) - wavePhase)
                    lineTo(x, waveY)
                }
                lineTo(center.x + r, center.y + r)
                close()
            }
            drawPath(
                path = frontWavePath,
                brush = Brush.verticalGradient(
                    colors = listOf(waveColor.copy(alpha = 0.85f), waveColor),
                    startY = waterLevelY - 10f,
                    endY = center.y + r
                )
            )
        }
    }
}

/**
 * Animated Radial Arc Gauge with progress dot (Stress Level)
 * Arc sweep angle animates smoothly as stress level changes
 */
@Composable
fun ArcGauge(
    modifier: Modifier = Modifier,
    fraction: Float = 0.22f,
    activeColor: Color = SafeGreen,
    trackColor: Color = Color(0xFFE2E8F0)
) {
    val animatedFraction by animateFloatAsState(
        targetValue = fraction.coerceIn(0.05f, 1f),
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "stressArc"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "arcGlow")
    val dotPulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.30f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotPulse"
    )

    Canvas(modifier = modifier) {
        val strokeWidth = 7.dp.toPx()
        val arcSize = Size(size.width - strokeWidth, (size.height * 2) - strokeWidth)
        val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)

        // Background Track Arc
        drawArc(
            color = trackColor,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        // Active Arc
        val sweep = (180f * animatedFraction).coerceIn(0f, 180f)
        drawArc(
            brush = Brush.horizontalGradient(
                colors = listOf(activeColor.copy(alpha = 0.75f), activeColor)
            ),
            startAngle = 180f,
            sweepAngle = sweep,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        // Indicator dot at the tip of the sweep
        val angleRad = Math.toRadians((180.0 + sweep))
        val rx = arcSize.width / 2f
        val ry = arcSize.height / 2f
        val cx = topLeft.x + rx
        val cy = topLeft.y + ry

        val dotX = (cx + rx * cos(angleRad)).toFloat()
        val dotY = (cy + ry * sin(angleRad)).toFloat()

        drawCircle(
            color = activeColor.copy(alpha = 0.30f),
            radius = 6.dp.toPx() * dotPulse,
            center = Offset(dotX, dotY)
        )
        drawCircle(
            color = SurfaceWhite,
            radius = 5.dp.toPx(),
            center = Offset(dotX, dotY)
        )
        drawCircle(
            color = activeColor,
            radius = 3.5.dp.toPx(),
            center = Offset(dotX, dotY)
        )
    }
}

/**
 * Animated Vertical Bar Level Gauge (Mobile Battery)
 * 5 bars whose active count and illumination respond to realtime phone battery level
 */
@Composable
fun VerticalBarGauge(
    modifier: Modifier = Modifier,
    levelPercentage: Int = 47,
    isCharging: Boolean = false,
    barColor: Color = AccentPurple,
    inactiveColor: Color = Color(0xFFEDE9FE)
) {
    val barCount = 5
    val activeCount = ((levelPercentage / 100f) * barCount).roundToInt().coerceIn(1, barCount)
    val heights = listOf(14.dp, 20.dp, 26.dp, 33.dp, 40.dp)

    val infiniteTransition = rememberInfiniteTransition(label = "batteryWave")
    val waveIndex by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isCharging) 1200 else 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "waveIndex"
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        heights.forEachIndexed { index, barHeight ->
            val isActive = index < activeCount
            val isWaveHighlight = isCharging && (index == (waveIndex.toInt() % barCount))

            Box(
                modifier = Modifier
                    .width(7.dp)
                    .height(barHeight)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        if (isActive) {
                            Brush.verticalGradient(
                                if (isWaveHighlight) {
                                    listOf(Color(0xFFC084FC), Color(0xFFA855F7))
                                } else {
                                    listOf(barColor.copy(alpha = 0.7f), barColor)
                                }
                            )
                        } else {
                            SolidColor(inactiveColor)
                        }
                    )
            )
        }
    }
}

/**
 * Emergency Quick Dial Card for Police, Ambulance & Fire
 * Initiates direct dialing via Intent.ACTION_DIAL
 */
@Composable
fun EmergencyHelplinesCard(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(BorderSubtle))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(EmergencyRedSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhoneInTalk,
                            contentDescription = null,
                            tint = CrimsonRed,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Emergency Helplines",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SkyBlueLight
                ) {
                    Text(
                        text = "1-Tap Dial",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimarySkyBlueDark,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Police (112)
                HelplineDialButton(
                    modifier = Modifier.weight(1f),
                    title = "Police",
                    number = "100",
                    icon = Icons.Default.LocalPolice,
                    accentColor = PoliceBlue,
                    backgroundColor = PoliceBlueSoft,
                    onClick = {
                        val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:100"))
                        context.startActivity(dialIntent)
                    }
                )

                // Ambulance (108)
                HelplineDialButton(
                    modifier = Modifier.weight(1f),
                    title = "Ambulance",
                    number = "108",
                    icon = Icons.Default.LocalHospital,
                    accentColor = AmbulanceRed,
                    backgroundColor = AmbulanceRedSoft,
                    onClick = {
                        val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:108"))
                        context.startActivity(dialIntent)
                    }
                )

                // Fire Force (101)
                HelplineDialButton(
                    modifier = Modifier.weight(1f),
                    title = "Fire",
                    number = "101",
                    icon = Icons.Default.LocalFireDepartment,
                    accentColor = FireOrange,
                    backgroundColor = FireOrangeSoft,
                    onClick = {
                        val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:101"))
                        context.startActivity(dialIntent)
                    }
                )
            }
        }
    }
}

/**
 * Emergency shortcuts matching Section C:
 * - Single white card container with three horizontal pills: Police, Ambulance, Fire
 * - Each contains only an icon badge and a text label
 * - Subtle blue, pink, and orange accents distinguish the services
 * - Direct 1-tap dialing, no phone numbers or clutter
 */
@Composable
fun EmergencyShortcutsRow(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(BorderSubtle))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Police: soft blue background, blue circle with white star/shield, bold "Police"
            EmergencyShortcutPill(
                modifier = Modifier.weight(1f),
                label = "Police",
                icon = Icons.Default.Shield,
                iconTint = Color.White,
                iconBadgeColor = Color(0xFF2563EB),
                backgroundColor = Color(0xFFF0F6FF),
                borderColor = Color(0xFFDBEAFE),
                onClick = {
                    val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:112"))
                    context.startActivity(dialIntent)
                }
            )

            // Ambulance: soft pink background, red square with white cross, bold "Ambulance"
            EmergencyShortcutPill(
                modifier = Modifier.weight(1f),
                label = "Ambulance",
                icon = Icons.Default.Add,
                iconTint = Color.White,
                iconBadgeColor = Color(0xFFEF4444),
                backgroundColor = Color(0xFFFEF2F2),
                borderColor = Color(0xFFFEE2E2),
                isSquareBadge = true,
                onClick = {
                    val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:108"))
                    context.startActivity(dialIntent)
                }
            )

            // Fire: soft orange background, orange flame icon, bold "Fire"
            EmergencyShortcutPill(
                modifier = Modifier.weight(1f),
                label = "Fire",
                icon = Icons.Default.LocalFireDepartment,
                iconTint = Color(0xFFEA580C),
                iconBadgeColor = SurfaceWhite,
                backgroundColor = Color(0xFFFFF7ED),
                borderColor = Color(0xFFFFEDD5),
                onClick = {
                    val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:101"))
                    context.startActivity(dialIntent)
                }
            )
        }
    }
}

@Composable
fun EmergencyShortcutPill(
    label: String,
    icon: ImageVector,
    iconTint: Color,
    iconBadgeColor: Color,
    backgroundColor: Color,
    borderColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSquareBadge: Boolean = false
) {
    Surface(
        modifier = modifier
            .height(50.dp)
            .clip(RoundedCornerShape(13.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(13.dp),
        color = backgroundColor,
        border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(borderColor))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(if (isSquareBadge) RoundedCornerShape(7.dp) else CircleShape)
                    .background(iconBadgeColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = iconTint,
                    modifier = Modifier.size(14.dp)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }
    }
}

@Composable
fun ShortcutCardItem(
    label: String,
    icon: ImageVector,
    accentColor: Color,
    backgroundColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    EmergencyShortcutPill(
        label = label,
        icon = icon,
        iconTint = Color.White,
        iconBadgeColor = accentColor,
        backgroundColor = backgroundColor,
        borderColor = accentColor.copy(alpha = 0.22f),
        onClick = onClick,
        modifier = modifier
    )
}

@Composable
fun HelplineDialButton(
    modifier: Modifier = Modifier,
    title: String,
    number: String,
    icon: ImageVector,
    accentColor: Color,
    backgroundColor: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor,
        border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(accentColor.copy(alpha = 0.22f)))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(SurfaceWhite),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.height(7.dp))
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = number,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = accentColor
            )
        }
    }
}

/**
 * Floating Watch Status Badge on Header (matches the top right circular badge with status dot)
 */
@Composable
fun WatchStatusHeaderBadge(
    isConnected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .shadow(elevation = 3.dp, shape = CircleShape, spotColor = CardShadowLight)
            .clip(CircleShape)
            .background(SurfaceWhite)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Watch,
            contentDescription = if (isConnected) "Watch Connected" else "Watch Disconnected",
            tint = if (isConnected) PrimarySkyBlue else Color(0xFF334155),
            modifier = Modifier.size(22.dp)
        )

        // Status indicator dot (Top-Right)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 7.dp, end = 7.dp)
                .size(8.dp)
                .clip(CircleShape)
                .background(if (isConnected) SafeGreen else WarningAmber)
                .border(1.5.dp, SurfaceWhite, CircleShape)
        )
    }
}

/**
 * Compact Toast-Style Pill on right of Subheader
 * e.g. [ (watch icon) Watch not connected > ]
 */
@Composable
fun WatchToastPill(
    isConnected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = if (isConnected) Color(0xFFECFDF5) else Color(0xFFFFFBEB),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = SolidColor(if (isConnected) Color(0xFFA7F3D0) else Color(0xFFFED7AA))
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Watch,
                contentDescription = null,
                tint = if (isConnected) SafeGreen else Color(0xFFEA580C),
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = if (isConnected) "Watch connected" else "Watch not connected",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.width(3.dp))
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = if (isConnected) SafeGreen else Color(0xFFEA580C),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
fun WatchToastNotification(
    isConnected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    WatchToastPill(isConnected = isConnected, onClick = onClick, modifier = modifier)
}

/**
 * Central SOS control matching reference design:
 * - Large 3D red circular dome button with white shield, red "!", and bold "SOS"
 * - Concentric radar tick ring and thin pink circles
 * - Left and right curved bracket arcs
 * - Soft pastel pink glow with faint floating particles
 * - Text underneath: "Tap to activate sos"
 */
@Composable
fun CentralSosControl(
    modifier: Modifier = Modifier,
    isCountdown: Boolean = false,
    countdownRemaining: Int = 0,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "sosHeroAnimation")

    val wave1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave1"
    )
    val wave2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, delayMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave2"
    )

    val buttonPulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "buttonPulse"
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(260.dp),
            contentAlignment = Alignment.Center
        ) {
            // 1. Soft pastel background glow
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                Color(0xFFFFD1DC).copy(alpha = 0.60f),
                                Color(0xFFFCE7F3).copy(alpha = 0.35f),
                                Color(0xFFF0F9FF).copy(alpha = 0.15f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // 2. Concentric Radar & Compass Tick Rings
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)

                // Faint pink floating bokeh particles
                val bokehDots = listOf(
                    Offset(center.x - 72f, center.y - 88f) to 3.5f,
                    Offset(center.x + 82f, center.y - 78f) to 4.5f,
                    Offset(center.x + 92f, center.y + 64f) to 3f,
                    Offset(center.x - 88f, center.y + 68f) to 4f,
                    Offset(center.x + 108f, center.y - 12f) to 3f,
                    Offset(center.x - 102f, center.y - 8f) to 3.5f
                )
                bokehDots.forEach { (pos, r) ->
                    drawCircle(
                        color = Color(0xFFFFB3BA).copy(alpha = 0.5f),
                        radius = r.dp.toPx(),
                        center = pos
                    )
                }

                // Expanding ripple waves
                listOf(wave1, wave2).forEach { progress ->
                    val r = 66.dp.toPx() + (125.dp.toPx() - 66.dp.toPx()) * progress
                    val alpha = ((1f - progress) * 0.35f).coerceIn(0f, 1f)
                    drawCircle(
                        color = CrimsonRed.copy(alpha = alpha),
                        radius = r,
                        center = center,
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }

                // Outer Compass / Dashed Tick Ring
                val tickRadius = 108.dp.toPx()
                val totalTicks = 72
                for (i in 0 until totalTicks) {
                    val angle = Math.toRadians((i * (360.0 / totalTicks) - 90.0))
                    val cosA = cos(angle).toFloat()
                    val sinA = sin(angle).toFloat()
                    val isMajor = i % 6 == 0
                    val tickLen = if (isMajor) 5.dp.toPx() else 2.5.dp.toPx()
                    val p1 = Offset(center.x + (tickRadius - tickLen) * cosA, center.y + (tickRadius - tickLen) * sinA)
                    val p2 = Offset(center.x + tickRadius * cosA, center.y + tickRadius * sinA)
                    val tickColor = if (isMajor) Color(0xFFFDA4AF).copy(alpha = 0.75f) else Color(0xFFCBD5E1).copy(alpha = 0.5f)
                    drawLine(
                        color = tickColor,
                        start = p1,
                        end = p2,
                        strokeWidth = if (isMajor) 1.5.dp.toPx() else 1.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                // Outer thin pink ring
                drawCircle(
                    color = Color(0xFFFECDD3).copy(alpha = 0.65f),
                    radius = 96.dp.toPx(),
                    center = center,
                    style = Stroke(width = 1.2.dp.toPx())
                )

                // Middle thin pink ring
                drawCircle(
                    color = Color(0xFFFDE8E8).copy(alpha = 0.75f),
                    radius = 82.dp.toPx(),
                    center = center,
                    style = Stroke(width = 1.dp.toPx())
                )

                // Left & Right Curved Bracket Arcs (Tech Shield Accents)
                val arcRadius = 78.dp.toPx()
                val arcSize = Size(arcRadius * 2, arcRadius * 2)
                val arcTopLeft = Offset(center.x - arcRadius, center.y - arcRadius)

                // Left Bracket Arc (155° to 205°)
                drawArc(
                    color = Color(0xFFFDA4AF),
                    startAngle = 155f,
                    sweepAngle = 50f,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                )

                // Right Bracket Arc (-25° to 25°)
                drawArc(
                    color = Color(0xFFFDA4AF),
                    startAngle = -25f,
                    sweepAngle = 50f,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                )

                // Inner white halo ring right outside button
                drawCircle(
                    color = SurfaceWhite.copy(alpha = 0.9f),
                    radius = 65.dp.toPx(),
                    center = center,
                    style = Stroke(width = 2.5.dp.toPx())
                )
            }

            // 3. Central 3D Red Dome SOS Button
            Box(
                modifier = Modifier
                    .size(124.dp)
                    .scale(if (isCountdown) 1.05f else buttonPulse)
                    .shadow(
                        elevation = 16.dp,
                        shape = CircleShape,
                        spotColor = Color(0x66DC2626)
                    )
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFF4D6D),
                                Color(0xFFEF233C),
                                Color(0xFFD90429),
                                Color(0xFF9B0017)
                            ),
                            center = Offset(62f, 40f)
                        )
                    )
                    .clickable(onClick = onClick),
                contentAlignment = Alignment.Center
            ) {
                if (isCountdown) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "$countdownRemaining",
                            fontSize = 42.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Text(
                            text = "ABORT",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // White Shield with Red Exclamation Mark
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = "Shield",
                                tint = Color.White,
                                modifier = Modifier.size(44.dp)
                            )
                            Text(
                                text = "!",
                                color = Color(0xFFD90429),
                                fontWeight = FontWeight.Black,
                                fontSize = 21.sp,
                                modifier = Modifier.padding(bottom = 3.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "SOS",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            letterSpacing = 1.5.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Text underneath: "Tap to activate sos"
        Text(
            text = if (isCountdown) "Countdown: ${countdownRemaining}s • Tap to abort" else "Tap to activate sos",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isCountdown) CrimsonRed else Color(0xFF475569),
            letterSpacing = 0.2.sp
        )
    }
}

/**
 * Floating Footer Bottom Navigation Bar matching Section E:
 * - White pill-shaped container with rounded corners
 * - Four icons: Home, Cab Escort, Incident Log, Contacts
 * - No text labels
 * - A blue pill or circle highlights the selected tab
 * - Keeps enough bottom padding for Android system navigation
 */
@Composable
fun MaayaBottomNavBar(
    selectedRoute: String = "home",
    onHomeClick: () -> Unit,
    onSafeRideClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onContactsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp, vertical = 6.dp)
            .shadow(elevation = 14.dp, shape = RoundedCornerShape(36.dp), spotColor = Color(0x1F0F172A)),
        shape = RoundedCornerShape(36.dp),
        color = SurfaceWhite,
        border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(BorderSubtle))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tab 1: Home
            BottomNavIconTabItem(
                icon = Icons.Default.Shield,
                contentDescription = "Home",
                isSelected = selectedRoute == "home" || selectedRoute == "dashboard",
                onClick = onHomeClick
            )

            // Tab 2: Cab Escort
            BottomNavIconTabItem(
                icon = Icons.Default.DirectionsCar,
                contentDescription = "Cab Escort",
                isSelected = selectedRoute == "safe_ride" || selectedRoute == "cab_escort",
                onClick = onSafeRideClick
            )

            // Tab 3: Incident Log
            BottomNavIconTabItem(
                icon = Icons.Default.History,
                contentDescription = "Incident Log",
                isSelected = selectedRoute == "history" || selectedRoute == "incident_log",
                onClick = onHistoryClick
            )

            // Tab 4: Contacts
            BottomNavIconTabItem(
                icon = Icons.Default.Group,
                contentDescription = "Contacts",
                isSelected = selectedRoute == "contacts" || selectedRoute == "trusted_contacts",
                onClick = onContactsClick
            )
        }
    }
}

@Composable
fun BottomNavIconTabItem(
    icon: ImageVector,
    contentDescription: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    if (isSelected) {
        // Selected: Blue pill highlight with white icon (no text label)
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = PrimarySkyBlueBright,
            modifier = Modifier
                .clip(RoundedCornerShape(22.dp))
                .clickable(onClick = onClick)
        ) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    tint = SurfaceWhite,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    } else {
        // Unselected: Clean icon only, no text labels
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = TextSecondary,
                modifier = Modifier.size(23.dp)
            )
        }
    }
}

@Composable
fun BottomNavTabItem(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    BottomNavIconTabItem(
        icon = icon,
        contentDescription = label,
        isSelected = isSelected,
        onClick = onClick
    )
}

/**
 * Backward-compatible Legacy Components
 */
@Composable
fun SosEmergencyButton(
    modifier: Modifier = Modifier,
    isCountdown: Boolean = false,
    countdownRemaining: Int = 0,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(210.dp)
            .scale(if (isCountdown) 1.05f else pulseScale)
    ) {
        // Outer glow
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(CrimsonRed.copy(alpha = 0.25f), Color.Transparent)
                    )
                )
        )

        // Core Button
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(160.dp)
                .shadow(8.dp, CircleShape, spotColor = CrimsonRed.copy(alpha = 0.4f))
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        listOf(NeonRed, CrimsonRed)
                    )
                )
                .clickable(onClick = onClick)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (isCountdown) {
                    Text(
                        text = "$countdownRemaining",
                        fontSize = 46.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = "TAP TO CANCEL",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "SOS",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "SOS",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "PRESS FOR HELP",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }
        }
    }
}

@Composable
fun HealthVitalsCard(
    heartRateBpm: Int,
    sensorContact: Boolean,
    batteryPercentage: Int?,
    spO2Percentage: Float?,
    isWatchConnected: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(BorderSubtle))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Watch,
                        contentDescription = null,
                        tint = if (isWatchConnected) SafeGreen else TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Smartwatch Vitals",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                Text(
                    text = if (isWatchConnected) "Live BLE" else "Disconnected",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isWatchConnected) SafeGreen else TextMuted
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                VitalMetricItem(
                    label = "Heart Rate",
                    value = if (heartRateBpm > 0) "$heartRateBpm bpm" else "--",
                    icon = Icons.Default.Favorite,
                    iconTint = CrimsonRed
                )
                VitalMetricItem(
                    label = "SpO2 Oxygen",
                    value = if (spO2Percentage != null) "$spO2Percentage%" else "98%",
                    icon = Icons.Default.Air,
                    iconTint = AccentPurple
                )
                VitalMetricItem(
                    label = "Watch Battery",
                    value = if (batteryPercentage != null) "$batteryPercentage%" else "--",
                    icon = Icons.Default.BatteryChargingFull,
                    iconTint = SafeGreen
                )
            }
        }
    }
}

@Composable
fun VitalMetricItem(
    label: String,
    value: String,
    icon: ImageVector,
    iconTint: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = value,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = TextSecondary
        )
    }
}

@Composable
fun ConnectionStatusBadge(
    label: String,
    isActive: Boolean,
    activeIcon: ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = if (isActive) SafeGreenSoft else CardSurfaceElevated,
        border = CardDefaults.outlinedCardBorder().copy(
            brush = SolidColor(if (isActive) SafeGreen.copy(alpha = 0.35f) else BorderSubtle)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = activeIcon,
                contentDescription = null,
                tint = if (isActive) SafeGreen else TextMuted,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = if (isActive) TextPrimary else TextMuted
            )
        }
    }
}
