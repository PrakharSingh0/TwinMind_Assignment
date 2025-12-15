package com.example.twinmind.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SummaryDao {

    @Query("SELECT * FROM summaries WHERE meetingId = :meetingId LIMIT 1")
    fun observeSummaryForMeeting(meetingId: Long): Flow<SummaryEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(summary: SummaryEntity)
}
