package com.example.twinmind.ui.screens.recording

import java.io.File

data class RecordingState(
    val isRecording: Boolean = false,
    val totalSeconds: Int = 0,
    val statusText: String = "Not recording",
    val chunkFilePaths: List<String> = emptyList(),
    val currentChunkFile: File? = null
)
