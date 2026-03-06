package com.example.sos

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
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val defaultTemplates = listOf(
    "SOS" to "Acil yardım gerekiyor",
    "YARALIYIM" to "Yaralıyım, tıbbi yardım gerekiyor",
    "SU" to "Su ihtiyacım var, nehir/kaynak nerede?",
    "KAYIP" to "Kayboldum, harita koordinatlarım nerede?",
    "BARINMA" to "Barınak arıyorum, fırtına geliyor",
    "YANGIN" to "Yangın var, tahliye edin!",
    "SUBMERGEd" to "Sel tehlikesi, yüksek zemine çıkın"
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
    var isVibrating by remember { mutableStateOf(false) }
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

    Column(modifier = Modifier.fillMaxSize().background(PipBlack).imePadding().systemBarsPadding()) {
        ScreenHeader(title = "SOS Şablonları", subtitle = "Onceden kayıtlı mesajlar", onBack = onBack)

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(bottom = 24.dp, top = 8.dp)
        ) {
            item {
                // Add button
                Row(
                    modifier = Modifier.fillMaxWidth().border(1.dp, PipAmber.copy(0.5f), RoundedCornerShape(4.dp)).clickable { showAdd = !showAdd }.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, null, tint = PipAmber, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("YENİ ŞABLON EKLE", color = PipAmber, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                if (showAdd) {
                    Column(modifier = Modifier.padding(top = 6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        PipTextField(value = newLabel, onValueChange = { newLabel = it.uppercase() }, placeholder = "ETIKET (örn. TUZAK)")
                        PipTextField(value = newText, onValueChange = { newText = it }, placeholder = "Açıklama metni")
                        Box(
                            Modifier.fillMaxWidth().background(PipAmber, RoundedCornerShape(4.dp)).clickable {
                                if (newLabel.isNotEmpty()) {
                                    templates = templates + (newLabel to newText)
                                    val custom = templates.drop(defaultTemplates.size).joinToString("||") { "${it.first}:${it.second}" }
                                    prefs.edit().putString("custom", custom).apply()
                                    newLabel = ""; newText = ""; showAdd = false
                                }
                            }.padding(10.dp),
                            contentAlignment = Alignment.Center
                        ) { Text("KAYDET", color = PipBlack, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold) }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Divider(color = PipAmber.copy(0.4f))
                Spacer(Modifier.height(4.dp))
            }

            items(templates) { (label, text) ->
                val isActive = activeTemplate?.first == label
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, if (isActive) PipAmber else PipAmber.copy(0.4f), RoundedCornerShape(4.dp))
                        .background(if (isActive) PipAmber.copy(0.1f) else PipBlack)
                        .padding(10.dp)
                ) {
                    Column {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text(label, color = PipAmber, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            // Morse pattern preview
                            Text(encodeMorse(label).take(20), color = PipAmber.copy(0.5f), fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                        }
                        Text(text, color = PipAmber.copy(0.7f), fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SmallPipButton("⚡ FLASH") { flashMorse(label) }
                            SmallPipButton("KOPYALA") {
                                /* clipboard handled by system */
                            }
                            if (templates.indexOf(label to text) >= defaultTemplates.size) {
                                Box(
                                    Modifier.background(PipRed.copy(0.15f), RoundedCornerShape(3.dp)).border(1.dp, PipRed.copy(0.5f), RoundedCornerShape(3.dp)).clickable {
                                        templates = templates.filter { it != (label to text) }
                                        val custom = templates.drop(defaultTemplates.size).joinToString("||") { "${it.first}:${it.second}" }
                                        prefs.edit().putString("custom", custom).apply()
                                    }.padding(horizontal = 8.dp, vertical = 4.dp)
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

@Composable
fun SmallPipButton(label: String, onClick: () -> Unit) {
    Box(
        Modifier.background(PipAmber.copy(0.15f), RoundedCornerShape(3.dp)).border(1.dp, PipAmber.copy(0.6f), RoundedCornerShape(3.dp)).clickable { onClick() }.padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(label, color = PipAmber, fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun PipTextField(value: String, onValueChange: (String) -> Unit, placeholder: String) {
    Box(
        Modifier.fillMaxWidth().border(1.dp, PipAmber.copy(0.6f), RoundedCornerShape(4.dp)).padding(10.dp)
    ) {
        if (value.isEmpty()) Text(placeholder, color = PipAmber.copy(0.3f), fontFamily = FontFamily.Monospace, fontSize = 13.sp)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(color = PipAmber, fontFamily = FontFamily.Monospace, fontSize = 13.sp),
            cursorBrush = SolidColor(PipAmber),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
