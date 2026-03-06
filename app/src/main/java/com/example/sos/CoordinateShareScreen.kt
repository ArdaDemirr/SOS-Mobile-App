package com.example.sos

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.telephony.SmsManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

@SuppressLint("MissingPermission")
@Composable
fun CoordinateShareScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    var lat by remember { mutableStateOf(0.0) }
    var lon by remember { mutableStateOf(0.0) }
    var accuracy by remember { mutableStateOf(0f) }
    var hasGps by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("GPS ARANYOR...") }
    var smsTarget by remember { mutableStateOf("") }
    var smsSent by remember { mutableStateOf(false) }

    val hasLocPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val hasSmsPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED

    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {}

    DisposableEffect(hasLocPerm) {
        if (!hasLocPerm) return@DisposableEffect onDispose {}
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val listener = object : LocationListener {
            override fun onLocationChanged(loc: Location) {
                lat = loc.latitude; lon = loc.longitude; accuracy = loc.accuracy; hasGps = true
                val src = if (loc.provider == LocationManager.GPS_PROVIDER) "SAT" else "NET"
                statusText = "[$src] ±${accuracy.toInt()}m"
            }
            override fun onProviderEnabled(p: String) {}
            override fun onProviderDisabled(p: String) {}
        }
        try { lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000L, 1f, listener) } catch (e: Exception) {}
        try { lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 2000L, 1f, listener) } catch (e: Exception) {}
        onDispose { lm.removeUpdates(listener) }
    }

    val coordText = if (hasGps) "LAT: %.6f\nLON: %.6f\nACC: ±${accuracy.toInt()}m".format(lat, lon) else "GPS sinyali bekleniyor..."
    val googleMapsUrl = if (hasGps) "https://maps.google.com/?q=$lat,$lon" else ""
    val smsBody = if (hasGps) "🆘 ACİL KONUM: https://maps.google.com/?q=$lat,$lon (±${accuracy.toInt()}m)" else ""

    Column(modifier = Modifier.fillMaxSize().background(PipBlack).systemBarsPadding()) {
        ScreenHeader(title = "COORD SHARE", subtitle = "KOORDİNAT PAYLAŞMA", onBack = onBack)

        Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

            if (!hasLocPerm) {
                Box(Modifier.fillMaxWidth().border(2.dp, PipRed, RectangleShape).clickable {
                    permLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.SEND_SMS))
                }.padding(14.dp), contentAlignment = Alignment.Center) {
                    Text("KONUM + SMS İZNİ GEREKLİ — DOKUNUN", color = PipRed, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            // GPS Display
            Box(
                Modifier.fillMaxWidth().border(2.dp, if (hasGps) PipAmber else PipAmber.copy(0.4f), RectangleShape).background(if (hasGps) PipAmber.copy(0.05f) else PipBlack).padding(16.dp)
            ) {
                Column {
                    Text(if (hasGps) statusText else "GPS ARANYOR...", color = if (hasGps) PipGreen else PipRed, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(coordText, color = PipAmber, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 22.sp)
                }
            }

            Divider(color = PipAmber.copy(0.3f))
            Text("PAYLAŞIM YÖNTEMLERİ", color = PipAmber.copy(0.6f), fontFamily = FontFamily.Monospace, fontSize = 11.sp)

            // SMS
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("SMS:", color = PipAmber.copy(0.7f), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                PipTextField(value = smsTarget, onValueChange = { smsTarget = it }, placeholder = "Telefon numarası (+905xxxxxxxxx)")
                Box(
                    Modifier.fillMaxWidth().height(52.dp)
                        .background(if (smsSent) PipGreen.copy(0.15f) else PipAmber.copy(0.1f), RoundedCornerShape(4.dp))
                        .border(2.dp, if (smsSent) PipGreen else PipAmber, RoundedCornerShape(4.dp))
                        .clickable {
                            if (hasGps && smsTarget.isNotEmpty()) {
                                if (hasSmsPerm) {
                                    try {
                                        @Suppress("DEPRECATION")
                                        SmsManager.getDefault().sendTextMessage(smsTarget, null, smsBody, null, null)
                                        smsSent = true
                                        statusText = "SMS GÖNDERİLDİ ✓"
                                    } catch (e: Exception) { statusText = "SMS HATASI: ${e.message}" }
                                } else {
                                    permLauncher.launch(arrayOf(Manifest.permission.SEND_SMS))
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (smsSent) "✓ SMS GÖNDERİLDİ" else "📱 KONUM SMS'LE GÖNDER", color = if (smsSent) PipGreen else PipAmber, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }

            // Intent Share (other apps: WhatsApp, etc.)
            Box(
                Modifier.fillMaxWidth().height(52.dp)
                    .background(PipAmber.copy(0.1f), RoundedCornerShape(4.dp))
                    .border(1.dp, PipAmber.copy(0.6f), RoundedCornerShape(4.dp))
                    .clickable {
                        if (hasGps) {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"; putExtra(Intent.EXTRA_TEXT, smsBody)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(Intent.createChooser(intent, "Konum Paylaş").also { it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("🔗 DİĞER UYGULAMALAR İLE PAYLAŞ", color = PipAmber, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }

            // Bluetooth share note
            Box(Modifier.fillMaxWidth().border(1.dp, PipAmber.copy(0.2f), RectangleShape).padding(10.dp)) {
                Text("Bluetooth ile paylaşmak için BT MESH ekranından mesaj gönder.\nSMS internet gerektirmez — GSM sinyali yeterlidir.", color = PipAmber.copy(0.4f), fontFamily = FontFamily.Monospace, fontSize = 10.sp, lineHeight = 14.sp)
            }
        }
    }
}
