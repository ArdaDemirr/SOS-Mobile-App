package com.example.sos

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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class StarInfo(
    val name: String,
    val designation: String,
    val mag: Float,
    val navUse: String,
    val howToFind: String
)

private val northernStars = listOf(
    StarInfo("Polaris", "α UMi", 1.97f, "TRUE KUZEY - neredeyse tam kuzey kutbunda. Sapma <1 derece.", "Buyuk Ayi'nin kurek yildizlarindan duz cizgi ciz. Kucuk Ayi'nin ucundadir."),
    StarInfo("Cassiopeia W", "α/β/γ Cas", 2.2f, "Kuzey dogrulama. Polaris karsi tarafindan bulunur.", "Cesme/W sekli. Polaris, Cassiopeia ile Buyuk Ayi'nin ortasindadir."),
    StarInfo("Vega", "α Lyr", 0.03f, "Yaz ucgeni referansi. Hemen batidan yukselir.", "En parlak yaz yildizi. Deneb + Altair ile Yaz Ucgeni'ni olusturur."),
    StarInfo("Betelgeuse", "α Ori", 0.5f, "Orion referansi. Bati-dogu yonu icin Orion Kusagi.", "Kirmizimsi, Orion'un sol omzunda. Sabah yilbasinda guneyde en yuksektedir."),
    StarInfo("Sirius", "α CMa", -1.46f, "Yonlendirme: Orion Kusagi boyunca guneydo guba bak.", "Tum gokyuzunun en parlak yildizi. Mavi-beyaz, ufka yakin titreye rek parlar.")
)

private val southernStars = listOf(
    StarInfo("Southern Cross (Crux)", "α-δ Cru", 0.8f, "TRUE GUNEY. Uzun ekseni x4.5 uzatinca guney kutbu.", "Guney Yarimkure'nin en kucuk, en parlak takimyildizi. 4 parlak yildiz."),
    StarInfo("Canopus", "α Car", -0.74f, "2. en parlak. Guney yarimkure navigasyonu icin Sirius ile kullanilir.", "Ara Carinae. Her zaman gorunur, ufka yakin. NASA uzay araclari bu yildizi referans alir."),
    StarInfo("Centaurus (Alfa/Beta)", "α/β Cen", -0.29f, "Crux'a isaret pointer yildizlari. Guneyi dogrular.", "En parlak iki yildiz. Guneydogu-kuzeybati hatti uzerindedir, Crux'a isaret eder.")
)

private data class NavTechnique(val title: String, val steps: List<String>)

private val techniques = listOf(
    NavTechnique("Polaris ile Kuzey Bulma", listOf(
        "1. Polaris'i bul (Buyuk Ayi'nin kurek yildizlari guzergahi)",
        "2. Polaris'e dogru yuzunu don",
        "3. Onunuz KUZEYDIF -- sag: DOGU, sol: BATI, arkasi: GUNEY",
        "4. Sapma <1 derece -- pusula ustten gecersizdir"
    )),
    NavTechnique("Guney Haci (Crux) ile Guney Bulma", listOf(
        "1. Crux'u bul -- 4 yildizdan olusan kucuk dortgen",
        "2. Uzun eksenini (alfa'dan delta'ya) x4.5 uzat",
        "3. O noktada Guney Gok Kutbu'dur",
        "4. Oradan dik asagiya ufka bak -- TRUE GUNEY orada"
    )),
    NavTechnique("Yildiz Hizi Yontemi (Evrensel)", listOf(
        "1. Herhangi bir parlak yildiz sec",
        "2. Cubugu yere sap, ucuna yildizi hizala",
        "3. 10-15 dakika bekle ve yildizin hareketini gozlemle:",
        "   Sol geliyor = KUZEYE bakiyorsun",
        "   Sag geliyor = GUNEYE bakiyorsun",
        "   Yukari cikiyor = DOGUYA bakiyorsun",
        "   Asagi iniyor = BATIYA bakiyorsun"
    )),
    NavTechnique("Orion Kusagi Yontemi", listOf(
        "1. Orion'un 3 kusak yildizini bul (hizalanmis 3 yildiz)",
        "2. Dogudan Bati'ya Alnitak-Alnilam-Mintaka sirasi",
        "3. Mintaka (bati ucu) neredeyse tam BATI'da dogar ve tam BATI'da batar",
        "4. Her enlemde calisir -- kuzey veya guney yarimkure fark etmez"
    ))
)

