# Smartwatch BLE Integration & Standard GATT Health Profile Specification

## 1. Bluetooth SIG Standard Specifications Adherence

Maaya adheres strictly to official **Bluetooth SIG Assigned Numbers and Adopted GATT Specifications**. We do **not** invent proprietary UUIDs or undocumented binary commands for hardware control.

### Supported Standard Services and Characteristics:

| Service / Characteristic Name | Standard UUID (16-bit / 128-bit base `0000xxxx-0000-1000-8000-00805F9B34FB`) | Operation | Data Format & Decoding Notes |
| :--- | :--- | :--- | :--- |
| **Heart Rate Service** | `0x180D` (`0000180D-0000-1000-8000-00805F9B34FB`) | GATT Primary Service | Standard Heart Rate Monitor |
| ↳ **Heart Rate Measurement** | `0x2A37` (`00002A37-0000-1000-8000-00805F9B34FB`) | **Notify** | Byte 0 Flags: Bit 0 = 0 (UINT8 bpm) or 1 (UINT16 bpm). Bit 1 = Sensor Contact Status. Bit 3 = Energy Expended present. Bit 4 = RR-Intervals present. |
| ↳ **Body Sensor Location** | `0x2A38` (`00002A38-0000-1000-8000-00805F9B34FB`) | **Read** | 0=Other, 1=Chest, 2=Wrist, 3=Finger, 4=Hand, 5=Ear Lobe, 6=Foot. |
| **Battery Service** | `0x180F` (`0000180F-0000-1000-8000-00805F9B34FB`) | GATT Primary Service | Peripheral battery status |
| ↳ **Battery Level** | `0x2A19` (`00002A19-0000-1000-8000-00805F9B34FB`) | **Read / Notify** | UINT8 (0–100%) |
| **Pulse Oximeter Service** | `0x1822` (`00001822-0000-1000-8000-00805F9B34FB`) | GATT Primary Service | SpO2 blood oxygen saturation |
| ↳ **PLX Continuous Measurement** | `0x2A5F` (`00002A5F-0000-1000-8000-00805F9B34FB`) | **Notify** | SFLOAT (SpO2 percentage) and SFLOAT (Pulse Rate) |
| ↳ **PLX Spot-Check Measurement** | `0x2A5E` (`00002A5E-0000-1000-8000-00805F9B34FB`) | **Indicate** | SFLOAT SpO2 + timestamp |
| **Immediate Alert Service** | `0x1802` (`00001802-0000-1000-8000-00805F9B34FB`) | GATT Primary Service | Two-way alert signaling |
| ↳ **Alert Level** | `0x2A06` (`00002A06-0000-1000-8000-00805F9B34FB`) | **Write Without Response** | 0=No Alert, 1=Mild Alert, 2=High Alert (SOS Trigger) |
| **Client Characteristic Config (CCCD)** | `0x2902` (`00002902-0000-1000-8000-00805F9B34FB`) | **Read / Write** | Enables notifications (`0x0001`) or indications (`0x0002`) |

---

## 2. Notification vs. Request Distinction

Critical architectural constraint:
- **Passive Notifications (`NOTIFY`)**: Emitted periodically by the peripheral (e.g. `0x2A37` heart rate tick every second). The app parses incoming payload, checks timestamp, and updates cached metric.
- **Active Explicit Reads / Indication Acknowledgements**: Initiated intentionally by user or scheduler.
- **State Integrity**: The UI and SOS engine must never indicate "Connected & Streaming" when a peripheral is merely bonded or disconnected. The connection state machine is strictly verified via `BluetoothGattCallback.onConnectionStateChange` with `BluetoothProfile.STATE_CONNECTED` and `onServicesDiscovered` status `GATT_SUCCESS`.

---

## 3. Anomaly & Emergency SOS Trigger Algorithms

The BLE health pipeline monitors real-time physiological metrics:
1. **Tachycardia / Stress Spike Trigger**:
   - Resting baseline heart rate calculated over a rolling 15-minute window (e.g. 70 bpm).
   - If heart rate suddenly spikes above a configurable threshold (e.g. >= 145 bpm) without accelerometer evidence of intense running or workouts, triggers high-stress SOS pre-alert countdown (10-second cancelable window).
2. **Fall / Impact Correlation**:
   - If connected peripheral or phone accelerometer detects sudden acceleration (> 3.5g) followed by zero movement, prompts emergency check-in.
3. **Peripheral Hardware Button Trigger**:
   - Watches or BLE smart bands supporting the standard Alert Level or HID key press trigger the SOS pipeline immediately.
