package com.example.sos.notused

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
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
import com.example.sos.PipAmber
import com.example.sos.PipBlack
import com.example.sos.PipGreen
import com.example.sos.PipRed

@Composable
fun BarometerScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    // --- STATE ---
    var currentTemp by remember { mutableFloatStateOf(0f) }
    var minTemp by remember { mutableFloatStateOf(999f) }
    var maxTemp by remember { mutableFloatStateOf(-999f) }
    var readCount by remember { mutableIntStateOf(0) }
    var tempSum by remember { mutableFloatStateOf(0f) }

    // --- SENSOR ENGINE (Battery) ---
    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                val tempInt = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
                // Battery temp is returned in tenths of a degree Celsius (e.g., 300 = 30.0C)
                val tempC = tempInt / 10f

                if (tempC > 0) {
                    currentTemp = tempC

                    // Update Stats
                    if (tempC < minTemp) minTemp = tempC
                    if (tempC > maxTemp) maxTemp = tempC

                    tempSum += tempC
                    readCount++
                }
            }
        }

        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        context.registerReceiver(receiver, filter)

        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    val avgTemp = if (readCount > 0) tempSum / readCount else 0f

    // --- LAYOUT ---
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PipBlack)
            .systemBarsPadding()
            .imePadding()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // HEADER
        Row(
            modifier = Modifier.fillMaxWidth().height(60.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .background(PipAmber, RoundedCornerShape(8.dp))
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.ArrowBack, null, tint = PipBlack)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text("THERMAL OPTICS", color = PipAmber, fontSize = 24.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Divider(color = PipAmber, thickness = 2.dp)
        Spacer(modifier = Modifier.height(8.dp))

        // MAIN DISPLAY (Current Temp)
        Text("INTERNAL SENSOR", color = Color.Gray, fontFamily = FontFamily.Monospace)
        Text(
            text = "%.1f°C".format(currentTemp),
            color = if (currentTemp > 40) PipRed else PipAmber,
            fontSize = 70.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(30.dp))

        // THE "3 VALUES" GRID
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // LOW
            ThermalCard("LOW", minTemp, PipGreen)
            // AVG
            ThermalCard("AVG", avgTemp, PipAmber)
            // HIGH
            ThermalCard("HIGH", maxTemp, PipRed)
        }

        Spacer(modifier = Modifier.height(30.dp))

        // INTERPRETATION GUIDE
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color.Gray, RectangleShape)
                .padding(16.dp)
        ) {
            Column {
                Text("TACTICAL ANALYSIS:", color = PipAmber, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• IDLE STATE: ~Ambient Temp (+2°C)\n" +
                            "• LOAD STATE: ~CPU Temp (Not Weather)\n" +
                            "• >45°C: CRITICAL OVERHEAT - COOL DOWN",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // BACK BUTTON
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .border(1.dp, PipAmber, RectangleShape)
                .clickable { onBack() },
            contentAlignment = Alignment.Center
        ) {
            Text("< RETURN TO GRID", color = PipAmber, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun RowScope.ThermalCard(label: String, value: Float, color: Color) {
    Box(
        modifier = Modifier
            .weight(1f)
            .height(100.dp)
            .padding(4.dp)
            .border(2.dp, color, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, color = color.copy(alpha = 0.7f), fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            Text(
                text = if (value > 900 || value < -900) "--" else "%.1f".format(value),
                color = color,
                fontSize = 24.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }
    }
}