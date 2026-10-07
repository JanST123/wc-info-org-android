# WC-info Android App

Native Android implementation of **WC-info**, replicated with high fidelity from the iOS Swift codebase and the [Web App Rebuild Specification](rebuild_specification/WEB_APP_REBUILD_SPECIFICATION.md).

Built using **Kotlin**, **Jetpack Compose**, and **Material 3**.

---

## 📱 Features

- **Home & Places Search (`HomeScreen`)**:
  - Lavender background hero layout with centered branding.
  - Search bar with live Google Places Autocomplete dropdown.
  - "In der Nähe" (Nearby) GPS button with location permission handling.
- **Interactive Map & Results List (`ResultsScreen`)**:
  - Responsive layout: Tablet / Desktop Landscape two-pane split and Mobile Portrait split.
  - Google Maps Compose with custom category markers (Accessible, Unisex, Gender-separated, Temporarily Closed).
  - Floating controls: Satellite layer toggle & My Location FAB.
  - Real-time viewport bounds fetching (`GET /toilets/bounds/...`) on map camera idle.
  - Rich filter summary banner and filter configuration modal sheet.
- **Toilet Details (`DetailScreen`)**:
  - Live status indicator (24/7 detection, open countdown e.g. *"schließt in 25 Min."*, open at time).
  - Comprehensive feature matrix (Wheelchair access, Euro-key requirement, Changing table, Gender separation, Storage space).
  - Photo gallery with horizontal scroll and "+ Foto" upload integration.
  - Weekly opening hours table (Monday – Sunday with today highlighted).
  - Direct actions: In-app Compass Navigation, external navigation (Google Maps), edit suggestions, problem reporting, and system share sheet.
- **Sensor-Driven Compass Navigation (`CompassScreen`)**:
  - Precision rotating compass dial powered by device orientation sensors (`Sensor.TYPE_ROTATION_VECTOR` with low-pass smoothing).
  - Gradient destination needle pointing towards target bearing.
  - Target alignment glow when facing within `±15°`.
  - Live distance countdown and German direction prompts (*"Geradeaus"*, *"Halb rechts"*, *"Rechts"*, etc.).
  - Target arrival celebration (< 10 m).
- **16-Step Crowdsourced Creation Wizard (`CreateToiletScreen`)**:
  - Conversational step-by-step contribution flow.
  - Step 2: Interactive Google Map pin placement.
  - **Progressive Baseline Save**: Persists baseline payload at Step 6 to `POST /toilet/add`, then progressively patches attributes in steps 7–16 (`PATCH /toilet/{id}/update`).
  - Step 16: Celebration view with confirmation.
- **Edit & Feedback Workflows**:
  - `UpdateToiletScreen`: Suggest updates for existing toilets.
  - `FeedbackScreen`: Report closed, broken, or inaccurate listings.
- **Deep Linking**:
  - `wcinfo://urgent-navigate`: Triggers 500m public fallback urgent navigation and launches compass.
  - `https://wc-info.org/Toilets/...`: Opens specific toilet detail view.

---

## 🛠️ Architecture & Tech Stack

| Layer | Technology |
| :--- | :--- |
| **Language** | Kotlin 2.1 |
| **UI Toolkit** | Jetpack Compose + Material 3 |
| **Architecture** | MVVM with Unidirectional Data Flow (`StateFlow`, Coroutines) |
| **Maps** | Google Maps Compose (`com.google.maps.android:maps-compose`) |
| **Places & Location** | Google Places SDK for Android & Fused Location Provider |
| **Networking** | Retrofit 2 + OkHttp 4 + Kotlinx Serialization |
| **Image Loading** | Coil 2 |
| **Sensors** | Android SensorManager (`ROTATION_VECTOR`, `ACCELEROMETER`, `MAGNETIC_FIELD`) |
| **Internationalization** | German (`res/values-de`) and English (`res/values`) |

---

## 🚀 Getting Started

1. Open this directory in **Android Studio**.
2. Android Studio will automatically recognize the Gradle project and sync dependencies using `gradle/libs.versions.toml`.
3. Select your target device or emulator (Android 8.0+ / API 26+) and click **Run** (`▶`).

---

## 📸 Play Store Screenshots

Automated screenshot capture is available via `scripts/capture_screenshots.py`. It uses ADB to set Demo Mode (clean status bar, standard time 09:41), navigates using deep links, mocks locations, and organizes screenshots into German and English sets for both Phone (Portrait) and Tablet (Landscape).

```bash
# Capture all screenshots (Phone + Tablet, DE + EN)
python3 scripts/capture_screenshots.py

# Phone only or Tablet only
python3 scripts/capture_screenshots.py --phone-only
python3 scripts/capture_screenshots.py --tablet-only

# Specific locale
python3 scripts/capture_screenshots.py --locale de-DE
python3 scripts/capture_screenshots.py --locale en-US
```

