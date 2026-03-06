package com.example.sos.notused

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sos.PipAmber
import com.example.sos.PipBlack
import com.example.sos.PipGreen
import com.example.sos.PipRed
import java.time.LocalTime

@Composable
fun SkyScannerScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    // --- STATE ---
    var currentLux by remember { mutableFloatStateOf(0f) }
    var smoothedLux by remember { mutableFloatStateOf(0f) }
    var maxLux by remember { mutableFloatStateOf(0f) } // <-- YENİ: Zirve Değeri

    var weatherPrediction by remember { mutableStateOf("SCANNING SKY...") }
    var confidenceColor by remember { mutableStateOf(Color.Gray) }

    // Varsayılan olarak kapalı (Kilit devrede), ama test için açabilirsin
    var isTestMode by remember { mutableStateOf(false) }

    // --- SENSOR ENGINE ---
    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    val rawLux = it.values[0]
                    currentLux = rawLux

                    // 1. MAKSİMUM DEĞERİ KAYDET (PEAK HOLD)
                    if (rawLux > maxLux) {
                        maxLux = rawLux
                    }

                    // 2. YUMUŞATMA (SMOOTHING)
                    // Yükselirken anında yüksel, düşerken yavaş in (Gözü yormaz)
                    if (rawLux > smoothedLux) {
                        smoothedLux = rawLux
                    } else {
                        smoothedLux -= (smoothedLux - rawLux) * 0.05f // Daha da yavaş düşüş
                    }

                    // 3. HAVA TAHMİNİ (Maksimum değere göre yapıyoruz ki indirince kaybolmasın)
                    val hour = LocalTime.now().hour
                    val isRealNight = (hour !in 7..18)

                    if (isRealNight && !isTestMode) {
                        weatherPrediction = "NIGHT MODE\n(Sensor Locked)"
                        confidenceColor = Color.DarkGray
                    } else {
                        // Analizi "maxLux" üzerinden yapıyoruz.
                        // Böylece telefonu indirsen bile "GÜNEŞLİ" yazısı ekranda kalır.
                        if (maxLux > 40000) {
                            weatherPrediction = "CLEAR / SUNNY"
                            confidenceColor = PipAmber
                        } else if (maxLux in 10000.0..40000.0) {
                            weatherPrediction = "PARTLY CLOUDY"
                            confidenceColor = PipGreen
                        } else if (maxLux in 1000.0..10000.0) {
                            weatherPrediction = "OVERCAST"
                            confidenceColor = Color.Gray
                        } else if (maxLux < 1000.0) {
                            weatherPrediction = "STORM CLOUDS"
                            confidenceColor = PipRed
                        }
                    }
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        if (lightSensor != null) {
            sensorManager.registerListener(listener, lightSensor, SensorManager.SENSOR_DELAY_GAME)
        }
        onDispose { sensorManager.unregisterListener(listener) }
    }

    // --- UI ---
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
                modifier = Modifier.size(50.dp).background(PipAmber, RoundedCornerShape(8.dp)).clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.ArrowBack, null, tint = PipBlack)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text("SKY OPTICS", color = PipAmber, fontSize = 24.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(8.dp))

        Divider(color = PipAmber, thickness = 2.dp)


        // TEST SWITCH (Gece de çalışsın mı?)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("TEST MODE", color = Color.Gray, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            Spacer(modifier = Modifier.width(8.dp))
            Switch(
                checked = isTestMode,
                onCheckedChange = { isTestMode = it },
                colors = SwitchDefaults.colors(checkedThumbColor = PipAmber, uncheckedTrackColor = Color.DarkGray)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // MAIN GAUGE (Anlık Smoothed Değer)
        Box(
            modifier = Modifier
                .size(220.dp)
                .border(4.dp, confidenceColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("CURRENT", color = Color.Gray, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                Text(
                    text = "%.0f".format(smoothedLux),
                    color = Color.White,
                    fontSize = 40.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Text("LUX", color = Color.Gray, fontFamily = FontFamily.Monospace)
            }
        }

        Spacer(modifier = Modifier.height(30.dp))

        // PEAK VALUE CARD (En Önemli Kısım)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .border(2.dp, PipAmber, RoundedCornerShape(12.dp))
                .clickable { maxLux = 0f }, // Tıklayınca Resetleme Özelliği
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.padding(start = 20.dp)) {
                Text("MAX PEAK (HOLD)", color = PipAmber, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                Text(
                    text = "%.0f".format(maxLux),
                    color = PipAmber,
                    fontSize = 32.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }

            // RESET ICON
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Reset",
                tint = PipAmber,
                modifier = Modifier
                    .padding(end = 20.dp)
                    .size(30.dp)
            )
        }
        Text("Tap box to reset peak", color = Color.DarkGray, fontSize = 10.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(top = 4.dp))

        Spacer(modifier = Modifier.height(20.dp))

        // PREDICTION (Max değere göre)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(confidenceColor.copy(alpha = 0.1f))
                .border(1.dp, confidenceColor, RectangleShape)
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("SKY ANALYSIS (BASED ON PEAK)", color = Color.Gray, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = weatherPrediction,
                    color = confidenceColor,
                    fontSize = 24.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}