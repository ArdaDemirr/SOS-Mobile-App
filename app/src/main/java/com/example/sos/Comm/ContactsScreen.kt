package com.example.sos.Comm

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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sos.*
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
    val clipboardManager = LocalClipboardManager.current // PASTE MANAGER

    var contacts by remember { mutableStateOf(listOf<ContactEntity>()) }
    var showAddDialog by remember { mutableStateOf(false) }

    var inputUuid by remember { mutableStateOf("") }
    var inputName by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        contacts = withContext(Dispatchers.IO) { db.getAllContacts() }
    }

    SosScreenScaffold(
        title = "REHBER",
        subtitle = "İletişim kurabileceğiniz kişileri yönetin",
        accentColor = PipAmber,
        onBack = onBack
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            // Manual padding logic: 16.dp horizontal via contentPadding
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                // ADD BUTTON (Moved to top of list to fit the tactical theme)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(PipAmber.copy(0.1f), RoundedCornerShape(4.dp))
                        .border(1.dp, PipAmber.copy(0.5f), RoundedCornerShape(4.dp))
                        .clickable { inputUuid = ""; inputName = ""; showAddDialog = true }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.height(SosSpaceSm))
                    Icon(Icons.Default.Add, null, tint = PipAmber, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Yeni Kişi Ekleyin", color = PipAmber, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
            }

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
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            containerColor = PipBlack,
            title = { Text("Yeni Kişi", color = PipAmber, fontFamily = FontFamily.Monospace) },
            text = {
                Column {
                    OutlinedTextField(
                        value = inputName, onValueChange = { inputName = it },
                        label = { Text("İsim", color = PipAmber) },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PipAmber, unfocusedBorderColor = PipAmber.copy(0.5f), focusedTextColor = PipAmber)
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = inputUuid, onValueChange = { inputUuid = it },
                        label = { Text("Kişinin UUID'sini Girin", color = PipAmber) },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PipAmber, unfocusedBorderColor = PipAmber.copy(0.5f), focusedTextColor = PipAmber)
                    )
                    Spacer(Modifier.height(8.dp))

                    Button(
                        onClick = {
                            val pasteText = clipboardManager.getText()?.text
                            if (!pasteText.isNullOrEmpty()) {
                                inputUuid = pasteText
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PipAmber.copy(0.15f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PipAmber.copy(0.5f))
                    ) {
                        Text("Yapıştır", color = PipAmber, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
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
                    Text("Kaydet", color = PipBlack)
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