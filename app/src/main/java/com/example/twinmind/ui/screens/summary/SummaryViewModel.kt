package com.example.twinmind.ui.screens.summary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.twinmind.AudioChunkRepository
import com.example.twinmind.Graph
import com.example.twinmind.MeetingRepository
import com.example.twinmind.SummaryRepository
import com.example.twinmind.TranscriptRepository
import com.example.twinmind.data.local.AudioChunkEntity
import com.example.twinmind.data.local.MeetingEntity
import com.example.twinmind.data.local.SummaryEntity
import com.example.twinmind.data.local.TranscriptSegmentEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SummaryViewModel(
    private val meetingId: Long,
    private val meetingRepository: MeetingRepository,
    private val audioChunkRepository: AudioChunkRepository,
    private val transcriptRepository: TranscriptRepository,
    private val summaryRepository: SummaryRepository
) : ViewModel() {

    // --------- State ---------

    private val _meeting = MutableStateFlow<MeetingEntity?>(null)
    val meeting: StateFlow<MeetingEntity?> = _meeting

    private val _chunks = MutableStateFlow<List<AudioChunkEntity>>(emptyList())
    val chunks: StateFlow<List<AudioChunkEntity>> = _chunks

    private val _transcript = MutableStateFlow<List<TranscriptSegmentEntity>>(emptyList())
    val transcript: StateFlow<List<TranscriptSegmentEntity>> = _transcript

    private val _summary = MutableStateFlow<SummaryEntity?>(null)
    val summary: StateFlow<SummaryEntity?> = _summary

    private val _isSummaryLoading = MutableStateFlow(false)
    val isSummaryLoading: StateFlow<Boolean> = _isSummaryLoading

    private val _isTranscriptLoading = MutableStateFlow(false)
    val isTranscriptLoading: StateFlow<Boolean> = _isTranscriptLoading.asStateFlow()

    private val _summaryError = MutableStateFlow<String?>(null)
    val summaryError: StateFlow<String?> = _summaryError

    private val _transcriptError = MutableStateFlow<String?>(null)
    val transcriptError: StateFlow<String?> = _transcriptError

    init {
        // Observe meeting
        viewModelScope.launch {
            meetingRepository.getMeeting(meetingId).collect { m ->
                _meeting.value = m
            }
        }

        // Observe chunks
        viewModelScope.launch {
            audioChunkRepository.getChunksForMeeting(meetingId).collect { list ->
                _chunks.value = list
            }
        }

        // Observe transcript
        viewModelScope.launch {
            transcriptRepository.getSegmentsForMeeting(meetingId).collect { list ->
                _transcript.value = list
            }
        }

        // Observe summary
        viewModelScope.launch {
            summaryRepository.getSummaryForMeeting(meetingId).collect { s ->
                _summary.value = s
            }
        }
    }

    fun updateMeetingTitle(newTitle: String) {
        viewModelScope.launch {
            meeting.value?.let { current ->
                meetingRepository.updateMeetingTitle(
                    meetingId = current.id,
                    title = newTitle
                )
            }
        }
    }


    // ---------- Gemini summary via backend ----------

    fun generateSummaryFromApi() {
        viewModelScope.launch {
            _isSummaryLoading.value = true
            _summaryError.value = null

            try {
                // Ensure we have some transcript; if not, use current state
                val currentTranscript = _transcript.value
                if (currentTranscript.isEmpty()) {
                    _summaryError.value = "No transcript available. Please transcribe first."
                    _isSummaryLoading.value = false
                    return@launch
                }

                val result = summaryRepository.generateSummaryFromApi(
                    meetingId = meetingId,
                    transcriptSegments = currentTranscript
                )
                _summary.value = result
            } catch (e: Exception) {
                e.printStackTrace()
                _summaryError.value = "Failed to fetch summary: ${e.message ?: "Unknown error"}"
            } finally {
                _isSummaryLoading.value = false
            }
        }
    }

    // ---------- NEW: Transcribe from audio (calls /transcribe) ----------

    fun generateTranscriptFromAudio() {
        viewModelScope.launch {
            _isTranscriptLoading.value = true
            _transcriptError.value = null
            try {
                val chunkList = audioChunkRepository.getChunksForMeeting(meetingId).first()

                if (chunkList.isEmpty()) {
                    _transcriptError.value = "No audio recorded for this meeting."
                    return@launch
                }

                chunkList.forEach { chunk ->
                    summaryRepository.generateTranscriptFromAudio(meetingId, chunk)
                }

            } catch (e: Exception) {
                e.printStackTrace()
                _transcriptError.value = "Failed to transcribe: ${e.message}"
            } finally {
                _isTranscriptLoading.value = false
            }
        }
    }

    companion object {
        fun provideFactory(meetingId: Long): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val graph = Graph
                    @Suppress("UNCHECKED_CAST")
                    return SummaryViewModel(
                        meetingId = meetingId,
                        meetingRepository = graph.meetingRepository,
                        audioChunkRepository = graph.audioChunkRepository,
                        transcriptRepository = graph.transcriptRepository,
                        summaryRepository = graph.summaryRepository
                    ) as T
                }
            }
        }
    }
}
