package com.example.sos

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.sos.database.AppDatabase
import com.example.sos.notused.BatterySaverScreen
import com.example.sos.notused.BiometricLockScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        NetworkStateManager.startObservingLocalNetwork(applicationContext)

        // --- PERMISSION CHECK: ALLOW DRAWING OVER OTHER APPS (For SOS Wakeup) ---
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            startActivity(intent)
        }

        enableEdgeToEdge()

        setContent {
            val context = LocalContext.current
            val db = remember { AppDatabase.getDatabase(context) }

            // --- STATE VARIABLES ---
            var currentScreen by remember { mutableStateOf(Screen.Dashboard) }

            var myUuid by remember { mutableStateOf("UNKNOWN") }
            var targetChatUuid by remember { mutableStateOf("") }
            var targetChatName by remember { mutableStateOf("") }

            // --- FETCH DOGTAG UUID ---
            LaunchedEffect(Unit) {
                withContext(Dispatchers.IO) {
                    val myDogtag = db.dogtagDao().getDogtag()
                    if (myDogtag != null) {
                        myUuid = myDogtag.userUuid
                    }
                    // --- THE NEW LINE ---
                    // "Load the gun" - Give the UUID to the network interceptor
                    RetrofitInstance.currentUserUuid = myDogtag?.userUuid
                }
            }

            // --- NEARBY CONNECTIONS & MESH SERVICE PERMISSIONS ---
            val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_ADVERTISE,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.NEARBY_WIFI_DEVICES
                )
            } else {
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_WIFI_STATE,
                    Manifest.permission.CHANGE_WIFI_STATE
                )
            }

            val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms ->
                val allGranted = perms.values.all { it }
                if (allGranted) {
                    // Start the background mesh mule service
                    val serviceIntent = Intent(context, MeshService::class.java)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        ContextCompat.startForegroundService(context, serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                }
            }

            LaunchedEffect(Unit) {
                permissionLauncher.launch(permissions)
            }

            // --- CRASH DETECTION LOGIC ---
            val isCrashDetected by CrashManager.isCrashDetected
            LaunchedEffect(isCrashDetected) {
                if (isCrashDetected) {
                    currentScreen = Screen.CrashCountdown
                }
            }

            // --- HARDWARE BACK BUTTON LOGIC ---
            BackHandler(enabled = currentScreen != Screen.Dashboard) {
                if (currentScreen != Screen.CrashCountdown) {
                    currentScreen = when (currentScreen) {
                        Screen.ChatScreen -> Screen.ChatHub // Go back to Inbox from Chat
                        else -> Screen.Dashboard
                    }
                }
            }

            // --- MAIN NAVIGATION ROUTER ---
            when (currentScreen) {
                Screen.Dashboard -> DashboardScreen(onNavigate = { newScreen -> currentScreen = newScreen })

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

                // MESH button now redirects to the Inbox for the seamless SMS experience
                Screen.Mesh -> {
                    ChatHubScreen(
                        myUuid = myUuid,
                        onConversationClick = { uuid, name ->
                            targetChatUuid = uuid
                            targetChatName = name
                            currentScreen = Screen.ChatScreen
                        },
                        onBack = { currentScreen = Screen.Dashboard }
                    )
                }

                Screen.ChatHub -> {
                    ChatHubScreen(
                        myUuid = myUuid,
                        onConversationClick = { uuid, name ->
                            targetChatUuid = uuid
                            targetChatName = name
                            currentScreen = Screen.ChatScreen
                        },
                        onBack = { currentScreen = Screen.Dashboard }
                    )
                }

                Screen.ChatScreen -> {
                    TacticalChatScreen(
                        myUuid = myUuid,
                        targetUuid = targetChatUuid,
                        targetName = targetChatName,
                        onBack = { currentScreen = Screen.ChatHub }
                    )
                }

                Screen.Contact -> {
                    ContactsScreen(
                        onBack = { currentScreen = Screen.Dashboard },
                        onChat = { uuid, name ->
                            targetChatUuid = uuid
                            targetChatName = name
                            currentScreen = Screen.ChatScreen
                        }
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