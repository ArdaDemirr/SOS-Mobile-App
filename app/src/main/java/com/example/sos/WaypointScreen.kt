package com.example.sos

import android.content.Context
import android.content.SharedPreferences
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
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
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.*

data class Waypoint(
    val id: Long,
    val name: String,
    val type: String,
    val lat: Double,
    val lon: Double,
    val note: String
)

private val WAYPOINT_TYPES = listOf("KAMP" to "🏕", "SU" to "💧", "TEHLİKE" to "⚠", "SIĞINAK" to "🏠", "YIYECEK" to "🌿", "DİĞER" to "📍")

@Composable
fun WaypointScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("Waypoints", Context.MODE_PRIVATE) }

    var waypoints by remember { mutableStateOf(loadWaypoints(prefs)) }
    var currentLat by remember { mutableStateOf(0.0) }
    var currentLon by remember { mutableStateOf(0.0) }
    var hasGps by remember { mutableStateOf(false) }
    var showAddForm by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var newNote by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("KAMP") }

    val hasLocPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    DisposableEffect(hasLocPerm) {
        if (!hasLocPerm) return@DisposableEffect onDispose {}
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val listener = object : LocationListener {
            override fun onLocationChanged(loc: Location) { currentLat = loc.latitude; currentLon = loc.longitude; hasGps = true }
            override fun onProviderEnabled(p: String) {}
            override fun onProviderDisabled(p: String) {}
        }
        try { lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 3000L, 5f, listener) } catch (e: Exception) {}
        try { lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 3000L, 5f, listener) } catch (e: Exception) {}
        onDispose { lm.removeUpdates(listener) }
    }

    Column(modifier = Modifier.fillMaxSize().background(PipBlack).systemBarsPadding()) {
        ScreenHeader(title = "WAYPOINTS", subtitle = "ÖNEMLİ NOKTA İŞARETLEME", onBack = onBack)

        Column(modifier = Modifier.fillMaxSize()) {
            // Current GPS status
            Box(
                Modifier.fillMaxWidth().padding(8.dp).border(1.dp, PipAmber.copy(0.4f), RoundedCornerShape(4.dp)).padding(8.dp)
            ) {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text(if (hasGps) "GPS: %.5f, %.5f".format(currentLat, currentLon) else "GPS aranıyor...", color = if (hasGps) PipAmber else PipAmber.copy(0.5f), fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    Box(
                        Modifier.background(PipAmber, RoundedCornerShape(3.dp)).clickable { if (hasGps) showAddForm = !showAddForm }.padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(if (showAddForm) "KAPAT" else "+ EKLE", color = PipBlack, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }

            // Add form
            if (showAddForm) {
                Column(
                    modifier = Modifier.padding(horizontal = 8.dp).border(1.dp, PipAmber.copy(0.6f), RoundedCornerShape(4.dp)).padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PipTextField(value = newName, onValueChange = { newName = it }, placeholder = "Nokta adı (örn. ANA KAMP)")
                    PipTextField(value = newNote, onValueChange = { newNote = it }, placeholder = "Not (isteğe bağlı)")
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        WAYPOINT_TYPES.forEach { (type, emoji) ->
                            Box(
                                Modifier
                                    .background(if (selectedType == type) PipAmber else PipAmber.copy(0.1f), RoundedCornerShape(3.dp))
                                    .border(1.dp, PipAmber.copy(if (selectedType == type) 1f else 0.3f), RoundedCornerShape(3.dp))
                                    .clickable { selectedType = type }
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                            ) {
                                Text("$emoji $type", color = if (selectedType == type) PipBlack else PipAmber, fontFamily = FontFamily.Monospace, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Box(
                        Modifier.fillMaxWidth().background(PipAmber, RoundedCornerShape(4.dp)).clickable {
                            if (newName.isNotEmpty() && hasGps) {
                                val wp = Waypoint(System.currentTimeMillis(), newName.uppercase(), selectedType, currentLat, currentLon, newNote)
                                waypoints = waypoints + wp
                                saveWaypoints(prefs, waypoints)
                                newName = ""; newNote = ""; showAddForm = false
                            }
                        }.padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("📍 MEVCUT KONUMU KAYDET", color = PipBlack, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            Divider(color = PipAmber.copy(0.3f), modifier = Modifier.padding(horizontal = 8.dp))
            Text("  KAYITLI NOKTALAR (${waypoints.size})", color = PipAmber.copy(0.6f), fontFamily = FontFamily.Monospace, fontSize = 11.sp, modifier = Modifier.padding(vertical = 4.dp))

            if (waypoints.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text("Henüz kayıtlı nokta yok.\n+ EKLE ile mevcut GPS konumunu kaydet.", color = PipAmber.copy(0.3f), fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                }
            }

            LazyColumn(contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(waypoints, key = { it.id }) { wp ->
                    val distM = if (hasGps) haversineM(currentLat, currentLon, wp.lat, wp.lon) else null
                    val emoji = WAYPOINT_TYPES.firstOrNull { it.first == wp.type }?.second ?: "📍"
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, PipAmber.copy(0.4f), RoundedCornerShape(4.dp))
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(emoji, fontSize = 20.sp)
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(wp.name, color = PipAmber, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("%.5f, %.5f".format(wp.lat, wp.lon), color = PipAmber.copy(0.5f), fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                                if (wp.note.isNotEmpty()) Text(wp.note, color = PipAmber.copy(0.6f), fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                                distM?.let { Text(formatDistance(it), color = PipGreen, fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                            }
                        }
                        Icon(Icons.Default.Delete, null, tint = PipRed.copy(0.6f), modifier = Modifier.size(20.dp).clickable {
                            waypoints = waypoints.filter { it.id != wp.id }
                            saveWaypoints(prefs, waypoints)
                        })
                    }
                }
            }
        }
    }
}

private fun haversineM(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val R = 6371000.0
    val phi1 = Math.toRadians(lat1); val phi2 = Math.toRadians(lat2)
    val dPhi = Math.toRadians(lat2 - lat1); val dLambda = Math.toRadians(lon2 - lon1)
    val a = sin(dPhi / 2).pow(2) + cos(phi1) * cos(phi2) * sin(dLambda / 2).pow(2)
    return R * 2 * atan2(sqrt(a), sqrt(1 - a))
}

private fun formatDistance(m: Double) = when {
    m < 1000 -> "${"%.0f".format(m)}m"
    else -> "${"%.2f".format(m / 1000)}km"
}

private fun saveWaypoints(prefs: SharedPreferences, wps: List<Waypoint>) {
    val arr = JSONArray()
    wps.forEach { wp ->
        arr.put(JSONObject().apply {
            put("id", wp.id); put("name", wp.name); put("type", wp.type)
            put("lat", wp.lat); put("lon", wp.lon); put("note", wp.note)
        })
    }
    prefs.edit().putString("data", arr.toString()).apply()
}

private fun loadWaypoints(prefs: SharedPreferences): List<Waypoint> {
    val json = prefs.getString("data", "[]") ?: "[]"
    return try {
        val arr = JSONArray(json)
        (0 until arr.length()).map { i ->
            val obj = arr.getJSONObject(i)
            Waypoint(obj.getLong("id"), obj.getString("name"), obj.getString("type"), obj.getDouble("lat"), obj.getDouble("lon"), obj.optString("note", ""))
        }
    } catch (e: Exception) { emptyList() }
}
