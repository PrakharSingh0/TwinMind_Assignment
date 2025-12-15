package com.example.twinmind.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TranscriptSegmentDao {

    @Query("SELECT * FROM transcript_segments WHERE meetingId = :meetingId ORDER BY id")
    fun observeSegmentsForMeeting(
        meetingId: Long
    ): Flow<List<TranscriptSegmentEntity>>

    @Query("DELETE FROM transcript_segments WHERE meetingId = :meetingId")
    suspend fun deleteForMeeting(meetingId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(segments: List<TranscriptSegmentEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(segment: TranscriptSegmentEntity)

    @Query("SELECT * FROM transcript_segments WHERE meetingId = :meetingId AND startMs = :startMs LIMIT 1")
    suspend fun getSegmentByStartMs(meetingId: Long, startMs: Long): TranscriptSegmentEntity?
   @Transaction
    suspend fun upsert(segment: TranscriptSegmentEntity) {
        val existing = getSegmentByStartMs(segment.meetingId, segment.startMs)
        if (existing != null) {
            insert(segment.copy(id = existing.id))
        } else {
            insert(segment)
        }
    }
}
