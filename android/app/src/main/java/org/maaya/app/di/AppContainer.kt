package org.maaya.app.di

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.maaya.app.ble.BleGattManager
import org.maaya.app.data.local.MaayaDatabase
import org.maaya.app.data.local.SecurePreferences
import org.maaya.app.data.remote.MaayaApiService
import org.maaya.app.data.repository.BleHealthRepository
import org.maaya.app.data.repository.ContactRepository
import org.maaya.app.data.repository.IncidentRepository
import org.maaya.app.data.repository.SafeRideRepository
import org.maaya.app.domain.engine.BatteryMonitor
import org.maaya.app.domain.engine.EmergencySosEngine
import org.maaya.app.domain.engine.LocationTracker
import org.maaya.app.domain.engine.HeartRateAutoScheduler
import org.maaya.app.domain.engine.SafeRideMonitor
import org.maaya.app.domain.engine.ShakeDetector
import org.maaya.app.domain.engine.SmsDispatcher
import org.maaya.app.domain.model.SosTriggerType
import org.maaya.app.media.EmergencyMediaProvider
import org.maaya.app.media.MockEmergencyMediaProvider
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

interface AppContainer {
    val database: MaayaDatabase
    val securePreferences: SecurePreferences
    val apiService: MaayaApiService
    val contactRepository: ContactRepository
    val incidentRepository: IncidentRepository
    val safeRideRepository: SafeRideRepository
    val bleHealthRepository: BleHealthRepository
    val emergencySosEngine: EmergencySosEngine
    val mediaProvider: EmergencyMediaProvider
    val locationTracker: LocationTracker
    val shakeDetector: ShakeDetector
    val batteryMonitor: BatteryMonitor
    val safeRideMonitor: SafeRideMonitor
    val heartRateAutoScheduler: HeartRateAutoScheduler
}

class DefaultAppContainer(private val context: Context) : AppContainer {

    override val database: MaayaDatabase by lazy {
        MaayaDatabase.getInstance(context)
    }

    override val securePreferences: SecurePreferences by lazy {
        SecurePreferences(context)
    }

    private val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val resilientInterceptor = org.maaya.app.data.remote.ResilientNetworkInterceptor(
            getActiveBaseUrl = { securePreferences.backendBaseUrl },
            onWorkingUrlConfirmed = { workingUrl ->
                if (securePreferences.backendBaseUrl != workingUrl) {
                    securePreferences.backendBaseUrl = workingUrl
                }
            }
        )
        OkHttpClient.Builder()
            .addInterceptor(resilientInterceptor)
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    override val apiService: MaayaApiService by lazy {
        val baseUrl = securePreferences.backendBaseUrl.trimEnd('/') + "/"
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MaayaApiService::class.java)
    }

    override val contactRepository: ContactRepository by lazy {
        ContactRepository(database.contactDao())
    }

    override val incidentRepository: IncidentRepository by lazy {
        IncidentRepository(database.incidentDao(), apiService)
    }

    override val safeRideRepository: SafeRideRepository by lazy {
        SafeRideRepository(database.safeRideDao(), apiService)
    }

    private val bleGattManager: BleGattManager by lazy {
        BleGattManager(context)
    }

    override val bleHealthRepository: BleHealthRepository by lazy {
        BleHealthRepository(bleGattManager, database.healthMetricDao())
    }

    override val mediaProvider: EmergencyMediaProvider by lazy {
        org.maaya.app.media.RealEmergencyMediaProvider(
            context = context,
            backendBaseUrlProvider = { securePreferences.backendBaseUrl },
            customOkHttpClient = okHttpClient
        )
    }

    override val locationTracker: LocationTracker by lazy {
        LocationTracker(context)
    }

    private val smsDispatcher: SmsDispatcher by lazy {
        SmsDispatcher(context)
    }

    override val emergencySosEngine: EmergencySosEngine by lazy {
        EmergencySosEngine(
            context = context,
            incidentRepository = incidentRepository,
            contactRepository = contactRepository,
            mediaProvider = mediaProvider,
            locationTracker = locationTracker,
            smsDispatcher = smsDispatcher,
            securePreferences = securePreferences
        )
    }

    override val shakeDetector: ShakeDetector by lazy {
        ShakeDetector(context) {
            if (securePreferences.isShakeDetectionEnabled) {
                emergencySosEngine.triggerEmergency(SosTriggerType.SHAKE_GESTURE)
            }
        }.apply {
            sensitivityG = securePreferences.shakeSensitivityThreshold
        }
    }

    override val batteryMonitor: BatteryMonitor by lazy {
        BatteryMonitor(context) { level ->
            if (securePreferences.isLowBatteryEscalationEnabled) {
                emergencySosEngine.triggerEmergency(SosTriggerType.LOW_BATTERY_ESCALATION)
            }
        }.apply {
            thresholdPercentage = securePreferences.lowBatteryThreshold
        }
    }

    override val safeRideMonitor: SafeRideMonitor by lazy {
        SafeRideMonitor(
            context = context,
            safeRideRepository = safeRideRepository,
            emergencySosEngine = emergencySosEngine
        )
    }

    override val heartRateAutoScheduler: HeartRateAutoScheduler by lazy {
        HeartRateAutoScheduler(
            bleHealthRepository = bleHealthRepository,
            intervalSeconds = 30
        )
    }
}
