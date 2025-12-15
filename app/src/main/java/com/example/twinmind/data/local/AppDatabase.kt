package com.example.twinmind.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        MeetingEntity::class,
        AudioChunkEntity::class,
        TranscriptSegmentEntity::class,
        SummaryEntity::class
    ],
    version = 2
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun meetingDao(): MeetingDao
    abstract fun audioChunkDao(): AudioChunkDao
    abstract fun transcriptSegmentDao(): TranscriptSegmentDao
    abstract fun summaryDao(): SummaryDao
}
