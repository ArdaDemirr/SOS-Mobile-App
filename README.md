# 📟 Tactical SOS Terminal

An offline-first, defense-grade emergency communication network and survival suite built natively for Android. 

**The Goal:** Designed for environments with absolutely zero cellular infrastructure. When the power grid fails and towers go dark, this system turns every phone into a lighthouse and every person into a relay node. Infrastructure is fragile; human connection is not.

## 🚀 Core Systems

* **Mesh Network & "Mule" Relay:** A decentralized Delay-Tolerant Network (DTN). Devices act as "data mules," securely storing encrypted SOS packets offline and passing them device-to-device. When any node finally reaches an internet connection, it automatically syncs the data with the HQ Server. 
* **Tactical SOS & Mapping:**
    * **Public Flares:** Broadcasts an emergency signal containing vital Dogtag data (blood type, allergies) to anyone in range, dropping a visible pin on their local offline map.
    * **Private Handshakes:** Encrypted SOS payloads routed exclusively to pre-selected emergency contacts. 
* **Automated Crash Sensor:** A background telemetry service that tracks G-force spikes. Upon detecting a severe vehicle impact or fall, it initiates a dead-man's switch countdown before auto-broadcasting an SOS.
* **Multisensory Morse Code:** Hardware-level communication for when RF bands fail completely. Features high-frequency optical flash generation, 3000Hz acoustic distress beacons, and silent haptic (vibration) signaling.

## 🛠 Tech Stack & Architecture

* **UI/UX:** 100% Kotlin & Jetpack Compose featuring a custom, high-contrast tactical FUI design system.
* **Local Persistence:** Room Database (SQLite) ensures your vital medical dogtag and pending mesh packets survive process kills and device reboots.
* **Networking:** Retrofit2 for idempotent server handshakes (handling "Synchronization Storms" without crashing).
* **Concurrency:** Kotlin Coroutines & `Dispatchers.IO` for continuous, non-blocking background sensor telemetry.
