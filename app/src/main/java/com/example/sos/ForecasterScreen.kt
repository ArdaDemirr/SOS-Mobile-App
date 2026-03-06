package com.example.sos

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ForecasterScreen(onBack: () -> Unit) {
    // --- STATE ---
    var selectedCloud by remember { mutableStateOf<String?>(null) }
    var selectedWind by remember { mutableStateOf<String?>(null) }
    var selectedSky by remember { mutableStateOf<String?>(null) }

    // --- PREDICTION LOGIC ---
    val prediction = remember(selectedCloud, selectedWind, selectedSky) {
        calculateWeather(selectedCloud, selectedWind, selectedSky)
    }

    // --- UI ---
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PipBlack)
            .systemBarsPadding()
            .imePadding()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // HEADER
        Row(
            modifier = Modifier.fillMaxWidth().height(60.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(50.dp).background(PipAmber, RoundedCornerShape(8.dp)).clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.ArrowBack, null, tint = PipBlack)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text("FORECASTER", color = PipAmber, fontSize = 24.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Divider(color = PipAmber, thickness = 2.dp)

        // SCROLLABLE CONTENT
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // --- TACTICAL GUIDE POSTER (NEW IMAGE) ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, PipAmber, RoundedCornerShape(12.dp))
                    // Inner padding between border and image for a "screen inside a screen" look
                    .padding(6.dp)
            ) {
                // MAKE SURE you added the image to res/drawable/ and named it 'cloud_guide_poster'
                Image(
                    painter = painterResource(id = R.drawable.weather_cloud),
                    contentDescription = "Cloud Identification Guide",
                    modifier = Modifier
                        .fillMaxWidth()
                        // Clip corners slightly to match the container border
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.FillWidth // Ensure it fits width-wise
                )
            }

            // SECTION 1: CLOUDS
            TacticalSelector(
                title = "1. CLOUD FORMATION (SEE GUIDE ABOVE)",
                options = listOf("High Wisps (Cirrus)", "Flat Sheets (Stratus)", "Fluffy/Tall (Cumulus)", "Dark Wall (Nimbus)"),
                selected = selectedCloud,
                onSelect = { selectedCloud = it }
            )


            // --- TACTICAL GUIDE POSTER (NEW IMAGE) ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, PipAmber, RoundedCornerShape(12.dp))
                    // Inner padding between border and image for a "screen inside a screen" look
                    .padding(6.dp)
            ) {
                // MAKE SURE you added the image to res/drawable/ and named it 'cloud_guide_poster'
                Image(
                    painter = painterResource(id = R.drawable.weather_wind),
                    contentDescription = "Cloud Identification Guide",
                    modifier = Modifier
                        .fillMaxWidth()
                        // Clip corners slightly to match the container border
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.FillWidth // Ensure it fits width-wise
                )
            }

            // SECTION 2: WIND
            TacticalSelector(
                title = "2. WIND SHIFT",
                options = listOf("Steady / No Change", "Veering (Clockwise)", "Backing (Counter-CW)"),
                selected = selectedWind,
                onSelect = { selectedWind = it }
            )


            // --- TACTICAL GUIDE POSTER (NEW IMAGE) ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, PipAmber, RoundedCornerShape(12.dp))
                    // Inner padding between border and image for a "screen inside a screen" look
                    .padding(6.dp)
            ) {
                // MAKE SURE you added the image to res/drawable/ and named it 'cloud_guide_poster'
                Image(
                    painter = painterResource(id = R.drawable.weather_sign),
                    contentDescription = "Cloud Identification Guide",
                    modifier = Modifier
                        .fillMaxWidth()
                        // Clip corners slightly to match the container border
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.FillWidth // Ensure it fits width-wise
                )
            }

            // SECTION 3: SKY SIGNS
            TacticalSelector(
                title = "3. VISUAL SIGNS",
                options = listOf("None", "Red Sky at Morning", "Red Sky at Night", "Halo around Sun/Moon"),
                selected = selectedSky,
                onSelect = { selectedSky = it }
            )

            // RESULT BOX
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, if (prediction.contains("STORM")) PipRed else PipGreen, RectangleShape)
                    .padding(16.dp)
            ) {
                Column {
                    Text("TACTICAL FORECAST:", color = Color.Gray, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = prediction,
                        color = if (prediction.contains("STORM") || prediction.contains("RAIN")) PipRed else PipGreen,
                        fontSize = 20.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // RESET BUTTON
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .border(1.dp, Color.Gray, RectangleShape)
                    .clickable {
                        selectedCloud = null
                        selectedWind = null
                        selectedSky = null
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("RESET DATA", color = Color.Gray, fontFamily = FontFamily.Monospace)
            }

            // Extra spacer for scrolling past the bottom
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

// --- REUSABLE COMPONENT (Unchanged) ---
@Composable
fun TacticalSelector(title: String, options: List<String>, selected: String?, onSelect: (String) -> Unit) {
    Column {
        Text(title, color = PipAmber, fontSize = 14.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        options.forEach { option ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .height(40.dp)
                    .background(if (selected == option) PipAmber.copy(alpha = 0.2f) else Color.Transparent)
                    .border(1.dp, if (selected == option) PipAmber else Color.DarkGray, RoundedCornerShape(4.dp))
                    .clickable { onSelect(option) }
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = option,
                    color = if (selected == option) PipAmber else Color.Gray,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f)
                )
                if (selected == option) {
                    Icon(Icons.Default.Check, null, tint = PipAmber, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

// --- LOGIC ENGINE (Unchanged) ---
fun calculateWeather(cloud: String?, wind: String?, sky: String?): String {
    if (cloud == null && wind == null && sky == null) return "AWAITING INPUT..."

    // 1. CRITICAL THREATS (Immediate)
    if (cloud == "Dark Wall (Nimbus)") return "STORM IMMINENT. SEEK COVER."
    if (wind == "Backing (Counter-CW)") return "LOW PRESSURE APPROACHING. RAIN LIKELY."
    if (sky == "Halo around Sun/Moon") return "MOISTURE HIGH. RAIN IN 12-24H."
    if (sky == "Red Sky at Morning") return "STORM APPROACHING (Sailor's Warning)."

    // 2. GOOD SIGNS
    if (sky == "Red Sky at Night") return "HIGH PRESSURE. CLEAR WEATHER AHEAD."
    if (cloud == "Flat Sheets (Stratus)" && wind == "Steady / No Change") return "STABLE. LIGHT DRIZZLE OR OVERCAST."
    if (cloud == "High Wisps (Cirrus)") return "CHANGE COMING IN 24H (WARM FRONT)."
    if (cloud == "Fluffy/Tall (Cumulus)") return "FAIR WEATHER (Unless growing tall)."

    // 3. COMPLEX COMBOS
    if (cloud == "High Wisps (Cirrus)" && wind == "Backing (Counter-CW)") return "STORM FRONT IN 24H."

    return "ANALYZING... ADD MORE DATA."
}