package org.maaya.app.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SecurePreferences(context: Context) {

    private val prefs: SharedPreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            "maaya_secure_vault",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        // Fallback for isolated test environments or uninitialized key stores
        context.getSharedPreferences("maaya_fallback_vault", Context.MODE_PRIVATE)
    }

    var isOnboardingCompleted: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_DONE, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDING_DONE, value).apply()

    var userName: String
        get() = prefs.getString(KEY_USER_NAME, "") ?: ""
        set(value) = prefs.edit().putString(KEY_USER_NAME, value).apply()

    var userPhoneNumber: String
        get() = prefs.getString(KEY_USER_PHONE, "") ?: ""
        set(value) = prefs.edit().putString(KEY_USER_PHONE, value).apply()

    var safetyPin: String
        get() = prefs.getString(KEY_SAFETY_PIN, "1234") ?: "1234"
        set(value) = prefs.edit().putString(KEY_SAFETY_PIN, value).apply()

    var backendBaseUrl: String
        get() {
            val stored = prefs.getString(KEY_BACKEND_URL, null)
            return if (stored.isNullOrBlank() || stored.contains("10.0.2.2") || stored.contains(":8080")) {
                "http://127.0.0.1:8088"
            } else {
                stored
            }
        }
        set(value) = prefs.edit().putString(KEY_BACKEND_URL, value).apply()

    var dashboardBaseUrl: String
        get() {
            val stored = prefs.getString(KEY_DASHBOARD_URL, null)
            if (!stored.isNullOrBlank()) return stored
            val backend = backendBaseUrl
            return if (backend.contains("onrender.com")) {
                "https://maaya-henna.vercel.app"
            } else {
                backend.replace(":8088", ":5173").replace(":8080", ":5173")
            }
        }
        set(value) = prefs.edit().putString(KEY_DASHBOARD_URL, value).apply()

    var shakeSensitivityThreshold: Float
        get() = prefs.getFloat(KEY_SHAKE_SENSITIVITY, 2.7f)
        set(value) = prefs.edit().putFloat(KEY_SHAKE_SENSITIVITY, value).apply()

    var isShakeDetectionEnabled: Boolean
        get() = prefs.getBoolean(KEY_SHAKE_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_SHAKE_ENABLED, value).apply()

    var isLowBatteryEscalationEnabled: Boolean
        get() = prefs.getBoolean(KEY_BATTERY_ALERT_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_BATTERY_ALERT_ENABLED, value).apply()

    var lowBatteryThreshold: Int
        get() = prefs.getInt(KEY_BATTERY_THRESHOLD, 15)
        set(value) = prefs.edit().putInt(KEY_BATTERY_THRESHOLD, value).apply()

    var lastBondedWatchAddress: String?
        get() = prefs.getString(KEY_BONDED_WATCH_MAC, null)
        set(value) = prefs.edit().putString(KEY_BONDED_WATCH_MAC, value).apply()

    var countdownDurationSeconds: Int
        get() = prefs.getInt(KEY_COUNTDOWN_SECONDS, 4)
        set(value) = prefs.edit().putInt(KEY_COUNTDOWN_SECONDS, value).apply()

    companion object {
        private const val KEY_ONBOARDING_DONE = "onboarding_done"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_PHONE = "user_phone"
        private const val KEY_SAFETY_PIN = "safety_pin"
        private const val KEY_BACKEND_URL = "backend_url"
        private const val KEY_DASHBOARD_URL = "dashboard_url"
        private const val KEY_SHAKE_SENSITIVITY = "shake_sensitivity"
        private const val KEY_SHAKE_ENABLED = "shake_enabled"
        private const val KEY_BATTERY_ALERT_ENABLED = "battery_alert_enabled"
        private const val KEY_BATTERY_THRESHOLD = "battery_threshold"
        private const val KEY_BONDED_WATCH_MAC = "bonded_watch_mac"
        private const val KEY_COUNTDOWN_SECONDS = "countdown_seconds"
    }
}
