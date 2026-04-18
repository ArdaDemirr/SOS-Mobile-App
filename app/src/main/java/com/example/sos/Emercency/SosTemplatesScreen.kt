package com.example.sos.Emercency

import android.content.Context
import android.hardware.camera2.CameraManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sos.* // Pulls PipAmber, PipBlack, PipRed, SosRadiusSm, SosSpaceSm, SosScreenScaffold, etc.
import com.example.sos.Morse.MorseTiming
import com.example.sos.Morse.encodeMorse
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val defaultTemplates = listOf(
    "SOS" to "Acil yardım gerekiyor",
    "YARALIYIM" to "Yaralıyım, tıbbi yardım gerekiyor",
    "SU" to "Su ihtiyacım var, nehir/kaynak nerede?",
    "KAYIP" to "Kayboldum, harita koordinatlarım nerede?",
    "BARINMA" to "Barınak arıyorum, fırtına geliyor",
    "YANGIN" to "Yangın var, tahliye edin!",
    "SEL" to "Sel tehlikesi, yüksek zemine çıkın"
)

@Composable
fun SosTemplatesScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("SosTemplates", Context.MODE_PRIVATE) }

    var templates by remember {
        mutableStateOf(
            defaultTemplates.map { (code, text) -> code to text } +
                    (prefs.getString("custom", "")?.split("||")?.filter { it.contains(":") }
                        ?.map { it.substringBefore(":") to it.substringAfter(":") } ?: emptyList())
        )
    }
    var isFlashing by remember { mutableStateOf(false) }
    var activeTemplate by remember { mutableStateOf<Pair<String,String>?>(null) }
    var newLabel by remember { mutableStateOf("") }
    var newText by remember { mutableStateOf("") }
    var showAdd by remember { mutableStateOf(false) }

    val cameraManager = remember { context.getSystemService(Context.CAMERA_SERVICE) as CameraManager }
    val cameraId = remember { try { cameraManager.cameraIdList[0] } catch (e: Exception) { "" } }

    fun flashMorse(label: String) {
        if (isFlashing) return
        isFlashing = true
        activeTemplate = templates.first { it.first == label }
        val morse = encodeMorse(label)
        scope.launch {
            try {
                for (ch in morse) {
                    when (ch) {
                        '.' -> { flash(cameraManager, cameraId, MorseTiming.DOT_MS); delay(MorseTiming.SYMBOL_GAP_MS) }
                        '-' -> { flash(cameraManager, cameraId, MorseTiming.DASH_MS); delay(MorseTiming.SYMBOL_GAP_MS) }
                        ' ' -> delay(MorseTiming.LETTER_GAP_MS)
                        '/' -> delay(MorseTiming.WORD_GAP_MS)
                    }
                }
            } catch (e: Exception) {}
            isFlashing = false
            activeTemplate = null
        }
    }

    // ─── MASTER SCAFFOLD ────────────────────────────────────────────────────
    SosScreenScaffold(
        title = "SOS Şablonları",
        subtitle = "Kayıtlı acil durum mesajları",
        accentColor = PipAmber,
        onBack = onBack
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(
                start = 0.dp,
                end = 0.dp,
                top = 16.dp,
                bottom = 24.dp
            )
        ) {
            item {
                // Add button section
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(SosRadiusSm))
                        .background(PipBlack)
                        .border(1.dp, PipAmber.copy(alpha = 0.5f), RoundedCornerShape(SosRadiusSm))
                        .clickable { showAdd = !showAdd }
                        .padding(SosSpaceSm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, null, tint = PipAmber, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(SosSpaceSm))
                    Text("YENİ ŞABLON EKLE", color = PipAmber, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                if (showAdd) {
                    Column(modifier = Modifier.padding(top = 6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        PipTextField(value = newLabel, onValueChange = { newLabel = it.uppercase() }, placeholder = "ETİKET (ÖRN. TUZAK)")
                        PipTextField(value = newText, onValueChange = { newText = it }, placeholder = "Açıklama metni")
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .clip(RoundedCornerShape(SosRadiusSm))
                                .background(PipAmber)
                                .clickable {
                                    if (newLabel.isNotEmpty()) {
                                        templates = templates + (newLabel to newText)
                                        val custom = templates.drop(defaultTemplates.size).joinToString("||") { "${it.first}:${it.second}" }
                                        prefs.edit().putString("custom", custom).apply()
                                        newLabel = ""; newText = ""; showAdd = false
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("KAYDET", color = PipBlack, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(Modifier.height(SosSpaceSm))
                SosSectionLabel("ŞABLON LİSTESİ", PipAmber)
                Spacer(Modifier.height(4.dp))
            }

            // ── TEMPLATE LIST ────────────────────────────────────────────────
            items(templates) { (label, text) ->
                val isActive = activeTemplate?.first == label
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(SosRadiusSm))
                        .border(1.dp, if (isActive) PipAmber else PipAmber.copy(alpha = 0.4f), RoundedCornerShape(SosRadiusSm))
                        .background(if (isActive) PipAmber.copy(alpha = 0.1f) else PipBlack)
                        .padding(SosSpaceSm)
                ) {
                    Column {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text(label, color = PipAmber, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(encodeMorse(label).take(20), color = PipAmber.copy(alpha = 0.5f), fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                        }
                        Text(text, color = PipAmber.copy(alpha = 0.7f), fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                        Spacer(Modifier.height(SosSpaceSm))
                        Row(horizontalArrangement = Arrangement.spacedBy(SosSpaceSm)) {
                            SmallPipButton("⚡ FLASH") { flashMorse(label) }

                            Spacer(Modifier.weight(1f))

                            if (templates.indexOf(label to text) >= defaultTemplates.size) {
                                Box(
                                    Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(SosRadiusSm))
                                        .background(PipRed.copy(alpha = 0.15f))
                                        .border(1.dp, PipRed.copy(alpha = 0.5f), RoundedCornerShape(SosRadiusSm))
                                        .clickable {
                                            templates = templates.filter { it != (label to text) }
                                            val custom = templates.drop(defaultTemplates.size).joinToString("||") { "${it.first}:${it.second}" }
                                            prefs.edit().putString("custom", custom).apply()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Delete, null, tint = PipRed, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── LOCAL THEMED HELPERS ──────────────────────────────────────────────────

@Composable
fun SmallPipButton(label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(SosRadiusSm))
            .background(PipAmber.copy(alpha = 0.15f))
            .border(1.dp, PipAmber.copy(alpha = 0.6f), RoundedCornerShape(SosRadiusSm))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(label, color = PipAmber, fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun PipTextField(value: String, onValueChange: (String) -> Unit, placeholder: String) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(SosRadiusSm))
            .border(1.dp, PipAmber.copy(alpha = 0.6f), RoundedCornerShape(SosRadiusSm))
            .padding(SosSpaceSm)
    ) {
        if (value.isEmpty()) {
            Text(placeholder, color = PipAmber.copy(alpha = 0.3f), fontFamily = FontFamily.Monospace, fontSize = 13.sp)
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(color = PipAmber, fontFamily = FontFamily.Monospace, fontSize = 13.sp),
            cursorBrush = SolidColor(PipAmber),
            modifier = Modifier.fillMaxWidth()
        )
    }
}