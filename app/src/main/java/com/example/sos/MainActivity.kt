package com.example.sos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.* // Import State Management
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.ui.graphics.Color

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // --- PERMISSION CHECK: ALLOW DRAWING OVER OTHER APPS ---
        // This is required for the app to wake up from the background
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            startActivity(intent)
        }
        enableEdgeToEdge()

        setContent {
            // 1. Create a variable to hold the Current Screen
            // remember keywordu androidin bu variable'ı unutmamasını sağlıyor, tıpkı global bir variable gibi düşün
            var currentScreen by remember { mutableStateOf(Screen.Dashboard) }

            // --- ADD THIS BLOCK ---
            val isCrashDetected by CrashManager.isCrashDetected
            LaunchedEffect(isCrashDetected) {
                if (isCrashDetected) {
                    currentScreen = Screen.CrashCountdown
                }
            }

            // 2. Handle the "Physical Back Button" on the phone
            // If we are NOT on dashboard, back button brings us home.
            // 2. Handle the "Physical Back Button"
            BackHandler(enabled = currentScreen != Screen.Dashboard) {
                // PREVENT escaping the Countdown with the back button
                if (currentScreen != Screen.CrashCountdown) {
                    currentScreen = Screen.Dashboard
                }
            }

            // 3. The Switch Logic
            when (currentScreen) {
                Screen.Dashboard -> {
                    DashboardScreen(onNavigate = { newScreen -> currentScreen = newScreen })
                }

                // --- ADD THIS BLOCK ---
                Screen.CrashCountdown -> {
                    CrashCountdownScreen(
                        onCancel = {
                            // User is fine: Reset variables and go home
                            CrashManager.isCrashDetected.value = false
                            CrashManager.peakG.floatValue = 0f
                            currentScreen = Screen.Dashboard
                        },
                        onTimeout = {
                            // User passed out: Go to SOS (later we will auto-send SMS here)
                            CrashManager.isCrashDetected.value = false
                            currentScreen = Screen.Sos
                        }
                    )
                }

                Screen.Sos -> {
                    SosScreen(onBack = { currentScreen = Screen.Dashboard })
                }
                Screen.Map -> {
                    MapScreen(onBack = { currentScreen = Screen.Dashboard })
                }
                // In 'when(currentScreen)'
                Screen.Compass -> {
                    CompassScreen(onBack = { currentScreen = Screen.Dashboard })
                }
                Screen.Bio -> {
                    BioScreen(onBack = { currentScreen = Screen.Dashboard })
                }
                Screen.Morse -> {
                    MorseScreen(onBack = { currentScreen = Screen.Dashboard })
                }
                /*
                Screen.Barometer -> {
                    BarometerScreen(onBack = { currentScreen = Screen.Dashboard })
                }

                 */
                /*
                Screen.SkyScanner -> {
                    SkyScannerScreen(onBack = { currentScreen = Screen.Dashboard })
                }

                 */
                Screen.Forecaster -> {
                    ForecasterScreen(onBack = { currentScreen = Screen.Dashboard })
                }
                Screen.Ai -> {
                    AiScreen(onBack = { currentScreen = Screen.Dashboard })
                }
                Screen.Guide -> {
                    GuideScreen(onBack = { currentScreen = Screen.Dashboard })
                }
                Screen.Rad -> {
                    RadioScreen(onBack = { currentScreen = Screen.Dashboard })
                }
                Screen.Mesh -> {
                    BluetoothMeshScreen(onBack = { currentScreen = Screen.Dashboard })
                }
                /*
                Screen.Voice -> {
                    VoiceActivationScreen(onBack = { currentScreen = Screen.Dashboard })
                }
                 */
                // --- NEW SCREENS (Batch 2024) ---
                Screen.VibrationMorse -> {
                    VibrationMorseScreen(onBack = { currentScreen = Screen.Dashboard })
                }
                Screen.AudioMorse -> {
                    AudioMorseScreen(onBack = { currentScreen = Screen.Dashboard })
                }
                Screen.CameraMorse -> {
                    CameraMorseScreen(onBack = { currentScreen = Screen.Dashboard })
                }
                Screen.SosTemplates -> {
                    SosTemplatesScreen(onBack = { currentScreen = Screen.Dashboard })
                }
                Screen.DeadReckoning -> {
                    DeadReckoningScreen(onBack = { currentScreen = Screen.Dashboard })
                }
                Screen.CoordShare -> {
                    CoordinateShareScreen(onBack = { currentScreen = Screen.Dashboard })
                }
                Screen.Waypoints -> {
                    WaypointScreen(onBack = { currentScreen = Screen.Dashboard })
                }
                Screen.SunCalc -> {
                    SunCalcScreen(onBack = { currentScreen = Screen.Dashboard })
                }
                Screen.StarNav -> {
                    StarNavScreen(onBack = { currentScreen = Screen.Dashboard })
                }
                Screen.SurvivalCalc -> {
                    SurvivalCalcScreen(onBack = { currentScreen = Screen.Dashboard })
                }
                /*
                Screen.TtsSos -> {
                    TextToSpeechSosScreen(onBack = { currentScreen = Screen.Dashboard })
                }
                */
                Screen.QRSos -> {
                    QRCodeSosScreen(onBack = { currentScreen = Screen.Dashboard })
                }
                Screen.BatterySaver -> {
                    BatterySaverScreen(onBack = { currentScreen = Screen.Dashboard })
                }
                /*
                Screen.EmergencyContacts -> {
                    EmergencyContactsScreen(onBack = { currentScreen = Screen.Dashboard })
                }
                 */
                Screen.BiometricLock -> {
                    BiometricLockScreen(onBack = { currentScreen = Screen.Dashboard })
                }
                Screen.Checklist -> {
                    SurvivalChecklistScreen(onBack = { currentScreen = Screen.Dashboard })
                }
                Screen.Dogtag -> {
                    DogtagScreen(onBack = { currentScreen = Screen.Dashboard })
                }
                else -> {
                    DetailScreen(
                        screenName = currentScreen.name,
                        onBack = { currentScreen = Screen.Dashboard }
                    )
                }
            }
        }
    }
}