package com.example.twinmind.audio

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.twinmind.R

class RecordingNotificationManager(private val context: Context) {

    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Recording",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun buildNotification(timer: String, isPaused: Boolean) = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setContentTitle("Recording in progress")
        .setContentText(timer)
        .addAction(createPauseResumeAction(isPaused))
        .addAction(createStopAction())
        .build()

    private fun createPauseResumeAction(isPaused: Boolean): NotificationCompat.Action {
        val intent = Intent(context, RecordingService::class.java).apply {
            action = if (isPaused) RecordingService.ACTION_RESUME else RecordingService.ACTION_PAUSE
        }
        val pendingIntent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        val title = if (isPaused) "Resume" else "Pause"
        return NotificationCompat.Action(0, title, pendingIntent)
    }

    private fun createStopAction(): NotificationCompat.Action {
        val intent = Intent(context, RecordingService::class.java).apply {
            action = RecordingService.ACTION_STOP
        }
        val pendingIntent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        return NotificationCompat.Action(0, "Stop", pendingIntent)
    }

    companion object {
        const val CHANNEL_ID = "recording_channel"
        const val NOTIFICATION_ID = 1
    }
}
