package org.maaya.app.media

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MockEmergencyMediaProvider(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default),
    private val webViewerBaseUrl: String = "http://localhost:5173/emergency/stream"
) : EmergencyMediaProvider {

    private val _state = MutableStateFlow<MediaStreamState>(MediaStreamState.Idle)
    override val state: StateFlow<MediaStreamState> = _state.asStateFlow()

    private var currentSessionId: String? = null
    private var isMuted = false
    private var isPaused = false

    override suspend fun initialize(): Result<Unit> {
        _state.value = MediaStreamState.Initializing
        delay(200)
        _state.value = MediaStreamState.Idle
        return Result.success(Unit)
    }

    override suspend fun startStream(
        sessionId: String,
        authKey: String,
        constraints: MediaConstraints
    ): Result<MediaSessionInfo> {
        currentSessionId = sessionId
        _state.value = MediaStreamState.Connecting
        delay(600)

        val sessionInfo = MediaSessionInfo(
            sessionId = sessionId,
            streamToken = authKey,
            publishUrl = "webrtc://mock-sfu.maaya.internal/$sessionId",
            webViewerUrl = "$webViewerBaseUrl?session=$sessionId&token=$authKey"
        )

        _state.value = MediaStreamState.Streaming(
            sessionId = sessionId,
            viewersCount = 1,
            isVideoActive = !isPaused && constraints.enableVideo,
            isAudioActive = !isMuted && constraints.enableAudio,
            currentFps = constraints.targetFps,
            bitrateKbps = 750
        )

        return Result.success(sessionInfo)
    }

    override suspend fun stopStream(): Result<Unit> {
        currentSessionId = null
        _state.value = MediaStreamState.Stopped
        delay(200)
        _state.value = MediaStreamState.Idle
        return Result.success(Unit)
    }

    override fun switchCamera(): Result<Unit> {
        val current = _state.value
        if (current is MediaStreamState.Streaming) {
            // Emulate quick camera flip transition
            scope.launch {
                _state.value = current.copy(currentFps = 15)
                delay(150)
                _state.value = current.copy(currentFps = 30)
            }
        }
        return Result.success(Unit)
    }

    override fun toggleAudioMute(isMuted: Boolean): Result<Unit> {
        this.isMuted = isMuted
        val current = _state.value
        if (current is MediaStreamState.Streaming) {
            _state.value = current.copy(isAudioActive = !isMuted)
        }
        return Result.success(Unit)
    }

    override fun toggleVideoPause(isPaused: Boolean): Result<Unit> {
        this.isPaused = isPaused
        val current = _state.value
        if (current is MediaStreamState.Streaming) {
            _state.value = current.copy(isVideoActive = !isPaused)
        }
        return Result.success(Unit)
    }

    override fun broadcastLiveFrame(frameBase64: String, incidentId: String?) {
        // No-op for mock provider
    }
}
