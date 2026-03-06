package com.example.sos.notused

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sos.PipAmber
import com.example.sos.PipBlack
import com.example.sos.PipRed
import com.example.sos.PipTextField
import com.example.sos.ScreenHeader
import com.example.sos.SmallPipButton
import java.util.Locale
import kotlin.math.roundToInt

private val emergencyPhrases = listOf(
    "Mayday! Acil yardıma ihtiyacım var!",
    "Yardım! Yaralıyım.",
    "Kayboldum, konumum:",
    "Su ve tıbbi yardım gerekiyor.",
    "Buraya gel, SOS işareti veriyorum.",
    "Helikopter için temiz alan güneyde.",
    "Dikkat! Tehlikeli bölge, geri çekil.",
    "Help! I need emergency assistance!",
    "Mayday Mayday Mayday! Emergency!"
)

@Composable
fun TextToSpeechSosScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    var ttsReady by remember { mutableStateOf(false) }
    var tts: TextToSpeech? by remember { mutableStateOf(null) }
    var inputText by remember { mutableStateOf("") }
    var repeatCount by remember { mutableIntStateOf(1) }
    var speechRate by remember { mutableStateOf(0.8f) }
    var pitch by remember { mutableStateOf(1.0f) }
    var isSpeaking by remember { mutableStateOf(false) }
    var selectedLang by remember { mutableStateOf("TR") }

    DisposableEffect(selectedLang) {
        tts?.shutdown()
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ttsReady = true
                val locale = if (selectedLang == "TR") Locale("tr", "TR") else Locale.US
                tts?.language = locale
                tts?.setSpeechRate(speechRate)
                tts?.setPitch(pitch)
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(id: String?) { isSpeaking = true }
                    override fun onDone(id: String?) { isSpeaking = false }
                    override fun onError(id: String?) { isSpeaking = false }
                })
            }
        }
        onDispose { tts?.shutdown(); tts = null }
    }

    fun speak(text: String) {
        if (!ttsReady || text.isEmpty()) return
        tts?.setSpeechRate(speechRate)
        tts?.setPitch(pitch)
        tts?.stop()
        val bundle = Bundle().apply { putInt(TextToSpeech.Engine.KEY_PARAM_VOLUME, 100) }
        repeat(repeatCount) { i ->
            val mode = if (i == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
            tts?.speak(text, mode, bundle, "sos_$i")
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(PipBlack).systemBarsPadding()) {
        ScreenHeader(title = "TTS SOS", subtitle = "SES KODU DÖNÜŞTÜRÜcü", onBack = onBack)

        Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (!ttsReady) {
                Box(Modifier.fillMaxWidth().border(1.dp, PipRed.copy(0.5f), RoundedCornerShape(4.dp)).padding(10.dp)) {
                    Text("TTS MOToru BAŞLATILAMADI — cihazda TTS desteği yok.", color = PipRed, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                }
            }

            // Language selector
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("TR", "EN").forEach { lang ->
                    Box(
                        Modifier.weight(1f).height(40.dp)
                            .background(if (selectedLang == lang) PipAmber.copy(0.2f) else PipBlack, RoundedCornerShape(4.dp))
                            .border(2.dp, if (selectedLang == lang) PipAmber else PipAmber.copy(0.3f), RoundedCornerShape(4.dp))
                            .clickable { selectedLang = lang },
                        contentAlignment = Alignment.Center
                    ) { Text(lang, color = PipAmber, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold) }
                }
            }

            // Text input
            PipTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = "Okunacak metni gir..."
            )

            // Repeat control
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("TEKRAR: $repeatCount×", color = PipAmber, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(1, 3, 5, 10).forEach { n ->
                        Box(Modifier.Companion.background(if (repeatCount == n) PipAmber else PipAmber.copy(0.1f), RoundedCornerShape(3.dp)).clickable { repeatCount = n }.padding(horizontal = 8.dp, vertical = 4.dp)) {
                            Text("$n×", color = if (repeatCount == n) PipBlack else PipAmber, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }

            // Speed slider
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("HIZ", color = PipAmber.copy(0.7f), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                Text("${(speechRate * 100).roundToInt()}%", color = PipAmber, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            }
            Slider(value = speechRate, onValueChange = { speechRate = it }, valueRange = 0.3f..2.0f, colors = SliderDefaults.colors(thumbColor = PipAmber, activeTrackColor = PipAmber, inactiveTrackColor = PipAmber.copy(0.3f)))

            // Speak button
            Box(
                modifier = Modifier.fillMaxWidth().height(64.dp)
                    .background(if (isSpeaking) PipRed.copy(0.2f) else PipAmber.copy(0.15f), RoundedCornerShape(4.dp))
                    .border(2.dp, if (isSpeaking) PipRed else PipAmber, RoundedCornerShape(4.dp))
                    .clickable { if (isSpeaking) { tts?.stop(); isSpeaking = false } else speak(inputText) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (isSpeaking) "■ DURDUR" else "▶ KONUŞ",
                    color = if (isSpeaking) PipRed else PipAmber, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 20.sp
                )
            }

            Divider(color = PipAmber.copy(0.3f))
            Text("ACİL MESAJ ŞABLONLARI", color = PipAmber.copy(0.6f), fontFamily = FontFamily.Monospace, fontSize = 11.sp)

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(emergencyPhrases) { phrase ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 3.dp)
                            .border(1.dp, PipAmber.copy(0.3f), RoundedCornerShape(4.dp))
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(phrase, color = PipAmber, fontFamily = FontFamily.Monospace, fontSize = 12.sp, modifier = Modifier.weight(1f))
                        Spacer(Modifier.width(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            SmallPipButton("SE Ç") { inputText = phrase }
                            SmallPipButton("▶") { speak(phrase) }
                        }
                    }
                }
            }
        }
    }
}
