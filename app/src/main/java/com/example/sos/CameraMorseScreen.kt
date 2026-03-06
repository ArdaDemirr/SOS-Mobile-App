package com.example.sos

import android.Manifest
import android.content.pm.PackageManager
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import java.util.concurrent.Executors

/**
 * CameraMorseScreen — Uses CameraX ImageAnalysis to detect light flashing patterns.
 * Analyzes per-frame luminance averages and decodes bright/dark transitions
 * into morse code timing → dots, dashes, letters.
 */
@Composable
fun CameraMorseScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var isAnalyzing by remember { mutableStateOf(false) }
    var currentLuminance by remember { mutableStateOf(0.0) }
    var threshold by remember { mutableStateOf(128.0) }
    var currentMorse by remember { mutableStateOf("") }
    var decodedText by remember { mutableStateOf("") }
    var statusText by remember { mutableStateOf("HAZIR") }

    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasPermission = it }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val previewView = remember { PreviewView(context) }

    // Morse decode state (mutable outside to update from analysis callback)
    val toneStartMs = remember { mutableStateOf(0L) }
    val silenceStartMs = remember { mutableStateOf(0L) }
    val prevBright = remember { mutableStateOf(false) }
    val symbolBuilder = remember { StringBuilder() }

    DisposableEffect(hasPermission, isAnalyzing) {
        if (!hasPermission || !isAnalyzing) return@DisposableEffect onDispose {}

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = androidx.camera.core.Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val imageAnalysis = ImageAnalysis.Builder()
                .setTargetResolution(Size(320, 240)) // Low res = faster analysis
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                val lum = imageProxy.averageLuminance()
                imageProxy.close()

                val bright = lum > threshold
                val now = System.currentTimeMillis()

                currentLuminance = lum

                if (bright && !prevBright.value) {
                    // Light ON
                    toneStartMs.value = now
                    if (silenceStartMs.value > 0) {
                        val silenceMs = now - silenceStartMs.value
                        when {
                            silenceMs >= MorseTiming.WORD_SILENCE_MS -> {
                                val letter = decodeMorseSymbol(symbolBuilder.toString())
                                if (letter != null) decodedText += letter
                                decodedText += " "
                                currentMorse += " / "
                                symbolBuilder.clear()
                            }
                            silenceMs >= MorseTiming.LETTER_SILENCE_MS -> {
                                val letter = decodeMorseSymbol(symbolBuilder.toString())
                                if (letter != null) decodedText += letter
                                currentMorse += " "
                                symbolBuilder.clear()
                            }
                        }
                        silenceStartMs.value = 0
                    }
                } else if (!bright && prevBright.value) {
                    // Light OFF
                    silenceStartMs.value = now
                    val durationMs = now - toneStartMs.value
                    val symbol = if (durationMs >= MorseTiming.DOT_DASH_BOUNDARY_MS) "-" else "."
                    symbolBuilder.append(symbol)
                    currentMorse += symbol
                }

                prevBright.value = bright
                statusText = if (bright) "● IŞIK ALGILANDI" else "○ KARANLK"
            }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalysis
                )
            } catch (e: Exception) {
                statusText = "KAMERA HATASI: ${e.message}"
            }
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            ProcessCameraProvider.getInstance(context).get()?.unbindAll()
            cameraExecutor.shutdown()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(PipBlack).systemBarsPadding()
    ) {
        ScreenHeader(title = "Kamera-Morse", subtitle = "Kamera ile Morse Okuma", onBack = onBack)

        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (!hasPermission) {
                Box(modifier = Modifier.fillMaxWidth().border(2.dp, PipRed, RectangleShape).padding(16.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("KAMERA İZNİ GEREKLİ", color = PipRed, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Box(Modifier.background(PipAmber, RoundedCornerShape(4.dp)).clickable { permLauncher.launch(Manifest.permission.CAMERA) }.padding(10.dp)) {
                            Text("İZİN VER", color = PipBlack, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                // Camera preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .border(2.dp, if (isAnalyzing) PipAmber else PipAmber.copy(0.3f), RectangleShape)
                ) {
                    AndroidView(
                        factory = { previewView },
                        modifier = Modifier.fillMaxSize()
                    )
                    // Luminance overlay
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .background(PipBlack.copy(0.7f))
                            .align(Alignment.BottomStart)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth((currentLuminance / 255.0).toFloat().coerceIn(0f, 1f))
                                .background(if (currentLuminance > threshold) PipAmber else PipAmber.copy(0.3f))
                        )
                    }
                }

                // Threshold slider-like control
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("EŞİK: ${threshold.toInt()}", color = PipAmber, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(-20, -5, +5, +20).forEach { delta ->
                            Box(
                                Modifier.background(PipAmber.copy(0.2f), RoundedCornerShape(3.dp))
                                    .clickable { threshold = (threshold + delta).coerceIn(50.0, 230.0) }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(if (delta > 0) "+$delta" else "$delta", color = PipAmber, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // Start/stop
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .background(if (isAnalyzing) PipRed.copy(0.15f) else PipAmber.copy(0.1f), RoundedCornerShape(4.dp))
                        .border(2.dp, if (isAnalyzing) PipRed else PipAmber, RoundedCornerShape(4.dp))
                        .clickable { if (hasPermission) isAnalyzing = !isAnalyzing else permLauncher.launch(Manifest.permission.CAMERA) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (isAnalyzing) "■ DURDUR" else "● ANALİZ BAŞLAT", color = if (isAnalyzing) PipRed else PipAmber, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Box(
                    Modifier.height(56.dp).background(PipAmber.copy(0.1f), RoundedCornerShape(4.dp)).border(1.dp, PipAmber.copy(0.5f), RoundedCornerShape(4.dp)).clickable { currentMorse = ""; decodedText = ""; symbolBuilder.clear() }.padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("SİL", color = PipAmber, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            }

            Text(statusText, color = PipAmber.copy(0.6f), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            Divider(color = PipAmber.copy(0.3f))

            Text("ALGILANAN MORS:", color = PipAmber.copy(0.7f), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            Box(Modifier.fillMaxWidth().height(50.dp).border(1.dp, PipAmber.copy(0.4f), RectangleShape).padding(8.dp)) {
                Text(currentMorse.takeLast(60).ifEmpty { "· · ·  — — —  · · ·" }, color = PipAmber.copy(if (currentMorse.isEmpty()) 0.3f else 1f), fontFamily = FontFamily.Monospace, fontSize = 18.sp, modifier = Modifier.verticalScroll(rememberScrollState()))
            }

            Text("ÇÖZÜLEN METİN:", color = PipAmber.copy(0.7f), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            Box(Modifier.fillMaxWidth().weight(1f).border(2.dp, PipAmber, RectangleShape).padding(12.dp)) {
                Text(decodedText.ifEmpty { "Fener veya yansıma ışığını kameraya tutun..." }, color = PipAmber.copy(if (decodedText.isEmpty()) 0.3f else 1f), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.verticalScroll(rememberScrollState()))
            }
        }
    }
}

/** Efficiently compute average luminance from ImageProxy Y plane */
private fun ImageProxy.averageLuminance(): Double {
    val buffer = planes[0].buffer
    val data = ByteArray(buffer.remaining())
    buffer.get(data)
    // Sample every 8th pixel for performance
    var sum = 0L; var count = 0
    var i = 0
    while (i < data.size) {
        sum += (data[i].toInt() and 0xFF)
        count++
        i += 8
    }
    return if (count == 0) 0.0 else sum.toDouble() / count
}
