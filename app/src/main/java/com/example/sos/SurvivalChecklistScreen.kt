package com.example.sos

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class CheckItem(val id: String, val category: String, val label: String, val critical: Boolean = false)

private val checklistItems = listOf(
    // Ekipman
    CheckItem("eq1", "EKİPMAN", "Telefon tam şarj", critical = true),
    CheckItem("eq2", "EKİPMAN", "Güç bankası şarj"),
    CheckItem("eq3", "EKİPMAN", "İlk yardım çantası"),
    CheckItem("eq4", "EKİPMAN", "Bıçak / multitool"),
    CheckItem("eq5", "EKİPMAN", "El feneri + batarya"),
    CheckItem("eq6", "EKİPMAN", "Düdük"),
    CheckItem("eq7", "EKİPMAN", "Yanmaz çakmak / kibrıt"),
    CheckItem("eq8", "EKİPMAN", "Emergency blanket (alüminyum folyo battaniye)"),
    // İletişim
    CheckItem("co1", "İLETİŞİM", "Acil kişiler kayıtlı (app'te)", critical = true),
    CheckItem("co2", "İLETİŞİM", "Aileni rotanı bilip biliyor mu?", critical = true),
    CheckItem("co3", "İLETİŞİM", "GSM/VHF telsiz yanında mı?"),
    CheckItem("co4", "İLETİŞİM", "GPS koordinatlarını test et"),
    // Navigasyon
    CheckItem("na1", "NAVİGASYON", "Offline harita indirildi mi?"),
    CheckItem("na2", "NAVİGASYON", "Rota waypoint'leri işaretlendi mi?"),
    CheckItem("na3", "NAVİGASYON", "Pusula kalibre edildi mi?"),
    CheckItem("na4", "NAVİGASYON", "Gün batımı saati kontrol edildi mi?"),
    // Tıbbi
    CheckItem("me1", "TIBBİ", "Kişisel ilaçlar yanında", critical = true),
    CheckItem("me2", "TIBBİ", "Alerji bilgisi acil kişilere iletildi"),
    CheckItem("me3", "TIBBİ", "Tur öncesi sağlık durumu uygun"),
    // Su & Yiyecek
    CheckItem("sf1", "SU & YİYECEK", "Su hesabı yapıldı (SurvCalc)"),
    CheckItem("sf2", "SU & YİYECEK", "Su arıtma tableti / filtre"),
    CheckItem("sf3", "SU & YİYECEK", "Acil enerji barı / tatlı"),
    // Acil Plan
    CheckItem("ep1", "ACİL PLAN", "Toplanma noktası belirlendi", critical = true),
    CheckItem("ep2", "ACİL PLAN", "SOS sinyal kodu biliniyor (... --- ...)"),
    CheckItem("ep3", "ACİL PLAN", "Geri dönüş saati belirlenip iletildi"),
)

@Composable
fun SurvivalChecklistScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("SurvivalChecklist", Context.MODE_PRIVATE) }

    var checkedIds by remember { mutableStateOf(loadChecked(prefs)) }
    val categories = remember { checklistItems.map { it.category }.distinct() }

    val totalCount = checklistItems.size
    val checkedCount = checkedIds.size
    val criticalTotal = checklistItems.count { it.critical }
    val criticalChecked = checklistItems.count { it.critical && checkedIds.contains(it.id) }
    val progress = checkedCount.toFloat() / totalCount

    fun toggle(id: String) {
        checkedIds = if (checkedIds.contains(id)) checkedIds - id else checkedIds + id
        prefs.edit().putStringSet("checked", checkedIds).apply()
    }

    fun resetAll() {
        checkedIds = emptySet()
        prefs.edit().remove("checked").apply()
    }

    Column(modifier = Modifier.fillMaxSize().background(PipBlack).systemBarsPadding()) {
        ScreenHeader(title = "CHECKLIST", subtitle = "HAYATTA KALMA KONTROL LİSTESİ", onBack = onBack)

        Column(modifier = Modifier.fillMaxSize()) {
            // Progress header
            Box(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)
                    .border(2.dp, PipAmber, RoundedCornerShape(4.dp))
                    .padding(10.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("HAZIRLIK", color = PipAmber, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("$checkedCount / $totalCount", color = PipAmber, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                        color = when { progress >= 0.8f -> PipGreen; progress >= 0.5f -> PipAmber; else -> PipRed },
                        trackColor = PipAmber.copy(0.2f)
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            when { progress >= 0.8f -> "✓ HAZIR"; progress >= 0.5f -> "◑ KISMI HAZIR"; else -> "✗ HAZIR DEĞİL" },
                            color = when { progress >= 0.8f -> PipGreen; progress >= 0.5f -> PipAmber; else -> PipRed },
                            fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp
                        )
                        Text(
                            "KRİTİK: $criticalChecked/$criticalTotal",
                            color = if (criticalChecked == criticalTotal) PipGreen else PipRed,
                            fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Reset button
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), horizontalArrangement = Arrangement.End) {
                Box(
                    Modifier.background(PipRed.copy(0.1f), RoundedCornerShape(3.dp)).border(1.dp, PipRed.copy(0.4f), RoundedCornerShape(3.dp)).clickable { resetAll() }.padding(horizontal = 10.dp, vertical = 4.dp)
                ) { Text("↺ SIFIRLA", color = PipRed, fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            }

            Divider(color = PipAmber.copy(0.3f), modifier = Modifier.padding(horizontal = 8.dp))

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                categories.forEach { category ->
                    item {
                        Text(
                            "  ── $category ──",
                            color = PipAmber.copy(0.5f),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                        )
                    }
                    val catItems = checklistItems.filter { it.category == category }
                    items(catItems, key = { it.id }) { item ->
                        val isChecked = checkedIds.contains(item.id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    when { isChecked -> PipAmber.copy(0.08f); item.critical -> PipRed.copy(0.05f); else -> PipBlack },
                                    RoundedCornerShape(4.dp)
                                )
                                .border(
                                    1.dp,
                                    when { isChecked -> PipAmber.copy(0.6f); item.critical -> PipRed.copy(0.4f); else -> PipAmber.copy(0.2f) },
                                    RoundedCornerShape(4.dp)
                                )
                                .clickable { toggle(item.id) }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Checkbox
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .border(2.dp, if (isChecked) PipAmber else if (item.critical) PipRed else PipAmber.copy(0.4f), RoundedCornerShape(3.dp))
                                    .background(if (isChecked) PipAmber else Color.Transparent, RoundedCornerShape(3.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isChecked) Text("✓", color = PipBlack, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    item.label,
                                    color = if (isChecked) PipAmber.copy(0.5f) else PipAmber,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 13.sp,
                                    fontWeight = if (item.critical) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                            if (item.critical && !isChecked) {
                                Text("!", color = PipRed, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun loadChecked(prefs: SharedPreferences): Set<String> =
    prefs.getStringSet("checked", emptySet()) ?: emptySet()
