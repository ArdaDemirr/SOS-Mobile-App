package com.example.sos

import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf

// This is a "Singleton" - A global object that lives as long as the app is alive.
// Both the Service (Background) and UI (Screen) read/write to this same object.
object CrashManager {

    // 1. Service Status
    // Tells the UI if the "Bodyguard" is currently active or sleeping.
    var isServiceRunning = mutableStateOf(false)

    // 2. Live Sensor Data
    // The Service writes these values 50 times a second.
    // The UI observes them to draw the numbers.
    var currentG = mutableFloatStateOf(1.0f)
    var peakG = mutableFloatStateOf(1.0f)

    // 3. Alert State
    // If this becomes true, the UI turns RED.
    var isCrashDetected = mutableStateOf(false)

    // 4. Hardware Specs
    // We store the sensor limit here so the UI can display it.
    var sensorLimit = mutableFloatStateOf(0f)
}