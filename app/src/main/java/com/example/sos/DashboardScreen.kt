package com.example.sos

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ─── PALETTE ────────────────────────────────────────────────────────────────
private val ColorRed     = Color(0xFFFF3B3B)
private val ColorAmber   = Color(0xFFFFBF00)
private val ColorCyan    = Color(0xFF00E5FF)
private val ColorGreen   = Color(0xFF39FF14)
private val ColorPurple  = Color(0xFFBB86FC)
private val ColorBlue    = Color(0xFF4FC3F7)
private val ColorBg      = Color(0xFF000000)
private val ColorSurface = Color(0xFF000000)
val ColorBorder  = Color(0xFF2A2A2A)

// ─── DATA MODELS ────────────────────────────────────────────────────────────
data class MenuOption(
    val title: String,
    val icon: ImageVector,
    val isActive: Boolean = false,
    val destination: Screen,
    val subtitle: String = ""
)

data class FeatureCategory(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val accentColor: Color,
    val items: List<MenuOption>
)

// ─── CATEGORIES DEFINITION ──────────────────────────────────────────────────
private fun buildCategories(): List<FeatureCategory> = listOf(

    FeatureCategory(
        id = "emergency",
        title = "ACİL DURUM",
        subtitle = "EMERGENCY SIGNALS",
        icon = Icons.Default.Warning,
        accentColor = PipAmber,
        items = listOf(
            MenuOption("SOS Sinyali",        Icons.Default.Warning,    isActive = false, Screen.Sos,              "Acil yardım çağrısı"),
            MenuOption("SOS Şablonları",     Icons.Default.Star,       false,           Screen.SosTemplates,     "Hazır acil mesajlar"),
            MenuOption("Biyometrik Sensör",  Icons.Default.AccountBox, false, Screen.Bio,           "Sensör okumaları"),
            //MenuOption("Sesli SOS",          Icons.Default.Call,       false,           Screen.TtsSos,           "Metinden sese SOS"),
            MenuOption("QR SOS",             Icons.Default.Share,      false,           Screen.QRSos,            "QR ile bilgi paylaş"),
            //MenuOption("Acil Kişiler",       Icons.Default.Person,     false,           Screen.EmergencyContacts,"Kişi & telefon listesi"),
            //MenuOption("Sesli Aktivasyon",   Icons.Default.Phone,      false,           Screen.Voice,            "Elleri-serbest SOS"),

        )
    ),

    FeatureCategory(
        id = "comms",
        title = "İLETİŞİM",
        subtitle = "COMMUNICATIONS",
        icon = Icons.Default.Share,
        accentColor = PipAmber,
        items = listOf(
            MenuOption("Mesh Ağı",           Icons.Default.Share,  isActive = false, Screen.Mesh, "Cihazdan cihaza iletişim"),
            MenuOption("Radyo",              Icons.Default.Email,  false,           Screen.Rad,  "FM radyo alıcısı"),
        )
    ),

    FeatureCategory(
        id = "morse",
        title = "MORS KODU",
        subtitle = "MORSE SIGNALING",
        icon = Icons.Default.Notifications,
        accentColor = PipAmber,
        items = listOf(
            MenuOption("Mors (Metin)",       Icons.Default.Edit,          false, Screen.Morse,         "Metin → mors çeviri"),
            MenuOption("Sesli Mors",         Icons.Default.Phone,         false, Screen.AudioMorse,    "Ses ile mors sinyali"),
            MenuOption("Kameralı Mors",      Icons.Default.Info,          false, Screen.CameraMorse,   "Flaş ile mors sinyali"),
            MenuOption("Titreşimli Mors",    Icons.Default.Notifications, false, Screen.VibrationMorse,"Titreşim ile mors"),
        )
    ),

    FeatureCategory(
        id = "navigation",
        title = "HARİTA & NAVİGASYON",
        subtitle = "MAP & NAVIGATION",
        icon = Icons.Default.LocationOn,
        accentColor = PipAmber,
        items = listOf(
            MenuOption("Harita",             Icons.Default.LocationOn, false, Screen.Map,           "Çevrimiçi/çevrimdışı harita"),
            MenuOption("Pusula",             Icons.Default.Place,      false, Screen.Compass,       "Manyetik yön bulma"),
            MenuOption("Ölü Hesap",          Icons.Default.Place,      false, Screen.DeadReckoning, "GPS'siz konum tahmini"),
            MenuOption("Koordinat Paylaş",   Icons.Default.Share,      false, Screen.CoordShare,    "Konumunu ilet"),
            MenuOption("Güzergah Noktaları", Icons.Default.Add,        false, Screen.Waypoints,     "Rotayı işaretle"),
            MenuOption("Yıldız Navigasyon",  Icons.Default.Star,       false, Screen.StarNav,       "Yıldızlarla yön bul"),
            MenuOption("Güneş Hesabı",       Icons.Default.Face,       false, Screen.SunCalc,       "Gündoğumu/batımı"),
            MenuOption("Hava Tahmini",       Icons.Default.List,    false, Screen.Forecaster, "5 günlük tahmin"),
        )
    ),

    FeatureCategory(
        id = "tools",
        title = "ARAÇLAR & GÜVENLİK",
        subtitle = "TOOLS & SECURITY",
        icon = Icons.Default.Build,
        accentColor = PipAmber,
        items = listOf(
            MenuOption("AI Asistan",         Icons.Default.Star,       false, Screen.Ai,            "Yapay zeka yardımcı"),
            MenuOption("Hayatta Kalma Hes.", Icons.Default.List,       false, Screen.SurvivalCalc,  "Su, yiyecek hesabı"),
            MenuOption("Kontrol Listesi",    Icons.Default.Check,      false, Screen.Checklist,     "Hazırlık denetimi"),
            //MenuOption("Hayatta Kalma K.",   Icons.Default.Menu,       false, Screen.Guide,         "Acil durum kılavuzu"),
            //MenuOption("Biyometrik Kilit",   Icons.Default.Lock,       false, Screen.BiometricLock, "Uygulama kilidi"),
            //MenuOption("Pil Tasarrufu",      Icons.Default.Warning,    false, Screen.BatterySaver,  "Enerji yönetimi"),
            MenuOption("Künye",              Icons.Default.AccountBox, false, Screen.Dogtag,        "Kişisel Bilgileriniz"),
        )
    ),
)

