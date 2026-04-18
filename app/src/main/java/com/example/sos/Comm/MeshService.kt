package com.example.sos.Comm

import android.app.*
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.sos.R

class MeshService : Service() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val channelId = "mesh_channel"
        val channel = NotificationChannel(channelId, "Tactical Mesh", NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Tactical Mesh Active")
            .setContentText("Acting as a data mule for SOS packets...")
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Use your icon
            .build()

        startForeground(1, notification)

        // Start the mesh engine
        MeshManager.getInstance(this).start()

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null
}