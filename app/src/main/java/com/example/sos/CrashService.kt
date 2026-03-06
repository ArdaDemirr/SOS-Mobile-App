package com.example.sos

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlin.math.sqrt

class CrashService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null

    // THRESHOLD: 3.5G is a good balance.
    // Lower (e.g., 2.5f) makes it easier to test by shaking.
    private val THRESHOLD = 3.5f

    override fun onCreate() {
        super.onCreate()
        // 1. Setup Notification Channel
        createNotificationChannel()

        // 2. Start Service in Foreground (Required to run in background)
        startForeground(1, buildNotification("Detection Active", "Monitoring..."))

        // 3. Init Hardware Sensors
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }

        // Update Global State
        CrashManager.isServiceRunning.value = true
    }

    // MAKE SERVICE STICKY: If Android kills it, restart it immediately.
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            val x = it.values[0]
            val y = it.values[1]
            val z = it.values[2]

            // Calculate G-Force
            val gX = x / 9.81f
            val gY = y / 9.81f
            val gZ = z / 9.81f
            val gForce = sqrt(gX * gX + gY * gY + gZ * gZ)

            // Update UI Live Data
            CrashManager.currentG.floatValue = gForce
            if (gForce > CrashManager.peakG.floatValue) {
                CrashManager.peakG.floatValue = gForce
            }

            // --- CRASH DETECTED LOGIC ---
            if (gForce > THRESHOLD) {
                // Only trigger if not already triggered
                if (!CrashManager.isCrashDetected.value) {
                    triggerCrashProtocol()
                }
            }
        }
    }

    private fun triggerCrashProtocol() {
        // 1. Set the Global Flag (MainActivity watches this)
        CrashManager.isCrashDetected.value = true

        // 2. Prepare the Intent to launch MainActivity
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
        }

        // 3. METHOD A: Full Screen Notification (For Android 10+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val fullScreenPendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(this, "SOS_CHANNEL")
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("CRASH DETECTED")
                .setContentText("Are you okay?")
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setFullScreenIntent(fullScreenPendingIntent, true) // Wakes screen
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(true)
                .build()

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(999, notification)
        }

        // 4. METHOD B: Direct Launch (Nuclear Option)
        // This works if you granted "Display Over Other Apps" permission
        try {
            startActivity(intent)
        } catch (e: Exception) {
            // Log error if permission missing, but Notification (Method A) should still work
            e.printStackTrace()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "SOS_CHANNEL",
                "SOS Crash Monitor",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Detects impacts and launches SOS"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(title: String, text: String): Notification {
        return NotificationCompat.Builder(this, "SOS_CHANNEL")
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setOngoing(true) // Cannot be swiped away easily
            .build()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
        CrashManager.isServiceRunning.value = false
    }

    override fun onBind(intent: Intent?): IBinder? = null
}