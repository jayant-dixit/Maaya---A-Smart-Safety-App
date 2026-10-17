package org.maaya.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import org.maaya.app.ui.navigation.MaayaNavGraph
import org.maaya.app.ui.theme.DarkNavyBackground
import org.maaya.app.ui.theme.MaayaTheme

class MainActivity : ComponentActivity() {

    private val container by lazy {
        (application as MaayaApplication).container
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaayaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkNavyBackground
                ) {
                    MaayaNavGraph(container = container)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (container.securePreferences.isOnboardingCompleted) {
            try {
                org.maaya.app.service.MaayaSafetyDaemonService.start(this)
            } catch (_: Exception) {}

            if (container.securePreferences.isShakeDetectionEnabled) {
                container.shakeDetector.start()
            }
            if (container.securePreferences.isLowBatteryEscalationEnabled) {
                container.batteryMonitor.start()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // Do NOT stop shakeDetector and batteryMonitor!
        // MaayaSafetyDaemonService keeps background monitoring and shake detection alive 24/7.
    }
}
