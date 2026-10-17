# Emergency Media Provider Evaluation: Native WebRTC + Coturn vs. Managed WebRTC (Agora / LiveKit)

## 1. Executive Summary & Recommendation

For **Maaya (Women's Safety and Emergency Response System)**, the primary media topology is:
- **Publisher**: 1 mobile Android client operating under unpredictable emergency conditions (cellular data 4G/5G, switching cell towers, poor signal, high packet loss, battery conservation mode, background/lockscreen constraints).
- **Subscribers**: 1 to 5 authorized trusted emergency contacts viewing concurrently via web browsers on desktop or mobile.
- **Latency Requirement**: Ultra-low latency (< 1.5 seconds) with rapid stream establishment (< 3 seconds) and resilient reconnection.

### Architectural Recommendation: Managed Hybrid Adapter with Provider-Independent Abstraction

1. **Architecture Pattern**: Provider-Independent `EmergencyMediaProvider` contract in Android and `EmergencyMediaViewer` contract in the web dashboard.
2. **Recommended Engine for Production**: **Managed WebRTC SFU (LiveKit / Agora)** with a headless fallback to standard WebRTC signaling or low-bandwidth snapshot/audio fallback.
3. **Rationale**:
   - Running a bare **Coturn + Native WebRTC Mesh / self-hosted SFU** requires maintaining public STUN/TURN servers, custom ICE renegotiation, NAT traversal over symmetric NATs/carrier-grade NAT (CGNAT) on cellular networks, manual bitrate/resolution throttling during cell-tower handoffs, and self-managed server orchestration.
   - During a critical emergency SOS, if Coturn certificate renewal fails, port forwarding fails, or ICE candidates fail across cellular carriers, stream setup fails silently.
   - Managed providers (or containerized LiveKit SFU with global TURN relays) handle adaptive bitrate (ABR), simulcast, ICE trickle, and automatic fallback out of the box with 99.95%+ uptime SLAs.
4. **Decoupling Guarantee**:
   The core SOS engine (`EmergencySosEngine`) does **NOT** import or reference any specific vendor SDK. Instead, it interacts solely with:
   ```kotlin
   interface EmergencyMediaProvider {
       val state: StateFlow<MediaStreamState>
       suspend fun initialize(config: MediaProviderConfig): Result<Unit>
       suspend fun startStream(sessionId: String, authKey: String, constraints: MediaConstraints): Result<MediaSessionInfo>
       suspend fun stopStream(): Result<Unit>
       fun switchCamera(): Result<Unit>
       fun toggleMute(muted: Boolean): Result<Unit>
   }
   ```
   The application ships with:
   - `MockEmergencyMediaProvider`: For deterministic offline testing, automated CI/CD, and simulated emergency scenarios.
   - `WebRtcEmergencyMediaProvider`: Direct WebRTC adapter communicating with backend signaling server.
   - `LiveKitMediaProvider` / `AgoraMediaProvider`: Production-grade adapter adhering to the identical interface.

---

## 2. Deep Technical Comparison

| Dimension | Option A: Native WebRTC + Self-Hosted Coturn | Option B: Managed Provider (Agora) | Option C: Open-Source SFU (LiveKit / Mediasoup) |
| :--- | :--- | :--- | :--- |
| **Android Integration** | `org.webrtc:google-webrtc` (binary size ~15MB). Requires manual PeerConnectionFactory, EglBase, SurfaceViewRenderer, AudioRecord plumbing, SDP negotiation, ICE gathering. | Vendor SDK (~8MB). Ready-made camera/microphone capture and token-based room joining. | `livekit-android` SDK (~6MB). Native Kotlin coroutines support, room/track subscriptions. |
| **Cellular Reliability & CGNAT** | Requires Coturn configured with static public IP, valid TLS certificates, open UDP/TCP ports (3478, 5349, 49152-65535). Vulnerable to carrier firewall blocks without TURNS (443). | Built-in global edge network with automatic multi-protocol fallback (UDP -> TCP -> TLS). | High reliability when deployed behind Cloudflare / Global Accelerator + dedicated TURN relay. |
| **Multi-Viewer Fanout** | **Mesh topology** fails on mobile: publishing 3 separate video streams over LTE consumes 3x upstream bandwidth (~3-5 Mbps) and overheats the phone. Requires building or hosting an SFU (Janus/Kurento/Mediasoup). | Native SFU: Phone uploads **one single stream**; the edge server fans out to unlimited trusted contact viewers. | Native SFU: 1 upload stream, server handles multi-viewer fanout. |
| **Latency** | 200ms - 500ms (P2P or single-hop TURN). | 200ms - 400ms (global routing backbone). | 250ms - 500ms (depending on server hosting region). |
| **Battery & CPU on Android** | High if misconfigured. Requires careful hardware encoder selection (`HardwareVideoEncoderFactory`). | Highly optimized hardware acceleration pipelines for Qualcomm/MediaTek/Exynos. | Well-optimized hardware codecs via Google WebRTC wrapper. |
| **Operational Overhead** | **Very High**: Continuous TURN monitoring, security patching, certificate rotations, DDoS protection, bandwidth costs. | **Zero server maintenance**: API keys and token generation only. Free tier available (10,000 mins/month). | **Moderate**: Single container deployment (`livekit-server`), self-hosted or managed Cloud. |
| **Viewer Access Control** | Custom signaling server must validate JWT tokens before sending SDP offers/answers. | Token-based channel authorization; expiring tokens with role `SUBSCRIBER`. | Token-based room access; expiring JWT signed with room secret. |

---

## 3. Infrastructure & Deployment Architecture

```
                       +----------------------------------+
                       |   Maaya Android Emergency App   |
                       |  (Single Publisher: Audio+Video) |
                       +-----------------+----------------+
                                         |
                       Emergency SOS     | Live Stream
                       REST / WebSocket  | (WebRTC / SFU)
                                         |
                                         v
   +--------------------+     +----------+-----------+
   |   Node.js / Express |     | Managed / Cloud SFU  |
   |   Backend Server   |     | (Agora / LiveKit /   |
   |   - Auth & Session |     |  Coturn Relay)       |
   |   - SOS State      |     +----------+-----------+
   |   - Token Minting  |                |
   +---------+----------+                | Fanout to N viewers
             |                           |
             | WebSocket Status Updates  v
             |               +-----------+------------+
             +-------------->| Trusted Contacts Web   |
                             | Dashboard (Viewers)    |
                             +------------------------+
```

### Infrastructure Requirements:
1. **Backend**:
   - Node.js/TypeScript service generating cryptographically signed, short-lived JWT tokens (expiring in 15–30 minutes) for each active emergency incident.
   - WebSocket signaling server to exchange session IDs and status between Android publisher and web viewers.
2. **Android Application**:
   - CameraX / Audio Record integration or WebRTC track capture.
   - Background service tolerance: Must run in a high-priority Foreground Service with `FOREGROUND_SERVICE_TYPE_CAMERA | FOREGROUND_SERVICE_TYPE_MICROPHONE`.
   - Dynamic resolution fallback: 720p @ 30fps -> 480p @ 15fps -> 360p @ 10fps -> Audio-only when bandwidth drops below 150 kbps.
3. **Web Dashboard**:
   - Modern browser WebRTC player with HTML5 `<video>` element, automatic autoplay with user interaction fallback, stream reconnection on drop, and snapshot fallback.
