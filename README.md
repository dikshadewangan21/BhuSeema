# BhuSeema (भूसीमा)

[![Android](https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white)](https://www.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.20-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20(M3)-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![MinSdk](https://img.shields.io/badge/MinSdk-26%20(Android%208.0)-brightgreen)](https://developer.android.com/about/versions/oreo)
[![TargetSdk](https://img.shields.io/badge/TargetSdk-35%20(Android%2015)-blue)](https://developer.android.com/about/versions/15)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

> **Centimeter-accurate cadastral land surveying and boundary demarcation app for Android.**  
> *स्मार्ट, पारदर्शी और विवाद-मुक्त ज़मीन सीमांकन।*

---

## 📌 Overview

**BhuSeema (भूसीमा)** is an offline-first Android application designed for legal land parcel surveying, cadastral boundary verification, and agricultural land administration. 

By interfacing directly with low-cost hardware rovers (**ESP32 + multi-band RTK GNSS** like the u-blox ZED-F9P) over **Bluetooth Classic SPP**, BhuSeema delivers **$\pm 1\text{ to }2\text{ cm}$ positioning precision** at **1/50th the cost** of commercial Total Stations (~₹25,000 vs ₹15,00,000).

The application is tailored for **Indian land revenue workflows**, incorporating legal Record of Rights (RoR / खतौनी / जमाबंदी) verification, automated polygon area compliance, and **mandatory dual-witness demarcation** to make field surveys dispute-proof.

---

## ⚖️ Why BhuSeema? (The Problem It Solves)

- **66% of Indian Court Litigations are Land Disputes**: A landmark study by NITI Aayog and CPR confirmed that boundary disputes comprise over two-thirds of all pending civil cases in India, taking an average of 20 years to resolve.
- **Manual Measurement Errors**: Surveys still rely on British-era iron chains (ज़रब / Gunter chain) and cloth tapes, leading to 1–2 meter discrepancies due to thermal expansion, terrain slope, and human pacing.
- **Standard GPS is Inadequate**: Consumer smartphone GPS is only accurate to 3–5 meters—completely useless for establishing legal property boundaries.
- **Cost Barrier**: Enterprise survey equipment (Trimble, Leica Total Stations) costs ₹12–18 Lakhs ($15,000+), making it inaccessible to Gram Panchayats, village surveyors, and local farmers.
- **No Witness Accountability**: Traditional demarcations lack recorded neighbour consent, leading to immediate boundary clashes post-survey.

**BhuSeema solves this by delivering centimeter precision, legal witness protection, and real-time revenue record auditing in an accessible mobile tool.**

---

## ✨ Key Features

### 🗺️ 1. 2D Interactive Cadastral Map Visualizer
- **Dynamic Vector Projection**: Automatically normalizes and plots boundary coordinates on a calibrated survey grid.
- **Real Metric Dimensions**: Displays real-time distances in meters (e.g. `50.9m`, `50.1m`) along each boundary edge.
- **Numbered Peg Indicators**: Distinct circular markers (`#1`, `#2`, `#3`, `#4`) with special highlights for Ground Control Points (GCP).
- **Surveyor North Compass**: Prominent directional orientation arrow.
- **Live Rover Marker**: Pulsating GPS indicator showing real-time rover location relative to the plot.

### 🛡️ 2. Strict RTK Quality Gating
- A boundary corner peg can **only** be saved if the GNSS solution is **RTK Fixed** (`Quality == 4`) and **HDOP $\le$ 2.0**.
- Low-precision GPS or float fixes are blocked from saving, ensuring every recorded peg is legally defensible in revenue courts.

### ✍️ 3. Dispute-Proof Dual-Witness Demarcation (पंचनामा)
- Mandatory recording of both the **Land Owner** and the **Adjacent Neighbour Witness** before any corner peg can be captured.
- Eliminates post-survey boundary contestations by embedding recorded witness consent into the cadastral record.

### 📊 4. Automated Legal RoR Compliance Audit
- Calculates the true polygon surface area using the **Gauss Shoelace Formula** projected onto local planar coordinates.
- Automatically audits measured area against the legal **Record of Rights (RoR)**:
  - **$\le 5\%$ variance**: Displays a verified compliance badge.
  - **$> 5\%$ variance**: Instantly triggers an alert flag recommending Revenue Officer inspection or corner re-check.

### 🎯 5. Tactical Stakeout Radar HUD (निशानदेही)
- Built-in navigation HUD to relocate buried, damaged, or disputed boundary stones.
- Concentric tactical radar rings ($1\text{m}, 5\text{m}, 10\text{m}$) and an azimuth pointer needle.
- Shifts to **vibrant green with instant target lock** when the surveyor is within **$0.5\text{ meters}$** (`🎯 TARGET LOCKED`).

### 🛰️ 6. Virtual Rover Simulator (Demo Mode)
- Built-in simulation engine that generates realistic RTK Fixed NMEA streams with realistic $\pm 1-2\text{ cm}$ jitter.
- Enables complete product demonstrations, stakeholder pitches, and field training without requiring physical GNSS hardware.

### 📴 7. 100% Offline-First Architecture & Encrypted Sync
- Operates entirely offline in remote rural farmlands using a local **AndroidX Room** database (`seema.db`).
- Background sync via **Android WorkManager** with AES-256 GCM encrypted credentials via `EncryptedSharedPreferences`.
- One-tap standard **RFC 7946 GeoJSON** export compatible with **QGIS**, **BhuNaksha**, and government land portals.

---

## 🏗️ System Architecture & Data Flow

```
┌─────────────────────────────────┐
│     ESP32 + RTK GNSS Rover      │ (u-blox ZED-F9P Multi-band)
└───────────────┬─────────────────┘
                │ Bluetooth Classic SPP (NMEA GGA Sentences at 5Hz)
                ▼
┌─────────────────────────────────┐
│       Rover.kt / Nmea.kt        │ (NMEA Parsing: Lat, Lon, Quality, HDOP, Sats)
└───────────────┬─────────────────┘
                │
                ├──▶ Quality Gate Check (Quality == 4 [RTK Fixed] & HDOP <= 2.0)
                │
                ▼
┌─────────────────────────────────┐
│       Screens.kt / Canvas       │
│  - 2D Cadastral Polygon Map     │ ◀── Live Rover Pin & Distance Annotations
│  - Dual-Witness Demarcation     │ ◀── Owner & Neighbour Signatures
│  - Stakeout Radar HUD           │ ◀── Real-time Distance & Forward Azimuth
└───────────────┬─────────────────┘
                │
                ▼
┌─────────────────────────────────┐
│        Room Database (Data.kt)  │ (Local Offline SQLite: Parcels & Corners)
└───────────────┬─────────────────┘
                │
        ┌───────┴───────────────────┐
        ▼                           ▼
┌───────────────┐           ┌──────────────────┐
│ GeoJSON Share │           │ Encrypted Sync   │ (WorkManager -> Central Server)
└───────────────┘           └──────────────────┘
```

---

## 📁 Codebase Structure

```
app/src/main/java/com/seemakit/field/
├── App.kt           # Application setup, ViewModel (VM), SyncWorker, and MainActivity
├── CanvasView.kt    # Interactive 2D Cadastral Map Visualizer (polygon, pins, dimensions, compass)
├── Data.kt          # Room Entities (Parcel, Corner), DAO (SurveyDao), and Database (Db)
├── Geo.kt           # NMEA GGA parser, Shoelace area, perimeter, Haversine distance, forward bearing, GeoJSON
├── RadarView.kt     # Tactical Stakeout Radar HUD with range rings and target lock
├── Rover.kt         # Bluetooth SPP connection + Virtual Rover Simulator
├── Screens.kt       # Compose Material 3 screens: Parcels, AddParcel, SurveyScreen, StakeoutScreen, Settings
└── Theme.kt         # Material 3 dynamic color palette with earth-green cadastral theming
```

---

## 🛠️ Hardware Setup & Bill of Materials (BOM)

BhuSeema is designed to interface with low-cost, open-hardware RTK rovers:

| Component | Specification | Approx. Cost |
| :--- | :--- | :--- |
| **RTK GNSS Receiver** | u-blox ZED-F9P (Multi-band L1/L2/E5b) | ~₹18,500 ($220) |
| **Microcontroller** | ESP32-WROOM-32 (Bluetooth Classic SPP) | ~₹350 ($4) |
| **GNSS Antenna** | Multi-band survey helical or patch antenna | ~₹2,800 ($35) |
| **Battery & Power** | 3.7V 18650 Li-ion battery + TP4056 charge/boost module | ~₹650 ($8) |
| **Enclosure & Pole** | Weatherproof 3D-printed enclosure + 2m carbon fiber survey pole | ~₹1,800 ($22) |
| **Total Hardware Cost** | | **~₹24,100 ($290)** |

### Rover Connection
1. Power on the ESP32 RTK rover.
2. In your Android phone's Bluetooth settings, pair with the rover.
3. Open BhuSeema $\rightarrow$ open any survey $\rightarrow$ tap **Hardware** under the rover card $\rightarrow$ select your paired device.
4. The rover streams standard NMEA GGA sentences over SPP UUID `00001101-0000-1000-8000-00805F9B34FB`.

*(Alternatively, tap **Simulate Rover** inside the app to test everything without any physical hardware.)*

---

## 🚀 Getting Started & Build Instructions

### Prerequisites
- **Android Studio**: Koala (2024.1.1) or newer
- **JDK**: Java Development Kit 17
- **Android Device / Emulator**: Running Android 8.0+ (API 26+)

### Build via Command Line

```bash
# Clone the repository
git clone https://github.com/dikshadewangan21/BhuSeema.git
cd BhuSeema

# Build Debug APK using the included Gradle wrapper
./gradlew assembleDebug

# Install directly on a connected device/emulator
./gradlew installDebug
```

The generated APK will be available at:  
`app/build/outputs/apk/debug/app-debug.apk`

---

## 🏛️ Alignment with Government Initiatives

BhuSeema is architected to integrate seamlessly with digital land administration initiatives in India:

- **PM SVAMITVA Scheme**: Supports rapid cadastral mapping of rural inhabited (*Abadi*) areas.
- **DILRMP (Digital India Land Records Modernization Programme)**: Facilitates boundary digitization and mutation verifications.
- **BhuNaksha & State Land Portals**: RFC 7946 GeoJSON export enables direct import into BhuNaksha, Bhulekh, AnyRoR, Dharani, and QGIS.

---

## 📄 License

This project is licensed under the [MIT License](LICENSE).
