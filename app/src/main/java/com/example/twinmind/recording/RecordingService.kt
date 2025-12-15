package com.example.twinmind.recording

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.*
import androidx.core.app.NotificationCompat
import com.example.twinmind.MainActivity
import com.example.twinmind.R
import com.example.twinmind.audio.AudioRecorder
import java.util.concurrent.TimeUnit

class RecordingService : Service() {

    companion object {
        const val CHANNEL_ID = "recording_channel"
        const val NOTIFICATION_ID = 1

        @Volatile
        var lastRecordedFilePath: String? = null
            private set
    }

    private var recorder: AudioRecorder? = null
    private var startTime = 0L

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var notificationManager: NotificationManager

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            RecordingActions.ACTION_START -> startRecording()
            RecordingActions.ACTION_STOP -> stopRecording()
        }
        return START_NOT_STICKY
    }

    private fun contentIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        return PendingIntent.getActivity(
            this,
            100,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }


    private fun startRecording() {
        if (recorder != null) return

        // 🔥 Samsung FIX: start foreground IMMEDIATELY
        startForeground(
            NOTIFICATION_ID,
            buildNotification(0L)
        )

        recorder = AudioRecorder(applicationContext)
        lastRecordedFilePath = recorder?.startRecording()
        startTime = System.currentTimeMillis()

        startTimer()
    }





    private fun stopRecording() {
        handler.removeCallbacksAndMessages(null)

        try {
            recorder?.stopRecording()
        } catch (_: Exception) {
        } finally {
            recorder = null
        }

        stopForeground(true)
        stopSelf()
    }

    private fun startTimer() {
        handler.post(object : Runnable {
            override fun run() {
                val elapsed = System.currentTimeMillis() - startTime
                notificationManager.notify(
                    NOTIFICATION_ID,
                    buildNotification(elapsed)
                )
                handler.postDelayed(this, 1000)
            }
        })
    }

    private fun buildNotification(elapsedMs: Long): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("TwinMind Recording")
            .setContentText("⏱ ${formatTime(elapsedMs)}")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .addAction(stopAction())
            .build()
    }

    private fun stopAction(): NotificationCompat.Action {
        val intent = Intent(this, RecordingService::class.java).apply {
            action = RecordingActions.ACTION_STOP
        }

        val pendingIntent = PendingIntent.getService(
            this,
            200, // MUST be unique (important for Samsung)
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Action.Builder(
            0,
            "Stop",
            pendingIntent
        ).build()
    }


    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Recording",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }
    }
}

private fun formatTime(ms: Long): String {
    val seconds = TimeUnit.MILLISECONDS.toSeconds(ms)
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return "%02d:%02d".format(minutes, remainingSeconds)
}
