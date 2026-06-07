package com.videocleaner.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

/**
 * Foreground service for active video scanning sessions.
 * Keeps the scan alive even when the app is in the background.
 *
 * The actual scan logic lives in [VideoRepository]; this service
 * just manages the Android lifecycle/notification.
 */
class VideoScanService : Service() {
    companion object {
        const val CHANNEL_ID = "scan_service"
        const val NOTIFICATION_ID = 2001
        const val ACTION_START = "com.videocleaner.SCAN_START"
        const val ACTION_STOP = "com.videocleaner.SCAN_STOP"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        when (intent?.action) {
            ACTION_START -> startForeground(NOTIFICATION_ID, buildNotification("Scanning videos..."))
            ACTION_STOP -> stopSelf()
        }
        return START_NOT_STICKY
    }

    fun updateNotification(message: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(message))
    }

    private fun buildNotification(message: String) =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Video Duplicate Cleaner")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_popup_sync)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    "Video Scanning",
                    NotificationManager.IMPORTANCE_LOW,
                ).apply {
                    description = "Shows progress while scanning for duplicate videos"
                }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
}
