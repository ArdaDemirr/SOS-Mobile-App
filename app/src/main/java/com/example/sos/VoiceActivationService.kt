package com.example.sos

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf

/**
 * VoiceActivationService — Arka planda "Kanki SOS" sesini dinleyen Foreground Service.
 *
 * Android'de SpeechRecognizer, UI thread'de çalışmak zorundadır.
 * Bu yüzden servis main looper üzerinde çalışır.
 * Dinleme → süre dolunca otomatik yeniden başlatma döngüsü kullanılır.
 */
class VoiceActivationService : Service() {

    companion object {
        const val CHANNEL_ID = "VoiceActivationChannel"
        const val NOTIF_ID = 1001

        // Trigger keywords (küçük harf, trim edilmiş)
        private val TRIGGER_KEYWORDS = listOf("kanki", "sos", "kanki sos", "yardım", "imdat")

        // Shared state — Compose ekranlarından gözlemlenebilir
        val isListening = mutableStateOf(false)
        val lastDetectedText = mutableStateOf("")
        val detectionHistory = mutableStateListOf<String>()
    }

    private var speechRecognizer: SpeechRecognizer? = null
    private var isServiceRunning = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIF_ID, buildNotification("Dinleniyor..."))
        startListening()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        isServiceRunning = true
        return START_STICKY // Sistem öldürürse otomatik yeniden başlat
    }

    override fun onDestroy() {
        isServiceRunning = false
        isListening.value = false
        speechRecognizer?.destroy()
        speechRecognizer = null
        super.onDestroy()
    }

    private fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            stopSelf()
            return
        }

        speechRecognizer?.destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "tr-TR")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 500L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
        }

        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                isListening.value = true
                updateNotification("Dinleniyor... Tetikleyici: \"Kanki SOS\"")
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val partial = partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()?.lowercase()?.trim() ?: return

                if (checkTrigger(partial)) {
                    onTriggerDetected(partial)
                }
            }

            override fun onResults(results: Bundle?) {
                val text = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()?.lowercase()?.trim() ?: ""

                if (text.isNotEmpty()) {
                    lastDetectedText.value = text
                    if (detectionHistory.size >= 5) detectionHistory.removeAt(0)
                    detectionHistory.add(text)
                }

                if (checkTrigger(text)) {
                    onTriggerDetected(text)
                } else {
                    // Tekrar dinlemeye başla
                    if (isServiceRunning) restartListening()
                }
            }

            override fun onError(error: Int) {
                isListening.value = false
                val msg = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> "eşleşme yok"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "zaman aşımı"
                    SpeechRecognizer.ERROR_NETWORK -> "ağ hatası"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "mikrofon izni yok"
                    else -> "hata: $error"
                }
                updateNotification("Hata: $msg — yeniden başlatılıyor...")
                if (isServiceRunning) restartListening()
            }

            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { isListening.value = false }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        speechRecognizer?.startListening(intent)
    }

    private fun restartListening() {
        android.os.Handler(mainLooper).postDelayed({
            if (isServiceRunning) startListening()
        }, 1000L)
    }

    private fun checkTrigger(text: String): Boolean {
        return TRIGGER_KEYWORDS.any { keyword -> text.contains(keyword) }
    }

    private fun onTriggerDetected(text: String) {
        isListening.value = false
        lastDetectedText.value = "⚠ TETİKLENDİ: $text"
        if (detectionHistory.size >= 5) detectionHistory.removeAt(0)
        detectionHistory.add("⚠ SOS TETİKLENDİ: $text")

        updateNotification("SOS TETİKLENDİ! \"$text\"")

        // SOS modunu etkinleştir — CrashManager üzerinden (mevcut kod)
        CrashManager.isCrashDetected.value = true

        // Uygulamayı ön plana getir
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        launchIntent?.let { startActivity(it) }

        // Kısa bekleme sonrası tekrar dinle
        if (isServiceRunning) restartListening()
    }

    private fun buildNotification(contentText: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            packageManager.getLaunchIntentForPackage(packageName),
            PendingIntent.FLAG_IMMUTABLE
        )
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Kanki SOS — Ses Aktivasyon")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(text: String) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIF_ID, buildNotification(text))
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Ses Aktivasyon Servisi",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "\"Kanki SOS\" sesini arka planda dinler"
            setShowBadge(false)
        }
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)
    }
}
