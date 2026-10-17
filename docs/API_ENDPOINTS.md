# Maaya Emergency Response System - API Endpoints Documentation

## Base URLs
- **Development**: `http://localhost:8080/api` (Android Emulator: `http://10.0.2.2:8080/api`)
- **WebSocket Gateway**: `ws://localhost:8080/ws/emergency`

---

## 1. Emergency Incident APIs

### `POST /api/incidents`
Initiates a new emergency incident. Generates short-lived, cryptographically signed JWT tokens for trusted contact web access and broadcasts to subscribers.

**Request Headers**:
- `Content-Type: application/json`

**Request Body**:
```json
{
  "triggerType": "MANUAL_IN_APP | SHAKE_GESTURE | SMARTWATCH_BUTTON | HEART_RATE_ANOMALY | FALL_DETECTED | LOW_BATTERY_ESCALATION | SAFE_RIDE_OVERDUE",
  "latitude": 28.6139,
  "longitude": 77.2090,
  "accuracy": 4.8,
  "heartRateBpm": 142,
  "batteryLevel": 68,
  "notes": "Emergency trigger"
}
```

**Response (201 Created)**:
```json
{
  "incidentId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "triggerType": "MANUAL_IN_APP",
  "status": "ACTIVE",
  "initialLatitude": 28.6139,
  "initialLongitude": 77.2090,
  "currentLatitude": 28.6139,
  "currentLongitude": 77.2090,
  "accuracyMeters": 4.8,
  "heartRateBpm": 142,
  "batteryPercentage": 68,
  "dashboardAccessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "dashboardViewerUrl": "http://localhost:5173/emergency/3fa85f64-5717-4562-b3fc-2c963f66afa6?token=eyJhbGci...",
  "streamToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "createdAt": 1729080000000
}
```

---

### `POST /api/incidents/location`
Pushes continuous live location updates and physiological telemetry from Android device during an active incident.

**Request Body**:
```json
{
  "incidentId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "latitude": 28.6145,
  "longitude": 77.2098,
  "accuracy": 3.2,
  "speed": 1.4,
  "altitude": 216.0,
  "heartRateBpm": 148,
  "batteryPercentage": 67
}
```

**Response (200 OK)**:
```json
{
  "success": true,
  "message": "Telemetry updated and broadcasted"
}
```

---

### `POST /api/incidents/resolve`
Resolves an active emergency incident with verified Safety PIN confirmation.

**Request Body**:
```json
{
  "incidentId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "safetyPin": "1234",
  "resolutionReason": "User verified safe"
}
```

**Response (200 OK)**:
```json
{
  "success": true,
  "message": "Incident resolved"
}
```

---

### `GET /api/incidents/:id`
Retrieves incident status and complete coordinate path breadcrumbs. Requires expiring JWT token for authorization.

**Query Parameters / Header**:
- `?token=<dashboardAccessToken>` or `Authorization: Bearer <dashboardAccessToken>`

**Response (200 OK)**:
```json
{
  "incidentId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "status": "ACTIVE",
  "triggerType": "SMARTWATCH_BUTTON",
  "currentLatitude": 28.6145,
  "currentLongitude": 77.2098,
  "accuracyMeters": 3.2,
  "heartRateBpm": 148,
  "batteryPercentage": 67,
  "locationHistory": [
    {
      "latitude": 28.6139,
      "longitude": 77.2090,
      "accuracy": 4.8,
      "timestamp": 1729080000000
    },
    {
      "latitude": 28.6145,
      "longitude": 77.2098,
      "accuracy": 3.2,
      "timestamp": 1729080004000
    }
  ]
}
```

---

## 2. Safe Ride Journey APIs

### `POST /api/saferide/start`
Starts a monitored journey escort.

**Request Body**:
```json
{
  "rideId": "ride-uuid",
  "vehiclePlate": "DL01AB1234",
  "driverName": "Rajesh Kumar",
  "destinationName": "Saket City Centre",
  "expectedArrivalMinutes": 30,
  "checkInIntervalMinutes": 5
}
```

---

## 3. Real-Time WebSocket Signaling

**URL**: `ws://localhost:8080/ws/emergency?incidentId=<incidentId>&token=<token>`

### Inbound Events to Contact Viewer:
1. `CONNECTION_ESTABLISHED`: Sent upon successful token validation.
2. `LOCATION_UPDATE`: Emitted whenever device publishes new GPS coordinates or vitals.
   ```json
   {
     "type": "LOCATION_UPDATE",
     "incidentId": "3fa85f64...",
     "data": {
       "latitude": 28.6145,
       "longitude": 77.2098,
       "accuracy": 3.2,
       "heartRateBpm": 148,
       "batteryPercentage": 67,
       "timestamp": 1729080004000
     }
   }
   ```
3. `INCIDENT_RESOLVED`: Emitted when safety PIN is confirmed.
