package com.example.sos

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import com.example.sos.database.ContactEntity
import com.example.sos.database.DogtagEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

@Composable
fun DogtagScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Access both DAOs from the database
    val database = AppDatabase.getDatabase(context)
    val dogtagDao = remember { database.dogtagDao() }
    val contactDao = remember { database.contactDao() }

    // --- STATE VARIABLES ---
    var availableContacts by remember { mutableStateOf(listOf<ContactEntity>()) }
    val selectedEmergencyContacts = remember { mutableStateListOf<String>() }

    var uuid by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var surname by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var bloodType by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var allergies by remember { mutableStateOf("") }
    var medications by remember { mutableStateOf("") }
    var pastOperations by remember { mutableStateOf("") }

    // --- STRICT DATA LISTS ---
    val genderOptions = listOf("Male", "Female", "Other", "Prefer not to say")
    val bloodOptions = listOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")
    val ageOptions = (18..100).map { it.toString() }

    // --- LOAD DATA ON START ---
    LaunchedEffect(Unit) {
        val existingData = withContext(Dispatchers.IO) { dogtagDao.getDogtag() }
        val contactsFromDb = withContext(Dispatchers.IO) { contactDao.getAllContacts() }

        availableContacts = contactsFromDb

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

            // Sync the selected list with saved data
            selectedEmergencyContacts.clear()
            selectedEmergencyContacts.addAll(existingData.emergencyContacts)
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
            title = "Künye / Identity",
            subtitle = "NODE ID: $uuid",
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Identity Fields
            DogtagField("FIRST NAME", name) { name = it }
            DogtagField("SURNAME", surname) { surname = it }
            PipDropdown("GENDER", genderOptions, gender) { gender = it }
            PipDropdown("AGE", ageOptions, age) { age = it }

            // Body Metrics
            Row(Modifier.fillMaxWidth()) {
                Box(Modifier.weight(1f)) { DogtagField("WEIGHT (kg)", weight, KeyboardOptions(keyboardType = KeyboardType.Number)) { weight = it } }
                Spacer(Modifier.width(8.dp))
                Box(Modifier.weight(1f)) { DogtagField("HEIGHT (cm)", height, KeyboardOptions(keyboardType = KeyboardType.Number)) { height = it } }
            }

            // Medical Info
            PipDropdown("BLOOD TYPE", bloodOptions, bloodType) { bloodType = it }
            DogtagField("KNOWN ALLERGIES", allergies) { allergies = it }
            DogtagField("MEDICINES IN USE", medications) { medications = it }
            DogtagField("PAST OPERATIONS", pastOperations) { pastOperations = it }

            // --- SOS CONTACT SELECTION (DIRECT FROM DIRECTORY) ---
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "SOS TARGETS (FROM DIRECTORY)",
                color = PipAmber,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (availableContacts.isEmpty()) {
                Text(
                    text = "NO CONTACTS FOUND. ADD ENTRIES TO DIRECTORY FIRST.",
                    color = PipRed,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            } else {
                availableContacts.forEach { contact ->
                    val isSelected = selectedEmergencyContacts.contains(contact.contactUuid)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .border(1.dp, if (isSelected) PipAmber else PipAmber.copy(0.2f), RoundedCornerShape(4.dp))
                            .clickable {
                                if (isSelected) selectedEmergencyContacts.remove(contact.contactUuid)
                                else selectedEmergencyContacts.add(contact.contactUuid)
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = null, // Logic handled by Row click
                            colors = CheckboxDefaults.colors(
                                checkedColor = PipAmber,
                                checkmarkColor = PipBlack,
                                uncheckedColor = PipAmber.copy(0.4f)
                            )
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(contact.displayName, color = PipAmber, fontWeight = FontWeight.Bold)
                            Text(
                                contact.contactUuid.take(16) + "...",
                                color = PipAmber.copy(0.6f),
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // --- SAVE & SYNC BUTTON ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .background(PipAmber.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                    .border(2.dp, PipAmber, RoundedCornerShape(4.dp))
                    .clickable {
                        scope.launch(Dispatchers.IO) {
                            val newDogtag = DogtagEntity(
                                userUuid = uuid,
                                publicKey = "SECURE_ELEMENT_ACTIVE",
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
                                emergencyContacts = selectedEmergencyContacts.toList() // Saving selection
                            )

                            // 1. Save locally
                            dogtagDao.saveDogtag(newDogtag)

                            // 2. Sync to Server
                            try {
                                val response = RetrofitInstance.api.syncDogtag(newDogtag)
                                if (response.isSuccessful) {
                                    println("Tactical Sync: SUCCESS")
                                }
                            } catch (e: Exception) {
                                println("Tactical Sync: OFFLINE (Stored Locally)")
                            }

                            withContext(Dispatchers.Main) {
                                onBack()
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "LOCK IN DATA & SYNC",
                    color = PipAmber,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black
                )
            }
            Spacer(modifier = Modifier.height(50.dp))
        }
    }
}

// --- REUSABLE COMPONENTS ---

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
                Text("...", color = PipAmber.copy(alpha = 0.2f), fontFamily = FontFamily.Monospace, fontSize = 16.sp)
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
                modifier = Modifier.background(PipBlack).border(1.dp, PipAmber)
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(text = option, color = PipAmber, fontFamily = FontFamily.Monospace) },
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