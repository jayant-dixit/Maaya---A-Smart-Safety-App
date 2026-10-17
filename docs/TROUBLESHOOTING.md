# Maaya Emergency Response System - Troubleshooting Guide

## 1. Bluetooth Low Energy (BLE) Issues

### Issue: "Scan for Watches" yields no devices
- **Root Cause**: On Android 12+ (API 31+), `BLUETOOTH_SCAN` and `BLUETOOTH_CONNECT` permissions are strictly required at runtime. On Android 11 and below, location permission must be granted for BLE scans to return results.
- **Fix**: Ensure Bluetooth and Location (GPS) are toggled ON in device system settings. In Maaya, go to Settings -> check permissions.
- **Root Cause 2**: Watch is paired in the phone's system Bluetooth settings.
- **Fix**: Unpair/forget the smartwatch in the Android OS Bluetooth settings, then let Maaya initiate direct GATT pairing so service discovery succeeds.

### Issue: GATT Connection Drops After a Few Seconds
- **Root Cause**: Smartwatch firmware may request connection parameter updates or have aggressive power-saving timeouts.
- **Fix**: Ensure `SmartwatchBleForegroundService` is active. Check `android.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE` in AndroidManifest.

---

## 2. SMS Delivery Failures

### Issue: SMS not delivered to contacts
- **Root Cause 1**: Android device lacks an active SIM card or cellular carrier credit.
- **Root Cause 2**: Carrier-level spam filtering blocks messages containing URL links.
- **Fix**: Test with standard alphanumeric phone numbers formatted with international country codes (e.g., `+91...`).
- **Resilience**: Maaya's decoupled architecture ensures that an SMS delivery failure will **never** block GPS coordinate tracking, local persistence, or live video streaming.

---

## 3. Background Execution & Android Battery Optimizations

### Issue: Location tracking stops when screen turns off
- **Root Cause**: Aggressive OEM battery killers (Xiaomi MIUI, Samsung OneUI, Huawei EMUI) suspend background apps.
- **Fix**:
  1. Maaya runs high-priority foreground services with ongoing notifications (`EmergencySosForegroundService`).
  2. In Android Device Settings -> Apps -> Maaya -> Battery: Select **"Unrestricted"**.

---

## 4. Backend & Web Dashboard Connection Issues

### Issue: "Failed to fetch emergency incident" on Dashboard
- **Root Cause**: The dashboard is trying to connect to `http://localhost:8080`, but the backend is not running or the token expired.
- **Fix**:
  1. Check backend status: `curl http://localhost:8080/api/health`
  2. Verify the incident was generated with a valid expiring token.
  3. For testing from a physical mobile phone, update `backendBaseUrl` in Maaya Settings to your local network IP (e.g. `http://192.168.1.100:8080`).
