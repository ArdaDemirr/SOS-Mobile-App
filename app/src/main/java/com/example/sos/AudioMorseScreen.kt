package com.example.sos

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
import androidx.compose.material3.Divider
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * AudioMorseScreen — Uses AudioRecord + Goertzel algorithm to detect 700Hz morse tones.
 * Decodes timing of tones and silences into dots/dashes → letters.
 * Fully offline, no network needed.
 */
@Composable
fun AudioMorseScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isListening by remember { mutableStateOf(false) }
    var hasPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
    }
    var decodedText by remember { mutableStateOf("") }
    var currentMorse by remember { mutableStateOf("") }
    var energy by remember { mutableStateOf(0f) }
    var statusText by remember { mutableStateOf("HAZIR") }

    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        hasPermission = it
    }

    val sampleRate = 8000
    val bufferSize = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        .coerceAtLeast(2048)

    LaunchedEffect(isListening) {
        if (!isListening || !hasPermission) return@LaunchedEffect

        withContext(Dispatchers.IO) {
            val recorder = try {
                AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize
                )
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { statusText = "MİKROFON HATASI"; isListening = false }
                return@withContext
            }

            recorder.startRecording()

            val samples = ShortArray(bufferSize / 2)
            val symbolBuilder = StringBuilder()
            var toneStartMs = 0L
            var silenceStartMs = 0L
            var prevTonePresent = false

            withContext(Dispatchers.Main) { statusText = "DİNLENİYOR — 700Hz MORS TONU" }

            while (isActive && isListening) {
                val read = recorder.read(samples, 0, samples.size)
                if (read <= 0) continue

                val chunk = samples.copyOf(read)
                val freq700 = goertzel(chunk, 700.0, sampleRate)
                val rms = rmsEnergy(chunk)

                withContext(Dispatchers.Main) { energy = freq700.toFloat().coerceIn(0f, 1f) }

                val tonePresent = freq700 > MorseTiming.PRESENCE_THRESHOLD

                val now = System.currentTimeMillis()

                if (tonePresent && !prevTonePresent) {
                    // Tone started
                    toneStartMs = now
                    if (silenceStartMs > 0) {
                        val silenceMs = now - silenceStartMs
                        when {
                            silenceMs >= MorseTiming.WORD_SILENCE_MS -> {
                                // Decode what we have and add space
                                val letter = decodeMorseSymbol(symbolBuilder.toString())
                                withContext(Dispatchers.Main) {
                                    if (letter != null) decodedText += letter
                                    decodedText += " "
                                    currentMorse += " / "
                                    symbolBuilder.clear()
                                }
                            }
                            silenceMs >= MorseTiming.LETTER_SILENCE_MS -> {
                                // Decode letter
                                val letter = decodeMorseSymbol(symbolBuilder.toString())
                                withContext(Dispatchers.Main) {
                                    if (letter != null) decodedText += letter
                                    currentMorse += " "
                                    symbolBuilder.clear()
                                }
                            }
                        }
                        silenceStartMs = 0
                    }
                } else if (!tonePresent && prevTonePresent) {
                    // Tone ended
                    silenceStartMs = now
                    val durationMs = now - toneStartMs
                    val symbol = if (durationMs >= MorseTiming.DOT_DASH_BOUNDARY_MS) "-" else "."
                    symbolBuilder.append(symbol)
                    withContext(Dispatchers.Main) { currentMorse += symbol }
                }

                prevTonePresent = tonePresent
                delay(20) // ~50Hz analysis rate
            }

            recorder.stop()
            recorder.release()
            withContext(Dispatchers.Main) { statusText = "DURDU" }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "audio")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse), label = "pulse"
    )

    Column(
        modifier = Modifier.fillMaxSize().background(PipBlack).systemBarsPadding().imePadding()
    ) {
        ScreenHeader(title = "Sesli-Morse", subtitle = "Sesli Morse Tanıma", onBack = onBack)

        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!hasPermission) {
                Box(modifier = Modifier.fillMaxWidth().border(2.dp, PipRed, RectangleShape).padding(16.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text("MİKROFON İZNİ GEREKLİ", color = PipRed, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Box(Modifier.background(PipAmber, RoundedCornerShape(4.dp)).clickable { permLauncher.launch(Manifest.permission.RECORD_AUDIO) }.padding(10.dp)) {
                            Text("İZİN VER", color = PipBlack, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Energy bar
            Box(
                modifier = Modifier.fillMaxWidth().height(40.dp).border(1.dp, PipAmber.copy(0.5f), RectangleShape)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(energy)
                        .background(if (energy > 0.3f) PipAmber else PipAmber.copy(0.3f))
                        .alpha(if (isListening) pulse else 0.3f)
                )
                Text(
                    "700Hz ENERJİ: ${(energy * 100).toInt()}%",
                    color = PipBlack,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            // Status + Listen button
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp)
                        .background(if (isListening) PipRed.copy(0.15f) else PipAmber.copy(0.1f), RoundedCornerShape(4.dp))
                        .border(2.dp, if (isListening) PipRed else PipAmber, RoundedCornerShape(4.dp))
                        .clickable {
                            if (hasPermission) isListening = !isListening
                            else permLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (isListening) "■ DURDUR" else "● BAŞLAT",
                        color = if (isListening) PipRed else PipAmber,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .height(64.dp)
                        .background(PipAmber.copy(0.1f), RoundedCornerShape(4.dp))
                        .border(1.dp, PipAmber.copy(0.5f), RoundedCornerShape(4.dp))
                        .clickable { decodedText = ""; currentMorse = "" }
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("SİL", color = PipAmber, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            }

            Text(statusText, color = PipAmber.copy(0.6f), fontFamily = FontFamily.Monospace, fontSize = 11.sp)

            Divider(color = PipAmber.copy(0.3f))

            Text("ALGILANAN MORS:", color = PipAmber.copy(0.7f), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .border(1.dp, PipAmber.copy(0.4f), RectangleShape)
                    .padding(8.dp)
            ) {
                Text(
                    currentMorse.takeLast(80).ifEmpty { "· · ·  — — —  · · ·" },
                    color = PipAmber.copy(if (currentMorse.isEmpty()) 0.3f else 1f),
                    fontFamily = FontFamily.Monospace, fontSize = 18.sp,
                    modifier = Modifier.verticalScroll(rememberScrollState())
                )
            }

            Text("ÇÖZÜLEN METİN:", color = PipAmber.copy(0.7f), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .border(2.dp, PipAmber, RectangleShape)
                    .padding(12.dp)
            ) {
                Text(
                    decodedText.ifEmpty { "Mors sesi algılanmayı bekliyor..." },
                    color = PipAmber.copy(if (decodedText.isEmpty()) 0.3f else 1f),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    modifier = Modifier.verticalScroll(rememberScrollState())
                )
            }

            Box(Modifier.fillMaxWidth().border(1.dp, PipAmber.copy(0.2f), RectangleShape).padding(8.dp)) {
                Text(
                    "700Hz standardında bip sesi algılar. İdeal mesafe: 0.5m–3m.\n· (kısa) = nokta   — (uzun) = çizgi",
                    color = PipAmber.copy(0.4f), fontFamily = FontFamily.Monospace, fontSize = 10.sp, lineHeight = 14.sp
                )
            }
        }
    }
}
