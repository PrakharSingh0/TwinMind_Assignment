package com.example.twinmind.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MeetingDao {

    @Query("SELECT * FROM meetings ORDER BY createdAt DESC")
    fun observeAllMeetings(): Flow<List<MeetingEntity>>

    @Insert
    suspend fun insert(meeting: MeetingEntity): Long

    @Query("SELECT * FROM meetings WHERE id = :id")
    fun getMeetingById(id: Long): Flow<MeetingEntity?>

    @Delete
    suspend fun delete(meeting: MeetingEntity)

    @Query("UPDATE meetings SET title = :title WHERE id = :meetingId")
    suspend fun updateMeetingTitle(meetingId: Long, title: String)
}
