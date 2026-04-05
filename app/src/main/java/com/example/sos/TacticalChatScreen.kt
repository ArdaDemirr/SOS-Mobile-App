package com.example.sos

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sos.database.AppDatabase
import com.example.sos.database.MessageEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

@Composable
fun TacticalChatScreen(myUuid: String, targetUuid: String, targetName: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dao = remember { AppDatabase.getDatabase(context).messageDao() }

    var inputText by remember { mutableStateOf("") }
    val messages by dao.getChatThread(myUuid, targetUuid).collectAsState(initial = emptyList())

    // --- GRAB PUBLIC KEY FOR SIGNATURE ---
    var myPublicKey by remember { mutableStateOf("NO_KEY") }
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val dogtag = AppDatabase.getDatabase(context).dogtagDao().getDogtag()
            if (dogtag != null) {
                myPublicKey = dogtag.publicKey
            }
        }
    }

    // --- POLL SERVER FOR NEW REPLIES EVERY 3 SECONDS ---
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            while (true) {
                try {
                    val res = RetrofitInstance.api.getConversation(myUuid, targetUuid)
                    if (res.isSuccessful) {
                        res.body()?.forEach { msg ->
                            msg.isSynced = true
                            dao.insertMessage(msg)
                        }
                    }
                } catch (e: Exception) { /* Offline */ }
                delay(3000)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(PipBlack).systemBarsPadding()) {
        ScreenHeader(title = "LINK: $targetName", subtitle = "UUID: ${targetUuid.take(8)}", onBack = onBack)

        LazyColumn(modifier = Modifier.weight(1f).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(messages) { msg ->
                val isMe = msg.senderId == myUuid
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = if (isMe) Alignment.End else Alignment.Start) {
                    Box(
                        modifier = Modifier
                            .widthIn(max = 280.dp)
                            .background(if (isMe) PipAmber.copy(0.1f) else PipBlack, RoundedCornerShape(4.dp))
                            .border(1.dp, if (isMe) PipAmber else PipAmber.copy(0.4f), RoundedCornerShape(4.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(msg.content, color = PipAmber, fontFamily = FontFamily.Monospace)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                if (msg.isSynced) "RELAYED VIA SERVER" else "PENDING MESH",
                                color = if (msg.isSynced) PipGreen else PipAmber.copy(0.5f),
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .border(1.dp, PipAmber, RoundedCornerShape(4.dp))
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = inputText, onValueChange = { inputText = it },
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                textStyle = TextStyle(color = PipAmber, fontFamily = FontFamily.Monospace, fontSize = 16.sp),
                cursorBrush = SolidColor(PipAmber)
            )
            IconButton(onClick = {
                if (inputText.isNotBlank()) {
                    val msg = MessageEntity(
                        messageId = UUID.randomUUID().toString(),
                        senderId = myUuid,
                        targetId = targetUuid,
                        messageType = 0,
                        ttl = 10, // Increased to 10 hops
                        digitalSignature = myPublicKey, // Filled with PK
                        content = inputText,
                        timestamp = System.currentTimeMillis()
                    )
                    scope.launch(Dispatchers.IO) {
                        dao.insertMessage(msg) // 1. Save locally
                        try {
                            val res = RetrofitInstance.api.uploadMessage(msg) // 2. Try Server
                            if (res.isSuccessful || res.code() == 200) dao.markAsSynced(msg.messageId)
                        } catch (e: Exception) {}
                    }
                    inputText = ""
                }
            }) { Icon(Icons.Default.Send, contentDescription = "Send", tint = PipAmber) }
        }
    }
}