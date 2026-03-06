package com.example.sos

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Composable
fun SosScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    var isVisualSosActive by remember { mutableStateOf(false) }
    var isAudioSosActive by remember { mutableStateOf(false) }
    var isGpsActive by remember { mutableStateOf(false) }
    var gpsText by remember { mutableStateOf("KONUM: KAPALI") }
    var gpsSource by remember { mutableStateOf("") }
    var gpsLat by remember { mutableStateOf("--") }
    var gpsLon by remember { mutableStateOf("--") }
    var gpsAlt by remember { mutableStateOf("--") }

    // --- FLASHLIGHT ---
    LaunchedEffect(isVisualSosActive) {
        if (isVisualSosActive) {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = cameraManager.cameraIdList.firstOrNull() ?: return@LaunchedEffect
            try {
                while (isActive) {
                    repeat(3) { flash(cameraManager, cameraId, 100); delay(100) }
                    delay(300)
                    repeat(3) { flash(cameraManager, cameraId, 400); delay(100) }
                    delay(300)
                    repeat(3) { flash(cameraManager, cameraId, 100); delay(100) }
                    delay(2000)
                }
            } catch (e: Exception) { isVisualSosActive = false }
        }
    }

    // --- AUDIO BEACON ---
    DisposableEffect(isAudioSosActive) {
        var audioTrack: AudioTrack? = null
        var isRunning = true
        val job = if (isAudioSosActive) {
            CoroutineScope(Dispatchers.IO).launch {
                val sampleRate = 44100
                val freqOfTone = 3000.0
                val buffSize = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
                audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                    .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(sampleRate).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                    .setBufferSizeInBytes(buffSize).setTransferMode(AudioTrack.MODE_STREAM).build()
                val samples = ShortArray(buffSize)
                for (i in samples.indices) {
                    val angle = 2.0 * Math.PI * i / (sampleRate / freqOfTone)
                    samples[i] = (Math.sin(angle) * Short.MAX_VALUE).toInt().toShort()
                }
                audioTrack?.play()
                while (isActive && isRunning) { audioTrack?.write(samples, 0, buffSize) }
            }
        } else null
        onDispose {
            isRunning = false; job?.cancel()
            try { if (audioTrack?.playState == AudioTrack.PLAYSTATE_PLAYING) { audioTrack?.pause(); audioTrack?.flush() }; audioTrack?.release() } catch (e: Exception) {}
        }
    }

    // --- GPS ---
    val locationManager = remember { context.getSystemService(Context.LOCATION_SERVICE) as LocationManager }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) isGpsActive = true else gpsText = "İZİN REDDEDİLDİ"
    }

    DisposableEffect(isGpsActive) {
        if (isGpsActive) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                gpsText = "SİNYAL ARANYOR..."
                val listener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        gpsSource = if (location.provider == LocationManager.GPS_PROVIDER) "SAT" else "NET"
                        gpsLat = "%.5f".format(location.latitude)
                        gpsLon = "%.5f".format(location.longitude)
                        gpsAlt = if (location.hasAltitude()) "%.1f m".format(location.altitude) else "N/A"
                        gpsText = "LOCKED"
                    }
                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                    override fun onProviderEnabled(provider: String) {}
                    override fun onProviderDisabled(provider: String) {}
                }
                try { locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000L, 5f, listener) } catch (e: Exception) {}
                try { locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 2000L, 5f, listener) } catch (e: Exception) {}
                onDispose { locationManager.removeUpdates(listener) }
            } else {
                permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                onDispose {}
            }
        } else {
            gpsText = "KONUM: KAPALI"; gpsSource = ""; gpsLat = "--"; gpsLon = "--"; gpsAlt = "--"
            onDispose {}
        }
    }

    val isGpsLocked = gpsText == "LOCKED"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SosBg)
            .imePadding()
            .systemBarsPadding()
    ) {
        ScreenHeader(title = "SOS-Acil", subtitle = "Acil durum bildirin", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(SosSpaceMd)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(SosSpaceMd)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // ── VISUAL SIGNAL BUTTON ─────────────────────────────────────────
            SosSignalButton(
                title = "FLAŞ SİNYALİ",
                subtitle = "S.O.S Mors kodu — kamera flaşı",
                icon = Icons.Default.Warning,
                isActive = isVisualSosActive,
                onClick = { isVisualSosActive = !isVisualSosActive }
            )

            // ── AUDIO SIGNAL BUTTON ──────────────────────────────────────────
            SosSignalButton(
                title = "SES FARÖ",
                subtitle = "3000 Hz kesintisiz alarm tonu",
                icon = Icons.Default.Phone,
                isActive = isAudioSosActive,
                onClick = { isAudioSosActive = !isAudioSosActive }
            )

            SosSectionLabel("KONUM BİLGİSİ", SosAmber)

            // ── GPS SECTION ──────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(SosRadiusMd))
                    .background(SosSurface)
                    .border(
                        1.dp,
                        if (isGpsLocked) SosCyan.copy(0.5f) else SosBorder,
                        RoundedCornerShape(SosRadiusMd)
                    )
            ) {
                // GPS Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isGpsLocked) SosCyan.copy(0.1f) else SosSurface
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (!isGpsActive) {
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                                    isGpsActive = true
                                } else {
                                    permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                                }
                            } else {
                                isGpsActive = false
                            }
                        }
                        .padding(SosSpaceMd),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(36.dp).clip(RoundedCornerShape(SosRadiusSm))
                                .background(SosCyan.copy(0.12f))
                                .border(1.dp, SosCyan.copy(0.3f), RoundedCornerShape(SosRadiusSm)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.LocationOn, null, tint = SosCyan, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("GPS KONUMU", color = SosCyan, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                            Text(
                                if (isGpsLocked) "KAYNAK: $gpsSource" else if (isGpsActive) "SİNYAL ARANYOR..." else "Aktifleştirmek için dokun",
                                color = SosCyan.copy(0.55f), fontFamily = FontFamily.Monospace, fontSize = 9.sp
                            )
                        }
                    }
                    SosStatusBadge(if (isGpsActive) "AKTİF" else "KAPALI", if (isGpsActive) SosCyan else SosTextDisabled)
                }

                // GPS Data Grid (only when active)
                if (isGpsActive) {
                    Divider(color = SosCyan.copy(0.15f), thickness = 1.dp)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SosInfoCard("ENLEM", gpsLat, SosCyan, Modifier.weight(1f))
                        SosInfoCard("BOYLAM", gpsLon, SosCyan, Modifier.weight(1f))
                        SosInfoCard("YÜKSEKLİK", gpsAlt, SosCyan, Modifier.weight(1f))
                    }
                }
            }

            Spacer(modifier = Modifier.height(SosSpaceSm))
        }
    }
}

