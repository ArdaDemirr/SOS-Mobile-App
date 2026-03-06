package com.example.sos

import android.content.Intent
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sos.CrashService
import com.example.sos.CrashManager

@Composable
fun BioScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    val gForce = CrashManager.currentG.floatValue
    val maxG = CrashManager.peakG.floatValue
    val isRunning = CrashManager.isServiceRunning.value

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PipBlack)
            .imePadding()
            .systemBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ScreenHeader(title = "Düşme Takibi", subtitle = "Arkaplanda durumunuzu izler", onBack = onBack)

        Spacer(modifier = Modifier.height(16.dp))

        // LIVE METER
        Box(
            modifier = Modifier
                .size(250.dp)
                .border(4.dp, PipAmber, RectangleShape)
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if(isRunning) "ACTIVE MONITOR" else "SYSTEM PAUSED", color = Color.Gray, fontFamily = FontFamily.Monospace)
                Text(
                    text = "%.1f G".format(gForce),
                    color = if (gForce > 5.0f) PipRed else PipAmber,
                    fontSize = 48.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Text("PEAK: %.1f G".format(maxG), color = PipAmber, fontFamily = FontFamily.Monospace)
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // SERVICE TOGGLE
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .background(if (isRunning) PipRed else PipAmber)
                .clickable {
                    val intent = Intent(context, CrashService::class.java)
                    if (isRunning) {
                        context.stopService(intent)
                    } else {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            context.startForegroundService(intent)
                        } else {
                            context.startService(intent)
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isRunning) "Stop Detection" else "Activate Crash/Fall Detection",
                color = if (isRunning) Color.White else PipBlack,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                fontSize = 18.sp
            )
        }
    }
}