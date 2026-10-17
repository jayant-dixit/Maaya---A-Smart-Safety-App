package org.maaya.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.maaya.app.ui.theme.*

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
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(240.dp)
            .scale(if (isCountdown) 1.05f else pulseScale)
    ) {
        // Outer glow ripple
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(CrimsonRed.copy(alpha = 0.45f), Color.Transparent)
                    )
                )
        )

        // Middle ring
        Box(
            modifier = Modifier
                .size(195.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(CrimsonRed, DarkCrimson)
                    )
                )
        )

        // Core Button
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(170.dp)
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
                        fontSize = 54.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = "TAP TO CANCEL",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "SOS",
                        tint = Color.White,
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "SOS",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "PRESS FOR HELP",
                        fontSize = 10.sp,
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
        colors = CardDefaults.cardColors(containerColor = CardSurfaceDark),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(BorderSubtle, CardSurfaceElevated)))
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
                        fontWeight = FontWeight.Bold
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
            modifier = Modifier.size(26.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = value,
            fontSize = 18.sp,
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
        color = if (isActive) SafeGreen.copy(alpha = 0.15f) else CardSurfaceElevated,
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(
            listOf(if (isActive) SafeGreen.copy(alpha = 0.4f) else BorderSubtle, BorderSubtle)
        ))
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