// ─── SOS SIGNAL BUTTON ──────────────────────────────────────────────────────
@Composable
private fun SosSignalButton(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(SosRadiusMd))
            .background(if (isActive) SosRed.copy(0.15f) else SosSurface)
            .border(
                width = if (isActive) 2.dp else 1.dp,
                color = if (isActive) SosRed else SosBorder,
                shape = RoundedCornerShape(SosRadiusMd)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .padding(SosSpaceMd)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(52.dp).clip(CircleShape)
                    .background(if (isActive) SosRed.copy(0.2f) else SosBorder.copy(0.5f))
                    .border(2.dp, if (isActive) SosRed else SosBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = if (isActive) SosRed else SosTextSecondary, modifier = Modifier.size(28.dp))
            }
            Spacer(modifier = Modifier.width(SosSpaceMd))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = if (isActive) SosRed else SosTextPrimary, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                Text(subtitle, color = if (isActive) SosRed.copy(0.7f) else SosTextSecondary, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
            }
            SosStatusBadge(if (isActive) "AKTİF" else "KAPALI", if (isActive) SosRed else SosTextDisabled)
        }
    }
}

suspend fun flash(cameraManager: CameraManager, cameraId: String, duration: Long) {
    try {
        cameraManager.setTorchMode(cameraId, true); delay(duration)
        cameraManager.setTorchMode(cameraId, false)
    } catch (e: Exception) {}
}