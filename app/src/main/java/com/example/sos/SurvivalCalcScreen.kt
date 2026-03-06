package com.example.sos

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

/**
 * SurvivalCalcScreen — Offline water & calorie calculator.
 * Formulas based on US Army Survival FM 21-76 and WHO guidelines.
 */
@Composable
fun SurvivalCalcScreen(onBack: () -> Unit) {
    var persons by remember { mutableIntStateOf(1) }
    var days by remember { mutableIntStateOf(3) }
    var tempC by remember { mutableIntStateOf(25) }
    var activityLevel by remember { mutableIntStateOf(1) } // 0=rest, 1=light, 2=hard
    var hasWounds by remember { mutableStateOf(false) }

    // Water calc: base 2L/person/day, +0.5L per 5°C above 20°C, ×1.5 for hard activity
    val tempBonus = ((tempC - 20).coerceAtLeast(0) / 5.0) * 0.5
    val activityMult = listOf(1.0, 1.3, 1.8)[activityLevel]
    val woundBonus = if (hasWounds) 0.5 else 0.0
    val waterPerPersonPerDay = (2.0 + tempBonus + woundBonus) * activityMult
    val totalWaterL = waterPerPersonPerDay * persons * days
    val criticalWaterL = 1.0 * persons * days // absolute minimum

    // Calorie calc: ~2000 kcal base, activity and temp adjusted
    val caloricBase = 2000.0
    val calTemp = if (tempC < 10) 200.0 else 0.0 // cold weather bonus
    val calAct = listOf(0.0, 400.0, 900.0)[activityLevel]
    val calPerPersonPerDay = caloricBase + calTemp + calAct
    val totalKcal = calPerPersonPerDay * persons * days

    // Food weight approximations
    val riceKg = totalKcal / 3600.0  // rice ~3600 kcal/kg
    val energyBars = (totalKcal / 250.0).roundToInt() // bar ~250 kcal each

    val actLabels = listOf("DİNLENME\n(saklanma)", "HAFİF\n(yürüyüş)", "AĞIR\n(tırmanma)")

    Column(modifier = Modifier.fillMaxSize().background(PipBlack).systemBarsPadding()) {
        ScreenHeader(title = "SURV CALC", subtitle = "SU & YİYECEK HESAPLAYICI", onBack = onBack)

        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Inputs
            CalcRow("KİŞİ SAYISI", persons.toString()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1, 2, 3, 5, 10).forEach { n ->
                        Box(
                            Modifier.background(if (persons == n) PipAmber else PipAmber.copy(0.1f), RoundedCornerShape(3.dp))
                                .border(1.dp, PipAmber.copy(if (persons == n) 1f else 0.3f), RoundedCornerShape(3.dp))
                                .clickable { persons = n }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) { Text("$n", color = if (persons == n) PipBlack else PipAmber, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold) }
                    }
                }
            }

            CalcRow("SÜRE (GÜN)", days.toString()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1, 3, 7, 14, 30).forEach { d ->
                        Box(
                            Modifier.background(if (days == d) PipAmber else PipAmber.copy(0.1f), RoundedCornerShape(3.dp))
                                .border(1.dp, PipAmber.copy(if (days == d) 1f else 0.3f), RoundedCornerShape(3.dp))
                                .clickable { days = d }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) { Text("$d", color = if (days == d) PipBlack else PipAmber, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold) }
                    }
                }
            }

            // Temperature slider
            Column {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("SICAKLIK", color = PipAmber.copy(0.7f), fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    Text("${tempC}°C", color = PipAmber, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Slider(
                    value = tempC.toFloat(),
                    onValueChange = { tempC = it.roundToInt() },
                    valueRange = -10f..50f,
                    steps = 59,
                    colors = SliderDefaults.colors(thumbColor = PipAmber, activeTrackColor = PipAmber, inactiveTrackColor = PipAmber.copy(0.3f))
                )
                Text(
                    when {
                        tempC < 5 -> "❄ SOĞUK — Hipotermi riski"
                        tempC < 25 -> "🌤 ILIMAN"
                        else -> "🔥 SICAK — Dehidrasyon riski"
                    },
                    color = PipAmber.copy(0.5f), fontFamily = FontFamily.Monospace, fontSize = 10.sp
                )
            }

            // Activity level
            Column {
                Text("AKTİVİTE SEVİYESİ", color = PipAmber.copy(0.7f), fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    actLabels.forEachIndexed { i, label ->
                        Box(
                            Modifier.weight(1f).height(60.dp)
                                .background(if (activityLevel == i) PipAmber.copy(0.2f) else PipBlack, RoundedCornerShape(4.dp))
                                .border(1.dp, if (activityLevel == i) PipAmber else PipAmber.copy(0.3f), RoundedCornerShape(4.dp))
                                .clickable { activityLevel = i },
                            contentAlignment = Alignment.Center
                        ) { Text(label, color = PipAmber, fontFamily = FontFamily.Monospace, fontSize = 9.sp, fontWeight = FontWeight.Bold) }
                    }
                }
            }

            // Wound checkbox
            Row(
                Modifier.fillMaxWidth().border(1.dp, PipAmber.copy(0.3f), RoundedCornerShape(4.dp)).clickable { hasWounds = !hasWounds }.padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
            ) {
                Text("YARA / HASTALAMA (+0.5L/gün)", color = PipAmber, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                Box(Modifier.size(20.dp).border(2.dp, if (hasWounds) PipRed else PipAmber.copy(0.5f)).background(if (hasWounds) PipRed else PipBlack))
            }

            Divider(color = PipAmber, thickness = 1.dp)

            // Results
            Text("HESAP SONUÇLARI", color = PipAmber.copy(0.7f), fontFamily = FontFamily.Monospace, fontSize = 11.sp)

            // Water result
            Box(Modifier.fillMaxWidth().border(2.dp, PipAmber, RectangleShape).background(PipAmber.copy(0.05f)).padding(12.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("💧 SU İHTİYACI", color = PipAmber, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Kişi başı/gün:", color = PipAmber.copy(0.7f), fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                        Text("${"%.1f".format(waterPerPersonPerDay)} L", color = PipAmber, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("TOPLAM ($persons kişi, $days gün):", color = PipAmber.copy(0.7f), fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                        Text("${"%.1f".format(totalWaterL)} L", color = PipAmber, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    }
                    Text("⚠ KRİTİK MİNİMUM: ${"%.1f".format(criticalWaterL)}L (hayatta kalmak için)", color = PipRed, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                }
            }

            // Calorie result
            Box(Modifier.fillMaxWidth().border(2.dp, PipAmber, RectangleShape).background(PipAmber.copy(0.05f)).padding(12.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("🍫 KALORI İHTİYACI", color = PipAmber, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Kişi başı/gün:", color = PipAmber.copy(0.7f), fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                        Text("${calPerPersonPerDay.roundToInt()} kcal", color = PipAmber, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("TOPLAM:", color = PipAmber.copy(0.7f), fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                        Text("${totalKcal.roundToInt()} kcal", color = PipAmber, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    }
                    Divider(color = PipAmber.copy(0.3f))
                    Text("≈ ${"%.1f".format(riceKg)} kg pirinç / $energyBars enerji barı", color = PipAmber.copy(0.7f), fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                }
            }

            Box(Modifier.fillMaxWidth().border(1.dp, PipAmber.copy(0.2f), RectangleShape).padding(8.dp)) {
                Text("US Army FM 21-76 + WHO hidrasyon yönergeleri.\nEgzersiz, hastalık, yüksek irtifa durumlarında %20 ekle.", color = PipAmber.copy(0.4f), fontFamily = FontFamily.Monospace, fontSize = 10.sp, lineHeight = 14.sp)
            }
        }
    }
}

@Composable
fun CalcRow(label: String, value: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = PipAmber.copy(0.7f), fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            Text(value, color = PipAmber, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        content()
    }
}
