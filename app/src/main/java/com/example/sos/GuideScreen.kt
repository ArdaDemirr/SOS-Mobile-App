package com.example.sos

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun GuideScreen(onBack: () -> Unit) {
    // Basic Survival Database
    val topics = listOf(
        GuideTopic("MORSE CODE", "SOS: ... --- ...\nHELP: .... . .-.. .--.\nYES: -. .--\nNO: -. ---"),
        GuideTopic("WATER PURITY", "1. BOIL: Rolling boil for 1 min.\n2. BLEACH: 2 drops per liter, wait 30m.\n3. SOLAR: UV bottle in sun for 6h."),
        GuideTopic("HYPOTHERMIA", "SYMPTOMS: Shivering, confusion.\nTREAT: Remove wet clothes. Skin-to-skin contact. Warm (not hot) drinks."),
        GuideTopic("BLEEDING", "1. PRESSURE: Direct pressure on wound.\n2. ELEVATE: Above heart level.\n3. TOURNIQUET: High & tight if life-threatening."),
        GuideTopic("FIRE STARTING", "TINDER: Dry grass, bark.\nKINDLING: Small twigs.\nFUEL: Large logs.\nSTRUCTURE: Teepee or Log Cabin.")
    )

    var selectedTopic by remember { mutableStateOf<GuideTopic?>(null) }

    Column(modifier = Modifier.fillMaxSize().background(PipBlack).padding(40.dp)) {

        // HEADER
        Text("SURVIVAL DATABASE", color = PipAmber, fontSize = 24.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(PipAmber))

        Spacer(modifier = Modifier.height(16.dp))

        if (selectedTopic == null) {
            // LIST VIEW
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(topics) { topic ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, PipAmber, RectangleShape)
                            .clickable { selectedTopic = topic }
                            .padding(16.dp)
                    ) {
                        Text("> ${topic.title}", color = PipAmber, fontFamily = FontFamily.Monospace, fontSize = 18.sp)
                    }
                }
            }
        } else {
            // DETAIL VIEW
            Column {
                Text("> ${selectedTopic!!.title}", color = PipAmber, fontSize = 20.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                Text(selectedTopic!!.content, color = PipGreen, fontFamily = FontFamily.Monospace, fontSize = 16.sp, lineHeight = 24.sp)

                Spacer(modifier = Modifier.weight(1f))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 50.dp)
                        .border(1.dp, PipAmber)
                        .clickable { selectedTopic = null } // Go back to list
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("BACK TO INDEX", color = PipAmber, fontFamily = FontFamily.Monospace)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // MAIN BACK BUTTON
        if (selectedTopic == null) {
            Box(
                modifier = Modifier.fillMaxWidth().height(60.dp).background(PipAmber).clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Text("< RETURN TO DASHBOARD", color = PipBlack, fontSize = 18.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            }
        }
    }
}

data class GuideTopic(val title: String, val content: String)