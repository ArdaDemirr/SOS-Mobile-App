package com.example.sos

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BatterySaverScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    val powerManager = remember { context.getSystemService(Context.POWER_SERVICE) as PowerManager }
    val isIgnoringBatteryOptimizations = remember { powerManager.isIgnoringBatteryOptimizations(context.packageName) }

    var isSaverEnabled by remember { mutableStateOf(false) }

    // Current battery level
    val batteryLevel = remember {
        val intent = context.registerReceiver(null, android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = intent?.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1) ?: -1
        if (level >= 0 && scale > 0) (level * 100 / scale) else 0
    }

    val tips = listOf(
        "📵 Uçak moduna geç — radyo güç tüketimini %70 azaltır",
        "🌑 Ekran rengi siyah — AMOLED'de %40 pil tasarrufu",
        "📶 Bluetooth & Wi-Fi kapat — her biri %5–10/saat",
        "📍 GPS'i sadece gerektiğinde aç",
        "🔆 Parlaklığı minimuma indir",
        "🔕 Titreşimi kapat — motor sürekli güç tüketir",
        "❄ Pili soğuk tut — sıcakta kapasite düşer",
        "🔋 Güç bankası şarj eder gibi telefonu pinlerine değdirme — oksitlenme"
    )

    val criticalApps = listOf(
        "SOS" to Screen.Sos,
        "HARİTA" to Screen.Map,
        "PUSULA" to Screen.Compass,
        "KOORD PAYLAŞ" to Screen.CoordShare
    )

    Column(modifier = Modifier.fillMaxSize().background(PipBlack).systemBarsPadding()) {
        ScreenHeader(title = "BAT SAVER", subtitle = "BATARYA TASARRUF MODU", onBack = onBack)

        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

            // Battery level display
            Box(
                Modifier.fillMaxWidth().border(2.dp, when { batteryLevel < 20 -> PipRed; batteryLevel < 50 -> PipAmber; else -> PipGreen }, RectangleShape).padding(16.dp)
            ) {
                Column {
                    Text("MEVCUT PİL SEVİYESİ", color = PipAmber.copy(0.6f), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("$batteryLevel", color = when { batteryLevel < 20 -> PipRed; batteryLevel < 50 -> PipAmber; else -> PipGreen }, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 48.sp)
                        Text("%", color = PipAmber, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 24.sp, modifier = Modifier.padding(bottom = 8.dp))
                    }
                    // Bar
                    Box(Modifier.fillMaxWidth().height(12.dp).border(1.dp, PipAmber.copy(0.5f))) {
                        Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(batteryLevel / 100f).background(when { batteryLevel < 20 -> PipRed; batteryLevel < 50 -> PipAmber; else -> PipGreen }))
                    }
                    if (batteryLevel < 20) {
                        Spacer(Modifier.height(6.dp))
                        Text("⚠ KRİTİK SEVİYE — Hemen tasarruf moduna geç!", color = PipRed, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }

            // Battery optimization
            if (!isIgnoringBatteryOptimizations) {
                Box(
                    Modifier.fillMaxWidth().border(1.dp, PipAmber.copy(0.5f), RoundedCornerShape(4.dp)).background(PipAmber.copy(0.05f)).clickable {
                        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                            data = Uri.parse("package:${context.packageName}")
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    }.padding(12.dp)
                ) {
                    Column {
                        Text("PİL OPTİMİZASYONUNDAN MUAF TUT", color = PipAmber, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Sistem arka plan servisleri öldürürse SOS servisi çalışmaz. Muaf tutmak için dokunun.", color = PipAmber.copy(0.6f), fontFamily = FontFamily.Monospace, fontSize = 10.sp, lineHeight = 14.sp)
                    }
                }
            } else {
                Box(Modifier.fillMaxWidth().border(1.dp, PipGreen.copy(0.5f), RoundedCornerShape(4.dp)).padding(10.dp)) {
                    Text("✓ Pil optimizasyonundan muaf — SOS servisleri arka planda çalışabilir", color = PipGreen, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                }
            }

            Divider(color = PipAmber.copy(0.3f))
            Text("HIZLI BAĞLANTI — KRİTİK ÖZELLIKLER", color = PipAmber.copy(0.6f), fontFamily = FontFamily.Monospace, fontSize = 11.sp)

            // Quick launch grid for critical features
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                criticalApps.take(2).forEach { (label, _) ->
                    Box(Modifier.weight(1f).height(52.dp).border(1.dp, PipAmber.copy(0.6f), RoundedCornerShape(4.dp)).background(PipAmber.copy(0.08f)), contentAlignment = Alignment.Center) {
                        Text(label, color = PipAmber, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                criticalApps.takeLast(2).forEach { (label, _) ->
                    Box(Modifier.weight(1f).height(52.dp).border(1.dp, PipAmber.copy(0.6f), RoundedCornerShape(4.dp)).background(PipAmber.copy(0.08f)), contentAlignment = Alignment.Center) {
                        Text(label, color = PipAmber, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            Divider(color = PipAmber.copy(0.3f))
            Text("MANUEL TASARRUF İPUÇLARI", color = PipAmber.copy(0.6f), fontFamily = FontFamily.Monospace, fontSize = 11.sp)

            tips.forEach { tip ->
                Box(Modifier.fillMaxWidth().border(1.dp, PipAmber.copy(0.25f), RoundedCornerShape(4.dp)).padding(10.dp)) {
                    Text(tip, color = PipAmber.copy(0.8f), fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                }
            }

            // System battery settings
            Box(
                Modifier.fillMaxWidth().height(48.dp)
                    .border(1.dp, PipAmber.copy(0.5f), RoundedCornerShape(4.dp))
                    .clickable {
                        context.startActivity(Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS).also { it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("⚙ PİL AYARLARI (Sistem)", color = PipAmber, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            }
        }
    }
}
