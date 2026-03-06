package com.example.sos

import android.media.AudioManager
import android.media.ToneGenerator
import android.speech.tts.TextToSpeech
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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

// --- MORSE DICTIONARY ---
val morseMap = mapOf(
    'A' to ".-", 'B' to "-...", 'C' to "-.-.", 'D' to "-..", 'E' to ".", 'F' to "..-.",
    'G' to "--.", 'H' to "....", 'I' to "..", 'J' to ".---", 'K' to "-.-", 'L' to ".-..",
    'M' to "--", 'N' to "-.", 'O' to "---", 'P' to ".--.", 'Q' to "--.-", 'R' to ".-.",
    'S' to "...", 'T' to "-", 'U' to "..-", 'V' to "...-", 'W' to ".--", 'X' to "-..-",
    'Y' to "-.--", 'Z' to "--..",
    '1' to ".----", '2' to "..---", '3' to "...--", '4' to "....-", '5' to ".....",
    '6' to "-....", '7' to "--...", '8' to "---..", '9' to "----.", '0' to "-----",
    ' ' to "/"
)
val reverseMorseMap = morseMap.entries.associate { (k, v) -> v to k }

@Composable
fun MorseScreen(onBack: () -> Unit) {
    // STATE
    var plainText by remember { mutableStateOf("") }
    var morseText by remember { mutableStateOf("") }
    var isMorseMode by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()

    // --- AUDIO ENGINES ---
    // 1. Text To Speech (TTS)
    var tts: TextToSpeech? by remember { mutableStateOf(null) }

    DisposableEffect(Unit) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
            }
        }
        onDispose { tts?.shutdown() }
    }

    // 2. Tone Generator (For Morse Beeps)
    val toneGen = remember { ToneGenerator(AudioManager.STREAM_MUSIC, 100) }

    // --- LOGIC ---
    fun convertToMorse(input: String) {
        plainText = input
        morseText = input.uppercase().map { char -> morseMap[char] ?: "" }.joinToString(" ")
    }

    fun convertToPlain(input: String) {
        morseText = input
        plainText = input.trim().split(" ").map { code -> reverseMorseMap[code] ?: "" }.joinToString("")
    }

    fun speakText() {
        if (plainText.isNotEmpty()) {
            tts?.speak(plainText, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    fun playMorse() {
        if (isPlaying || morseText.isEmpty()) return
        isPlaying = true
        scope.launch(Dispatchers.IO) {
            val units = morseText.toCharArray()
            for (char in units) {
                if (!isPlaying) break // Stop if screen closed
                when (char) {
                    '.' -> {
                        toneGen.startTone(ToneGenerator.TONE_DTMF_1, 150) // Short Beep
                        delay(200)
                    }
                    '-' -> {
                        toneGen.startTone(ToneGenerator.TONE_DTMF_1, 400) // Long Beep
                        delay(450)
                    }
                    ' ' -> delay(200) // Gap between letters
                    '/' -> delay(600) // Gap between words
                }
            }
            isPlaying = false
        }
    }

    // --- LAYOUT ---
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PipBlack) // Global Black
            .systemBarsPadding()
            .imePadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- HEADER (Embedded directly here) ---
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
                    text = "MORSE",
                    color = PipAmber,
                    fontSize = 24.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Divider(color = PipAmber, thickness = 2.dp)
        }

        // --- 1. ENGLISH INPUT (Top) ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("PLAIN TEXT", color = if(!isMorseMode) PipAmber else PipAmber.copy(0.5f), fontFamily = FontFamily.Monospace)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // SPEAK BUTTON
                Text(
                    "SPEAK",
                    color = PipBlack, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.background(PipGreen, RoundedCornerShape(4.dp))
                        .clickable { speakText() }
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                )
                // COPY BUTTON
                Text(
                    "COPY",
                    color = PipBlack, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.background(PipAmber, RoundedCornerShape(4.dp))
                        .clickable { clipboardManager.setText(AnnotatedString(plainText)) }
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .border(2.dp, if(!isMorseMode) PipAmber else PipAmber.copy(0.3f), RoundedCornerShape(8.dp))
                .padding(16.dp)
        ) {
            if (plainText.isEmpty()) {
                Text("TYPE HERE...", color = PipAmber.copy(0.5f), fontFamily = FontFamily.Monospace)
            }
            BasicTextField(
                value = plainText,
                onValueChange = { convertToMorse(it) },
                textStyle = TextStyle(color = PipAmber, fontSize = 20.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold),
                cursorBrush = SolidColor(PipAmber),
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
                modifier = Modifier.fillMaxSize().onFocusChanged { if (it.isFocused) isMorseMode = false }
            )
        }

        // --- 2. MORSE OUTPUT (Bottom) ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("MORSE CODE", color = if(isMorseMode) PipAmber else PipAmber.copy(0.5f), fontFamily = FontFamily.Monospace)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // PLAY AUDIO BUTTON
                Text(
                    if(isPlaying) "PLAYING..." else "PLAY",
                    color = PipBlack, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.background(if(isPlaying) PipRed else PipGreen, RoundedCornerShape(4.dp))
                        .clickable { playMorse() }
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                )
                // COPY BUTTON
                Text(
                    "COPY",
                    color = PipBlack, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.background(PipAmber, RoundedCornerShape(4.dp))
                        .clickable { clipboardManager.setText(AnnotatedString(morseText)) }
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .border(2.dp, if(isMorseMode) PipAmber else PipAmber.copy(0.3f), RoundedCornerShape(8.dp))
                .clickable {
                    isMorseMode = true
                    focusManager.clearFocus()
                    keyboardController?.hide()
                }
                .padding(16.dp)
        ) {
            if (morseText.isEmpty()) {
                Text("... --- ...", color = PipAmber.copy(0.5f), fontFamily = FontFamily.Monospace)
            }
            Text(
                text = morseText,
                color = PipAmber,
                fontSize = 24.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            )
        }

        // --- 3. TACTICAL KEYPAD ---
        if (isMorseMode) {
            Row(
                modifier = Modifier.fillMaxWidth().height(80.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TacticalButton(".", 1f) { convertToPlain(morseText + ".") }
                TacticalButton("-", 1f) { convertToPlain(morseText + "-") }
                TacticalButton("SPACE", 1f) { convertToPlain(morseText + " ") }

                Box(
                    modifier = Modifier.weight(0.8f).fillMaxHeight()
                        .background(PipRed, RoundedCornerShape(8.dp))
                        .border(2.dp, PipRed, RoundedCornerShape(8.dp))
                        .clickable {
                            if (morseText.isNotEmpty()) {
                                val newText = if (morseText.endsWith(" ")) morseText.dropLast(1) else morseText.dropLast(1)
                                convertToPlain(newText)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Clear, null, tint = PipBlack, modifier = Modifier.size(32.dp))
                }
            }
        }
    }
}

@Composable
fun RowScope.TacticalButton(label: String, weight: Float, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .weight(weight)
            .fillMaxHeight()
            .background(PipAmber, RoundedCornerShape(8.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = PipBlack,
            fontSize = if (label.length > 1) 16.sp else 40.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace
        )
    }
}