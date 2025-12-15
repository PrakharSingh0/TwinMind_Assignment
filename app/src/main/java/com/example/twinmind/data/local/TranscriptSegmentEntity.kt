package com.example.twinmind.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transcript_segments")
data class TranscriptSegmentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val meetingId: Long,
    val text: String,        // 💡 single text field per segment
    val startMs: Long,
    val endMs: Long
)
