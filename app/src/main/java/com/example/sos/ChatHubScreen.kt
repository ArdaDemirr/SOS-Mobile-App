package com.example.sos

import android.content.Context
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
import androidx.compose.material.icons.filled.Delete
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
import com.example.sos.database.MessageEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatHubScreen(myUuid: String, onConversationClick: (String, String) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val myUuid = RetrofitInstance.currentUserUuid ?: "UNKNOWN"
    val scope = rememberCoroutineScope()

    var contacts by remember { mutableStateOf(listOf<ContactEntity>()) }
    //val allMessages by db.messageDao().getAllMessagesFlow().collectAsState(initial = emptyList())
    val allMessages by db.messageDao().getMyMessagesFlow(myUuid).collectAsState(initial = emptyList())
    var showNewChatDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        // Load Contacts
        contacts = withContext(Dispatchers.IO) { db.contactDao().getAllContacts() }

        // --- FETCH SERVER INBOX ON LAUNCH ---
        if (myUuid != "UNKNOWN_USER") {
            withContext(Dispatchers.IO) {
                try {
                    // 1. Get the High-Water Mark from SharedPreferences (NOT the DB)
                    val prefs = context.getSharedPreferences("tactical_prefs", Context.MODE_PRIVATE)
                    val prefsKey = "LAST_SYNC_TIME_$myUuid"
                    val latestTime = prefs.getLong(prefsKey, 0L)

                    // 2. Pass it to the server!
                    val response = RetrofitInstance.api.fetchInbox(myUuid, latestTime)

                    if (response.isSuccessful) {
                        var highestTimestampInBatch = latestTime

                        response.body()?.forEach { msg ->
                            msg.isSynced = true // Mark synced since it came from server
                            db.messageDao().insertMessage(msg)

                            if (msg.timestamp > highestTimestampInBatch) {
                                highestTimestampInBatch = msg.timestamp
                            }
                        }

                        // 3. Save the new anchor safely in SharedPreferences
                        if (highestTimestampInBatch > latestTime) {
                            prefs.edit().putLong(prefsKey, highestTimestampInBatch).apply()
                        }
                    }
                } catch (e: Exception) {
                    // Offline, ignore.
                }
            }
        }
    }

    // Grouping messages into SMS threads
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
            FloatingActionButton(onClick = { showNewChatDialog = true }, containerColor = PipAmber) {
                Icon(Icons.Default.Add, contentDescription = "New", tint = PipBlack)
            }
        },
        containerColor = PipBlack
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).systemBarsPadding()) {
            ScreenHeader(title = "INBOX", subtitle = "SECURE MESH ACTIVE", onBack = onBack)

            LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                items(recentConversations) { (partnerInfo, lastMsg) ->
                    val (partnerId, partnerName) = partnerInfo
                    val timeString = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(lastMsg.timestamp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .border(1.dp, PipAmber, RoundedCornerShape(4.dp))
                            .clickable { onConversationClick(partnerId, partnerName) }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(40.dp).background(PipAmber.copy(0.1f), CircleShape).border(1.dp, PipAmber, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(partnerName.take(1).uppercase(), color = PipAmber, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(partnerName, color = PipAmber, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(lastMsg.content, color = PipAmber.copy(0.7f), fontSize = 14.sp, maxLines = 1)
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(timeString, color = PipAmber.copy(0.5f), fontSize = 10.sp)
                            IconButton(
                                onClick = {
                                    scope.launch(Dispatchers.IO) {
                                        db.messageDao().deleteConversation(myUuid, partnerId)
                                    }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Wipe", tint = PipAmber)
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
            containerColor = PipBlack,
            title = { Text("SELECT TARGET", color = PipAmber, fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)) {
                    items(contacts) { contact ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, PipAmber.copy(0.3f), RoundedCornerShape(4.dp))
                                .clickable {
                                    showNewChatDialog = false
                                    onConversationClick(contact.contactUuid, contact.displayName)
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = PipAmber)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(contact.displayName, color = PipAmber, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showNewChatDialog = false }) { Text("CANCEL", color = PipAmber) }
            }
        )
    }
}