package com.example.sos.Morse

import android.Manifest
import android.content.pm.PackageManager
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.sos.* // Pulls PipAmber, PipBlack, PipRed, SosScreenScaffold
import java.util.concurrent.Executors

/**
 * CameraMorseScreen — Tactical Light-Pattern Decoder.
 * Uses ImageAnalysis to detect luminance transitions and decodes via MorseUtil.
 */
@Composable
fun CameraMorseScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // --- STATE ---
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

    // Logic state
    val toneStartMs = remember { mutableStateOf(0L) }
    val silenceStartMs = remember { mutableStateOf(0L) }
    val prevBright = remember { mutableStateOf(false) }
    val symbolBuilder = remember { StringBuilder() }

    // --- CAMERA ENGINE ---
    DisposableEffect(hasPermission, isAnalyzing) {
        if (!hasPermission || !isAnalyzing) return@DisposableEffect onDispose {}

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val imageAnalysis = ImageAnalysis.Builder()
                .setTargetResolution(Size(320, 240))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                val lum = imageProxy.averageLuminance()
                imageProxy.close()

                val bright = lum > threshold
                val now = System.currentTimeMillis()
                currentLuminance = lum

                if (bright && !prevBright.value) {
                    // Light transitioned ON
                    toneStartMs.value = now
                    if (silenceStartMs.value > 0) {
                        val silenceMs = now - silenceStartMs.value
                        // Use MorseTiming constants from MorseUtil
                        if (silenceMs >= MorseTiming.WORD_SILENCE_MS && symbolBuilder.isNotEmpty()) {
                            val letter = decodeMorseSymbol(symbolBuilder.toString()) ?: ""
                            decodedText += "$letter "
                            currentMorse += " / "
                            symbolBuilder.clear()
                        } else if (silenceMs >= MorseTiming.LETTER_SILENCE_MS && symbolBuilder.isNotEmpty()) {
                            val letter = decodeMorseSymbol(symbolBuilder.toString()) ?: ""
                            decodedText += letter
                            currentMorse += " "
                            symbolBuilder.clear()
                        }
                        silenceStartMs.value = 0
                    }
                } else if (!bright && prevBright.value) {
                    // Light transitioned OFF
                    silenceStartMs.value = now
                    val durationMs = now - toneStartMs.value
                    val symbol = if (durationMs >= MorseTiming.DOT_DASH_BOUNDARY_MS) "-" else "."
                    symbolBuilder.append(symbol)
                    currentMorse += symbol
                }

                prevBright.value = bright
                statusText = if (bright) "● SINYAL ALGILANDI" else "○ TERMINAL BEKLEMEDE"
            }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalysis)
            } catch (e: Exception) {
                statusText = "SISTEM HATASI: ${e.message}"
            }
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            cameraProviderFuture.get()?.unbindAll()
            cameraExecutor.shutdown()
        }
    }

    // --- UI LAYOUT ---
    SosScreenScaffold(
        title = "KAMERA-MORS",
        subtitle = "Optik Sinyal Çözümleme",
        accentColor = PipAmber,
        onBack = onBack
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp) // Tactical 8dp padding
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            if (!hasPermission) {
                Box(modifier = Modifier.fillMaxWidth().border(2.dp, PipRed, RectangleShape).padding(16.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("KAMERA ERİŞİMİ GEREKLİ", color = PipRed, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(12.dp))
                        Box(Modifier.background(PipAmber, RoundedCornerShape(4.dp)).clickable { permLauncher.launch(Manifest.permission.CAMERA) }.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            Text("İZİN VER", color = PipBlack, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                // 1. Tactical Camera Feed
                // 1. Tactical Camera Feed
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .background(PipBlack) // Ensures a clean black base
                        .border(2.dp, if (isAnalyzing) PipAmber else PipAmber.copy(0.2f), RectangleShape)
                        .clip(RectangleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (isAnalyzing && hasPermission) {
                        // Only compose the camera view when active
                        AndroidView(
                            factory = {
                                previewView.apply {
                                    scaleType = PreviewView.ScaleType.FILL_CENTER
                                }
                            },
                            modifier = Modifier.fillMaxSize().clip(RectangleShape)
                        )
                    } else {
                        // Tactical placeholder when camera is "powered down"
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "KAMERA KAPALI",
                                color = PipAmber.copy(0.3f),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Görüntü Bekleniyor...",
                                color = PipAmber.copy(0.15f),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp
                            )
                        }
                    }

                    // Real-time luminance bar (Always stays at bottom of the frame)
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
                                .fillMaxWidth(if(isAnalyzing) (currentLuminance / 255.0).toFloat().coerceIn(0f, 1f) else 0f)
                                .background(if (currentLuminance > threshold && isAnalyzing) PipAmber else PipAmber.copy(0.2f))
                        )
                    }
                }

                // 2. Threshold Calibration
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("HASSASİYET EŞİĞİ: ${threshold.toInt()}", color = PipAmber, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(-20, +20).forEach { delta ->
                            Box(
                                Modifier.background(PipAmber.copy(0.1f), RoundedCornerShape(3.dp))
                                    .border(1.dp, PipAmber.copy(0.3f), RoundedCornerShape(3.dp))
                                    .clickable { threshold = (threshold + delta).coerceIn(40.0, 240.0) }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(if (delta > 0) "+$delta" else "$delta", color = PipAmber, fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // 3. System Controls
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .weight(1f).height(60.dp)
                        .background(if (isAnalyzing) PipRed.copy(0.1f) else PipAmber.copy(0.05f), RoundedCornerShape(4.dp))
                        .border(2.dp, if (isAnalyzing) PipRed else PipAmber, RoundedCornerShape(4.dp))
                        .clickable { if (hasPermission) isAnalyzing = !isAnalyzing else permLauncher.launch(Manifest.permission.CAMERA) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (isAnalyzing) "■ ANALİZİ DURDUR" else "● OPTİK TARAMAYI BAŞLAT", color = if (isAnalyzing) PipRed else PipAmber, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
                Box(
                    Modifier.height(60.dp).background(PipAmber.copy(0.05f), RoundedCornerShape(4.dp)).border(1.dp, PipAmber.copy(0.3f), RoundedCornerShape(4.dp))
                        .clickable { currentMorse = ""; decodedText = ""; symbolBuilder.clear() }.padding(horizontal = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("TEMİZLE", color = PipAmber, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            }

            Text(statusText, color = PipAmber.copy(0.6f), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            HorizontalDivider(color = PipAmber.copy(0.2f), thickness = 1.dp)

            // 4. Data Outputs (Sharp Aesthetic)
            Text("YAKALANAN SİNYAL:", color = PipAmber.copy(0.7f), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            Box(Modifier.fillMaxWidth().height(50.dp).border(1.dp, PipAmber.copy(0.4f), RectangleShape).padding(8.dp)) {
                Text(currentMorse.takeLast(60).ifEmpty { "BEKLENİYOR..." }, color = PipAmber.copy(if (currentMorse.isEmpty()) 0.2f else 1f), fontFamily = FontFamily.Monospace, fontSize = 18.sp)
            }

            Text("DEŞİFRE METİN:", color = PipAmber.copy(0.7f), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            Box(Modifier.fillMaxWidth().height(140.dp).border(2.dp, PipAmber, RectangleShape).padding(12.dp)) {
                Text(decodedText.ifEmpty { "Işık kaynağını kadraja odaklayın..." }, color = PipAmber.copy(if (decodedText.isEmpty()) 0.2f else 1f), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }

            // 5. Tactical Footer
            Box(Modifier.fillMaxWidth().border(1.dp, PipAmber.copy(0.1f), RectangleShape).padding(8.dp)) {
                Text("Optik Veri Notu: Ortam ışığına göre eşik değerini (Threshold) ayarlayın. Parlak nesnelerin hareketi yanlış sinyal üretebilir.", color = PipAmber.copy(0.4f), fontFamily = FontFamily.Monospace, fontSize = 10.sp, lineHeight = 14.sp)
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

/** Efficiently compute average luminance from ImageProxy Y plane */
private fun ImageProxy.averageLuminance(): Double {
    val buffer = planes[0].buffer
    val data = ByteArray(buffer.remaining())
    buffer.get(data)
    var sum = 0L; var count = 0
    var i = 0
    while (i < data.size) {
        sum += (data[i].toInt() and 0xFF)
        count++
        i += 8 // Sample every 8th pixel for mobile performance
    }
    return if (count == 0) 0.0 else sum.toDouble() / count
}