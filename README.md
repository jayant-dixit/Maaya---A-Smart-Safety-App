# Maaya: Smartwatch-Powered Women's Safety & Emergency Response System

Maaya is an enterprise-grade, privacy-first emergency response ecosystem designed for women's safety. It bridges real-time physiological anomaly detection from BLE smartwatches, intelligent Android SOS activation (in-app, shake, fall, BLE button), verified live location tracking, resilient SMS dispatch, and low-latency real-time audio/video streaming to an authorized contact dashboard.

---

## 1. System Architecture

```
 +-----------------------------------------------------------------------------------------+
 |                                    MAAYA ECOSYSTEM                                      |
 +-----------------------------------------------------------------------------------------+

 [ BLE Smartwatch ]                                  [ Android Core Application ]
 (Heart Rate 0x180D,                                 - Jetpack Compose + Material 3
  Battery 0x180F, SpO2 0x1822)                       - MVVM + Clean Architecture + Coroutines
         |                                           - Room Database (Local Persistence)
         | Bluetooth SIG GATT                        - EncryptedSharedPreferences (Credentials)
         v                                           - Foreground Services (Camera, Mic, GPS)
 +-----------------------+                                   |
 | BluetoothGattCallback |                                   |
 +-----------+-----------+                                   |
             |                                               |
             v                                               v
 +-----------------------+     +-------------------+     +--------------------------------+
 | BleHealthRepository   |---->| Anomaly Detector  |---->| Unified SOS Engine             |
 +-----------------------+     +-------------------+     | (State Machine: IDLE, TRIGGER, |
                                                         |  COUNTDOWN, ACTIVE, RESOLVED)  |
 +-------------------------------------------------------+-------+------------------------+
                                                                 |
            +--------------------+-------------------------------+--------------------+
            |                    |                               |                    |
            v                    v                               v                    v
   [ Local Database ]   [ SMS Dispatcher ]             [ Location Engine ]   [ Media Adapter ]
   - Room Incident Log  - Direct Android SMS           - FusedLocationClient - EmergencyMediaProvider
   - Offline Queue      - Backend Twilio API           - High Accuracy GPS   - WebRTC / Managed SFU
            |                    |                               |                    |
            +--------------------+-------------------------------+--------------------+
                                         |
                                         | Encrypted HTTPS / WSS
                                         v
               +---------------------------------------------------+
               |           Maaya Emergency Cloud Backend            |
               |        (Node.js + Express + TypeScript + WSS)      |
               +-------------------------+-------------------------+
                                         |
                       +-----------------+-----------------+
                       |                                   |
                       v                                   v
             [ PostgreSQL Database ]             [ Real-Time PubSub / WSS ]
             - Users & Trusted Contacts          - Ephemeral Location Streams
             - Emergency Incidents & Audit       - Media Signaling & Status
             - Expiring Access Tokens            - Dispatch Notifications
                                                           |
                                                           v
                                         +-----------------------------------+
                                         |    Trusted Contacts Dashboard     |
                                         |   (React 18 + Tailwind CSS + Map) |
                                         |   - Authenticated Emergency View  |
                                         |   - Interactive Live Map (Leaflet)|
                                         |   - Real-Time Audio/Video Player  |
                                         |   - Timeline & Health Vitals HUD  |
                                         +-----------------------------------+
```

---

## 2. Monorepo Structure

- **`/android`**: Android Studio project targeting Android 15 (API 35/36) with minSdk 26.
  - Kotlin, Jetpack Compose, Material 3, Room, Kotlin Coroutines & Flow, AndroidX Lifecycle, CameraX, BLE GATT manager.
- **`/backend`**: Node.js, Express, TypeScript backend service with PostgreSQL, WebSocket signaling, Twilio/configurable SMS integration, and secure expiring token authentication.
- **`/dashboard`**: Web application for trusted emergency contacts built with React 18, Vite, Tailwind CSS, Leaflet live maps, and WebRTC viewer.
- **`/docs`**: In-depth architecture, BLE GATT specifications, media provider evaluation, API references, test checklists, and troubleshooting guide.
- **`docker-compose.yml`**: One-command orchestration for PostgreSQL, Backend, and Web Dashboard.

---

## 3. Quick Start & Execution

### Prerequisites:
- JDK 17 or JDK 21 (Temurin 21 recommended)
- Android SDK 34+
- Node.js 18+ (Node 24 tested)
- PostgreSQL or Docker

### 1. Launch Backend & Dashboard
```bash
# In /backend
cd backend
npm install
npm run build
npm start

# In /dashboard
cd ../dashboard
npm install
npm run dev
```

### 2. Build and Test Android App
```bash
cd android
./gradlew assembleDebug test
```
