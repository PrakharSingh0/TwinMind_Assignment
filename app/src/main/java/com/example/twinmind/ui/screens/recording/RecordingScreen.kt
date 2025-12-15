package com.example.twinmind.ui.screens.recording

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.twinmind.recording.RecordingActions
import com.example.twinmind.recording.RecordingService
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingScreen(
    onBack: () -> Unit,
    onFinished: (Long) -> Unit
) {
    val context = LocalContext.current
    val viewModel: RecordingViewModel = viewModel(factory = RecordingViewModel.Factory)
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.initialize(context)
    }

    val state by viewModel.state.collectAsState()

    /* ---------- Permission ---------- */

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startRecordingService(context)
            viewModel.startRecording()
        }
    }


    val notificationPermissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { /* nothing needed */ }


    /* ---------- UI ---------- */

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New recording") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },

        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            if (
                                ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.POST_NOTIFICATIONS
                                ) != PackageManager.PERMISSION_GRANTED
                            ) {
                                notificationPermissionLauncher.launch(
                                    Manifest.permission.POST_NOTIFICATIONS
                                )
                                return@Button
                            }
                        }

                        if (!state.isRecording) {
                            val hasPermission =
                                ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED

                            if (hasPermission) {
                                startRecordingService(context)
                                viewModel.startRecording()
                            } else {
                                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        } else {
                            stopRecordingService(context)

                            val formatter = SimpleDateFormat(
                                "dd MMM yyyy, HH:mm a",
                                Locale.getDefault()
                            )
                            val autoTitle =
                                "Meeting @ ${formatter.format(System.currentTimeMillis())}"

                            scope.launch {
                                val meetingId = viewModel.stopRecording(autoTitle)
                                if (meetingId > 0) onFinished(meetingId)
                            }
                        }
                    }
                ) {
                    Text(
                        text = if (state.isRecording)
                            "Stop Recording"
                        else
                            "Start Recording"
                    )
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

            Text(
                text = "Capture a new meeting",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = String.format(
                    "%02d:%02d",
                    state.totalSeconds / 60,
                    state.totalSeconds % 60
                ),
                style = MaterialTheme.typography.displayLarge
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = state.statusText,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(Modifier.height(32.dp))

            RecordingWaveform(isRecording = state.isRecording)
        }
    }
}

/* ---------------------------------------------------
   🔊 Animated Recording Indicator
--------------------------------------------------- */

@Composable
fun RecordingWaveform(isRecording: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")

    val scale by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scale"
    )

    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha"
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

/* ---------------------------------------------------
   🔧 Service helpers
--------------------------------------------------- */

private fun startRecordingService(context: android.content.Context) {
    val intent = Intent(context, RecordingService::class.java).apply {
        action = RecordingActions.ACTION_START
    }
    ContextCompat.startForegroundService(context, intent)
}

private fun stopRecordingService(context: android.content.Context) {
    val intent = Intent(context, RecordingService::class.java).apply {
        action = RecordingActions.ACTION_STOP
    }
    context.startService(intent)
}


