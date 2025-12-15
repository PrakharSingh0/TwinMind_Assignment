package com.example.twinmind.ui.screens.recording

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.twinmind.Graph
import com.example.twinmind.audio.AudioRecorder
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class RecordingViewModel : ViewModel() {

    private lateinit var audioRecorder: AudioRecorder
    private val meetingRepository = Graph.meetingRepository
    private val audioChunkRepository = Graph.audioChunkRepository

    private val _state = MutableStateFlow(RecordingState())
    val state: StateFlow<RecordingState> = _state.asStateFlow()

    private var timerJob: Job? = null
    private val chunkFilePaths = mutableListOf<String>()

    fun initialize(context: Context) {
        if (!::audioRecorder.isInitialized) {
            audioRecorder = AudioRecorder(context)
        }
    }

    fun startRecording() {
        startNewChunk()
        _state.value = RecordingState(
            isRecording = true,
            statusText = "Recording...",
        )
        startTimer()
    }

    private fun startNewChunk() {
        val filePath = audioRecorder.startRecording()
        _state.value = _state.value.copy(currentChunkFile = File(filePath))
    }

    suspend fun stopRecording(title: String): Long {
        stopCurrentChunk()
        timerJob?.cancel()

        val newMeetingId = meetingRepository.createMeeting(
            title = title,
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

        _state.value = RecordingState()
        return newMeetingId
    }

    private fun stopCurrentChunk() {
        audioRecorder.stopRecording()?.let { filePath ->
            chunkFilePaths.add(filePath)
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (state.value.isRecording) {
                delay(1000)
                val newSeconds = _state.value.totalSeconds + 1
                _state.value = _state.value.copy(totalSeconds = newSeconds)

                if (newSeconds % 30 == 0) {
                    stopCurrentChunk()
                    startNewChunk()
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }

    companion object {
        val Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return RecordingViewModel() as T
            }
        }
    }
}
