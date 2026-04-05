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
import com.example.sos.database.AppDatabase
import com.example.sos.database.MessageEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun TacticalChatScreen(myUuid: String, targetUuid: String, targetName: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dao = remember { AppDatabase.getDatabase(context).messageDao() }

    var inputText by remember { mutableStateOf("") }
    val messages by dao.getChatThread(myUuid, targetUuid).collectAsState(initial = emptyList())

    SosScreenScaffold(title = "LINK: $targetName", subtitle = "UUID: ${targetUuid.take(8)}", onBack = onBack) {

        LazyColumn(modifier = Modifier.weight(1f).padding(vertical = SosSpaceMd), verticalArrangement = Arrangement.spacedBy(SosSpaceSm)) {
            items(messages) { msg ->
                val isMe = msg.senderId == myUuid
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = if (isMe) Alignment.End else Alignment.Start) {
                    Box(
                        modifier = Modifier.widthIn(max = 280.dp)
                            .background(if (isMe) SosAmber.copy(0.15f) else SosSurface2, RoundedCornerShape(SosRadiusSm))
                            .border(1.dp, if (isMe) SosAmber else SosBorder, RoundedCornerShape(SosRadiusSm))
                            .padding(SosSpaceMd)
                    ) {
                        Column {
                            Text(msg.content, color = SosTextPrimary, fontFamily = FontFamily.Monospace)
                            Spacer(Modifier.height(4.dp))
                            SosStatusBadge(
                                text = if (msg.isSynced) "RELAYED VIA SERVER" else "PENDING MESH",
                                accentColor = if (msg.isSynced) SosGreen else SosAmber
                            )
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = SosSpaceMd).background(SosSurface)
                .border(1.dp, SosBorder, RoundedCornerShape(SosRadiusSm)).padding(SosSpaceSm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = inputText, onValueChange = { inputText = it },
                modifier = Modifier.weight(1f).padding(horizontal = SosSpaceSm),
                textStyle = TextStyle(color = SosAmber, fontFamily = FontFamily.Monospace, fontSize = SosFontBody),
                cursorBrush = SolidColor(SosAmber)
            )
            IconButton(onClick = {
                if (inputText.isNotBlank()) {
                    val msg = MessageEntity(
                        messageId = UUID.randomUUID().toString(),
                        senderId = myUuid, targetId = targetUuid, content = inputText, timestamp = System.currentTimeMillis()
                    )
                    scope.launch(Dispatchers.IO) {
                        dao.insertMessage(msg) // 1. Save to Room (for Flood)
                        try {
                            val res = RetrofitInstance.api.uploadMessage(msg) // 2. Try Server
                            if (res.isSuccessful || res.code() == 200) dao.markAsSynced(msg.messageId)
                        } catch (e: Exception) {}
                    }
                    inputText = ""
                }
            }) { Icon(Icons.Default.Send, contentDescription = "Send", tint = SosAmber) }
        }
    }
}