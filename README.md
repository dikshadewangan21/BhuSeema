# BhuSeema (भूसीमा)

Centimeter-accurate land boundary surveying & cadastral field app for Android (Kotlin + Jetpack Compose).

BhuSeema connects directly to an RTK GNSS rover (ESP32 + u-blox ZED-F9P or compatible RTK module) over Bluetooth Classic SPP to capture legal land parcel boundaries with strict quality gating.

---

## Features

- **RTK Quality Gating**: Corners can only be captured when solution status is **RTK Fixed** (`Fix Quality 4`) and **HDOP $\le$ 2.0**.
- **Witness & Dispute Protection**: Mandatory capture of both Owner and Neighbour witness signatures/names for each corner peg.
- **RoR Discrepancy Detection**: Compares field-measured polygon area against the legal Record of Rights (RoR / Jamabandi / 7/12 extract). Flags any discrepancy exceeding 5%.
- **Stakeout Navigation**: Real-time distance and forward azimuth/bearing to help relocate or replace lost boundary stones in the field.
- **Offline First**: Entirely functional offline with Room database (`seema.db`).
- **GeoJSON Export & Sync**: Export RFC 7946 GeoJSON with comprehensive point metadata (HDOP, satellite count, timestamp, GCP tags, witnesses). Supports encrypted HTTPS background sync via Android WorkManager.

---

## Hardware Setup

1. **Rover**: ESP32 paired with an RTK GNSS module (e.g., u-blox ZED-F9P, Holybro, or ArduSimple).
2. Rover firmware must stream standard **NMEA GGA** sentences over Bluetooth Classic SPP (`00001101-0000-1000-8000-00805F9B34FB`).
3. Pair the rover in standard Android Bluetooth settings prior to connecting inside the app.

---

## Getting Started

1. Open this repository in **Android Studio** (Koala 2024.1.1 or newer, JDK 17).
2. Allow Gradle to sync.
3. Build and deploy to an Android device (API 26+ / Android 8.0+).
4. Grant the `BLUETOOTH_CONNECT` permission when prompted.
