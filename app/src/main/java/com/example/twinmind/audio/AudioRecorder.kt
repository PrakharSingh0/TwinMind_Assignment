package com.example.twinmind.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.os.Environment
import java.io.File

class AudioRecorder(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var currentFilePath: String? = null

    fun startRecording(): String {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
        if (dir != null && !dir.exists()) {
            dir.mkdirs()
        }

        val outputFile = File(dir, "rec_${System.currentTimeMillis()}.m4a")

        recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(128000)
            setAudioSamplingRate(44100)
            setOutputFile(outputFile.absolutePath)
            prepare()
            start()
        }

        currentFilePath = outputFile.absolutePath
        return outputFile.absolutePath
    }

    fun pause() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            recorder?.pause()
        }
    }

    fun resume() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            recorder?.resume()
        }
    }

    fun stopRecording(): String? {
        val filePath = currentFilePath
        try {
            recorder?.apply {
                try {
                    stop()
                } catch (e: RuntimeException) {
                    // Handle exception
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
