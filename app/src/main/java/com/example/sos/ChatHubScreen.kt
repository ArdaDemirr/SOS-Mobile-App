package com.example.sos

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sos.database.AppDatabase
import com.example.sos.database.ContactEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatHubScreen(myUuid: String, onConversationClick: (String, String) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }

    var contacts by remember { mutableStateOf(listOf<ContactEntity>()) }
    val allMessages by db.messageDao().getAllMessagesFlow().collectAsState(initial = emptyList())
    var showNewChatDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        contacts = withContext(Dispatchers.IO) { db.contactDao().getAllContacts() }
    }

    val recentConversations = remember(allMessages, contacts) {
        allMessages.groupBy { if (it.senderId == myUuid) it.targetId else it.senderId }
            .mapNotNull { (partnerId, msgs) ->
                val lastMsg = msgs.maxByOrNull { it.timestamp } ?: return@mapNotNull null
                val partnerName = contacts.find { it.contactUuid == partnerId }?.displayName ?: "Unknown Node"
                Pair(partnerId to partnerName, lastMsg)
            }.sortedByDescending { it.second.timestamp }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showNewChatDialog = true }, containerColor = SosAmber) {
                Icon(Icons.Default.Add, contentDescription = "New", tint = SosBg)
            }
        },
        containerColor = SosBg
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            SosScreenScaffold(title = "INBOX", subtitle = "SECURE MESH ACTIVE", onBack = onBack) {
                if (recentConversations.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("NO RECENT CONVERSATIONS", color = SosAmber.copy(0.4f), fontFamily = FontFamily.Monospace)
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(top = SosSpaceMd)) {
                        items(recentConversations) { (partnerInfo, lastMsg) ->
                            val (partnerId, partnerName) = partnerInfo
                            val timeString = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(lastMsg.timestamp))

                            Row(
                                modifier = Modifier.fillMaxWidth().padding(bottom = SosSpaceSm)
                                    .background(SosSurface, RoundedCornerShape(SosRadiusSm))
                                    .border(1.dp, SosBorder, RoundedCornerShape(SosRadiusSm))
                                    .clickable { onConversationClick(partnerId, partnerName) }
                                    .padding(SosSpaceMd),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier.size(40.dp).background(SosAmber.copy(0.1f), CircleShape).border(1.dp, SosAmber, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(partnerName.take(1).uppercase(), color = SosAmber, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.width(SosSpaceMd))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(partnerName, color = SosAmber, fontWeight = FontWeight.Bold, fontSize = SosFontBody)
                                    Text(lastMsg.content, color = SosTextSecondary, fontSize = SosFontCaption, maxLines = 1)
                                }
                                Text(timeString, color = SosTextSecondary, fontSize = SosFontCaption)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showNewChatDialog) {
        AlertDialog(
            onDismissRequest = { showNewChatDialog = false },
            containerColor = SosSurface2,
            title = { Text("SELECT TARGET", color = SosAmber, fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)) {
                    items(contacts) { contact ->
                        Row(
                            modifier = Modifier.fillMaxWidth().border(1.dp, SosBorder, RoundedCornerShape(SosRadiusSm))
                                .clickable {
                                    showNewChatDialog = false
                                    onConversationClick(contact.contactUuid, contact.displayName)
                                }.padding(SosSpaceMd),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = SosAmber)
                            Spacer(modifier = Modifier.width(SosSpaceMd))
                            Text(contact.displayName, color = SosAmber, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(SosSpaceSm))
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showNewChatDialog = false }) { Text("CANCEL", color = SosRed) } }
        )
    }
}