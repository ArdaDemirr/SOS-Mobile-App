// ============================================================
// FILE: AudioMorseScreen.kt
// ============================================================

package com.example.sos.Morse

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.sos.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

@Composable
fun AudioMorseScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    // ── State ────────────────────────────────────────────────────────────────
    var isListening       by remember { mutableStateOf(false) }
    var hasPermission     by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                    == PackageManager.PERMISSION_GRANTED
        )
    }
    var decodedText       by remember { mutableStateOf("") }
    var currentMorse      by remember { mutableStateOf("") }
    var energy            by remember { mutableStateOf(0f) }
    var snrDisplay        by remember { mutableStateOf(0f) }
    var statusText        by remember { mutableStateOf("SİSTEM HAZIR") }
    var calibrationStatus by remember { mutableStateOf("CALİBRASYON BEKLENİYOR") }
    var dotMsDisplay      by remember { mutableStateOf(0L) }
    var resetRequested    by remember { mutableStateOf(false) }
    var tonesCollected    by remember { mutableStateOf(0) }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { hasPermission = it }

    // ── Audio config ─────────────────────────────────────────────────────────
    val sampleRate = 8000
    val frameSize  = 160   // exactly 20ms at 8000 Hz
    val recordBuf  = AudioRecord.getMinBufferSize(
        sampleRate,
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT
    ).coerceAtLeast(frameSize * 4)

    // ── Detection engine ─────────────────────────────────────────────────────
    LaunchedEffect(isListening) {
        if (!isListening || !hasPermission) return@LaunchedEffect

        withContext(Dispatchers.IO) {
            val recorder = try {
                AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    recordBuf
                )
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    statusText  = "HATA: MİKROFON ERİŞİLEMİYOR"
                    isListening = false
                }
                return@withContext
            }

            recorder.startRecording()
            withContext(Dispatchers.Main) { statusText = "DİNLENİYOR (650-750 Hz)" }

            val timer           = AdaptiveTimer()
            val symbolBuilder   = StringBuilder()
            val frame           = ShortArray(frameSize)
            var toneStartMs     = 0L
            var silenceStartMs  = 0L
            var prevToneOn      = false
            var hysteresisCount = 0
            val HYSTERESIS_FRAMES = 3   // 3 × 20ms = 60ms debounce

            while (isActive && isListening) {

                // ── Reset from UI ─────────────────────────────────────────────
                if (resetRequested) {
                    timer.reset()
                    symbolBuilder.clear()
                    toneStartMs     = 0L
                    silenceStartMs  = 0L
                    prevToneOn      = false
                    hysteresisCount = 0
                    withContext(Dispatchers.Main) {
                        resetRequested    = false
                        tonesCollected    = 0
                        calibrationStatus = "CALİBRASYON BEKLENİYOR"
                        dotMsDisplay      = 0L
                    }
                }

                // ── Fill exactly one 20ms frame ───────────────────────────────
                var filled = 0
                while (filled < frameSize && isActive && isListening) {
                    val n = recorder.read(frame, filled, frameSize - filled)
                    if (n > 0) filled += n else break
                }
                if (filled == 0) continue

                val chunk = if (filled == frameSize) frame else frame.copyOf(filled)
                val now   = System.currentTimeMillis()

                // ── Signal detection ──────────────────────────────────────────
                val maxEnergy = listOf(650.0, 700.0, 750.0)
                    .maxOf { f -> goertzel(chunk, f, sampleRate) }

                val noiseFloor = listOf(200.0, 1400.0)
                    .map { f -> goertzel(chunk, f, sampleRate) }
                    .average()
                    .coerceAtLeast(1.0)

                val snr         = maxEnergy / noiseFloor
                val tonePresent = maxEnergy > MorseTiming.PRESENCE_THRESHOLD
                        && snr > MorseTiming.SNR_RATIO

                withContext(Dispatchers.Main) {
                    energy     = (maxEnergy / (MorseTiming.PRESENCE_THRESHOLD * 2))
                        .toFloat().coerceIn(0f, 1f)
                    snrDisplay = snr.toFloat().coerceIn(0f, 10f)
                }

                // ── Tone ON ───────────────────────────────────────────────────
                if (tonePresent) {
                    if (hysteresisCount > 0) hysteresisCount = 0

                    if (!prevToneOn) {
                        // Rising edge — classify the silence that just ended
                        if (silenceStartMs > 0) {
                            val silenceMs   = now - silenceStartMs
                            val morseSymbol = symbolBuilder.toString()
                            when {
                                silenceMs >= MorseTiming.WORD_SILENCE_MS
                                        && morseSymbol.isNotEmpty() -> {
                                    val letter = decodeMorseSymbol(morseSymbol) ?: "?"
                                    withContext(Dispatchers.Main) {
                                        decodedText  += "$letter "
                                        currentMorse += " / "
                                    }
                                    symbolBuilder.clear()
                                }
                                silenceMs >= MorseTiming.LETTER_SILENCE_MS
                                        && morseSymbol.isNotEmpty() -> {
                                    val letter = decodeMorseSymbol(morseSymbol) ?: "?"
                                    withContext(Dispatchers.Main) {
                                        decodedText  += letter
                                        currentMorse += " "
                                    }
                                    symbolBuilder.clear()
                                }
                            }
                        }
                        toneStartMs    = now
                        silenceStartMs = 0
                        prevToneOn     = true
                    }

                    // ── Tone OFF ──────────────────────────────────────────────────
                } else {
                    if (prevToneOn) {
                        hysteresisCount++
                        if (hysteresisCount >= HYSTERESIS_FRAMES) {
                            val toneDurationMs = (now - toneStartMs) -
                                    (hysteresisCount.toLong() * 20L)

                            // Feed calibrator before classifying
                            timer.recordTone(toneDurationMs)

                            val symbol = if (toneDurationMs >= MorseTiming.DOT_DASH_BOUNDARY_MS)
                                "-" else "."
                            symbolBuilder.append(symbol)

                            withContext(Dispatchers.Main) {
                                currentMorse = currentMorse + symbol
                                tonesCollected++
                                calibrationStatus = if (timer.isCalibrated)
                                    "CALİBRE ✓  dot=${timer.dotMs}ms  |  " +
                                            "sınır=${MorseTiming.DOT_DASH_BOUNDARY_MS}ms  |  " +
                                            "harf=${MorseTiming.LETTER_SILENCE_MS}ms"
                                else
                                    "CALİBRASYON: $tonesCollected/10 ses"
                                dotMsDisplay = timer.dotMs
                            }

                            silenceStartMs  = now - (hysteresisCount.toLong() * 20L)
                            prevToneOn      = false
                            hysteresisCount = 0
                        }
                    } else {
                        hysteresisCount = 0
                    }
                }
            }

            recorder.stop()
            recorder.release()
            timer.reset()
            withContext(Dispatchers.Main) { statusText = "SİSTEM ÇEVRİMDIŞI" }
        }
    }

    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse),
        label = "pulseAnim"
    )

    // ── UI ───────────────────────────────────────────────────────────────────
    SosScreenScaffold(
        title       = "SESLİ-MORS",
        subtitle    = "Geniş Bant Sinyal Analizi",
        accentColor = PipAmber,
        onBack      = onBack
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            // ── Permission guard ──────────────────────────────────────────────
            if (!hasPermission) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(2.dp, PipRed, RectangleShape)
                        .padding(16.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "ERİŞİM REDDEDİLDİ: MİKROFON",
                            color      = PipRed,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(12.dp))
                        Box(
                            Modifier
                                .background(PipAmber, RoundedCornerShape(4.dp))
                                .clickable {
                                    permLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                "İZİN İSTE",
                                color      = PipBlack,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // ── Signal bar ────────────────────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "SİNYAL GÜCÜ",
                        color      = PipAmber.copy(0.6f),
                        fontFamily = FontFamily.Monospace,
                        fontSize   = 10.sp
                    )
                    Text(
                        "SNR: ${"%.1f".format(snrDisplay)}x  |  " +
                                "EŞİK: ${MorseTiming.PRESENCE_THRESHOLD.toInt()}",
                        color      = PipAmber.copy(0.5f),
                        fontFamily = FontFamily.Monospace,
                        fontSize   = 10.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .border(1.dp, PipAmber.copy(0.4f), RectangleShape)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(energy)
                            .background(
                                when {
                                    energy > 0.6f -> PipAmber
                                    energy > 0.2f -> PipAmber.copy(0.5f)
                                    else          -> PipAmber.copy(0.15f)
                                }
                            )
                            .alpha(if (isListening) pulse else 0.4f)
                    )
                    Text(
                        "${(energy * 100).toInt()}%",
                        color      = PipBlack,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize   = 12.sp,
                        modifier   = Modifier.align(Alignment.Center)
                    )
                }
            }

            // ── Calibration status ────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        if (dotMsDisplay > 0L) PipAmber.copy(0.5f)
                        else PipAmber.copy(0.15f),
                        RectangleShape
                    )
                    .background(
                        if (dotMsDisplay > 0L) PipAmber.copy(0.05f)
                        else PipAmber.copy(0.0f)
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    calibrationStatus,
                    color      = if (dotMsDisplay > 0L) PipAmber
                    else PipAmber.copy(0.4f),
                    fontFamily = FontFamily.Monospace,
                    fontSize   = 11.sp
                )
            }

            // ── Controls ──────────────────────────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .background(
                            if (isListening) PipRed.copy(0.1f)
                            else PipAmber.copy(0.05f),
                            RoundedCornerShape(4.dp)
                        )
                        .border(
                            2.dp,
                            if (isListening) PipRed else PipAmber,
                            RoundedCornerShape(4.dp)
                        )
                        .clickable {
                            if (hasPermission) isListening = !isListening
                            else permLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (isListening) "■  ANALİZİ DURDUR"
                        else             "●  DİNLEMEYE BAŞLA",
                        color      = if (isListening) PipRed else PipAmber,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
                Box(
                    modifier = Modifier
                        .height(56.dp)
                        .background(PipAmber.copy(0.05f), RoundedCornerShape(4.dp))
                        .border(1.dp, PipAmber.copy(0.3f), RoundedCornerShape(4.dp))
                        .clickable {
                            decodedText       = ""
                            currentMorse      = ""
                            calibrationStatus = "CALİBRASYON BEKLENİYOR"
                            dotMsDisplay      = 0L
                            tonesCollected    = 0
                            resetRequested    = true
                        }
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "TEMİZLE",
                        color      = PipAmber,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                statusText,
                color      = PipAmber.copy(0.55f),
                fontFamily = FontFamily.Monospace,
                fontSize   = 11.sp
            )

            HorizontalDivider(color = PipAmber.copy(0.2f), thickness = 1.dp)

            // ── Raw morse stream ──────────────────────────────────────────────
            Text(
                "HAM SİNYAL VERİSİ:",
                color      = PipAmber.copy(0.7f),
                fontFamily = FontFamily.Monospace,
                fontSize   = 11.sp
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .border(1.dp, PipAmber.copy(0.4f), RectangleShape)
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Text(
                    currentMorse.takeLast(60).ifEmpty { "BEKLENİYOR..." },
                    color      = PipAmber.copy(if (currentMorse.isEmpty()) 0.2f else 1f),
                    fontFamily = FontFamily.Monospace,
                    fontSize   = 18.sp
                )
            }

            // ── Decoded output ────────────────────────────────────────────────
            Text(
                "ÇÖZÜMLENEN MESAJ:",
                color      = PipAmber.copy(0.7f),
                fontFamily = FontFamily.Monospace,
                fontSize   = 11.sp
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .border(2.dp, PipAmber, RectangleShape)
                    .padding(12.dp)
            ) {
                Text(
                    decodedText.ifEmpty { "Sinyal bekleniyor..." },
                    color      = PipAmber.copy(if (decodedText.isEmpty()) 0.2f else 1f),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 20.sp
                )
            }

            // ── Footer ────────────────────────────────────────────────────────
            Box(
                Modifier
                    .fillMaxWidth()
                    .border(1.dp, PipAmber.copy(0.1f), RectangleShape)
                    .padding(8.dp)
            ) {
                Text(
                    "Scan: 650-750 Hz  |  Eşik: ${MorseTiming.PRESENCE_THRESHOLD.toInt()}  |  " +
                            "SNR min: ${MorseTiming.SNR_RATIO}x  |  Kare: 20ms/160örn  |  Hysteresis: 60ms",
                    color      = PipAmber.copy(0.35f),
                    fontFamily = FontFamily.Monospace,
                    fontSize   = 10.sp,
                    lineHeight = 14.sp
                )
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}