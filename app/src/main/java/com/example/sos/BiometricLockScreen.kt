package com.example.sos

import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.fragment.app.FragmentActivity

@Composable
fun BiometricLockScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("BiometricLock", Context.MODE_PRIVATE) }
    val bm = remember { BiometricManager.from(context) }

    var isLockEnabled by remember { mutableStateOf(prefs.getBoolean("enabled", false)) }
    var authStatus by remember { mutableStateOf("") }
    var lastAuthResult by remember { mutableStateOf("") }

    val canAuthenticate = remember {
        bm.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
    }

    val biometricStatus = when (canAuthenticate) {
        BiometricManager.BIOMETRIC_SUCCESS -> "✓ Biyometrik donanım mevcut ve kayıtlı"
        BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> "✗ Biyometrik donanım yok"
        BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> "✗ Biyometrik donanım şu an kullanılamıyor"
        BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> "✗ Parmak izi/yüz kaydı yok — Ayarlar → Güvenlik'ten ekleyin"
        else -> "? Bilinmeyen durum"
    }

    fun showBiometricPrompt() {
        val activity = context as? FragmentActivity ?: return
        val executor = ContextCompat.getMainExecutor(context)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                authStatus = "✓ KİMLİK DOĞRULANDI"
                lastAuthResult = "Başarılı — ${System.currentTimeMillis()}"
            }
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                authStatus = "✗ HATA: $errString"
                lastAuthResult = "Hata: $errString"
            }
            override fun onAuthenticationFailed() {
                authStatus = "✗ KİMLİK DOĞRULANAMADI"
            }
        }
        val prompt = BiometricPrompt(activity, executor, callback)
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Kanki SOS Kilidi")
            .setSubtitle("Uygulamaya erişmek için kimliğinizi doğrulayın")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            .build()
        prompt.authenticate(promptInfo)
    }

    Column(modifier = Modifier.fillMaxSize().background(PipBlack).systemBarsPadding()) {
        ScreenHeader(title = "BIO LOCK", subtitle = "BİYOMETRİK KİLİT", onBack = onBack)

        Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {

            // Status box
            Box(Modifier.fillMaxWidth().border(1.dp, if (canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS) PipGreen.copy(0.6f) else PipRed.copy(0.6f), RoundedCornerShape(4.dp)).padding(12.dp)) {
                Text(biometricStatus, color = if (canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS) PipGreen else PipRed, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            }

            // Lock toggle
            Row(
                Modifier.fillMaxWidth().border(2.dp, PipAmber, RoundedCornerShape(4.dp)).padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("UYGULAMA KİLİDİ", color = PipAmber, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(if (isLockEnabled) "AKTIF — başlatmada kimlik ister" else "DEVREdışı", color = PipAmber.copy(0.6f), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                }
                // Toggle switch
                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .height(32.dp)
                        .background(if (isLockEnabled) PipAmber.copy(0.3f) else PipAmber.copy(0.1f), RoundedCornerShape(16.dp))
                        .border(2.dp, PipAmber, RoundedCornerShape(16.dp))
                        .clickable {
                            if (canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS) {
                                isLockEnabled = !isLockEnabled
                                prefs.edit().putBoolean("enabled", isLockEnabled).apply()
                            } else {
                                Toast.makeText(context, "Biyometrik kaydı yok", Toast.LENGTH_SHORT).show()
                            }
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .align(if (isLockEnabled) Alignment.CenterEnd else Alignment.CenterStart)
                            .padding(3.dp)
                            .background(if (isLockEnabled) PipAmber else PipAmber.copy(0.4f), CircleShape)
                    )
                }
            }

            Divider(color = PipAmber.copy(0.3f))

            // Test authentication
            Text("BİYOMETRİK TEST", color = PipAmber.copy(0.6f), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            Box(
                Modifier.fillMaxWidth().height(56.dp)
                    .background(PipAmber.copy(0.1f), RoundedCornerShape(4.dp))
                    .border(1.dp, PipAmber.copy(0.6f), RoundedCornerShape(4.dp))
                    .clickable { if (canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS) showBiometricPrompt() },
                contentAlignment = Alignment.Center
            ) {
                Text("👆 KİMLİGİ TEST ET (Parmak İzi / Yüz)", color = PipAmber, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }

            if (authStatus.isNotEmpty()) {
                Box(Modifier.fillMaxWidth().border(1.dp, if (authStatus.startsWith("✓")) PipGreen.copy(0.5f) else PipRed.copy(0.5f), RoundedCornerShape(4.dp)).padding(10.dp)) {
                    Text(authStatus, color = if (authStatus.startsWith("✓")) PipGreen else PipRed, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }

            Divider(color = PipAmber.copy(0.3f))

            // Info
            Box(Modifier.fillMaxWidth().border(1.dp, PipAmber.copy(0.2f), RectangleShape).padding(10.dp)) {
                Text(
                    "Kilit etkinse: Uygulama her açıldığında parmak izi veya yüz tanıma ya da PIN/desen ister.\n\n⚠ ACİL DURUMDA: Biometrik kilidi DEVRE DIŞI bırak — stres altında tanıma başarısız olabilir.",
                    color = PipAmber.copy(0.4f), fontFamily = FontFamily.Monospace, fontSize = 10.sp, lineHeight = 15.sp
                )
            }
        }
    }
}
