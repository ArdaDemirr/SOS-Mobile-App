package com.example.sos.Comm

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
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
import java.util.UUID
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.LaunchedEffect
import com.example.sos.PipYellow
import com.example.sos.PipBorder
import com.example.sos.PipGreen
import com.example.sos.SosIconButton
import com.example.sos.SosRadiusMd
import com.example.sos.SosRadiusSm
import com.example.sos.SosScreenScaffold
import com.example.sos.SosSpaceMd
import com.example.sos.SosStatusBadge
import com.example.sos.PipSurface
import com.example.sos.PipSurface2
import com.example.sos.PipTextPrimary

@Composable
fun TacticalChatScreen(myUuid: String, targetUuid: String, targetName: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dao = remember { AppDatabase.getDatabase(context).messageDao() }
    val meshManager = remember { MeshManager.getInstance(context) }

    var inputText by remember { mutableStateOf("") }
    val messages by dao.getChatThread(myUuid, targetUuid).collectAsState(initial = emptyList())

    val listState = rememberLazyListState()
    val messageCount = messages.size

    // 2. The Auto-Scroll Trigger
    LaunchedEffect(messageCount) {
        if (messageCount > 0) {
            // Animate smoothly to the last item in the index
            listState.animateScrollToItem(messageCount - 1)
        }
    }

    SosScreenScaffold(
        title = "CHANNEL: $targetName",
        subtitle = "SECURE LINK ACTIVE",
        onBack = onBack
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).padding(vertical = SosSpaceMd),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { msg ->
                val isMe = msg.senderId == myUuid
                ChatBubble(msg, isMe)
            }
        }

        // Tactical Input Area
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = SosSpaceMd)
                .background(PipSurface, RoundedCornerShape(SosRadiusMd))
                .border(1.dp, PipBorder, RoundedCornerShape(SosRadiusMd))
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                textStyle = TextStyle(
                    color = PipYellow,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 16.sp
                ),
                cursorBrush = SolidColor(PipYellow),
                decorationBox = { inner ->
                    if (inputText.isEmpty()) {
                        Text(
                            "ENTER PACKET DATA...",
                            color = PipYellow.copy(0.3f),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp
                        )
                    }
                    inner()
                }
            )
            SosIconButton(icon = Icons.Default.Send, contentDescription = "Send") {
                if (inputText.isNotBlank()) {
                    val msg = MessageEntity(
                        messageId = UUID.randomUUID().toString(),
                        senderId = myUuid,
                        targetId = targetUuid,
                        content = inputText,
                        timestamp = System.currentTimeMillis()
                    )
                    // The UI doesn't care HOW it's sent. MeshManager handles the logic.
                    meshManager.sendMessage(msg)
                    inputText = ""
                }
            }
        }
    }
}

@Composable
fun ChatBubble(msg: MessageEntity, isMe: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .background(
                    if (isMe) PipYellow.copy(0.12f) else PipSurface2,
                    RoundedCornerShape(SosRadiusSm)
                )
                .border(1.dp, if (isMe) PipYellow.copy(0.4f) else PipBorder, RoundedCornerShape(
                    SosRadiusSm
                ))
                .padding(12.dp)
        ) {
            Column {
                Text(msg.content, color = PipTextPrimary, fontFamily = FontFamily.Monospace, fontSize = 14.sp)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SosStatusBadge(
                        text = if (msg.isSynced) "RELAYED" else "MESH PENDING",
                        accentColor = if (msg.isSynced) PipGreen else PipYellow
                    )
                }
            }
        }
    }
}
