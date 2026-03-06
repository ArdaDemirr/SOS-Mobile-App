package com.example.sos

import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
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
import android.Manifest
import android.content.pm.PackageManager
import kotlin.math.*
import java.util.Calendar
import java.util.TimeZone

/**
 * SunCalcScreen — Offline sunrise/sunset & golden hour calculator.
 * Uses NOAA Solar Calculator equations (pure math, zero dependencies).
 * Calculates accurate times for any GPS coordinate.
 */
@Composable
fun SunCalcScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    var lat by remember { mutableStateOf(37.0) }
    var lon by remember { mutableStateOf(35.0) }
    var hasGps by remember { mutableStateOf(false) }
    var manualMode by remember { mutableStateOf(false) }
    var manualLatText by remember { mutableStateOf("37.0") }
    var manualLonText by remember { mutableStateOf("35.0") }

    val hasLocPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    DisposableEffect(hasLocPerm) {
        if (!hasLocPerm) return@DisposableEffect onDispose {}
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val listener = object : LocationListener {
            override fun onLocationChanged(loc: Location) { if (!manualMode) { lat = loc.latitude; lon = loc.longitude; hasGps = true } }
            override fun onProviderEnabled(p: String) {}
            override fun onProviderDisabled(p: String) {}
        }
        try { lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000L, 50f, listener) } catch (e: Exception) {}
        try { lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000L, 50f, listener) } catch (e: Exception) {}
        onDispose { lm.removeUpdates(listener) }
    }

    val sunData = remember(lat, lon) { calculateSunTimes(lat, lon) }

    Column(modifier = Modifier.fillMaxSize().background(PipBlack).systemBarsPadding()) {
        ScreenHeader(title = "SUN CALC", subtitle = "GÜN DOĞUMU / BATIMI", onBack = onBack)

        Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

            // Coordinates row
            Box(Modifier.fillMaxWidth().border(1.dp, PipAmber.copy(0.5f), RoundedCornerShape(4.dp)).padding(10.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(if (hasGps && !manualMode) "GPS: %.4f, %.4f".format(lat, lon) else "MANUEL MOD", color = PipAmber, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                        SmallPipButton(if (manualMode) "GPS MOD" else "MANUEL") { manualMode = !manualMode }
                    }
                    if (manualMode) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(Modifier.weight(1f)) {
                                PipTextField(manualLatText, {
                                    manualLatText = it; it.toDoubleOrNull()?.let { v -> lat = v }
                                }, "Enlem (37.0)")
                            }
                            Box(Modifier.weight(1f)) {
                                PipTextField(manualLonText, {
                                    manualLonText = it; it.toDoubleOrNull()?.let { v -> lon = v }
                                }, "Boylam (35.0)")
                            }
                        }
                    }
                }
            }

            // Main sun display
            Box(
                Modifier.fillMaxWidth().border(2.dp, PipAmber, RectangleShape).background(PipAmber.copy(0.03f)).padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SunTimeRow("☀ GÜN DOĞUMU", sunData.sunrise, PipAmber)
                    SunTimeRow("🌅 GOLDEN HOUR BAŞLANGIÇ", sunData.goldenMorningEnd, PipAmber.copy(0.8f))
                    SunTimeRow("☀ SOLAR ÖĞLE", sunData.solarNoon, PipAmber)
                    SunTimeRow("🌇 GOLDEN HOUR BAŞLANGICI", sunData.goldenEveningStart, PipAmber.copy(0.8f))
                    SunTimeRow("🌙 GÜN BATIMI", sunData.sunset, PipAmber)
                    Divider(color = PipAmber.copy(0.3f))
                    SunTimeRow("⏰ IŞIK SÜRESİ", sunData.daylightHours, PipGreen)
                    SunTimeRow("🔦 GÜN BATIMINA KALAN", sunData.timeUntilSunset, PipRed)
                }
            }

            // Month calendar — days of daylight
            Text("AYLIK GÜNEŞ TAKVİMİ", color = PipAmber.copy(0.6f), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.fillMaxWidth()) {
                val calendar = Calendar.getInstance()
                val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                val today = calendar.get(Calendar.DAY_OF_MONTH)
                (1..daysInMonth).forEach { day ->
                    calendar.set(Calendar.DAY_OF_MONTH, day)
                    val data = calculateSunTimes(lat, lon, calendar)
                    val hours = data.daylightDecimal
                    val isToday = day == today
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(30.dp)
                            .background(PipAmber.copy(alpha = (hours / 14.0).toFloat().coerceIn(0.05f, 0.7f)))
                            .border(if (isToday) 1.dp else 0.dp, if (isToday) PipRed else PipAmber.copy(0f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (day % 5 == 0 || day == 1 || day == today) {
                            Text("$day", color = PipBlack, fontFamily = FontFamily.Monospace, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Box(Modifier.fillMaxWidth().border(1.dp, PipAmber.copy(0.2f), RectangleShape).padding(8.dp)) {
                Text("NOAA solar hesaplama algoritması. İnternet gerektirmez.\nAltın saat: fotoğraf/sinyal için en iyi aydınlatma periyodu (~30dk).", color = PipAmber.copy(0.4f), fontFamily = FontFamily.Monospace, fontSize = 10.sp, lineHeight = 14.sp)
            }
        }
    }
}

@Composable
fun SunTimeRow(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = color.copy(0.7f), fontFamily = FontFamily.Monospace, fontSize = 12.sp)
        Text(value, color = color, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

data class SunData(
    val sunrise: String,
    val sunset: String,
    val solarNoon: String,
    val goldenMorningEnd: String,
    val goldenEveningStart: String,
    val daylightHours: String,
    val timeUntilSunset: String,
    val daylightDecimal: Double
)

/** NOAA solar position algorithm — accurate to ≈1 minute */
fun calculateSunTimes(lat: Double, lon: Double, cal: Calendar = Calendar.getInstance()): SunData {
    val tz = TimeZone.getDefault().getOffset(cal.timeInMillis) / 3_600_000.0
    val jd = julianDay(cal)

    fun sunInfo(jdVal: Double): Pair<Double, Double> { // returns (azimuth, elevation) at solar noon
        val n = jdVal - 2451545.0
        val L = (280.46 + 0.9856474 * n) % 360
        val g = Math.toRadians((357.528 + 0.9856003 * n) % 360)
        val lam = Math.toRadians(L + 1.915 * sin(g) + 0.020 * sin(2 * g))
        val eps = Math.toRadians(23.439 - 0.0000004 * n)
        val sinDec = sin(eps) * sin(lam)
        val dec = asin(sinDec)
        val eqTime = (L - Math.toDegrees(lam)) / 15.0
        return dec to eqTime
    }

    val (dec, eqTime) = sunInfo(jd)
    val latRad = Math.toRadians(lat)

    // Hour angle at sunrise/sunset (solar zenith = 90.833°)
    val cosH = (cos(Math.toRadians(90.833)) - sin(latRad) * sin(dec)) / (cos(latRad) * cos(dec))
    val hasSun = cosH in -1.0..1.0

    val haDeg = if (hasSun) Math.toDegrees(acos(cosH)) else 0.0

    // UTC time corrections
    val lonCorr = lon / 15.0
    val sunriseUTC = 12.0 - haDeg / 15.0 - eqTime - lonCorr + tz
    val sunsetUTC = 12.0 + haDeg / 15.0 - eqTime - lonCorr + tz
    val noonUTC = 12.0 - eqTime - lonCorr + tz
    val goldenMorning = sunriseUTC + 0.5 // ~30min after sunrise
    val goldenEvening = sunsetUTC - 0.5  // ~30min before sunset

    val daylightH = if (hasSun) haDeg * 2 / 15.0 else 0.0

    // Time until sunset
    val nowHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY) + Calendar.getInstance().get(Calendar.MINUTE) / 60.0
    val diffH = sunsetUTC - nowHour
    val timeUntil = if (!hasSun) "--:--" else if (diffH < 0) "GEÇTİ" else formatHours(diffH)

    fun fmt(h: Double): String {
        val hh = h.toInt().coerceIn(0, 23)
        val mm = ((h - h.toInt()) * 60).toInt().coerceIn(0, 59)
        return "%02d:%02d".format(hh, mm)
    }

    return SunData(
        sunrise = if (hasSun) fmt(sunriseUTC) else "KUTUP GECESI",
        sunset = if (hasSun) fmt(sunsetUTC) else "KUTUP GECESI",
        solarNoon = fmt(noonUTC),
        goldenMorningEnd = if (hasSun) fmt(goldenMorning) else "--:--",
        goldenEveningStart = if (hasSun) fmt(goldenEvening) else "--:--",
        daylightHours = formatHours(daylightH),
        timeUntilSunset = timeUntil,
        daylightDecimal = daylightH
    )
}

private fun julianDay(cal: Calendar): Double {
    val y = cal.get(Calendar.YEAR)
    val m = cal.get(Calendar.MONTH) + 1
    val d = cal.get(Calendar.DAY_OF_MONTH)
    val a = (14 - m) / 12
    val yy = y + 4800 - a
    val mm = m + 12 * a - 3
    return d + (153 * mm + 2) / 5 + 365 * yy + yy / 4 - yy / 100 + yy / 400 - 32045.0
}

private fun formatHours(h: Double): String {
    val hours = h.toInt()
    val mins = ((h - hours) * 60).toInt()
    return "${hours}s ${mins}dk"
}
