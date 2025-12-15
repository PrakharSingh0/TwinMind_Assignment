package com.example.twinmind.audio

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.example.twinmind.Graph
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class RecordingService : Service() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    private val notificationManager by lazy { RecordingNotificationManager(this) }
    private val audioRecorder by lazy { AudioRecorder(this) }
    private val meetingRepository by lazy { Graph.meetingRepository }
    private val audioChunkRepository by lazy { Graph.audioChunkRepository }

    private var timerJob: Job? = null
    private var seconds = 0
    private var chunkFilePaths = mutableListOf<String>()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> start()
            ACTION_STOP -> {
                val title = intent.getStringExtra(EXTRA_TITLE)
                stop(title)
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel() // Cancel all coroutines started by this service
    }

    private fun start() {
        notificationManager.createNotificationChannel()
        startForeground(RecordingNotificationManager.NOTIFICATION_ID, notificationManager.buildNotification("00:00"))

        startNewChunk()
        startTimer()
    }

    private fun stop(title: String?) {
        stopCurrentChunk()
        timerJob?.cancel()

        // Use the service's own scope to ensure this task is managed correctly
        serviceScope.launch {
            val newMeetingId = meetingRepository.createMeeting(
                title = title ?: "Meeting @ ${System.currentTimeMillis()}",
                status = "completed"
            )

            chunkFilePaths.forEachIndexed { index, filePath ->
                audioChunkRepository.insertChunk(
                    meetingId = newMeetingId,
                    filePath = filePath,
                    startMs = (index * 30 * 1000).toLong(),
                    endMs = ((index + 1) * 30 * 1000).toLong()
                )
            }
            stopSelf()
        }
    }

    private fun startNewChunk() {
        val filePath = audioRecorder.startRecording()
        chunkFilePaths.add(filePath)
    }

    private fun stopCurrentChunk() {
        audioRecorder.stopRecording()
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = serviceScope.launch(Dispatchers.Main) { // UI updates on Main
            while (true) {
                delay(1000)
                seconds++
                val timerText = String.format("%02d:%02d", seconds / 60, seconds % 60)
                notificationManager.buildNotification(timerText).let {
                    notificationManager.notificationManager.notify(RecordingNotificationManager.NOTIFICATION_ID, it)
                }
                if (seconds > 0 && seconds % 30 == 0) {
                    stopCurrentChunk()
                    startNewChunk()
                }
            }
        }
    }

    companion object {
        const val ACTION_START = "com.example.twinmind.action.START_RECORDING"
        const val ACTION_STOP = "com.example.twinmind.action.STOP_RECORDING"
        const val EXTRA_TITLE = "EXTRA_TITLE"
    }
}
