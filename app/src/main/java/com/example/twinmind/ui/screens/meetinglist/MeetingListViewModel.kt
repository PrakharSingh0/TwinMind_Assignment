package com.example.twinmind.ui.screens.meetinglist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.twinmind.Graph
import com.example.twinmind.MeetingRepository
import com.example.twinmind.data.local.MeetingEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MeetingListViewModel(
    private val meetingRepository: MeetingRepository = Graph.meetingRepository
) : ViewModel() {

    val meetings = meetingRepository.getMeetings()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun deleteMeeting(meeting: MeetingEntity) {
        viewModelScope.launch {
            meetingRepository.deleteMeeting(meeting)
        }
    }

    companion object {
        val Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MeetingListViewModel() as T
            }
        }
    }
}
