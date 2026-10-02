# BhuSeema (भूसीमा)

**Centimeter-accurate land boundary surveying & cadastral field app for Android.**

Built with Kotlin + Jetpack Compose. Connects directly to an RTK GNSS rover (ESP32 + u-blox ZED-F9P or compatible RTK module) over Bluetooth Classic SPP to capture legal land parcel boundaries with strict quality gating.

---

## Features

- **RTK Quality Gating** — Corners can only be captured when the fix is **RTK Fixed** (quality 4) and **HDOP ≤ 2.0**
- **Witness & Dispute Protection** — Mandatory Owner and Neighbour witness names for every corner peg
- **RoR Discrepancy Detection** — Compares field-measured area against the Record of Rights; flags differences > 5%
- **Stakeout Navigation** — Real-time distance and bearing to help relocate lost boundary stones
- **Offline First** — Fully functional without internet using a local Room database
- **GeoJSON Export** — RFC 7946 compliant with complete point metadata (HDOP, satellite count, timestamps, GCP tags, witness names)
- **Encrypted Sync** — Background HTTPS sync via WorkManager with encrypted credential storage
- **Material 3 Theming** — Dynamic Color (Android 12+) with light/dark mode support

---

## Hardware Setup

| Component | Details |
|-----------|---------|
| **Rover MCU** | ESP32 (or similar with Bluetooth Classic SPP) |
| **GNSS Module** | RTK-capable: u-blox ZED-F9P, Holybro H-RTK, ArduSimple, etc. |
| **Protocol** | NMEA GGA sentences over Bluetooth SPP (`00001101-0000-1000-8000-00805F9B34FB`) |
| **Pairing** | Pair the rover in Android Bluetooth settings before connecting in-app |

---

## Build Instructions

### Prerequisites
- **Android Studio** Koala 2024.1.1 or newer
- **JDK 17**
- Android device or emulator (API 26+ / Android 8.0+)

### Steps

```bash
# Clone the repository
git clone https://github.com/dikshadewangan21/BhuSeema.git
cd BhuSeema

# Build a debug APK
./gradlew assembleDebug

# Install on a connected device
./gradlew installDebug
```

Or open the project in Android Studio, let Gradle sync, and run on your device.

---

## Architecture

The app follows a single-module architecture with 6 Kotlin source files:

```
app/src/main/java/com/seemakit/field/
├── App.kt       — Application class, ViewModel, SyncWorker, MainActivity
├── Data.kt      — Room entities (Parcel, Corner), DAO, Database
├── Geo.kt       — NMEA parser, geodetic math (area, perimeter, distance, bearing), GeoJSON export
├── Rover.kt     — Bluetooth SPP connection to the RTK rover
├── Screens.kt   — All Compose UI screens (Parcels, Survey, Stakeout, Settings)
└── Theme.kt     — Material 3 theme with dynamic color support
```

### Data Flow

```
ESP32 Rover ──Bluetooth SPP──▶ Rover.kt (NMEA stream)
                                    │
                                    ▼
                              Nmea.parse() ──▶ Fix (lat, lon, quality, hdop, sats)
                                    │
                                    ▼
                              Screens.kt (quality gate check)
                                    │
                                    ▼
                              Data.kt (Room DB) ──▶ GeoJSON Export / HTTPS Sync
```

---

## Screens

| Screen | Purpose |
|--------|---------|
| **Parcels** | List all surveyed parcels, add new parcels, sync and manage |
| **Survey** | Connect rover, capture corners with witness names, view area summary |
| **Stakeout** | Navigate back to a saved corner using live distance & bearing |
| **Settings** | Configure HTTPS server URL and access token for sync |

---

## License

This project is provided as-is for educational and prototyping purposes.
