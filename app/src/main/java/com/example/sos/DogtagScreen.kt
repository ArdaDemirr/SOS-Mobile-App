package com.example.sos

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sos.database.AppDatabase
import com.example.sos.database.DogtagEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

@Composable
fun DogtagScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { AppDatabase.getDatabase(context).dogtagDao() }

    // --- STATE VARIABLES ---
    var uuid by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var bloodType by remember { mutableStateOf("") }
    var allergies by remember { mutableStateOf("") }
    var medications by remember { mutableStateOf("") }
    var pastOperations by remember { mutableStateOf("") }
    var emergencyUuids by remember { mutableStateOf("") }

    // --- LOAD DATA ON START ---
    LaunchedEffect(Unit) {
        val existingData = withContext(Dispatchers.IO) { db.getDogtag() }
        if (existingData != null) {
            uuid = existingData.userUuid
            fullName = existingData.fullName
            bloodType = existingData.bloodType
            allergies = existingData.allergies
            medications = existingData.medications
            pastOperations = existingData.pastOperations
            emergencyUuids = existingData.emergencyUuids
        } else {
            // Generate a permanent UUID for this phone if it is brand new
            uuid = UUID.randomUUID().toString()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PipBlack) // Using global PipBlack
            .imePadding()         // Adjusts for keyboard like BioScreen
            .systemBarsPadding()  // Keeps it out of the status bar area
    ) {
        // --- UNIFIED PIP-BOY HEADER ---
        ScreenHeader(
            title = "Künye",
            subtitle = "ID: $uuid",
            onBack = onBack
        )

        // --- SCROLLABLE CONTENT ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // --- INPUT FIELDS ---
            DogtagField("FULL NAME & SURNAME", fullName) { fullName = it }
            DogtagField("BLOOD TYPE (e.g., A+, O-)", bloodType) { bloodType = it }
            DogtagField("KNOWN ALLERGIES", allergies) { allergies = it }
            DogtagField("MEDICINES IN USE", medications) { medications = it }
            DogtagField("PAST OPERATIONS / CONDITIONS", pastOperations) { pastOperations = it }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "EMERGENCY CONTACTS",
                color = PipAmber,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Text(
                text = "Enter target UUIDs separated by commas",
                color = PipAmber.copy(alpha = 0.6f),
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            DogtagField("", emergencyUuids) { emergencyUuids = it }

            Spacer(modifier = Modifier.height(20.dp))

            // --- SAVE BUTTON ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp)
                    .background(PipAmber.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                    .border(2.dp, PipAmber, RoundedCornerShape(4.dp))
                    .clickable {
                        // SECURELY SAVE TO ROOM DATABASE
                        scope.launch(Dispatchers.IO) {
                            val newDogtag = DogtagEntity(
                                userUuid = uuid,
                                fullName = fullName,
                                bloodType = bloodType,
                                allergies = allergies,
                                medications = medications,
                                pastOperations = pastOperations,
                                emergencyUuids = emergencyUuids
                            )
                            db.saveDogtag(newDogtag)

                            withContext(Dispatchers.Main) {
                                onBack() // Return to dashboard once save is complete
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "SAVE TO SECURE STORAGE",
                    color = PipAmber,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

// Reusable Pip-Boy Input Field
@Composable
fun DogtagField(label: String, value: String, onValueChange: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
        if (label.isNotEmpty()) {
            Text(
                text = label,
                color = PipAmber.copy(alpha = 0.8f),
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, PipAmber.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                .background(PipBlack)
                .padding(12.dp)
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = TextStyle(color = PipAmber, fontFamily = FontFamily.Monospace, fontSize = 16.sp),
                cursorBrush = SolidColor(PipAmber),
                modifier = Modifier.fillMaxWidth()
            )
            if (value.isEmpty()) {
                Text(
                    text = "...",
                    color = PipAmber.copy(alpha = 0.2f),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 16.sp
                )
            }
        }
    }
}