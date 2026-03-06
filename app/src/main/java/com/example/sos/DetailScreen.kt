package com.example.sos

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DetailScreen(screenName: String, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PipBlack)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // HEADER
        Text(
            text = "MODULE: $screenName",
            color = PipGreen,
            fontFamily = FontFamily.Monospace,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 30.dp)
        )

        // CONTENT PLACEHOLDER
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .border(1.dp, PipGreen, RectangleShape)
        ) {
            Text(
                text = "WAITING FOR INPUT...",
                color = PipGreen,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // BACK BUTTON
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 40.dp)
                .height(60.dp)
                .background(PipGreen) // Inverted colors for the button
                .clickable { onBack() }, // CLICK LISTENER
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "< RETURN TO GRID",
                color = PipBlack,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        }
    }
}