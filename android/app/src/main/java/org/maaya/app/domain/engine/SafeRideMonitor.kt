package org.maaya.app.domain.engine

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.maaya.app.MainActivity
import org.maaya.app.MaayaApplication
import org.maaya.app.R
import org.maaya.app.data.repository.SafeRideRepository
import org.maaya.app.domain.model.SafeRideSession
import org.maaya.app.domain.model.SosTriggerType

data class SafeRideProgress(
    val activeRide: SafeRideSession?,
    val secondsUntilCheckIn: Int,
    val secondsUntilEta: Int,
    val isCheckInDue: Boolean,
    val isEtaExceeded: Boolean
)

class SafeRideMonitor(
    private val context: Context,
    private val safeRideRepository: SafeRideRepository,
    private val emergencySosEngine: EmergencySosEngine,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    private var monitorJob: Job? = null
    private var hasAlertedForCurrentCheckIn = false
    private var hasAlertedForEtaExceeded = false

    private val _progress = MutableStateFlow(
        SafeRideProgress(
            activeRide = null,
            secondsUntilCheckIn = 0,
            secondsUntilEta = 0,
            isCheckInDue = false,
            isEtaExceeded = false
        )
    )
    val progress: StateFlow<SafeRideProgress> = _progress.asStateFlow()

    fun start() {
        if (monitorJob?.isActive == true) return
        monitorJob = scope.launch {
            while (isActive) {
                try {
                    evaluateActiveRide()
                } catch (e: Exception) {
                    Log.e("SafeRideMonitor", "Error evaluating safe ride", e)
                }
                delay(1000L) // evaluate every second for smooth UI countdowns
            }
        }
    }

    fun stop() {
        monitorJob?.cancel()
        monitorJob = null
    }

    private suspend fun evaluateActiveRide() {
        val ride = safeRideRepository.getActiveRide()
        if (ride == null || ride.isCompleted) {
            _progress.value = SafeRideProgress(
                activeRide = null,
                secondsUntilCheckIn = 0,
                secondsUntilEta = 0,
                isCheckInDue = false,
                isEtaExceeded = false
            )
            hasAlertedForCurrentCheckIn = false
            hasAlertedForEtaExceeded = false
            return
        }

        val now = System.currentTimeMillis()
        val checkInIntervalMs = ride.checkInIntervalMinutes * 60 * 1000L
        val nextCheckInMs = ride.lastCheckInAt + checkInIntervalMs
        val remainingCheckInMs = nextCheckInMs - now
        val remainingCheckInSecs = (remainingCheckInMs / 1000).toInt().coerceAtLeast(0)

        val totalTripDurationMs = ride.expectedArrivalMinutes * 60 * 1000L
        val etaMs = ride.startedAt + totalTripDurationMs
        val remainingEtaMs = etaMs - now
        val remainingEtaSecs = (remainingEtaMs / 1000).toInt()

        val isCheckInDue = remainingCheckInMs <= 0
        val isEtaExceeded = remainingEtaMs < 0

        _progress.value = SafeRideProgress(
            activeRide = ride,
            secondsUntilCheckIn = remainingCheckInSecs,
            secondsUntilEta = remainingEtaSecs.coerceAtLeast(0),
            isCheckInDue = isCheckInDue,
            isEtaExceeded = isEtaExceeded
        )

        // 1. Check-in prompt & overdue escalation
        if (isCheckInDue) {
            val overdueMs = now - nextCheckInMs
            if (!hasAlertedForCurrentCheckIn) {
                hasAlertedForCurrentCheckIn = true
                showCheckInNotification(ride)
            }

            // If check-in is overdue by 90 seconds (1.5 minutes) without user confirmation -> AUTO ESCALATE
            if (overdueMs >= 90_000L && !ride.isEscalatedToSos) {
                Log.w("SafeRideMonitor", "Safe Ride check-in overdue by 90s! Escalating to emergency SOS...")
                emergencySosEngine.triggerEmergency(SosTriggerType.SAFE_RIDE_OVERDUE, bypassCountdown = false)
            }
        } else {
            hasAlertedForCurrentCheckIn = false
        }

        // 2. ETA exceeded escalation
        if (isEtaExceeded) {
            val etaOverdueMs = now - etaMs
            if (!hasAlertedForEtaExceeded) {
                hasAlertedForEtaExceeded = true
                showEtaExceededNotification(ride)
            }

            // If ETA is exceeded by 3 minutes and ride is not completed -> AUTO ESCALATE
            if (etaOverdueMs >= 180_000L && !ride.isEscalatedToSos) {
                Log.w("SafeRideMonitor", "Safe Ride ETA exceeded by 3m! Escalating to emergency SOS...")
                emergencySosEngine.triggerEmergency(SosTriggerType.SAFE_RIDE_OVERDUE, bypassCountdown = false)
            }
        }
    }

    private fun showCheckInNotification(ride: SafeRideSession) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            101,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, MaayaApplication.CHANNEL_SAFERIDE)
            .setContentTitle("Safe Ride Check-In Due")
            .setContentText("Destination: ${ride.destinationName}. Tap to confirm you are safe.")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.notify(NOTIFICATION_CHECKIN_ID, notification)
    }

    private fun showEtaExceededNotification(ride: SafeRideSession) {
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            102,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, MaayaApplication.CHANNEL_SAFERIDE)
            .setContentTitle("Safe Ride ETA Exceeded")
            .setContentText("Expected arrival for ${ride.destinationName} has passed. Please confirm your status.")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.notify(NOTIFICATION_ETA_ID, notification)
    }

    companion object {
        const val NOTIFICATION_CHECKIN_ID = 9101
        const val NOTIFICATION_ETA_ID = 9102
    }
}
