package com.example.sos

import androidx.compose.runtime.Composable

@Composable
fun ChatListScreen(myUuid: String, onConversationClick: (String, String) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val db = AppDatabase.getDatabase(context)
    val contactDao = db.contactDao()
    var contacts by remember { mutableStateOf(listOf<ContactEntity>()) }

    LaunchedEffect(Unit) {
        contacts = withContext(Dispatchers.IO) { contactDao.getAllContacts() }
    }

    Column(Modifier.fillMaxSize().background(PipBlack).systemBarsPadding()) {
        ScreenHeader("COMM-CHANNELS", "ID: ${myUuid.take(8)}", onBack)

        LazyColumn(Modifier.weight(1f).padding(16.dp)) {
            items(contacts) { contact ->
                Box(
                    Modifier.fillMaxWidth()
                        .border(1.dp, PipAmber)
                        .background(PipAmber.copy(0.05f))
                        .clickable { onConversationClick(contact.contactUuid, contact.displayName) }
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(10.dp).background(PipGreen)) // Online indicator (mock)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(contact.displayName, color = PipAmber, fontWeight = FontWeight.Bold)
                            Text("LAST TRANS: --:--", color = PipAmber.copy(0.5f), fontSize = 10.sp)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}