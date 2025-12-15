package com.example.twinmind.ui.screens.recording

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.twinmind.audio.RecordingService
import com.example.twinmind.audio.RecordingState
import kotlinx.coroutines.flow.StateFlow

class RecordingViewModel : ViewModel() {

    val recordingState: StateFlow<RecordingState> = RecordingService.state

    fun onStart(context: Context) {
        Intent(context, RecordingService::class.java).also {
            it.action = RecordingService.ACTION_START
            context.startService(it)
        }
    }

    fun onPause(context: Context) {
        Intent(context, RecordingService::class.java).also {
            it.action = RecordingService.ACTION_PAUSE
            context.startService(it)
        }
    }

    fun onResume(context: Context) {
        Intent(context, RecordingService::class.java).also {
            it.action = RecordingService.ACTION_RESUME
            context.startService(it)
        }
    }

    fun onStop(context: Context, title: String) {
        Intent(context, RecordingService::class.java).also {
            it.action = RecordingService.ACTION_STOP
            it.putExtra(RecordingService.EXTRA_TITLE, title)
            context.startService(it)
        }
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
