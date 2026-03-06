package com.example.sos

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun CrashCountdownScreen(onCancel: () -> Unit, onTimeout: () -> Unit) {
    // --- STATE ---
    var timeLeft by remember { mutableIntStateOf(30) }
    var isRedPhase by remember { mutableStateOf(true) }

    // --- AUDIO ENGINE (Safely Managed) ---
    // We use 'remember' to create it once.
    val toneGen = remember { ToneGenerator(AudioManager.STREAM_ALARM, 100) }

    // This block runs AUTOMATICALLY when the screen is removed (navigated away)
    DisposableEffect(Unit) {
        onDispose {
            toneGen.release() // <--- THIS GUARANTEES SILENCE
        }
    }

    // --- TIMER & SOUND LOOP ---
    LaunchedEffect(Unit) {
        while (timeLeft > 0 && isActive) {
            // Play Beep
            try {
                toneGen.startTone(ToneGenerator.TONE_CDMA_PIP, 150)
            } catch (e: Exception) { /* Ignore if released */ }

            delay(1000L)
            if (!isActive) break // Stop if user clicked cancel during delay
            timeLeft--
        }

        // TIME IS UP!
        if (isActive) {
            try {
                toneGen.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 1000)
            } catch (e: Exception) { }
            onTimeout()
        }
    }

    // --- FLASHING LIGHTS ---
    LaunchedEffect(Unit) {
        while (timeLeft > 0 && isActive) {
            isRedPhase = !isRedPhase
            delay(500L)
        }
    }

    // Colors
    val backgroundColor by animateColorAsState(
        targetValue = if (isRedPhase) PipRed else Color.Black,
        animationSpec = tween(500), label = "Flash"
    )
    val textColor = if (isRedPhase) Color.Black else PipRed

    // --- UI ---
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .systemBarsPadding() // Safety
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Text(
            text = "IMPACT DETECTED",
            color = textColor,
            fontSize = 32.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Black
        )

        Spacer(modifier = Modifier.height(40.dp))

        Box(
            modifier = Modifier
                .size(200.dp)
                .border(8.dp, textColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$timeLeft",
                color = textColor,
                fontSize = 100.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "SENDING SOS...",
            color = textColor,
            fontSize = 20.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(60.dp))

        // "I AM FINE" BUTTON
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .background(Color.White)
                .clickable {
                    // Just call onCancel().
                    // The 'DisposableEffect' above will handle killing the sound automatically.
                    onCancel()
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "I AM FINE - CANCEL",
                color = Color.Black,
                fontSize = 24.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }
    }
}