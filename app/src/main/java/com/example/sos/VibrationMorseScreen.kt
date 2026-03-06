package com.example.sos

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun VibrationMorseScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    var inputText by remember { mutableStateOf("") }
    var morseDisplay by remember { mutableStateOf("") }
    var isPlaying by remember { mutableStateOf(false) }
    var currentSymbol by remember { mutableStateOf("") }

    val vibrator = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    val presets = listOf(
        "SOS" to "... --- ...",
        "HELP" to ".... . .-.. .--.",
        "MAYDAY" to "-- .- -.-- -.. .- -.--",
        "WATER" to ".-- .- - . .-."
    )

    fun buildVibratePattern(morse: String): LongArray {
        val pattern = mutableListOf(0L) // Initial delay
        for (ch in morse) {
            when (ch) {
                '.' -> { pattern += MorseTiming.DOT_MS; pattern += MorseTiming.SYMBOL_GAP_MS }
                '-' -> { pattern += MorseTiming.DASH_MS; pattern += MorseTiming.SYMBOL_GAP_MS }
                ' ' -> { pattern += MorseTiming.LETTER_GAP_MS }
                '/' -> { pattern += MorseTiming.WORD_GAP_MS }
            }
        }
        return pattern.toLongArray()
    }

    fun playVibration(morse: String) {
        if (isPlaying || morse.isEmpty()) return
        isPlaying = true
        scope.launch {
            val pattern = buildVibratePattern(morse)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, -1)
            }
            // Estimate total duration and wait
            val totalMs = pattern.sum()
            delay(totalMs + 100)
            isPlaying = false
        }
    }

    fun stopVibration() {
        vibrator.cancel()
        isPlaying = false
    }

    Column(
        modifier = Modifier.fillMaxSize().background(PipBlack).systemBarsPadding().imePadding()
    ) {
        ScreenHeader(title = "Titreşim-Morse", subtitle = "Titreşimler ile morse kodu tanımlama", onBack = onBack)

        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Input field
            Text("METİN GİR:", color = PipAmber.copy(alpha = 0.7f), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .border(2.dp, PipAmber, RoundedCornerShape(4.dp))
                    .padding(12.dp)
            ) {
                if (inputText.isEmpty()) Text("SOS, HELP, MAYDAY...", color = PipAmber.copy(0.4f), fontFamily = FontFamily.Monospace)
                BasicTextField(
                    value = inputText,
                    onValueChange = {
                        inputText = it
                        morseDisplay = encodeMorse(it)
                    },
                    textStyle = TextStyle(color = PipAmber, fontFamily = FontFamily.Monospace, fontSize = 18.sp, fontWeight = FontWeight.Bold),
                    cursorBrush = SolidColor(PipAmber),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Morse display
            if (morseDisplay.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, PipAmber.copy(0.5f), RoundedCornerShape(4.dp))
                        .padding(10.dp)
                        .heightIn(max = 80.dp)
                ) {
                    Text(
                        morseDisplay,
                        color = PipAmber,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 16.sp,
                        modifier = Modifier.verticalScroll(rememberScrollState())
                    )
                }
            }

            // Play / Stop
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .background(if (isPlaying) PipRed.copy(0.2f) else PipAmber.copy(0.15f), RoundedCornerShape(4.dp))
                        .border(2.dp, if (isPlaying) PipRed else PipAmber, RoundedCornerShape(4.dp))
                        .clickable { if (isPlaying) stopVibration() else playVibration(morseDisplay) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (isPlaying) "■ DURDUR" else "▶ TİTREŞİM",
                        color = if (isPlaying) PipRed else PipAmber,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }

            Divider(color = PipAmber.copy(0.3f))
            Text("HAZIR ŞABLONLAR", color = PipAmber.copy(0.6f), fontFamily = FontFamily.Monospace, fontSize = 11.sp)

            presets.forEach { (label, morse) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, PipAmber.copy(0.4f), RoundedCornerShape(4.dp))
                        .clickable {
                            inputText = label
                            morseDisplay = morse
                        }
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(label, color = PipAmber, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(morse, color = PipAmber.copy(0.5f), fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                    }
                    Box(
                        modifier = Modifier
                            .background(PipAmber, RoundedCornerShape(3.dp))
                            .clickable { playVibration(morse) }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("▶", color = PipBlack, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Info box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, PipAmber.copy(0.2f), RoundedCornerShape(4.dp))
                    .padding(10.dp)
            ) {
                Text(
                    "Kısa titreşim = ·  Uzun titreşim = —\nEkrana bakamayan veya sesi duyamayan kullanıcılar için idealdir.",
                    color = PipAmber.copy(0.4f), fontFamily = FontFamily.Monospace, fontSize = 10.sp, lineHeight = 15.sp
                )
            }
        }
    }
}

// Reusable header composable for all new screens
@Composable
fun ScreenHeader(title: String, subtitle: String, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .padding(top = 40.dp, bottom = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(56.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(PipAmber, RoundedCornerShape(6.dp))
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = null, tint = PipBlack)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(title, color = PipAmber, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                Text(subtitle, color = PipAmber.copy(alpha = 0.6f), fontFamily = FontFamily.Monospace, fontSize = 10.sp)
            }
        }
        Divider(color = PipAmber, thickness = 2.dp, modifier = Modifier.padding(top = 6.dp))
    }
}
