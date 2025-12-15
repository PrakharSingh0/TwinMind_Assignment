package com.example.twinmind

import android.content.Context
import androidx.room.Room
import com.example.rec.network.SummaryApi
import com.example.rec.network.SummaryRequest
import com.example.twinmind.data.local.AppDatabase
import com.example.twinmind.data.local.AudioChunkDao
import com.example.twinmind.data.local.AudioChunkEntity
import com.example.twinmind.data.local.MeetingDao
import com.example.twinmind.data.local.MeetingEntity
import com.example.twinmind.data.local.SummaryDao
import com.example.twinmind.data.local.SummaryEntity
import com.example.twinmind.data.local.TranscriptSegmentDao
import com.example.twinmind.data.local.TranscriptSegmentEntity
import com.example.twinmind.data.remote.NetworkModule
import com.example.twinmind.data.remote.TranscriptionApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

object Graph {

    private lateinit var database: AppDatabase
    private val geminiApi: SummaryApi by lazy { NetworkModule.summaryApi }
    private val whisperApi: TranscriptionApi by lazy { NetworkModule.transcriptionApi }

    fun provide(context: Context) {
        if (!::database.isInitialized) {
            database = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "twinmind_db"
            )
                .fallbackToDestructiveMigration()
                .build()
        }
    }

    private val meetingDao: MeetingDao by lazy { database.meetingDao() }
    private val audioChunkDao: AudioChunkDao by lazy { database.audioChunkDao() }
    private val transcriptSegmentDao: TranscriptSegmentDao by lazy { database.transcriptSegmentDao() }
    private val summaryDao: SummaryDao by lazy { database.summaryDao() }

    val meetingRepository: MeetingRepository by lazy {
        MeetingRepository(meetingDao)
    }

    val audioChunkRepository: AudioChunkRepository by lazy {
        AudioChunkRepository(audioChunkDao)
    }

    val transcriptRepository: TranscriptRepository by lazy {
        TranscriptRepository(transcriptSegmentDao)
    }

    val summaryRepository: SummaryRepository by lazy {
        SummaryRepository(summaryDao, transcriptSegmentDao, geminiApi, whisperApi)
    }
}

// ---------------- Repositories ----------------

class MeetingRepository(
    private val meetingDao: MeetingDao
) {
    fun getMeetings(): Flow<List<MeetingEntity>> {
        return meetingDao.observeAllMeetings()
    }

    suspend fun updateMeetingTitle(meetingId: Long, title: String) {
        meetingDao.updateMeetingTitle(meetingId, title)
    }


    suspend fun createMeeting(
        title: String,
        status: String = "completed"
    ): Long {
        val meeting = MeetingEntity(
            title = title.ifBlank { "Meeting" },
            createdAt = System.currentTimeMillis(),
            status = status
        )
        return meetingDao.insert(meeting)
    }

    fun getMeeting(id: Long): Flow<MeetingEntity?> {
        return meetingDao.getMeetingById(id)
    }

    suspend fun deleteMeeting(meeting: MeetingEntity) {
        meetingDao.delete(meeting)
    }
}

class AudioChunkRepository(
    private val audioChunkDao: AudioChunkDao
) {
    suspend fun insertChunk(
        meetingId: Long,
        filePath: String,
        startMs: Long,
        endMs: Long
    ): Long {
        val chunk = AudioChunkEntity(
            meetingId = meetingId,
            filePath = filePath,
            startMs = startMs,
            endMs = endMs
        )
        return audioChunkDao.insert(chunk)
    }

    fun getChunksForMeeting(meetingId: Long): Flow<List<AudioChunkEntity>> {
        return audioChunkDao.observeChunksForMeeting(meetingId)
    }
}

class TranscriptRepository(
    private val transcriptSegmentDao: TranscriptSegmentDao
) {
    fun getSegmentsForMeeting(meetingId: Long): Flow<List<TranscriptSegmentEntity>> {
        return transcriptSegmentDao.observeSegmentsForMeeting(meetingId)
    }

    suspend fun saveSegments(segments: List<TranscriptSegmentEntity>) {
        transcriptSegmentDao.insertAll(segments)
    }
}

class SummaryRepository(
    private val summaryDao: SummaryDao,
    private val transcriptDao: TranscriptSegmentDao,
    private val summaryApi: SummaryApi,
    private val transcriptionApi: TranscriptionApi
) {
    fun getSummaryForMeeting(meetingId: Long): Flow<SummaryEntity?> {
        return summaryDao.observeSummaryForMeeting(meetingId)
    }


    // 🔥 Uses your backend -> Gemini (with mock fallback) for summarization
    suspend fun generateSummaryFromApi(
        meetingId: Long,
        transcriptSegments: List<TranscriptSegmentEntity>
    ): SummaryEntity {

        val transcriptTexts = transcriptSegments.map { it.text }

        val request = SummaryRequest(

            transcript = transcriptTexts.joinToString(" ")
        )

        // HTTP call through Retrofit -> backend -> Gemini (+ fallback)
        val response = summaryApi.analyze(request)
        val responseBody = response.body()!!

        val summaryEntity = SummaryEntity(
            meetingId = meetingId,
            title = responseBody.title,
            summary = responseBody.summary,
            actionItems = responseBody.action_items.joinToString("\n") { "- $it" },
            keyPoints = responseBody.key_points.joinToString("\n") { "- $it" }
        )

        summaryDao.upsert(summaryEntity)

        return summaryEntity
    }

    // 🔊 Uses your backend /transcribe (currently mock STT)
    suspend fun generateTranscriptFromAudio(
        meetingId: Long,
        audioChunk: AudioChunkEntity
    ) {
        val audioFile = File(audioChunk.filePath)
        if (!audioFile.exists()) {
            throw IllegalStateException("Audio file not found at ${audioChunk.filePath}")
        }

        // Prepare multipart for Retrofit
        val mediaType = "audio/mp4".toMediaType()
        val requestFile = audioFile.asRequestBody(mediaType)
        val filePart = MultipartBody.Part.createFormData(
            name = "audio",
            filename = audioFile.name,
            body = requestFile
        )

        // Corrected API call to use transcriptionApi
        val response = transcriptionApi.transcribe(filePart)
        val responseBody = response.body()

        if (response.isSuccessful && responseBody?.transcript != null) {
            val newTranscript = TranscriptSegmentEntity(
                id = 0L,
                meetingId = meetingId,
                text = responseBody.transcript.trim(),
                startMs = audioChunk.startMs,
                endMs = audioChunk.endMs
            )

            transcriptDao.upsert(newTranscript)

        } else {
            // Handle error case
            throw Exception("Failed to transcribe audio: ${response.errorBody()?.string()}")
        }

        // To avoid overwhelming the backend
        delay(500)
    }
}
