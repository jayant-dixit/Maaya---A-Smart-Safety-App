package org.maaya.app.service

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import org.maaya.app.MainActivity
import org.maaya.app.MaayaApplication
import org.maaya.app.R

class EmergencySosForegroundService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_STOP_SOS) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        val incidentId = intent?.getStringExtra(EXTRA_INCIDENT_ID) ?: "active"
        val notification = buildEmergencyNotification(incidentId)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            var serviceTypes = ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val hasLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                if (hasLocation) {
                    serviceTypes = serviceTypes or ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                }
            }

            try {
                startForeground(NOTIFICATION_ID, notification, serviceTypes)
            } catch (e: Exception) {
                Log.e("EmergencySosService", "Error starting foreground with specified types, falling back", e)
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
                    } else {
                        startForeground(NOTIFICATION_ID, notification)
                    }
                } catch (e2: Exception) {
                    Log.e("EmergencySosService", "Failed to start emergency foreground service", e2)
                }
            }
        } else {
            try {
                startForeground(NOTIFICATION_ID, notification)
            } catch (e: Exception) {
                Log.e("EmergencySosService", "Failed to start foreground notification", e)
            }
        }

        return START_STICKY
    }

    private fun buildEmergencyNotification(incidentId: String): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingOpen = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, MaayaApplication.CHANNEL_EMERGENCY)
            .setContentTitle("Maaya Emergency SOS Active")
            .setContentText("Continuous location & media stream active for incident $incidentId")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(pendingOpen)
            .build()
    }

    companion object {
        const val NOTIFICATION_ID = 9001
        const val ACTION_START_SOS = "org.maaya.app.action.START_SOS"
        const val ACTION_STOP_SOS = "org.maaya.app.action.STOP_SOS"
        const val EXTRA_INCIDENT_ID = "extra_incident_id"

        fun start(context: Context, incidentId: String) {
            try {
                val intent = Intent(context, EmergencySosForegroundService::class.java).apply {
                    action = ACTION_START_SOS
                    putExtra(EXTRA_INCIDENT_ID, incidentId)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e("EmergencySosService", "Could not start EmergencySosForegroundService safely", e)
            }
        }

        fun stop(context: Context) {
            try {
                val intent = Intent(context, EmergencySosForegroundService::class.java).apply {
                    action = ACTION_STOP_SOS
                }
                context.startService(intent)
            } catch (e: Exception) {
                Log.e("EmergencySosService", "Could not stop EmergencySosForegroundService safely", e)
            }
        }
    }
}
