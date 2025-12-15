package com.example.twinmind.ui.screens.recording

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.twinmind.audio.RecordingState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: RecordingViewModel = viewModel(factory = RecordingViewModel.Factory)

    val state by viewModel.recordingState.collectAsState()

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.onStart(context)
    }

    LaunchedEffect(state) {
        if (state is RecordingState.Idle) {
            onBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New recording") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (state is RecordingState.Recording) {
                    Button(onClick = { viewModel.onPause(context) }) {
                        Icon(Icons.Default.Pause, null)
                    }
                } else if (state is RecordingState.Paused) {
                    Button(onClick = { viewModel.onResume(context) }) {
                        Icon(Icons.Default.Mic, null)
                    }
                }

                Button(onClick = {
                    if (state == RecordingState.Idle) {
                        val hasPermission =
                            ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED

                        if (hasPermission) {
                            viewModel.onStart(context)
                        } else {
                            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    } else {
                        viewModel.onStop(context, "Meeting @ ${System.currentTimeMillis()}")
                    }
                }) {
                    Icon(if (state == RecordingState.Idle) Icons.Default.Mic else Icons.Default.Stop, null)
                }
            }
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            val (seconds, statusText) = when (val s = state) {
                is RecordingState.Recording -> s.seconds to "Recording..."
                is RecordingState.Paused -> s.seconds to "Paused"
                else -> 0 to "Not recording"
            }

            Text(
                text = "Capture a new meeting",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = String.format("%02d:%02d", seconds / 60, seconds % 60),
                style = MaterialTheme.typography.displayLarge
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(Modifier.height(32.dp))

            RecordingWaveform(
                isRecording = state is RecordingState.Recording
            )
        }
    }
}


@Composable
fun RecordingWaveform(
    isRecording: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "")

    val scale by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = ""
    )

    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200), 
            repeatMode = RepeatMode.Restart
        ),
        label = ""
    )

    Box(
        modifier = Modifier.size(140.dp),
        contentAlignment = Alignment.Center
    ) {

        if (isRecording) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                    }
                    .border(
                        width = 3.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape
                    )
            )
        }

        Box(
            modifier = Modifier
                .size(32.dp)
                .background(
                    MaterialTheme.colorScheme.primary,
                    CircleShape
                )
        )
    }
}
