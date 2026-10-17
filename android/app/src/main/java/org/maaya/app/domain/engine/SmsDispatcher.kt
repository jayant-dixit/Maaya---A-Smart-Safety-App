package org.maaya.app.domain.engine

import android.content.Context
import android.os.Build
import android.telephony.SmsManager
import android.util.Log
import org.maaya.app.domain.model.TrustedContact

class SmsDispatcher(private val context: Context) {

    fun dispatchEmergencySms(
        contacts: List<TrustedContact>,
        userName: String,
        latitude: Double,
        longitude: Double,
        dashboardUrl: String
    ): Result<Int> {
        if (contacts.isEmpty()) {
            return Result.failure(IllegalStateException("No priority emergency contacts configured"))
        }

        val message = buildString {
            append("EMERGENCY ALERT: ")
            append(if (userName.isNotBlank()) userName else "User")
            append(" triggered an SOS!\n")
            append("Location: https://maps.google.com/?q=$latitude,$longitude\n")
            append("Live Tracking & Stream: $dashboardUrl")
        }

        val smsManager: SmsManager? = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }
        } catch (e: Exception) {
            Log.e("SmsDispatcher", "Failed to retrieve SmsManager instance", e)
            null
        }

        if (smsManager == null) {
            return Result.failure(IllegalStateException("SMS service unavailable on this device"))
        }

        var dispatchedCount = 0
        for (contact in contacts) {
            try {
                val parts = smsManager.divideMessage(message)
                smsManager.sendMultipartTextMessage(contact.phoneNumber, null, parts, null, null)
                dispatchedCount++
                Log.i("SmsDispatcher", "Dispatched SOS SMS to ${contact.name} (${contact.phoneNumber})")
            } catch (e: Exception) {
                Log.e("SmsDispatcher", "Failed to deliver SMS to ${contact.phoneNumber}", e)
            }
        }

        return if (dispatchedCount > 0) {
            Result.success(dispatchedCount)
        } else {
            Result.failure(RuntimeException("Could not deliver SMS to any recipient"))
        }
    }
}
