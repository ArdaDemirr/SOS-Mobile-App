package com.example.sos.notused

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.sos.PipAmber
import com.example.sos.PipBlack
import com.example.sos.PipGreen
import com.example.sos.PipRed
import com.example.sos.VoiceActivationService

@Composable
fun VoiceActivationScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    var isServiceActive by remember { mutableStateOf(false) }
    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    // Observe shared state from the service
    val isListening by VoiceActivationService.Companion.isListening
    val lastDetected by VoiceActivationService.Companion.lastDetectedText
    val history = VoiceActivationService.Companion.detectionHistory

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasAudioPermission = granted
        if (granted && !isServiceActive) {
            startVoiceService(context)
            isServiceActive = true
        }
    }

    fun toggleService() {
        if (!hasAudioPermission) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }
        if (isServiceActive) {
            context.stopService(Intent(context, VoiceActivationService::class.java))
            isServiceActive = false
        } else {
            startVoiceService(context)
            isServiceActive = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PipBlack)
            .systemBarsPadding()
    ) {
        // HEADER
        VoiceHeader(onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- MIC VISUALIZER + TOGGLE ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isServiceActive && isListening) {
                    // Pulse animation rings
                    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                    val scale1 by infiniteTransition.animateFloat(
                        initialValue = 1f, targetValue = 2.2f,
                        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Restart),
                        label = "s1"
                    )
                    val alpha1 by infiniteTransition.animateFloat(
                        initialValue = 0.5f, targetValue = 0f,
                        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Restart),
                        label = "a1"
                    )
                    val scale2 by infiniteTransition.animateFloat(
                        initialValue = 1f, targetValue = 2.2f,
                        animationSpec = infiniteRepeatable(tween(1200, 400, easing = LinearEasing), RepeatMode.Restart),
                        label = "s2"
                    )
                    val alpha2 by infiniteTransition.animateFloat(
                        initialValue = 0.5f, targetValue = 0f,
                        animationSpec = infiniteRepeatable(tween(1200, 400, easing = LinearEasing), RepeatMode.Restart),
                        label = "a2"
                    )
                    Box(modifier = Modifier.size(80.dp).scale(scale1).alpha(alpha1).background(
                        PipAmber.copy(alpha = 0.3f), CircleShape))
                    Box(modifier = Modifier.size(80.dp).scale(scale2).alpha(alpha2).background(
                        PipAmber.copy(alpha = 0.3f), CircleShape))
                }

                // Center mic button
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            if (isServiceActive) (if (isListening) PipRed else PipAmber) else PipAmber.copy(alpha = 0.3f),
                            CircleShape
                        )
                        .border(2.dp, PipAmber, CircleShape)
                        .clickable { toggleService() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (isServiceActive) (if (isListening) "●" else "◌") else "MİC",
                        color = PipBlack,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = if (isServiceActive) 28.sp else 14.sp
                    )
                }
            }

            // Status text
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    when {
                        !hasAudioPermission -> "MİKROFON İZNİ GEREKLİ — DOKUNUN"
                        !isServiceActive -> "DEVRE DIŞI — AKTİFLEŞTİRMEK İÇİN DOKUNUN"
                        isListening -> "DİNLENİYOR..."
                        else -> "HAZIR — YENİDEN BAŞLATILIYOR..."
                    },
                    color = when {
                        !isServiceActive -> PipAmber.copy(alpha = 0.5f)
                        isListening -> PipGreen
                        else -> PipAmber
                    },
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            // Trigger keywords info
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, PipAmber.copy(alpha = 0.5f), RectangleShape)
                    .background(PipAmber.copy(alpha = 0.05f))
                    .padding(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("TETİKLEYİCİ KELİMELER:", color = PipAmber, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    listOf("\"Kanki SOS\"", "\"Kanki\"", "\"SOS\"", "\"Yardım\"", "\"İmdat\"").forEach { kw ->
                        Text("  ► $kw", color = PipAmber.copy(alpha = 0.7f), fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    }
                }
            }

            // Last detected
            if (lastDetected.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, if (lastDetected.contains("TETİKLENDİ")) PipRed else PipAmber.copy(alpha = 0.4f), RectangleShape)
                        .background(if (lastDetected.contains("TETİKLENDİ")) PipRed.copy(alpha = 0.1f) else PipBlack)
                        .padding(10.dp)
                ) {
                    Column {
                        Text("SON ALGILANAN:", color = PipAmber.copy(alpha = 0.6f), fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                        Text(lastDetected, color = if (lastDetected.contains("TETİKLENDİ")) PipRed else PipAmber, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }

            Divider(color = PipAmber.copy(alpha = 0.3f))
            Text("ALGILAMA GEÇMİŞİ", color = PipAmber.copy(alpha = 0.5f), fontFamily = FontFamily.Monospace, fontSize = 10.sp)

            // History
            if (history.isEmpty()) {
                Text(
                    "henüz algılama yok",
                    color = PipAmber.copy(alpha = 0.3f),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxHeight()) {
                    items(history.reversed()) { item ->
                        Text(
                            "  › $item",
                            color = if (item.contains("TETİKLENDİ")) PipRed else PipAmber.copy(alpha = 0.6f),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VoiceHeader(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .padding(top = 40.dp, bottom = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(56.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(PipAmber, RoundedCornerShape(6.dp))
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = null, tint = PipBlack)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text("VOICE ACT", color = PipAmber, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                Text("SES İLE AKTİVASYON", color = PipAmber.copy(alpha = 0.6f), fontFamily = FontFamily.Monospace, fontSize = 10.sp)
            }
        }
        Divider(color = PipAmber, thickness = 2.dp, modifier = Modifier.padding(top = 6.dp))
    }
}

private fun startVoiceService(context: Context) {
    val intent = Intent(context, VoiceActivationService::class.java)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(intent)
    } else {
        context.startService(intent)
    }
}
