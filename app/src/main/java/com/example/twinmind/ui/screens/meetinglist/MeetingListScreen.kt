package com.example.twinmind.ui.screens.meetinglist

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.twinmind.data.local.MeetingEntity
import com.example.twinmind.data.local.model.RecordingStatus
import com.example.twinmind.data.local.model.SummaryStatus
import java.text.SimpleDateFormat
import java.util.*

enum class MeetingFilter { ALL, RECORDING, SUMMARIZED }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeetingListScreen(
    onStartRecording: () -> Unit,
    onOpenMeeting: (String) -> Unit,
    viewModel: MeetingListViewModel = viewModel(factory = MeetingListViewModel.Factory)
) {
    val meetings by viewModel.meetings.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(MeetingFilter.ALL) }

    var pendingDelete by remember { mutableStateOf<MeetingEntity?>(null) }

    val filteredMeetings = meetings
        .filter { it.title.contains(searchQuery, ignoreCase = true) }
        .filter {
            when (filter) {
                MeetingFilter.ALL -> true
                MeetingFilter.RECORDING -> it.status == RecordingStatus.RECORDING.name
                MeetingFilter.SUMMARIZED -> it.summaryStatus == SummaryStatus.COMPLETED
            }
        }

    val grouped = filteredMeetings.groupBy {
        val today = Calendar.getInstance()
        val day = Calendar.getInstance().apply { timeInMillis = it.createdAt }

        when {
            today.get(Calendar.DAY_OF_YEAR) == day.get(Calendar.DAY_OF_YEAR) -> "Today"
            today.get(Calendar.DAY_OF_YEAR) - 1 == day.get(Calendar.DAY_OF_YEAR) -> "Yesterday"
            else -> "Earlier"
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("TwinMind", fontWeight = FontWeight.Bold) }
                )

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    placeholder = { Text("Search meetings") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp)
                )

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip("All", filter == MeetingFilter.ALL) {
                        filter = MeetingFilter.ALL
                    }
                    FilterChip("Recording", filter == MeetingFilter.RECORDING) {
                        filter = MeetingFilter.RECORDING
                    }
                    FilterChip("Summarized", filter == MeetingFilter.SUMMARIZED) {
                        filter = MeetingFilter.SUMMARIZED
                    }
                }

                Spacer(Modifier.height(12.dp))
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onStartRecording) {
                Icon(Icons.Default.Mic, null)
            }
        }
    ) { padding ->

        when {
            meetings.isEmpty() -> NoMeetingsState(padding)
            filteredMeetings.isEmpty() -> NoResultsState(padding)
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    grouped.forEach { (section, items) ->
                        item {
                            Text(
                                section,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }

                        items(items, key = { it.id }) { meeting ->
                            SwipeToDeleteConfirm(
                                onConfirmDelete = { pendingDelete = meeting }
                            ) {
                                MeetingItem(
                                    meeting = meeting,
                                    onClick = { onOpenMeeting(meeting.id.toString()) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    /* ------------------------------
       🛑 DELETE CONFIRMATION DIALOG
    ------------------------------ */
    if (pendingDelete != null) {
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            icon = { Icon(Icons.Default.Delete, null) },
            title = { Text("Delete meeting?") },
            text = {
                Text(
                    "This recording and its summary will be permanently removed."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteMeeting(pendingDelete!!)
                        pendingDelete = null
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/* ---------------------------------------------------
   🤷‍♂️ EMPTY STATES
--------------------------------------------------- */

@Composable
private fun NoMeetingsState(padding: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            Icon(
                Icons.Default.FolderOff,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "No meetings yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "Tap the microphone button to start a new recording.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun NoResultsState(padding: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            Icon(
                Icons.Default.SearchOff,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "No results found",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "Try adjusting your search or filter.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/* ---------------------------------------------------
   👈 SWIPE WITH FAIL-SAFE
--------------------------------------------------- */
@Composable
private fun SwipeToDeleteConfirm(
    onConfirmDelete: () -> Unit,
    content: @Composable () -> Unit
) {
    var offsetX by remember { mutableStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (offsetX < -180f) {
                            onConfirmDelete()
                        }
                        offsetX = 0f
                    }
                ) { _, drag ->
                    offsetX = (offsetX + drag).coerceAtLeast(-250f)
                }
            }
    ) {
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.toInt(), 0) }
        ) {
            content()
        }
    }
}

/* ---------------------------------------------------
   📄 MEETING ITEM
--------------------------------------------------- */
@Composable
private fun MeetingItem(
    meeting: MeetingEntity,
    onClick: () -> Unit
) {
    val isRecording = meeting.status == RecordingStatus.RECORDING.name
    val isSummarized = meeting.summaryStatus == SummaryStatus.COMPLETED

    val timeText = remember(meeting.createdAt) {
        SimpleDateFormat("dd MMM · HH:mm", Locale.getDefault())
            .format(Date(meeting.createdAt))
    }

    val accentColor = when {
        isRecording -> MaterialTheme.colorScheme.error
        isSummarized -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outlineVariant
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // 🎯 Subtle accent indicator
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(accentColor, CircleShape)
            )

            Spacer(Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {

                // 🧠 Title
                Text(
                    text = meeting.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(6.dp))

                // 🕒 Meta text
                Text(
                    text = timeText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}


/* ---------------------------------------------------
   🎛 FILTER CHIP
--------------------------------------------------- */
@Composable
private fun FilterChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(
                if (selected)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.surfaceVariant
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(
            text,
            color = if (selected)
                MaterialTheme.colorScheme.onPrimary
            else
                MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium
        )
    }
}
