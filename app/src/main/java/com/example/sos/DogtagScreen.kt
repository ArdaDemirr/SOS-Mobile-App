package com.example.sos

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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

    var name by remember { mutableStateOf("") }
    var surname by remember { mutableStateOf("") }

    // Dropdowns
    var gender by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var bloodType by remember { mutableStateOf("") }

    // Numeric Fields
    var weight by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }

    // String Fields
    var allergies by remember { mutableStateOf("") }
    var medications by remember { mutableStateOf("") }
    var pastOperations by remember { mutableStateOf("") }
    var emergencyContactsInput by remember { mutableStateOf("") }

    // --- STRICT DATA LISTS ---
    val genderOptions = listOf("Male", "Female", "Other", "Prefer not to say")
    val bloodOptions = listOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")
    val ageOptions = (18..100).map { it.toString() }

    // --- LOAD DATA ON START ---
    LaunchedEffect(Unit) {
        val existingData = withContext(Dispatchers.IO) { db.getDogtag() }
        if (existingData != null) {
            uuid = existingData.userUuid
            name = existingData.name
            surname = existingData.surname
            gender = existingData.gender
            age = if (existingData.age == 0) "" else existingData.age.toString()
            weight = if (existingData.weight == 0.0) "" else existingData.weight.toString()
            height = if (existingData.height == 0.0) "" else existingData.height.toString()
            bloodType = existingData.bloodType
            allergies = existingData.allergies
            medications = existingData.medications
            pastOperations = existingData.pastOperations

            // Join the list into a comma-separated string for editing
            emergencyContactsInput = existingData.emergencyContacts.joinToString(", ")
        } else {
            uuid = UUID.randomUUID().toString()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PipBlack)
            .imePadding()
            .systemBarsPadding()
    ) {
        ScreenHeader(
            title = "Künye",
            subtitle = "ID: $uuid",
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Identity
            DogtagField("FIRST NAME", name) { name = it }
            DogtagField("SURNAME", surname) { surname = it }

            // Dropdowns
            PipDropdown("GENDER", genderOptions, gender) { gender = it }
            PipDropdown("AGE", ageOptions, age) { age = it }

            // Body Metrics (Numeric Keyboards forced)
            DogtagField("WEIGHT (kg)", weight, KeyboardOptions(keyboardType = KeyboardType.Number)) { weight = it }
            DogtagField("HEIGHT (cm)", height, KeyboardOptions(keyboardType = KeyboardType.Number)) { height = it }

            // Medical
            PipDropdown("BLOOD TYPE", bloodOptions, bloodType) { bloodType = it }
            DogtagField("KNOWN ALLERGIES", allergies) { allergies = it }
            DogtagField("MEDICINES IN USE", medications) { medications = it }
            DogtagField("PAST OPERATIONS", pastOperations) { pastOperations = it }

            // Contacts
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
            DogtagField("", emergencyContactsInput) { emergencyContactsInput = it }

            Spacer(modifier = Modifier.height(20.dp))

            // --- SAVE & SYNC BUTTON ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp)
                    .background(PipAmber.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                    .border(2.dp, PipAmber, RoundedCornerShape(4.dp))
                    .clickable {
                        scope.launch(Dispatchers.IO) {
                            // Convert comma string back to a clean list
                            val contactsList = emergencyContactsInput
                                .split(",")
                                .map { it.trim() }
                                .filter { it.isNotEmpty() }

                            val newDogtag = DogtagEntity(
                                userUuid = uuid,
                                publicKey = "STATIC_KEY_GENERATE_LATER",
                                name = name,
                                surname = surname,
                                gender = gender,
                                age = age.toIntOrNull() ?: 0,
                                weight = weight.toDoubleOrNull() ?: 0.0,
                                height = height.toDoubleOrNull() ?: 0.0,
                                bloodType = bloodType,
                                allergies = allergies,
                                medications = medications,
                                pastOperations = pastOperations,
                                emergencyContacts = contactsList
                            )

                            // 1. Save locally to Room DB (Priority 1: Offline First)
                            db.saveDogtag(newDogtag)

                            // 2. Sync to Spring Boot Server (The Network Hop)
                            try {
                                val response = RetrofitInstance.api.syncDogtag(newDogtag)
                                if (response.isSuccessful) {
                                    println("Tactical Sync: SUCCESS - Profile locked in server.")
                                } else {
                                    println("Tactical Sync: FAILED - Server rejected packet. Error Code: ${response.code()}")
                                }
                            } catch (e: Exception) {
                                println("Tactical Sync: OFFLINE - Packet stored locally. Error: ${e.message}")
                            }

                            // 3. Navigate back to previous screen
                            withContext(Dispatchers.Main) {
                                onBack()
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "SAVE & SYNC TO SECURE STORAGE",
                    color = PipAmber,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black
                )
            }
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

// --- REUSABLE PIP-BOY COMPONENTS ---

@Composable
fun DogtagField(
    label: String,
    value: String,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    onValueChange: (String) -> Unit
) {
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
                keyboardOptions = keyboardOptions,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PipDropdown(
    label: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
        Text(
            text = label,
            color = PipAmber.copy(alpha = 0.8f),
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, PipAmber.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                    .background(PipBlack)
                    .padding(12.dp)
                    .menuAnchor()
            ) {
                Text(
                    text = selectedOption.ifEmpty { "..." },
                    color = if (selectedOption.isEmpty()) PipAmber.copy(alpha = 0.2f) else PipAmber,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 16.sp
                )
            }

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .background(PipBlack)
                    .border(1.dp, PipAmber)
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = option,
                                color = PipAmber,
                                fontFamily = FontFamily.Monospace
                            )
                        },
                        onClick = {
                            onOptionSelected(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}