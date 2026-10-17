# Maaya Physical Device Testing Checklist & Verification Guide

## 1. Pre-Flight Setup & Environment Verification

| Step | Action | Expected Result | Verified |
| :--- | :--- | :--- | :--- |
| 1.1 | Check Android OS version (`>= Android 8.0 / API 26`, target `API 35`). | Compatible OS confirmed. | [ ] |
| 1.2 | Verify Google Play Services is updated on device. | FusedLocationProviderClient operates without warnings. | [ ] |
| 1.3 | Check Bluetooth 4.2 / 5.0+ LE hardware support on phone. | `hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)` returns true. | [ ] |
| 1.4 | Backend reachable from mobile device (`http://<LAN-IP>:8080/api/health`). | Returns `{ status: "HEALTHY" }`. | [ ] |

---

## 2. Onboarding & Contextual Permission Flow

| Step | Test Procedure | Pass Criteria | Verified |
| :--- | :--- | :--- | :--- |
| 2.1 | Launch app on clean install. Verify Onboarding screen is displayed. | Onboarding screen loads, displaying Profile & Permissions cards. | [ ] |
| 2.2 | Tap "Grant Critical Permissions". | Android system runtime dialogs appear for Location, Bluetooth, Camera, Audio, SMS, Notifications. | [ ] |
| 2.3 | Grant all permissions and submit profile with valid PIN (e.g. `1234`). | Transitions smoothly to Maaya Shield Dashboard. `SecurePreferences.isOnboardingCompleted = true`. | [ ] |
| 2.4 | Force kill and restart app. | App opens directly to Dashboard without re-prompting onboarding. | [ ] |

---

## 3. Bluetooth LE Smartwatch Integration

| Step | Test Procedure | Pass Criteria | Verified |
| :--- | :--- | :--- | :--- |
| 3.1 | Navigate to **Wearable BLE** screen and tap "Scan for Watches". | App starts BLE scanning with `0x180D` filter. Discovered devices appear in real time with RSSI. | [ ] |
| 3.2 | Tap a standard BLE heart rate monitor (or pulse oximeter). | Connection changes: `Connecting` -> `GATT Connected` -> `Monitoring GATT Vitals`. | [ ] |
| 3.3 | Verify incoming notifications on GATT `0x2A37`. | Heart rate readout updates every 1–2 seconds with genuine sensor contact status. | [ ] |
| 3.4 | Emulate stress spike via "Stress Spike (> 150 bpm)" simulator button. | Anomaly detection evaluates rolling window, detects > 145 bpm, triggers SOS countdown. | [ ] |
| 3.5 | Power off BLE watch peripheral while connected. | App reflects `Disconnected` state within 3 seconds. Never falsely claims "Connected". | [ ] |

---

## 4. Emergency SOS Activation & Decoupled Execution

| Step | Test Procedure | Pass Criteria | Verified |
| :--- | :--- | :--- | :--- |
| 4.1 | Tap central **SOS** button on Dashboard. | 5-second cancelable countdown starts with high-visibility countdown number and vibration. | [ ] |
| 4.2 | Tap "TAP TO CANCEL" during countdown. | Countdown aborts immediately; state resets to `IDLE` with zero network or SMS calls. | [ ] |
| 4.3 | Allow countdown to elapse (or trigger with bypass). | Incident is created in Room local DB **before** network calls. High-priority Foreground Service starts. | [ ] |
| 4.4 | Verify SMS delivery. | SMS dispatch sends direct SMS to priority contacts with live dashboard URL. | [ ] |
| 4.5 | Test Offline Resilience: Turn on Airplane mode, trigger SOS. | Local incident is saved in Room. SMS is dispatched via cellular carrier even if Wi-Fi/data is down. App handles offline state without crashing. | [ ] |
| 4.6 | Verify live coordinates tracking during active SOS. | GPS coordinates continuously update every 4 seconds. | [ ] |
| 4.7 | Tap "I Am Safe - Resolve SOS". Enter incorrect PIN. | Error "Invalid Safety PIN" is displayed; emergency transmission remains active. | [ ] |
| 4.8 | Enter correct PIN (`1234`). | Incident resolves locally and remotely. Foreground service stops. Audio/video stream closes. | [ ] |

---

## 5. Multi-Shake & Hardware Triggers

| Step | Test Procedure | Pass Criteria | Verified |
| :--- | :--- | :--- | :--- |
| 5.1 | Walk or jog normally with phone in pocket. | Normal gait acceleration (< 2.5g) does not cause false trigger. | [ ] |
| 5.2 | Shake phone vigorously 3 times within 1.5 seconds. | Accelerometer variance detects pattern; initiates emergency countdown. | [ ] |

---

## 6. Contact Web Dashboard & Real-Time Viewer

| Step | Test Procedure | Pass Criteria | Verified |
| :--- | :--- | :--- | :--- |
| 6.1 | Open SMS alert link on desktop/mobile browser (`http://localhost:5173/emergency/<id>?token=<token>`). | Dashboard authenticates token and displays active emergency banner. | [ ] |
| 6.2 | Verify WebSocket stream updates. | When Android phone moves, map pin moves in real time without refreshing browser page. | [ ] |
| 6.3 | Verify heart rate and battery gauges. | Telemetry cards display live values received over WebSocket. | [ ] |
| 6.4 | Click "Google Maps" shortcut. | Opens Google Maps centered on the latest distress coordinates. | [ ] |
| 6.5 | Resolve emergency on Android app. | Dashboard updates to "Resolved / Safe" badge in real time over WebSocket. | [ ] |
