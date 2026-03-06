package com.example.sos

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlin.math.*

/**
 * DeadReckoningScreen — Estimates position when GPS is lost.
 * Uses step detector + compass (TYPE_ROTATION_VECTOR) to approximate movement.
 * Shows estimated position relative to last known GPS fix on a dot-grid canvas.
 */
@Composable
fun DeadReckoningScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }

    var hasLocationPerm by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
    }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasLocationPerm = it }

    var lastKnownLat by remember { mutableStateOf(0.0) }
    var lastKnownLon by remember { mutableStateOf(0.0) }
    var hasGpsFix by remember { mutableStateOf(false) }

    var stepCount by remember { mutableIntStateOf(0) }
    var bearing by remember { mutableStateOf(0f) }
    var estLat by remember { mutableStateOf(0.0) }
    var estLon by remember { mutableStateOf(0.0) }
    var estAccuracyM by remember { mutableStateOf(0.0) }

    // Each step ≈ 0.75m average stride
    val STEP_LENGTH_M = 0.75

    // Track path for drawing
    val pathPoints = remember { mutableStateListOf<Offset>() }

    // GPS for initial fix
    DisposableEffect(hasLocationPerm) {
        if (!hasLocationPerm) return@DisposableEffect onDispose {}
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val listener = object : android.location.LocationListener {
            override fun onLocationChanged(loc: Location) {
                lastKnownLat = loc.latitude
                lastKnownLon = loc.longitude
                if (!hasGpsFix) {
                    hasGpsFix = true
                    estLat = loc.latitude
                    estLon = loc.longitude
                    stepCount = 0
                    pathPoints.clear()
                    pathPoints.add(Offset(0f, 0f))
                }
            }
            override fun onProviderEnabled(p: String) {}
            override fun onProviderDisabled(p: String) {}
        }
        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 3000L, 5f, listener)
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 3000L, 5f, listener)
        } catch (e: Exception) {}
        onDispose { locationManager.removeUpdates(listener) }
    }

    // Sensors: step detector + rotation vector
    DisposableEffect(Unit) {
        val rotationListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                    val rotMatrix = FloatArray(9)
                    SensorManager.getRotationMatrixFromVector(rotMatrix, event.values)
                    val orientation = FloatArray(3)
                    SensorManager.getOrientation(rotMatrix, orientation)
                    bearing = Math.toDegrees(orientation[0].toDouble()).toFloat().let {
                        if (it < 0) it + 360 else it
                    }
                } else if (event.sensor.type == Sensor.TYPE_STEP_DETECTOR) {
                    stepCount++
                    // Dead reckoning: move in current bearing direction
                    val bearingRad = Math.toRadians(bearing.toDouble())
                    val deltaM = STEP_LENGTH_M
                    val deltaLat = deltaM * cos(bearingRad) / 111111.0
                    val deltaLon = deltaM * sin(bearingRad) / (111111.0 * cos(Math.toRadians(estLat)))
                    estLat += deltaLat
                    estLon += deltaLon
                    estAccuracyM = stepCount * STEP_LENGTH_M * 0.15 // ~15% error per step
                    // Track path (scaled to screen: 1m = 2px)
                    if (pathPoints.isNotEmpty()) {
                        val last = pathPoints.last()
                        pathPoints.add(Offset(
                            (last.x + (deltaLon * 111111.0 * 2).toFloat()),
                            (last.y - (deltaLat * 111111.0 * 2).toFloat())
                        ))
                        if (pathPoints.size > 200) pathPoints.removeAt(0)
                    }
                }
            }
            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
        }
        val rotSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
        rotSensor?.let { sensorManager.registerListener(rotationListener, it, SensorManager.SENSOR_DELAY_UI) }
        stepSensor?.let { sensorManager.registerListener(rotationListener, it, SensorManager.SENSOR_DELAY_NORMAL) }
        onDispose {
            sensorManager.unregisterListener(rotationListener)
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(PipBlack).systemBarsPadding()) {
        ScreenHeader(title = "DEAD REC", subtitle = "ÖLÜ HESAP NAVİGASYON", onBack = onBack)

        Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {

            if (!hasLocationPerm) {
                Box(Modifier.fillMaxWidth().border(2.dp, PipRed, RectangleShape).clickable { permLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) }.padding(14.dp), contentAlignment = Alignment.Center) {
                    Text("KONUM İZNİ GEREKLİ — DOKUNUN", color = PipRed, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            }

            // Status
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatBox("GPS FİX", if (hasGpsFix) "KILITLI" else "ARAMA...", if (hasGpsFix) PipGreen else PipRed)
                StatBox("ADIM", "$stepCount", PipAmber)
                StatBox("PUSULA", "${bearing.toInt()}°", PipAmber)
            }

            // Coordinate display
            Box(Modifier.fillMaxWidth().border(2.dp, PipAmber, RectangleShape).padding(12.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("BAŞLANGIÇ", color = PipAmber.copy(0.5f), fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                            Text("%.5f".format(lastKnownLat), color = PipAmber, fontFamily = FontFamily.Monospace, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("%.5f".format(lastKnownLon), color = PipAmber, fontFamily = FontFamily.Monospace, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("TAHMİNİ KONUM", color = PipAmber.copy(0.5f), fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                            Text("%.5f".format(estLat), color = if (hasGpsFix) PipAmber else PipAmber.copy(0.4f), fontFamily = FontFamily.Monospace, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("%.5f".format(estLon), color = if (hasGpsFix) PipAmber else PipAmber.copy(0.4f), fontFamily = FontFamily.Monospace, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Divider(color = PipAmber.copy(0.3f))
                    Text("HATA PAYI: ±${"%.0f".format(estAccuracyM)}m  |  MESAFE: ${"%.1f".format(stepCount * STEP_LENGTH_M)}m", color = PipAmber.copy(0.7f), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                }
            }

            // Path canvas
            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .border(1.dp, PipAmber.copy(0.4f), RectangleShape)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Grid
                    val gridSpacing = 40f
                    var x = center.x % gridSpacing
                    while (x < size.width) { drawLine(PipAmber.copy(0.08f), Offset(x, 0f), Offset(x, size.height), 1f); x += gridSpacing }
                    var y = center.y % gridSpacing
                    while (y < size.height) { drawLine(PipAmber.copy(0.08f), Offset(0f, y), Offset(size.width, y), 1f); y += gridSpacing }

                    // Path
                    if (pathPoints.size > 1) {
                        val origin = pathPoints.first()
                        val pts = pathPoints.map { pt ->
                            Offset(center.x + pt.x - origin.x, center.y + pt.y - origin.y)
                        }
                        for (i in 1 until pts.size) {
                            drawLine(PipAmber.copy(0.6f), pts[i - 1], pts[i], 2f)
                        }
                        // Start point
                        drawCircle(PipGreen, 6f, pts.first())
                        // Current position
                        pts.lastOrNull()?.let {
                            drawCircle(PipAmber, 8f, it)
                            drawCircle(Color.Transparent, 14f, it, style = Stroke(2f))
                        }
                    }

                    // North indicator
                    val northRad = Math.toRadians(bearing.toDouble())
                    val arrowLen = 30f
                    val arrowEnd = Offset(
                        center.x + (arrowLen * sin(northRad)).toFloat(),
                        center.y - (arrowLen * cos(northRad)).toFloat()
                    )
                    drawLine(PipRed, center, arrowEnd, 3f)
                    drawCircle(PipRed, 4f, arrowEnd)
                }
                Text("N", color = PipRed, fontFamily = FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.TopEnd).padding(4.dp))
                Text("● BAŞLANGIÇ   ◉ MEVCUT", color = PipAmber.copy(0.4f), fontFamily = FontFamily.Monospace, fontSize = 9.sp, modifier = Modifier.align(Alignment.BottomStart).padding(4.dp))
            }

            Box(Modifier.fillMaxWidth().border(1.dp, PipAmber.copy(0.2f), RectangleShape).padding(8.dp)) {
                Text("GPS kaybedilince aktif olur. Adım sayar + pusula ile konum tahmin eder.\nHer adım ~0.75m. Hata birikir, mümkün olan en kısa sürede GPS'e dön.", color = PipAmber.copy(0.4f), fontFamily = FontFamily.Monospace, fontSize = 10.sp, lineHeight = 14.sp)
            }
        }
    }
}

@Composable
fun StatBox(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Box(
        Modifier.border(1.dp, color.copy(0.5f), RoundedCornerShape(4.dp)).padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, color = color.copy(0.6f), fontFamily = FontFamily.Monospace, fontSize = 9.sp)
            Text(value, color = color, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}
