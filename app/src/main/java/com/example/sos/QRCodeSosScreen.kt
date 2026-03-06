package com.example.sos

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/**
 * QRCodeSosScreen — Generates a QR code containing GPS coordinates and a SOS message.
 * Uses ZXing core for matrix generation, draws on Android Bitmap (no ZXing UI dep).
 * The other person can scan with any QR reader — no app install required.
 */
@Composable
fun QRCodeSosScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    var lat by remember { mutableStateOf(0.0) }
    var lon by remember { mutableStateOf(0.0) }
    var hasGps by remember { mutableStateOf(false) }
    var customMessage by remember { mutableStateOf("ACİL! Yardım gerekiyor.") }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val hasLocPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}

    DisposableEffect(hasLocPerm) {
        if (!hasLocPerm) return@DisposableEffect onDispose {}
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val listener = object : LocationListener {
            override fun onLocationChanged(loc: Location) { lat = loc.latitude; lon = loc.longitude; hasGps = true }
            override fun onProviderEnabled(p: String) {}
            override fun onProviderDisabled(p: String) {}
        }
        try { lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 3000L, 5f, listener) } catch (e: Exception) {}
        try { lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 3000L, 5f, listener) } catch (e: Exception) {}
        onDispose { lm.removeUpdates(listener) }
    }

    fun generateQR() {
        val content = buildString {
            append("🆘 SOS ACİL\n")
            append(customMessage)
            if (hasGps) {
                append("\nKONUM: https://maps.google.com/?q=$lat,$lon")
                append("\nLAT: ${"%.6f".format(lat)}")
                append("\nLON: ${"%.6f".format(lon)}")
            } else {
                append("\n(GPS sinyali alınamadı)")
            }
        }
        qrBitmap = generateQRBitmap(content, 512)
    }

    fun shareQR() {
        qrBitmap?.let { bmp ->
            val file = java.io.File(context.cacheDir, "sos_qr.png")
            file.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
            val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "QR SOS Paylaş").also { it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(PipBlack).imePadding().systemBarsPadding()) {
        ScreenHeader(title = "QR SOS", subtitle = "QR kod acil mesaj", onBack = onBack)

        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // GPS status
            Box(Modifier.fillMaxWidth().border(1.dp, PipAmber.copy(if (hasGps) 0.6f else 0.3f), RoundedCornerShape(4.dp)).padding(10.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(if (hasGps) "GPS: ${"%.5f".format(lat)}, ${"%.5f".format(lon)}" else "GPS bekleniyor...", color = if (hasGps) PipGreen else PipAmber.copy(0.5f), fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    if (!hasLocPerm) SmallPipButton("İZİN") { permLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) }
                }
            }

            // Message input
            Text("ACİL MESAJ:", color = PipAmber.copy(0.7f), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            PipTextField(value = customMessage, onValueChange = { customMessage = it }, placeholder = "Acil mesajınızı girin...")

            // Generate button
            Box(
                Modifier.fillMaxWidth().height(56.dp)
                    .background(PipAmber.copy(0.15f), RoundedCornerShape(4.dp))
                    .border(2.dp, PipAmber, RoundedCornerShape(4.dp))
                    .clickable { generateQR() },
                contentAlignment = Alignment.Center
            ) {
                Text("⟳ QR KOD OLUŞTUR", color = PipAmber, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            // QR display
            qrBitmap?.let { bmp ->
                Box(
                    Modifier.fillMaxWidth().aspectRatio(1f).border(2.dp, PipAmber),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "SOS QR Kodu",
                        modifier = Modifier.fillMaxSize().padding(4.dp)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        Modifier.weight(1f).height(48.dp)
                            .background(PipAmber, RoundedCornerShape(4.dp))
                            .clickable { shareQR() },
                        contentAlignment = Alignment.Center
                    ) { Text("📤 PAYLAŞ", color = PipBlack, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold) }
                    Box(
                        Modifier.weight(1f).height(48.dp)
                            .border(1.dp, PipAmber, RoundedCornerShape(4.dp))
                            .clickable { generateQR() },
                        contentAlignment = Alignment.Center
                    ) { Text("⟳ YENİLE", color = PipAmber, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold) }
                }
            }

            Box(Modifier.fillMaxWidth().border(1.dp, PipAmber.copy(0.2f)).padding(8.dp)) {
                Text("Karşı taraf her QR okuyucu ile açabilir — özel uygulama gerektirmez.\nGoogle Maps linki içerir, internet varsa haritada açar.", color = PipAmber.copy(0.4f), fontFamily = FontFamily.Monospace, fontSize = 10.sp, lineHeight = 14.sp)
            }
        }
    }
}

/** Generate a QR code bitmap from content string using ZXing */
fun generateQRBitmap(content: String, sizePx: Int): Bitmap? {
    return try {
        val hints = mapOf(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.H,
            EncodeHintType.MARGIN to 1,
            EncodeHintType.CHARACTER_SET to "UTF-8"
        )
        val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
        val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.RGB_565)
        for (x in 0 until sizePx) {
            for (y in 0 until sizePx) {
                bmp.setPixel(x, y, if (matrix[x, y]) AndroidColor.BLACK else AndroidColor.WHITE)
            }
        }
        bmp
    } catch (e: Exception) { null }
}