// ─── MAIN SCREEN ────────────────────────────────────────────────────────────
@Composable
fun DashboardScreen(onNavigate: (Screen) -> Unit) {
    val categories = remember { buildCategories() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorBg)
    ) {
        StatusMonitor()

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 60.dp)
        ) {
            items(categories) { category ->
                CategorySection(category = category, onNavigate = onNavigate)
            }
        }
    }
}

// ─── CATEGORY SECTION ───────────────────────────────────────────────────────
@Composable
fun CategorySection(category: FeatureCategory, onNavigate: (Screen) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, category.accentColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .background(ColorSurface)
            .animateContentSize()
    ) {
        // ── HEADER ──────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(category.accentColor.copy(alpha = 0.12f))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(category.accentColor.copy(alpha = 0.18f))
                        .border(1.dp, category.accentColor, RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = category.icon,
                        contentDescription = null,
                        tint = category.accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = category.title,
                        color = category.accentColor,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        text = category.subtitle,
                        color = category.accentColor.copy(alpha = 0.6f),
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Normal,
                        fontSize = 9.sp,
                        letterSpacing = 0.5.sp
                    )
                }
            }
            // Item count badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(category.accentColor.copy(alpha = 0.2f))
                    .border(1.dp, category.accentColor.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "${category.items.size}",
                    color = category.accentColor,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }

        // ── DIVIDER ─────────────────────────────────────────────────────────
        Divider(color = category.accentColor.copy(alpha = 0.3f), thickness = 1.dp)

        // ── ITEMS GRID: 2 columns ───────────────────────────────────────────
        val chunkedItems = category.items.chunked(2)
        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            chunkedItems.forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowItems.forEach { item ->
                        FeatureButton(
                            item = item,
                            accentColor = category.accentColor,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigate(item.destination) }
                        )
                    }
                    if (rowItems.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

// ─── FEATURE BUTTON ─────────────────────────────────────────────────────────
@Composable
fun FeatureButton(
    item: MenuOption,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(80.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(ColorBg)
            .border(
                width = if (item.isActive) 1.5.dp else 0.8.dp,
                color = if (item.isActive) accentColor else ColorBorder,
                shape = RoundedCornerShape(6.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        // Active pulse indicator dot
        if (item.isActive) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(7.dp)
                    .clip(RoundedCornerShape(50))
                    .background(accentColor)
            )
        }

        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(accentColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.title,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    color = Color.White,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    maxLines = 2,
                    lineHeight = 14.sp
                )
                if (item.subtitle.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.subtitle,
                        color = accentColor.copy(alpha = 0.55f),
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Normal,
                        fontSize = 8.5.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

// ─── STATUS MONITOR ─────────────────────────────────────────────────────────
@Composable
fun StatusMonitor() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("BatteryIntel", Context.MODE_PRIVATE) }

    var batteryLevel   by remember { mutableIntStateOf(0) }
    var isCharging     by remember { mutableStateOf(false) }
    var timePerPercent by remember { mutableLongStateOf(prefs.getLong("timePerPercent", 0L)) }

    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                intent?.let {
                    val rawLevel = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    val scale    = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                    val status   = it.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                    val currentPct = (rawLevel * 100) / scale.toFloat()
                    batteryLevel = currentPct.toInt()
                    isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                            status == BatteryManager.BATTERY_STATUS_FULL

                    val lastSavedLevel = prefs.getInt("lastLevel", -1)
                    val lastSavedTime  = prefs.getLong("lastTime", 0L)
                    if (lastSavedLevel == -1 || batteryLevel > lastSavedLevel) {
                        with(prefs.edit()) {
                            putInt("lastLevel", batteryLevel)
                            putLong("lastTime", System.currentTimeMillis())
                            apply()
                        }
                    } else if (batteryLevel < lastSavedLevel) {
                        val now = System.currentTimeMillis()
                        val dropAmount = lastSavedLevel - batteryLevel
                        if (dropAmount > 0) {
                            val speed = (now - lastSavedTime) / dropAmount
                            with(prefs.edit()) {
                                putLong("timePerPercent", speed)
                                putInt("lastLevel", batteryLevel)
                                putLong("lastTime", now)
                                apply()
                            }
                            timePerPercent = speed
                        }
                    }
                }
            }
        }
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        context.registerReceiver(receiver, filter)
        onDispose { context.unregisterReceiver(receiver) }
    }

    val timeString = when {
        isCharging         -> "ŞARJ EDİYOR"
        timePerPercent > 0 -> {
            val ms = batteryLevel * timePerPercent
            val h  = java.util.concurrent.TimeUnit.MILLISECONDS.toHours(ms)
            val m  = java.util.concurrent.TimeUnit.MILLISECONDS.toMinutes(ms) % 60
            "${h}S ${m}D"
        }
        else -> {
            val minsLeft = batteryLevel * 4.5
            val h = minsLeft.toInt() / 60
            val m = minsLeft.toInt() % 60
            "~${h}S ${m}D"
        }
    }

    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as android.telephony.TelephonyManager
    val activeNetwork = cm.activeNetwork
    val caps = cm.getNetworkCapabilities(activeNetwork)
    val carrierName = tm.networkOperatorName

    val connectionType: String
    val isConnected: Boolean
    when {
        caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
            connectionType = "WI-FI"; isConnected = true
        }
        caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
            connectionType = "GSM NET"; isConnected = true
        }
        !carrierName.isNullOrBlank() -> {
            connectionType = "GSM YALNIZ"; isConnected = true
        }
        else -> {
            connectionType = "BAĞLANTI YOK"; isConnected = false
        }
    }

    var batColor = PipGreen;
    if(batteryLevel < 20)
    {
        batColor = ColorRed
    }
    else if (batteryLevel < 70)
    {
        batColor = PipAmber
    }
    else{
        batColor = ColorGreen
    }
    val connColor = if (isConnected) ColorGreen else ColorRed

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(top = 40.dp, bottom = 4.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(ColorSurface)
            .border(1.dp, ColorBorder, RoundedCornerShape(6.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // LEFT: Battery
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (batteryLevel < 20) Icons.Default.Warning else Icons.Default.Info,
                contentDescription = null,
                tint = batColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "GÜÇ: %$batteryLevel",
                    color = batColor,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = if (isCharging) "ŞARJDA" else "KALAN: $timeString",
                    color = batColor,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp
                )
            }
        }

        // Vertical separator
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(36.dp)
                .background(ColorBorder)
        )

        // RIGHT: Signal
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "SINYAL",
                    color = connColor,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = connectionType,
                    color = connColor,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                if (isConnected) Icons.Default.Info else Icons.Default.Warning,
                contentDescription = null,
                tint = connColor,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}