package com.example.twinmind.ui.screens.summary

import android.content.Intent
import android.media.MediaPlayer
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.twinmind.data.local.AudioChunkEntity
import com.example.twinmind.data.local.TranscriptSegmentEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryScreen(
    meetingId: Long,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    val viewModel: SummaryViewModel = viewModel(
        factory = SummaryViewModel.provideFactory(meetingId)
    )

    val meeting by viewModel.meeting.collectAsState()
    val summary by viewModel.summary.collectAsState()
    val transcript by viewModel.transcript.collectAsState()
    val chunks by viewModel.chunks.collectAsState()
    val isSummaryLoading by viewModel.isSummaryLoading.collectAsState()
    val isTranscriptLoading by viewModel.isTranscriptLoading.collectAsState()

    var isEditingTitle by remember { mutableStateOf(false) }
    var editedTitle by remember { mutableStateOf("") }

    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var playingChunkId by remember { mutableStateOf<Long?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    fun stopPlayback() {
        mediaPlayer?.release()
        mediaPlayer = null
        playingChunkId = null
    }

    fun playChunk(chunk: AudioChunkEntity) {
        stopPlayback()
        val mp = MediaPlayer()
        mp.setDataSource(chunk.filePath)
        mp.setOnCompletionListener { stopPlayback() }
        mp.prepare()
        mp.start()
        mediaPlayer = mp
        playingChunkId = chunk.id
    }

    fun shareSummary() {
        summary ?: return
        val text = """
            ${meeting?.title ?: "Meeting"}

            SUMMARY
            ${summary!!.summary}

            ACTION ITEMS
            ${summary!!.actionItems}

            KEY POINTS
            ${summary!!.keyPoints}
        """.trimIndent()

        context.startActivity(
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Meeting") },
                navigationIcon = {
                    IconButton(onClick = {
                        stopPlayback()
                        onBack()
                    }) {
                        Icon(Icons.Default.ArrowBack, null)
                    }
                },
                actions = {
                    if (summary != null) {
                        IconButton(onClick = ::shareSummary) {
                            Icon(Icons.Default.Share, null)
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (chunks.isNotEmpty()) {
                AudioPlayerBottomBar(
                    chunks = chunks,
                    playingChunkId = playingChunkId,
                    onPlay = { playChunk(it) },
                    onStop = ::stopPlayback
                )
            }
        }
    ) { padding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {

            /* ---------- HEADER ---------- */

            item {
                Column {
                    if (isEditingTitle) {
                        OutlinedTextField(
                            value = editedTitle,
                            onValueChange = { editedTitle = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.headlineSmall,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = {
                                viewModel.updateMeetingTitle(editedTitle)
                                isEditingTitle = false
                                focusManager.clearFocus()
                            })
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                meeting?.title ?: "Untitled meeting",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = {
                                editedTitle = meeting?.title.orEmpty()
                                isEditingTitle = true
                            }) {
                                Icon(Icons.Default.Edit, null)
                            }
                        }
                    }

                    Spacer(Modifier.height(6.dp))

                    AssistChip(
                        onClick = {},
                        label = { Text(meeting?.status ?: "Unknown") }
                    )
                }
            }

            /* ---------- SUMMARY ---------- */

            item {
                SectionHeader(
                    title = "Summary",
                    loading = isSummaryLoading,
                    onAction = { viewModel.generateSummaryFromApi() }
                )
            }

            if (summary == null) {
                item { EmptyHint("No summary yet")
                EmptyHint1("{If summary fails to generate, retry after 1 minute}")}
            } else {
                item {
                    Text(
                        summary!!.summary,
                        style = MaterialTheme.typography.bodyLarge,
                        lineHeight = 24.sp
                    )
                }
                item { InfoBlock("Action items", summary!!.actionItems) }
                item { InfoBlock("Key points", summary!!.keyPoints) }
            }

            /* ---------- TRANSCRIPT ---------- */

            item {
                SectionHeader(
                    title = "Transcript",
                    loading = isTranscriptLoading,
                    onAction = { viewModel.generateTranscriptFromAudio() }
                )
            }

            if (transcript.isEmpty()) {
                item { EmptyHint("No transcript available") }
            } else {
                items(transcript.sortedBy { it.startMs }) { seg ->
                    TranscriptBubble(seg)
                }
            }

            item { Spacer(Modifier.height(120.dp)) }
        }
    }
}

/* ================= COMPONENTS ================= */

@Composable
private fun SectionHeader(
    title: String,
    loading: Boolean,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        TextButton(onClick = onAction, enabled = !loading) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text("Generate")
            }
        }
    }
}

@Composable
private fun InfoBlock(
    title: String,
    text: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TranscriptBubble(segment: TranscriptSegmentEntity) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            formatTime(segment.startMs),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.width(48.dp)
        )
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Text(
                segment.text,
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun EmptyHint(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
@Composable
private fun EmptyHint1(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.surfaceVariant,
    )
}

@Composable
fun AudioPlayerBottomBar(
    chunks: List<AudioChunkEntity>,
    playingChunkId: Long?,
    onPlay: (AudioChunkEntity) -> Unit,
    onStop: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val maxHeight = if (expanded) 260.dp else 64.dp

    Surface(
        tonalElevation = 8.dp,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp, max = maxHeight)
            .animateContentSize()
    ) {
        Column {

            /* ---------- HANDLE + HEADER ---------- */

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(top = 8.dp, bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Drag Handle
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                )

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Audio recordings",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )

                    Icon(
                        imageVector = if (expanded)
                            Icons.Default.KeyboardArrowDown
                        else
                            Icons.Default.KeyboardArrowUp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            /* ---------- CONTENT ---------- */

            AnimatedVisibility(visible = expanded) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .heightIn(max = 200.dp)
                ) {
                    items(chunks) { chunk ->
                        val isPlaying = playingChunkId == chunk.id

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isPlaying)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else Color.Transparent
                                )
                                .clickable {
                                    if (isPlaying) onStop() else onPlay(chunk)
                                }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isPlaying)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isPlaying)
                                        Icons.Rounded.Stop
                                    else
                                        Icons.Rounded.PlayArrow,
                                    contentDescription = null,
                                    tint = if (isPlaying)
                                        MaterialTheme.colorScheme.onPrimary
                                    else
                                        MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Spacer(Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Part ${chunks.indexOf(chunk) + 1}",
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${formatTime(chunk.startMs)} – ${formatTime(chunk.endMs)}",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


/* ---------- HELPERS ---------- */

fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
