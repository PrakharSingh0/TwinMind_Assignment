package com.example.twinmind.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AudioChunkDao {

    @Query("SELECT * FROM audio_chunks WHERE meetingId = :meetingId ORDER BY startMs")
    fun observeChunksForMeeting(meetingId: Long): Flow<List<AudioChunkEntity>>

    @Insert
    suspend fun insert(entity: AudioChunkEntity): Long
}
