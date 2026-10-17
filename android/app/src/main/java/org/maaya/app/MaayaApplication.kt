package org.maaya.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import org.maaya.app.di.AppContainer
import org.maaya.app.di.DefaultAppContainer
import org.maaya.app.service.MaayaSafetyDaemonService

class MaayaApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
        createNotificationChannels()
        if (container.securePreferences.isOnboardingCompleted) {
            try {
                MaayaSafetyDaemonService.start(this)
            } catch (_: Exception) {}
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)

            val emergencyChannel = NotificationChannel(
                CHANNEL_EMERGENCY,
                "Emergency SOS Response",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Active emergency incident alerts and live transmission"
                enableVibration(true)
                setShowBadge(true)
            }

            val smartwatchChannel = NotificationChannel(
                CHANNEL_SMARTWATCH,
                "Smartwatch Health Monitor",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background BLE connection status and physiological health tracking"
                setShowBadge(false)
            }

            val safeRideChannel = NotificationChannel(
                CHANNEL_SAFERIDE,
                "Safe Ride Journey Escort",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Safe Ride check-ins and travel monitoring"
            }

            notificationManager.createNotificationChannels(
                listOf(emergencyChannel, smartwatchChannel, safeRideChannel)
            )
        }
    }

    companion object {
        const val CHANNEL_EMERGENCY = "channel_emergency_sos"
        const val CHANNEL_SMARTWATCH = "channel_smartwatch_ble"
        const val CHANNEL_SAFERIDE = "channel_safe_ride"
    }
}