@Composable
fun StarNavScreen(onBack: () -> Unit) {
    var selectedHemisphere by remember { mutableStateOf("KUZEY") }
    var selectedStar by remember { mutableStateOf<StarInfo?>(null) }
    var selectedTech by remember { mutableStateOf<NavTechnique?>(null) }

    val stars = if (selectedHemisphere == "KUZEY") northernStars else southernStars

    val northChart = """
         * POLARIS
        /          \
  * BUYUK AYI        CASSIOPEIA *
  (Kuzey)              (W sekli)

    * VEGA (Yaz)   * BETELGEUSE
         \
          * SIRIUS (guneyde parlak)""".trimIndent()

    val southChart = """
     * *  CRUX  * *
        ||     (Beta & Alfa)
        vv    POINTER YILDIZLAR
    GUNEY KUTBU
          * CANOPUS (batida)""".trimIndent()

    Column(modifier = Modifier.fillMaxSize().background(PipBlack).systemBarsPadding()) {
        ScreenHeader(title = "STAR NAV", subtitle = "YILDIZ NAVIGASYON REHBERI", onBack = onBack)

        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // ASCII Star chart
            Box(
                Modifier.fillMaxWidth()
                    .border(1.dp, PipAmber.copy(0.3f), RoundedCornerShape(4.dp))
                    .background(PipAmber.copy(0.03f))
                    .padding(10.dp)
            ) {
                val chartText = if (selectedHemisphere == "KUZEY") northChart else southChart
                Text(
                    text = chartText,
                    color = PipAmber,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            }

            // Hemisphere selector
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf("KUZEY", "GUNEY").forEach { hemi ->
                    Box(
                        Modifier.weight(1f).height(44.dp)
                            .background(if (selectedHemisphere == hemi) PipAmber.copy(0.2f) else PipBlack, RoundedCornerShape(4.dp))
                            .border(2.dp, if (selectedHemisphere == hemi) PipAmber else PipAmber.copy(0.3f), RoundedCornerShape(4.dp))
                            .clickable { selectedHemisphere = hemi },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(hemi, color = PipAmber, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Text("NAVIGASYON YILDIZLARI", color = PipAmber.copy(0.6f), fontFamily = FontFamily.Monospace, fontSize = 11.sp)

            stars.forEach { star ->
                val isSelected = selectedStar == star
                Box(
                    Modifier.fillMaxWidth()
                        .border(1.dp, if (isSelected) PipAmber else PipAmber.copy(0.3f), RoundedCornerShape(4.dp))
                        .background(if (isSelected) PipAmber.copy(0.08f) else PipBlack)
                        .clickable { selectedStar = if (isSelected) null else star }
                        .padding(10.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("* ${star.name}", color = PipAmber, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("mag ${star.mag}", color = PipAmber.copy(0.5f), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                        }
                        Text(star.navUse, color = PipAmber.copy(0.8f), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                        if (isSelected) {
                            Divider(color = PipAmber.copy(0.3f), modifier = Modifier.padding(vertical = 4.dp))
                            Text("NASIL BULUNUR:", color = PipAmber.copy(0.6f), fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                            Text(star.howToFind, color = PipAmber, fontFamily = FontFamily.Monospace, fontSize = 12.sp, lineHeight = 17.sp)
                        }
                    }
                }
            }

            Divider(color = PipAmber.copy(0.4f))
            Text("YON BULMA TEKNIKLERI", color = PipAmber.copy(0.6f), fontFamily = FontFamily.Monospace, fontSize = 11.sp)

            techniques.forEach { tech ->
                val isSelected = selectedTech == tech
                Box(
                    Modifier.fillMaxWidth()
                        .border(1.dp, if (isSelected) PipAmber else PipAmber.copy(0.3f), RoundedCornerShape(4.dp))
                        .background(if (isSelected) PipAmber.copy(0.08f) else PipBlack)
                        .clickable { selectedTech = if (isSelected) null else tech }
                        .padding(10.dp)
                ) {
                    Column {
                        Text(tech.title, color = PipAmber, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        if (isSelected) {
                            Spacer(Modifier.height(6.dp))
                            tech.steps.forEach { step ->
                                Text(step, color = PipAmber.copy(0.8f), fontFamily = FontFamily.Monospace, fontSize = 11.sp, lineHeight = 16.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
