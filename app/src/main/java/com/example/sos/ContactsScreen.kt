package com.example.sos

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sos.database.AppDatabase
import com.example.sos.database.ContactEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ContactsScreen(onBack: () -> Unit, onChat: (String, String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { AppDatabase.getDatabase(context).contactDao() }

    var contacts by remember { mutableStateOf(listOf<ContactEntity>()) }
    var showAddDialog by remember { mutableStateOf(false) }

    // State for Add/Edit
    var inputUuid by remember { mutableStateOf("") }
    var inputName by remember { mutableStateOf("") }

    // Load contacts from local phone storage
    LaunchedEffect(Unit) {
        contacts = withContext(Dispatchers.IO) { db.getAllContacts() }
    }

    Column(Modifier.fillMaxSize().background(PipBlack).systemBarsPadding()) {
        // Header
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("DIRECTORY", color = PipAmber, fontSize = 24.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            IconButton(onClick = {
                inputUuid = ""; inputName = ""; showAddDialog = true
            }, modifier = Modifier.border(1.dp, PipAmber, RoundedCornerShape(4.dp))) {
                Icon(Icons.Default.Add, "Add", tint = PipAmber)
            }
        }

        // List
        LazyColumn(Modifier.weight(1f).padding(horizontal = 16.dp)) {
            items(contacts) { contact ->
                ContactItem(
                    contact = contact,
                    onChat = { onChat(contact.contactUuid, contact.displayName) },
                    onDelete = {
                        scope.launch(Dispatchers.IO) {
                            db.deleteContact(contact)
                            contacts = db.getAllContacts()
                        }
                    }
                )
                Spacer(Modifier.height(8.dp))
            }
        }

        // Back Button
        Box(Modifier.fillMaxWidth().height(60.dp).background(PipAmber).clickable { onBack() }, contentAlignment = Alignment.Center) {
            Text("< RETURN <", color = PipBlack, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            containerColor = PipBlack,
            title = { Text("NEW CONTACT", color = PipAmber, fontFamily = FontFamily.Monospace) },
            text = {
                Column {
                    OutlinedTextField(
                        value = inputName, onValueChange = { inputName = it },
                        label = { Text("DISPLAY NAME", color = PipAmber) },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PipAmber, unfocusedBorderColor = PipAmber.copy(0.5f), focusedTextColor = PipAmber)
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = inputUuid, onValueChange = { inputUuid = it },
                        label = { Text("TARGET UUID", color = PipAmber) },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PipAmber, unfocusedBorderColor = PipAmber.copy(0.5f), focusedTextColor = PipAmber)
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (inputUuid.isNotBlank()) {
                        scope.launch(Dispatchers.IO) {
                            db.saveContact(ContactEntity(inputUuid, inputName.ifBlank { "Unknown Unit" }))
                            contacts = db.getAllContacts()
                        }
                        showAddDialog = false
                    }
                }, colors = ButtonDefaults.buttonColors(containerColor = PipAmber)) {
                    Text("SAVE", color = PipBlack)
                }
            }
        )
    }
}

@Composable
fun ContactItem(contact: ContactEntity, onChat: () -> Unit, onDelete: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().border(1.dp, PipAmber.copy(0.3f), RoundedCornerShape(4.dp))
            .background(PipAmber.copy(0.05f)).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(contact.displayName, color = PipAmber, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(contact.contactUuid.take(12) + "...", color = PipAmber.copy(0.6f), fontSize = 12.sp, fontFamily = FontFamily.Monospace)
        }
        IconButton(onClick = onChat) { Icon(Icons.Default.Send, null, tint = PipAmber) }
        IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, null, tint = PipRed) }
    }
}