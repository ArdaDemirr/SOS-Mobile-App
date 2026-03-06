package com.example.sos.notused

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.telephony.SmsManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.sos.PipAmber
import com.example.sos.PipBlack
import com.example.sos.PipGreen
import com.example.sos.PipRed
import com.example.sos.PipTextField
import com.example.sos.ScreenHeader
import org.json.JSONArray
import org.json.JSONObject

data class EmergencyContact(val id: Long, val name: String, val phone: String, val relation: String)

@Composable
fun EmergencyContactsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("EmergencyContacts", Context.MODE_PRIVATE) }

    var contacts by remember { mutableStateOf(loadContacts(prefs)) }
    var showAdd by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var newPhone by remember { mutableStateOf("") }
    var newRelation by remember { mutableStateOf("") }
    var sendStatus by remember { mutableStateOf("") }

    val hasSmsPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}

    val sosMessage = "🆘 ACİL DURUM! Yardım ihtiyacım var. Bu mesaj Kanki SOS uygulamasından otomatik gönderildi. Lütfen aramaya çalışın."

    fun sendSosToAll() {
        if (!hasSmsPerm) { permLauncher.launch(Manifest.permission.SEND_SMS); return }
        var sent = 0
        contacts.forEach { contact ->
            try {
                @Suppress("DEPRECATION")
                SmsManager.getDefault().sendTextMessage(contact.phone, null, sosMessage, null, null)
                sent++
            } catch (e: Exception) {}
        }
        sendStatus = "✓ $sent kişiye SOS SMS gönderildi"
    }

    fun sendSosTo(contact: EmergencyContact) {
        if (!hasSmsPerm) { permLauncher.launch(Manifest.permission.SEND_SMS); return }
        try {
            @Suppress("DEPRECATION")
            SmsManager.getDefault().sendTextMessage(contact.phone, null, sosMessage, null, null)
            sendStatus = "✓ ${contact.name}'e gönderildi"
        } catch (e: Exception) { sendStatus = "Hata: ${e.message}" }
    }

    Column(modifier = Modifier.fillMaxSize().background(PipBlack).systemBarsPadding()) {
        ScreenHeader(title = "EMER CONT", subtitle = "ÇEVRİMDIŞI ACİL KİŞİLER", onBack = onBack)

        Column(modifier = Modifier.fillMaxSize()) {
            // Alert all button
            Box(
                Modifier.fillMaxWidth().padding(8.dp)
                    .background(PipRed.copy(0.15f), RoundedCornerShape(4.dp))
                    .border(2.dp, PipRed, RoundedCornerShape(4.dp))
                    .clickable { sendSosToAll() }
                    .padding(14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("🆘 TÜM KİŞİLERE SOS SMS GÖNDER", color = PipRed, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }

            if (sendStatus.isNotEmpty()) {
                Box(Modifier.fillMaxWidth().padding(horizontal = 8.dp).border(1.dp, PipGreen.copy(0.5f), RoundedCornerShape(4.dp)).padding(8.dp)) {
                    Text(sendStatus, color = PipGreen, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                }
            }

            // Add button
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp).border(1.dp, PipAmber.copy(0.5f), RoundedCornerShape(4.dp)).clickable { showAdd = !showAdd }.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Add, null, tint = PipAmber, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (showAdd) "FORMU KAPAT" else "YENİ KİŞİ EKLE", color = PipAmber, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }

            if (showAdd) {
                Column(
                    Modifier.padding(horizontal = 8.dp).border(1.dp, PipAmber.copy(0.5f), RoundedCornerShape(4.dp)).padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PipTextField(newName, { newName = it }, "Ad Soyad")
                    PipTextField(newPhone, { newPhone = it }, "Telefon (+905xxxxxxxxx)")
                    PipTextField(newRelation, { newRelation = it }, "Yakınlık (Eş, Anne, vb.)")
                    Box(
                        Modifier.fillMaxWidth().background(PipAmber, RoundedCornerShape(4.dp)).clickable {
                            if (newName.isNotEmpty() && newPhone.isNotEmpty()) {
                                val c = EmergencyContact(System.currentTimeMillis(), newName, newPhone, newRelation)
                                contacts = contacts + c
                                saveContacts(prefs, contacts)
                                newName = ""; newPhone = ""; newRelation = ""; showAdd = false
                            }
                        }.padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) { Text("KAYDET", color = PipBlack, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold) }
                }
                Spacer(Modifier.height(6.dp))
            }

            Divider(color = PipAmber.copy(0.3f), modifier = Modifier.padding(horizontal = 8.dp))
            Text("  ACİL KİŞİLER (${contacts.size})", color = PipAmber.copy(0.6f), fontFamily = FontFamily.Monospace, fontSize = 11.sp, modifier = Modifier.padding(vertical = 4.dp))

            if (contacts.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text("Henüz acil kişi yok.\n+ EKLE ile kaydet. SMS internet gerektirmez.", color = PipAmber.copy(0.3f), fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                }
            }

            LazyColumn(contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(contacts, key = { it.id }) { contact ->
                    Row(
                        Modifier.fillMaxWidth().border(1.dp, PipAmber.copy(0.4f), RoundedCornerShape(4.dp)).padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(contact.name, color = PipAmber, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(contact.phone, color = PipAmber.copy(0.7f), fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                            if (contact.relation.isNotEmpty()) Text(contact.relation, color = PipAmber.copy(0.5f), fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Call button
                            Box(
                                Modifier.Companion.background(PipGreen.copy(0.2f), RoundedCornerShape(3.dp)).border(1.dp, PipGreen.copy(0.5f), RoundedCornerShape(3.dp)).clickable {
                                    context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${contact.phone}")).also { it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
                                }.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) { Text("📞", fontSize = 14.sp) }
                            // SMS button
                            Box(
                                Modifier.Companion.background(PipAmber.copy(0.2f), RoundedCornerShape(3.dp)).border(1.dp, PipAmber.copy(0.5f), RoundedCornerShape(3.dp)).clickable { sendSosTo(contact) }.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) { Text("SOS", color = PipAmber, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp) }
                            // Delete
                            Icon(Icons.Default.Delete, null, tint = PipRed.copy(0.6f), modifier = Modifier.size(20.dp).clickable {
                                contacts = contacts.filter { it.id != contact.id }
                                saveContacts(prefs, contacts)
                            })
                        }
                    }
                }
            }
        }
    }
}

private fun saveContacts(prefs: SharedPreferences, contacts: List<EmergencyContact>) {
    val arr = JSONArray()
    contacts.forEach { c ->
        arr.put(JSONObject().apply { put("id", c.id); put("name", c.name); put("phone", c.phone); put("relation", c.relation) })
    }
    prefs.edit().putString("data", arr.toString()).apply()
}

private fun loadContacts(prefs: SharedPreferences): List<EmergencyContact> {
    val json = prefs.getString("data", "[]") ?: "[]"
    return try {
        val arr = JSONArray(json)
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            EmergencyContact(o.getLong("id"), o.getString("name"), o.getString("phone"), o.optString("relation", ""))
        }
    } catch (e: Exception) { emptyList() }
}
