package org.maaya.app.media

import kotlinx.coroutines.flow.StateFlow

sealed class MediaStreamState {
    object Idle : MediaStreamState()
    object Initializing : MediaStreamState()
    object Connecting : MediaStreamState()
    data class Streaming(
        val sessionId: String,
        val viewersCount: Int,
        val isVideoActive: Boolean,
        val isAudioActive: Boolean,
        val currentFps: Int = 30,
        val bitrateKbps: Int = 800
    ) : MediaStreamState()
    data class Reconnecting(val attempt: Int, val reason: String) : MediaStreamState()
    data class Error(val message: String, val throwable: Throwable? = null) : MediaStreamState()
    object Stopped : MediaStreamState()
}

data class MediaConstraints(
    val enableVideo: Boolean = true,
    val enableAudio: Boolean = true,
    val useFrontCamera: Boolean = true,
    val targetWidth: Int = 1280,
    val targetHeight: Int = 720,
    val targetFps: Int = 30,
    val maxBitrateBps: Int = 1_000_000
)

data class MediaSessionInfo(
    val sessionId: String,
    val streamToken: String,
    val publishUrl: String?,
    val webViewerUrl: String
)

interface EmergencyMediaProvider {
    val state: StateFlow<MediaStreamState>

    suspend fun initialize(): Result<Unit>
    suspend fun startStream(sessionId: String, authKey: String, constraints: MediaConstraints): Result<MediaSessionInfo>
    suspend fun stopStream(): Result<Unit>
    fun switchCamera(): Result<Unit>
    fun toggleAudioMute(isMuted: Boolean): Result<Unit>
    fun toggleVideoPause(isPaused: Boolean): Result<Unit>
    fun broadcastLiveFrame(frameBase64: String, incidentId: String? = null)
}
