package com.example.twinmind.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "summaries")
data class SummaryEntity(
    @PrimaryKey val meetingId: Long,
    val title: String,
    val summary: String,
    val actionItems: String,
    val keyPoints: String
)
