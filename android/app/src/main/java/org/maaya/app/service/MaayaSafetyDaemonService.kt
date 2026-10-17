package org.maaya.app.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import org.maaya.app.MainActivity
import org.maaya.app.MaayaApplication
import org.maaya.app.R

class MaayaSafetyDaemonService : Service() {

    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    @SuppressLint("WakelockTimeout")
    override fun onCreate() {
        super.onCreate()
        Log.i("SafetyDaemon", "MaayaSafetyDaemonService created.")

        // Acquire partial wake lock to allow shake detection and BLE monitoring with screen off
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = powerManager?.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "Maaya:SafetyDaemonWakeLock"
            )?.apply {
                acquire()
            }
        } catch (e: Exception) {
            Log.w("SafetyDaemon", "Could not acquire partial wakelock", e)
        }

        val container = (application as MaayaApplication).container

        // 1. Shake detection 24/7
        if (container.securePreferences.isShakeDetectionEnabled) {
            container.shakeDetector.start()
        }

        // 2. Battery monitoring 24/7
        if (container.securePreferences.isLowBatteryEscalationEnabled) {
            container.batteryMonitor.start()
        }

        // 3. Safe ride background escalation monitor 24/7
        container.safeRideMonitor.start()

        // 4. Automatic periodic heart rate scheduler 24/7
        container.heartRateAutoScheduler.start()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_STOP_DAEMON) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        val notification = buildDaemonNotification()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                var serviceTypes = ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    val hasBleConnect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.BLUETOOTH_CONNECT
                        ) == PackageManager.PERMISSION_GRANTED
                    } else {
                        true
                    }
                    if (hasBleConnect) {
                        serviceTypes = serviceTypes or ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
                    }
                }
                startForeground(NOTIFICATION_ID, notification, serviceTypes)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: SecurityException) {
            Log.e("SafetyDaemon", "SecurityException starting foreground service with types, falling back to DATA_SYNC", e)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
            } catch (e2: Exception) {
                Log.e("SafetyDaemon", "Failed to start foreground service", e2)
                stopSelf()
                return START_NOT_STICKY
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i("SafetyDaemon", "MaayaSafetyDaemonService destroyed, releasing resources.")
        try {
            wakeLock?.release()
        } catch (_: Exception) {}

        val container = (application as MaayaApplication).container
        container.shakeDetector.stop()
        container.batteryMonitor.stop()
        container.safeRideMonitor.stop()
        container.heartRateAutoScheduler.stop()
    }

    private fun buildDaemonNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingOpen = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, MaayaApplication.CHANNEL_SMARTWATCH)
            .setContentTitle("Maaya Shield Active")
            .setContentText("Background safety protection, shake detection & vitals active")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingOpen)
            .build()
    }

    companion object {
        const val NOTIFICATION_ID = 8801
        const val ACTION_START_DAEMON = "org.maaya.app.action.START_DAEMON"
        const val ACTION_STOP_DAEMON = "org.maaya.app.action.STOP_DAEMON"

        fun start(context: Context) {
            val intent = Intent(context, MaayaSafetyDaemonService::class.java).apply {
                action = ACTION_START_DAEMON
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, MaayaSafetyDaemonService::class.java).apply {
                action = ACTION_STOP_DAEMON
            }
            context.startService(intent)
        }
    }
}
