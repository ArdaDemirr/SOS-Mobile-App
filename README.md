# Tactical SOS Terminal 📟

An offline-first, defense-grade survival terminal and emergency communication network built natively for Android. 

Designed with a lightweight, high-contrast tactical interface, this system provides critical offline tools, automated biometric SOS triggers, and secure local data storage for environments with absolutely zero cellular infrastructure.

## 🚀 Core Systems & Features

* **Medical Dogtag (Local Database):** Secure local SQLite (`Room`) engine storing encrypted critical health data, blood type, and emergency mesh UUIDs.
* **Automated BioSensor:** Background telemetry service tracking G-force spikes to detect severe vehicle impacts or incapacitating falls.
* **Tactical Navigation:** GPS-independent tools including Dead Reckoning, internal Compass, and Celestial (Sun/Star) navigation algorithms.
* **Signal & Comms:** Automated SOS payload generation, Morse code broadcasting (Audio, Haptic, Visual Flash), and FM radio integration.
* **Offline Mesh Network (Phase 5 - Planned):** Device-to-device Delay Tolerant Network (DTN) for routing emergency packets without internet.

## 🛠 Tech Stack & Architecture

* **UI/UX:** 100% Kotlin & Jetpack Compose (Custom `PipAmber` / `PipBlack` declarative design system).
* **Local Storage:** `Room` Database (SQLite) for critical data, `SharedPreferences` for lightweight environment caching.
* **Concurrency:** Kotlin Coroutines & `Dispatchers.IO` for non-blocking database operations.
* **Background Processing:** Android Foreground Services & Broadcast Receivers for continuous sensor telemetry.

## 📱 Interface Preview
*(You can upload screenshots of your Dashboard and Dogtag screens here later by dragging and dropping them into the GitHub web editor!)*
