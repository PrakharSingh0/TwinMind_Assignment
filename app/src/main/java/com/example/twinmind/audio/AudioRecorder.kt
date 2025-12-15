package com.example.twinmind.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Environment
import java.io.File

class AudioRecorder(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var currentFilePath: String? = null

    fun startRecording(): String {
        // Save in app-specific external music directory
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
        if (dir != null && !dir.exists()) {
            dir.mkdirs()
        }

        val outputFile = File(dir, "rec_${System.currentTimeMillis()}.m4a")

        val mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(128000)
            setAudioSamplingRate(44100)
            setOutputFile(outputFile.absolutePath)
            prepare()
            start()
        }

        recorder = mediaRecorder
        currentFilePath = outputFile.absolutePath
        return outputFile.absolutePath
    }

    fun stopRecording(): String? {
        val filePath = currentFilePath
        try {
            recorder?.apply {
                try {
                    stop()
                } catch (e: RuntimeException) {
                    // If stop is called too early, ignore crash and delete file later if needed
                }
                reset()
                release()
            }
        } finally {
            recorder = null
            currentFilePath = null
        }
        return filePath
    }
}
