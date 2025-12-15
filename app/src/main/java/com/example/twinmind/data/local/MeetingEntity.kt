package com.example.twinmind.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.twinmind.data.local.model.SummaryStatus

@Entity(tableName = "meetings")
data class MeetingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val createdAt: Long,
    val status: String, // e.g. "recording", "completed"
    val transcript: String = "",

    val summary: String? = null,
    val actionItems: String? = null,
    val keyPoints: String? = null,

    val summaryStatus: SummaryStatus = SummaryStatus.NOT_STARTED
)
