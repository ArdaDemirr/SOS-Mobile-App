package com.example.sos

class TacticalChatScreen {
    @Composable
    fun TacticalChatScreen(myUuid: String, targetUuid: String, targetName: String, onBack: () -> Unit) {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val dao = AppDatabase.getDatabase(context).messageDao()

        var inputText by remember { mutableStateOf("") }
        val messages by dao.getChatThread(myUuid, targetUuid).collectAsState(initial = emptyList())

        Column(Modifier.fillMaxSize().background(PipBlack).systemBarsPadding()) {
            // Custom Pip-Boy Header for Chat
            Row(Modifier.fillMaxWidth().background(PipAmber).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("<", Modifier.clickable { onBack() }, color = PipBlack, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(16.dp))
                Text("SECURE LINK: $targetName", color = PipBlack, fontWeight = FontWeight.Bold)
            }

            LazyColumn(Modifier.weight(1f).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(messages) { msg ->
                    val isMe = msg.senderUuid == myUuid
                    ChatBubble(msg, isMe)
                }
            }

            // Tactical Input Area
            Row(Modifier.padding(16.dp).imePadding(), verticalAlignment = Alignment.CenterVertically) {
                BasicTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f).border(1.dp, PipAmber).background(PipBlack).padding(12.dp),
                    textStyle = TextStyle(color = PipAmber, fontFamily = FontFamily.Monospace),
                    cursorBrush = SolidColor(PipAmber)
                )
                Spacer(Modifier.width(8.dp))
                Box(Modifier.size(48.dp).background(PipAmber).clickable {
                    if (inputText.isNotBlank()) {
                        val msg = MessageEntity(
                            packetId = java.util.UUID.randomUUID().toString(),
                            senderUuid = myUuid,
                            receiverUuid = targetUuid,
                            content = inputText,
                            timestamp = System.currentTimeMillis()
                        )
                        scope.launch(Dispatchers.IO) {
                            // 1. Save Locally for Mesh
                            dao.insertMessage(msg)
                            // 2. Try Server Push
                            try {
                                val response = RetrofitInstance.api.sendMessage(msg)
                                if (response.isSuccessful) dao.markAsSynced(msg.packetId)
                            } catch (e: Exception) { /* Fallback to Mesh only */ }
                        }
                        inputText = ""
                    }
                }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Send, null, tint = PipBlack)
                }
            }
        }
    }

    @Composable
    fun ChatBubble(msg: MessageEntity, isMe: Boolean) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = if(isMe) Alignment.End else Alignment.Start) {
            Box(
                Modifier.widthIn(max = 260.dp)
                    .background(if(isMe) PipAmber.copy(0.1f) else Color(0xFF1A1A1A))
                    .border(1.dp, if(isMe) PipAmber else Color.Gray)
                    .padding(10.dp)
            ) {
                Column {
                    Text(msg.content, color = if(isMe) PipAmber else Color.White, fontFamily = FontFamily.Monospace)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if(msg.isSynced) "RELAYED TO SERVER" else "MESH PENDING",
                            fontSize = 8.sp,
                            color = if(msg.isSynced) PipGreen else PipAmber.copy(0.5f)
                        )
                    }
                }
            }
        }
    }
}