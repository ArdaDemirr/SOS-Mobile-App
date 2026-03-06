package com.example.sos

import android.content.Context
import android.graphics.Rect
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CompassScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("CompassPrefs", Context.MODE_PRIVATE) }

    // --- STATE ---
    var azimuth by remember { mutableFloatStateOf(0f) }
    var isCalibrating by remember { mutableStateOf(false) }

    // Calibration Limits
    var xMin by remember { mutableFloatStateOf(prefs.getFloat("xMin", 1000f)) }
    var xMax by remember { mutableFloatStateOf(prefs.getFloat("xMax", -1000f)) }
    var yMin by remember { mutableFloatStateOf(prefs.getFloat("yMin", 1000f)) }
    var yMax by remember { mutableFloatStateOf(prefs.getFloat("yMax", -1000f)) }

    // --- SENSOR ENGINE ---
    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        var smoothAzimuth = 0f
        val alpha = 0.85f // Tuned for smoother movement

        val listener = object : SensorEventListener {
            var gravity = FloatArray(3)
            var geomagnetic = FloatArray(3)
            var hasG = false
            var hasM = false

            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    if (it.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                        gravity = it.values.clone()
                        hasG = true
                    } else if (it.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                        val rawX = it.values[0]
                        val rawY = it.values[1]
                        val rawZ = it.values[2]

                        if (isCalibrating) {
                            if (rawX < xMin) xMin = rawX
                            if (rawX > xMax) xMax = rawX
                            if (rawY < yMin) yMin = rawY
                            if (rawY > yMax) yMax = rawY
                        }

                        val xBias = (xMin + xMax) / 2f
                        val yBias = (yMin + yMax) / 2f

                        if (xMin != 1000f && xMax != -1000f) {
                            geomagnetic[0] = rawX - xBias
                            geomagnetic[1] = rawY - yBias
                            geomagnetic[2] = rawZ
                        } else {
                            geomagnetic = it.values.clone()
                        }
                        hasM = true
                    }

                    if (hasG && hasM) {
                        val R = FloatArray(9)
                        val I = FloatArray(9)
                        if (SensorManager.getRotationMatrix(R, I, gravity, geomagnetic)) {
                            val orientation = FloatArray(3)
                            SensorManager.getOrientation(R, orientation)
                            var targetAzimuth = Math.toDegrees(orientation[0].toDouble()).toFloat()
                            targetAzimuth = (targetAzimuth + 360) % 360

                            // Shortest Path Smoothing
                            var delta = targetAzimuth - smoothAzimuth
                            while (delta < -180) delta += 360
                            while (delta > 180) delta -= 360
                            smoothAzimuth += delta * (1f - alpha)
                            smoothAzimuth = (smoothAzimuth + 360) % 360
                            azimuth = smoothAzimuth
                        }
                    }
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        sensorManager.registerListener(listener, magnetometer, SensorManager.SENSOR_DELAY_GAME)

        onDispose { sensorManager.unregisterListener(listener) }
    }

    val displayAzimuth = azimuth.toInt()
    val isCalibrated = xMin != 1000f

    val directionText = when {
        displayAzimuth >= 337.5 || displayAzimuth < 22.5 -> "N"
        displayAzimuth >= 22.5 && displayAzimuth < 67.5 -> "NE"
        displayAzimuth >= 67.5 && displayAzimuth < 112.5 -> "E"
        displayAzimuth >= 112.5 && displayAzimuth < 157.5 -> "SE"
        displayAzimuth >= 157.5 && displayAzimuth < 202.5 -> "S"
        displayAzimuth >= 202.5 && displayAzimuth < 247.5 -> "SW"
        displayAzimuth >= 247.5 && displayAzimuth < 292.5 -> "W"
        displayAzimuth >= 292.5 && displayAzimuth < 337.5 -> "NW"
        else -> "?"
    }

    // --- MAIN LAYOUT ---
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PipBlack)
            .systemBarsPadding()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // 1. HEADER
        Column(modifier = Modifier.fillMaxWidth()) {
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
                Text(
                    text = "Pusula",
                    color = PipAmber,
                    fontSize = 24.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Divider(color = PipAmber, thickness = 2.dp)
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 2. DATA READOUT
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(directionText, color = if (isCalibrating) PipRed else PipAmber, fontSize = 60.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            Text("$displayAzimuth°", color = if (isCalibrating) PipRed else PipAmber, fontSize = 30.sp, fontFamily = FontFamily.Monospace)

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                if (isCalibrating) "MODE: LEARNING..." else if (isCalibrated) "MODE: LOCKED" else "UNCALIBRATED",
                color = if (isCalibrating) PipRed else Color.Gray,
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(30.dp))

        // 3. THE COMPASS RING
        Box(
            modifier = Modifier
                .size(300.dp)
                .border(2.dp, if (isCalibrating) PipRed else PipAmber, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val radius = size.minDimension / 2
                val center = center

                val paint = android.graphics.Paint().apply {
                    textAlign = android.graphics.Paint.Align.CENTER
                    typeface = android.graphics.Typeface.MONOSPACE
                    isFakeBoldText = true
                }

                // Rotate dial
                rotate(-azimuth) {

                    // STEP: Draw every 3 degrees
                    for (i in 0 until 360 step 3) {
                        val isMajor = i % 90 == 0 // N, E, S, W
                        val isMid   = i % 45 == 0 // NE, SE, SW, NW
                        val isNum   = i % 30 == 0 // 30, 60, 120...

                        // Tick Logic
                        val tickLength = when {
                            isMajor -> 40f
                            isMid -> 30f
                            isNum -> 25f
                            else -> 15f
                        }

                        val tickColor = if (i == 0) PipRed else PipAmber
                        val strokeW = if (isMajor) 4f else if (isMid || isNum) 3f else 1.5f

                        // Draw Tick
                        rotate(i.toFloat()) {
                            drawLine(
                                color = tickColor,
                                start = Offset(center.x, center.y - radius),
                                end = Offset(center.x, center.y - radius + tickLength),
                                strokeWidth = strokeW
                            )
                        }

                        // Draw Text Labels (Labels & Degrees)
                        if (isMajor || isMid || isNum) {
                            val angleRad = Math.toRadians((i - 90).toDouble())
                            val textDist = radius - 70f // Pushes text slightly deeper for cleanliness

                            val tx = center.x + (textDist * cos(angleRad)).toFloat()

                            // Calculate Label
                            val label = when {
                                i == 0 -> "N"
                                i == 45 -> "NE"
                                i == 90 -> "E"
                                i == 135 -> "SE"
                                i == 180 -> "S"
                                i == 225 -> "SW"
                                i == 270 -> "W"
                                i == 315 -> "NW"
                                else -> i.toString()
                            }

                            // Set Size & Color
                            when {
                                i == 0 -> { paint.color = PipRed.toArgb(); paint.textSize = 50f }
                                isMajor -> { paint.color = PipAmber.toArgb(); paint.textSize = 50f }
                                isMid -> { paint.color = PipAmber.toArgb(); paint.textSize = 35f }
                                else -> { paint.color = PipAmber.toArgb(); paint.textSize = 28f }
                            }

                            // PERFECT VERTICAL CENTERING MATH
                            // We measure the text to find its visual center, then subtract that from the target point.
                            // Ascent is negative (up), Descent is positive (down).
                            // Center offset = (Descent + Ascent) / 2
                            val vOffset = (paint.descent() + paint.ascent()) / 2f
                            val ty = center.y + (textDist * sin(angleRad)).toFloat() - vOffset

                            drawIntoCanvas {
                                it.nativeCanvas.drawText(label, tx, ty, paint)
                            }
                        }
                    }
                }

                // Fixed Pointer
                drawLine(
                    color = Color.White,
                    start = Offset(center.x, center.y - radius - 20),
                    end = Offset(center.x, center.y - radius + 30),
                    strokeWidth = 6f
                )

                // Crosshair
                drawLine(color = PipAmber.copy(0.3f), start = Offset(center.x - 20, center.y), end = Offset(center.x + 20, center.y), strokeWidth = 2f)
                drawLine(color = PipAmber.copy(0.3f), start = Offset(center.x, center.y - 20), end = Offset(center.x, center.y + 20), strokeWidth = 2f)
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // 4. BUTTON
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .background(if (isCalibrating) PipRed else Color.Transparent)
                .border(2.dp, if(isCalibrating) PipRed else PipAmber)
                .clickable {
                    if (isCalibrating) {
                        isCalibrating = false
                        with(prefs.edit()) {
                            putFloat("xMin", xMin); putFloat("xMax", xMax)
                            putFloat("yMin", yMin); putFloat("yMax", yMax)
                            apply()
                        }
                    } else {
                        isCalibrating = true
                        xMin = 1000f; xMax = -1000f; yMin = 1000f; yMax = -1000f
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                if (isCalibrating) "LOCK CONFIGURATION" else "UNLOCK & CALIBRATE",
                color = if(isCalibrating) Color.White else PipAmber,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }
    }
}