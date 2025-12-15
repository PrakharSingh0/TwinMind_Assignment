package com.example.twinmind.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "audio_chunks")
data class AudioChunkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val meetingId: Long,
    val filePath: String,
    val startMs: Long,
    val endMs: Long,
    val uploaded: Boolean = false,
    val transcribed: Boolean = false
)
