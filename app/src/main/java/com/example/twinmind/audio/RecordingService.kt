package com.example.twinmind.audio

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import com.example.twinmind.Graph
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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
            ACTION_PAUSE -> pause()
            ACTION_RESUME -> resume()
            ACTION_STOP -> {
                val title = intent.getStringExtra(EXTRA_TITLE)
                stop(title)
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }

    private fun start() {
        notificationManager.createNotificationChannel()
        startForeground(
            RecordingNotificationManager.NOTIFICATION_ID,
            notificationManager.buildNotification("00:00", isPaused = false)
        )

        startNewChunk()
        startTimer()
        _state.value = RecordingState.Recording(0)
    }

    private fun pause() {
        audioRecorder.pause()
        timerJob?.cancel()
        _state.value = RecordingState.Paused(seconds)
    }

    private fun resume() {
        audioRecorder.resume()
        startTimer()
        _state.value = RecordingState.Recording(seconds)
    }

    private fun stop(title: String?) {
        stopCurrentChunk()
        timerJob?.cancel()

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

        _state.value = RecordingState.Idle
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
        timerJob = serviceScope.launch(Dispatchers.Main) {
            while (true) {
                delay(1000)
                seconds++
                val timerText = String.format("%02d:%02d", seconds / 60, seconds % 60)
                val isPaused = _state.value is RecordingState.Paused
                notificationManager.buildNotification(timerText, isPaused).let {
                    notificationManager.notificationManager.notify(RecordingNotificationManager.NOTIFICATION_ID, it)
                }

                if (_state.value is RecordingState.Recording) {
                    _state.value = RecordingState.Recording(seconds)
                }

                if (seconds > 0 && seconds % 30 == 0) {
                    stopCurrentChunk()
                    startNewChunk()
                }
            }
        }
    }

    companion object {
        private val _state = MutableStateFlow<RecordingState>(RecordingState.Idle)
        val state = _state.asStateFlow()

        const val ACTION_START = "com.example.twinmind.action.START_RECORDING"
        const val ACTION_PAUSE = "com.example.twinmind.action.PAUSE_RECORDING"
        const val ACTION_RESUME = "com.example.twinmind.action.RESUME_RECORDING"
        const val ACTION_STOP = "com.example.twinmind.action.STOP_RECORDING"
        const val EXTRA_TITLE = "EXTRA_TITLE"
    }
}

sealed class RecordingState {
    object Idle : RecordingState()
    data class Recording(val seconds: Int) : RecordingState()
    data class Paused(val seconds: Int) : RecordingState()
}
