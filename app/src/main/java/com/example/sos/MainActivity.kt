package com.example.sos

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.example.sos.database.AppDatabase
import com.example.sos.notused.BatterySaverScreen
import com.example.sos.notused.BiometricLockScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // --- PERMISSION CHECK: ALLOW DRAWING OVER OTHER APPS ---
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            startActivity(intent)
        }
        enableEdgeToEdge()

        setContent {
            val context = LocalContext.current
            val db = remember { AppDatabase.getDatabase(context) }

            // 1. Create a variable to hold the Current Screen
            var currentScreen by remember { mutableStateOf(Screen.Dashboard) }

            // 2. ADD THESE VARIABLES: To hold Chat Data
            var savedUserUuid by remember { mutableStateOf("UNKNOWN_USER") }
            var targetChatUuid by remember { mutableStateOf("") }
            var targetChatName by remember { mutableStateOf("") }

            // 3. FETCH DOGTAG UUID: Get who the user is from the database automatically
            LaunchedEffect(Unit) {
                withContext(Dispatchers.IO) {
                    val myDogtag = db.dogtagDao().getDogtag()
                    if (myDogtag != null) {
                        savedUserUuid = myDogtag.userUuid
                    }
                }
            }

            // --- CRASH DETECTION LOGIC ---
            val isCrashDetected by CrashManager.isCrashDetected
            LaunchedEffect(isCrashDetected) {
                if (isCrashDetected) {
                    currentScreen = Screen.CrashCountdown
                }
            }

            // Handle the "Physical Back Button" on the phone
            BackHandler(enabled = currentScreen != Screen.Dashboard) {
                // PREVENT escaping the Countdown with the back button
                if (currentScreen != Screen.CrashCountdown) {
                    currentScreen = Screen.Dashboard
                }
            }

            // The Switch Logic
            when (currentScreen) {
                Screen.Dashboard -> {
                    DashboardScreen(onNavigate = { newScreen -> currentScreen = newScreen })
                }

                Screen.CrashCountdown -> {
                    CrashCountdownScreen(
                        onCancel = {
                            CrashManager.isCrashDetected.value = false
                            CrashManager.peakG.floatValue = 0f
                            currentScreen = Screen.Dashboard
                        },
                        onTimeout = {
                            CrashManager.isCrashDetected.value = false
                            currentScreen = Screen.Sos
                        }
                    )
                }

                Screen.Sos -> SosScreen(onBack = { currentScreen = Screen.Dashboard })
                Screen.Map -> MapScreen(onBack = { currentScreen = Screen.Dashboard })
                Screen.Compass -> CompassScreen(onBack = { currentScreen = Screen.Dashboard })
                Screen.Bio -> BioScreen(onBack = { currentScreen = Screen.Dashboard })
                Screen.Morse -> MorseScreen(onBack = { currentScreen = Screen.Dashboard })
                Screen.Forecaster -> ForecasterScreen(onBack = { currentScreen = Screen.Dashboard })
                Screen.Ai -> AiScreen(onBack = { currentScreen = Screen.Dashboard })
                Screen.Guide -> GuideScreen(onBack = { currentScreen = Screen.Dashboard })
                Screen.Rad -> RadioScreen(onBack = { currentScreen = Screen.Dashboard })

                // ==========================================
                // MESSAGING & MESH ROUTING
                // ==========================================

                // The SMS Inbox Screen (Replaces old chat logic)
                Screen.ChatHub -> {
                    ChatHubScreen(
                        myUuid = savedUserUuid,
                        onConversationClick = { uuid, name ->
                            targetChatUuid = uuid
                            targetChatName = name
                            currentScreen = Screen.ChatScreen // Go to the actual chat
                        },
                        onBack = { currentScreen = Screen.Dashboard }
                    )
                }

                // The Actual Messaging Screen
                Screen.ChatScreen -> {
                    TacticalChatScreen(
                        myUuid = savedUserUuid,
                        targetUuid = targetChatUuid,
                        targetName = targetChatName,
                        onBack = { currentScreen = Screen.ChatHub } // Go back to the Inbox
                    )
                }

                // ==========================================

                Screen.VibrationMorse -> VibrationMorseScreen(onBack = { currentScreen = Screen.Dashboard })
                Screen.AudioMorse -> AudioMorseScreen(onBack = { currentScreen = Screen.Dashboard })
                Screen.CameraMorse -> CameraMorseScreen(onBack = { currentScreen = Screen.Dashboard })
                Screen.SosTemplates -> SosTemplatesScreen(onBack = { currentScreen = Screen.Dashboard })
                Screen.DeadReckoning -> DeadReckoningScreen(onBack = { currentScreen = Screen.Dashboard })
                Screen.CoordShare -> CoordinateShareScreen(onBack = { currentScreen = Screen.Dashboard })
                Screen.Waypoints -> WaypointScreen(onBack = { currentScreen = Screen.Dashboard })
                Screen.SunCalc -> SunCalcScreen(onBack = { currentScreen = Screen.Dashboard })
                Screen.StarNav -> StarNavScreen(onBack = { currentScreen = Screen.Dashboard })
                Screen.SurvivalCalc -> SurvivalCalcScreen(onBack = { currentScreen = Screen.Dashboard })
                Screen.QRSos -> QRCodeSosScreen(onBack = { currentScreen = Screen.Dashboard })
                Screen.BatterySaver -> BatterySaverScreen(onBack = { currentScreen = Screen.Dashboard })
                Screen.BiometricLock -> BiometricLockScreen(onBack = { currentScreen = Screen.Dashboard })
                Screen.Checklist -> SurvivalChecklistScreen(onBack = { currentScreen = Screen.Dashboard })
                Screen.Dogtag -> DogtagScreen(onBack = { currentScreen = Screen.Dashboard })

                Screen.Contact -> {
                    ContactsScreen(
                        onBack = { currentScreen = Screen.Dashboard },
                        onChat = { uuid, name ->
                            // Allows you to jump straight into a chat from the Contacts page
                            targetChatUuid = uuid
                            targetChatName = name
                            currentScreen = Screen.ChatScreen
                        }
                    )
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