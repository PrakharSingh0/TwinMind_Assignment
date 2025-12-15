// In D:/Android/TwinMind2/app/src/main/java/com/example/twinmind/recording/RecordingService.kt

package com.example.twinmind.recording

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.twinmind.R
import com.example.twinmind.audio.AudioRecorder // 👈 Make sure you have this import

class RecordingService : Service() {

    companion object {
        const val CHANNEL_ID = "recording_channel"
        const val CHANNEL_NAME = "Recording"
        const val NOTIFICATION_ID = 1

        const val ACTION_START = "com.example.twinmind.action.START_RECORDING"
        const val ACTION_STOP = "com.example.twinmind.action.STOP_RECORDING"

        // Exposed so UI can read which file was recorded
        @Volatile
        var lastRecordedFilePath: String? = null
            private set
    }

    // 🔥 FIX: The recorder now lives inside the service
    private var recorder: AudioRecorder? = null
    private var currentFilePath: String? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startRecordingForeground()
            ACTION_STOP -> stopRecordingAndStopService()
        }
        // no need to restart if system kills us
        return START_NOT_STICKY
    }

    private fun startRecordingForeground() {
        if (recorder != null) return // already recording

        // 🔥 FIX: Create and start the recorder here
        recorder = AudioRecorder(applicationContext)
        currentFilePath = recorder?.startRecording()
        lastRecordedFilePath = currentFilePath

        val notification = buildNotification("Recording in progress")
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun stopRecordingAndStopService() {
        try {
            // 🔥 FIX: Stop the recorder here
            recorder?.stopRecording()
        } catch (_: Exception) {
            // Handle exceptions
        } finally {
            recorder = null
            currentFilePath = null
        }

        stopForeground(true)
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            )
            mgr.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(text: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("TwinMind Recording")
            .setContentText(text)
            .setSmallIcon(R.mipmap.ic_launcher) // or ic_launcher_foreground
            .setOngoing(true)
            .build()
    }
}
