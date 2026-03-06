package com.example.sos

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class RadioFrequency(
    val freq: String,
    val band: String,
    val name: String,
    val use: String,
    val category: String
)

private val frequencies = listOf(
    RadioFrequency("156.800 MHz", "VHF Ch.16", "DENİZCİLİK ACİL", "Uluslararası deniz acil çağrı ve güvenlik kanalı. Mayday çağrıları buradan yapılır.", "MARITIME"),
    RadioFrequency("156.300 MHz", "VHF Ch.06", "GEMİ-GEMİ", "Gemiler arası güvenli operasyon haberleşmesi.", "MARITIME"),
    RadioFrequency("156.650 MHz", "VHF Ch.13", "KÖPRÜ-KÖPRÜ", "Navigasyon güvenliği için gemi köprüleri arası kanal.", "MARITIME"),
    RadioFrequency("121.500 MHz", "VHF AM", "HAVACILIK ACİL", "Uluslararası havacılık acil kanalı. Uçaklarca ve kurtarma ekipleri tarafından izlenir.", "AVIATION"),
    RadioFrequency("243.000 MHz", "UHF AM", "NATO GUARD", "Askeri havacılık acil kanalı. SAR (Arama Kurtarma) operasyonlarında kullanılır.", "AVIATION"),
    RadioFrequency("406.028 MHz", "UHF", "EPIRB / ELT", "Uydu tabanlı acil konum işaretleyiciler bu frekansı kullanır (COSPAS-SARSAT).", "SATELLITE"),
    RadioFrequency("2.182 kHz", "HF MF", "KISA DALGA ACİL", "Denizde uzun mesafe acil haberleşme. Eski ama güvenilir.", "HF"),
    RadioFrequency("14.300 MHz", "HF", "ARES/RACES", "Amatör radyo acil iletişim ağı. Tüm diğer sistemler çöktüğünde çalışır.", "HAM"),
    RadioFrequency("7.060 MHz", "HF", "AMATÖR ACİL", "Uluslararası amatör radyo acil haberleşme frekansı.", "HAM"),
    RadioFrequency("462.675 MHz", "UHF GMRS", "GMRS KANAL 20", "Kara arama-kurtarma operasyonlarında yaygın kullanılan kanal.", "LAND"),
    RadioFrequency("155.340 MHz", "VHF", "YANGIN/SAR", "Orman yangını ve kara SAR operasyonları için kullanılan frekans.", "LAND"),
    RadioFrequency("27.065 MHz", "CB Ch.9", "CB ACİL", "Sivil Bant kanal 9 — karayolu acil çağrı kanalı.", "CIVIL"),
)

private val categoryColors = mapOf(
    "MARITIME" to Color(0xFF004080),
    "AVIATION" to Color(0xFF800040),
    "SATELLITE" to Color(0xFF004000),
    "HF" to Color(0xFF804000),
    "HAM" to Color(0xFF600060),
    "LAND" to Color(0xFF406000),
    "CIVIL" to Color(0xFF405060),
)

private val maydaySteps = listOf(
    "1. Radyonu Ch.16 (VHF) veya 121.5 MHz (Havacılık) frekansına ayarla",
    "2. PTT (konuşma) düğmesine bas ve şunu söyle:",
    "   \"MAYDAY MAYDAY MAYDAY\"",
    "   \"Burası [taşıt adı/seni tanıtan bilgi]\"",
    "   \"Konumum: [GPS koordinatları veya referans nokta]\"",
    "   \"Acil durumum: [ne olduğunu açıkla]\"",
    "   \"Yardıma ihtiyacım var\"",
    "   \"[Kişi sayısı] kişiyiz\"",
    "3. PTT'yi bırak ve yanıt bekle (min. 10 saniye)",
    "4. Yanıt gelmezse iletimi tekrarla",
    "5. Mümkünse radyonu sadece Ch.16'da bırak — kurtarma ekipleri seni takip edebilir"
)

@Composable
fun RadioScreen(onBack: () -> Unit) {
    var selectedFreq by remember { mutableStateOf<RadioFrequency?>(null) }
    var showMaydayGuide by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PipBlack)
            .imePadding()
            .systemBarsPadding()
    ) {
        // --- HEADER ---
        ScreenHeader(title = "Radyo Frekansları", subtitle = "Kullanılabilir frekanslar", onBack = onBack)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(bottom = 24.dp, top = 8.dp)
        ) {
            // MAYDAY GUIDE BUTTON
            item {
                val borderColor by animateColorAsState(
                    if (showMaydayGuide) PipRed else PipAmber,
                    animationSpec = tween(300), label = "maydayBorder"
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(2.dp, borderColor, RectangleShape)
                        .background(if (showMaydayGuide) PipRed.copy(alpha = 0.15f) else PipBlack)
                        .clickable { showMaydayGuide = !showMaydayGuide }
                        .padding(12.dp)
                ) {
                    Column {
                        Text(
                            "[ MAYDAY ÇAĞRISI REHBERİ ]",
                            color = if (showMaydayGuide) PipRed else PipAmber,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        if (showMaydayGuide) {
                            Spacer(modifier = Modifier.height(8.dp))
                            maydaySteps.forEach { step ->
                                Text(
                                    step,
                                    color = PipAmber,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp,
                                    modifier = Modifier.padding(vertical = 1.dp)
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "ACİL FREKANSLAR",
                    color = PipAmber,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                Divider(color = PipAmber.copy(alpha = 0.4f), thickness = 1.dp, modifier = Modifier.padding(top = 4.dp))
            }

            // FREQUENCY LIST
            items(frequencies) { freq ->
                val isSelected = selectedFreq == freq
                val catColor = categoryColors[freq.category] ?: PipAmber
                val borderCol by animateColorAsState(
                    if (isSelected) PipAmber else PipAmber.copy(alpha = 0.4f),
                    animationSpec = tween(200), label = "freqBorder"
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, borderCol, RectangleShape)
                        .background(if (isSelected) catColor.copy(alpha = 0.25f) else PipBlack)
                        .clickable { selectedFreq = if (isSelected) null else freq }
                        .padding(10.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    freq.freq,
                                    color = PipAmber,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                                Text(
                                    freq.band,
                                    color = PipAmber.copy(alpha = 0.6f),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .background(catColor.copy(alpha = 0.7f), RoundedCornerShape(2.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    freq.category,
                                    color = Color.White,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            freq.name,
                            color = PipAmber,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        if (isSelected) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Divider(color = PipAmber.copy(alpha = 0.3f), thickness = 1.dp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                freq.use,
                                color = PipAmber.copy(alpha = 0.85f),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
