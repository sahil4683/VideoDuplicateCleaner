package com.videocleaner.presentation.scan

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.videocleaner.domain.model.ScanPhase
import com.videocleaner.domain.model.ScanProgress

/**
 * Screen shown during an active video scan.
 * Displays real-time progress with phase indicators and cancellation support.
 */
@Composable
fun ScanProgressScreen(
    onComplete: () -> Unit,
    onBack: () -> Unit,
    viewModel: ScanProgressViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Permission launcher
    val permission =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_VIDEO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { granted ->
            if (granted) viewModel.startScan()
        }

    LaunchedEffect(uiState.isComplete) {
        if (uiState.isComplete) {
            kotlinx.coroutines.delay(2000)
            onComplete()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scanning Videos") },
                navigationIcon = {
                    if (!uiState.isScanning) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, "Back")
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            when {
                uiState.isComplete ->
                    CompleteScanState(
                        stats = uiState.completedStats,
                        onViewResults = onComplete,
                    )
                uiState.isScanning -> ActiveScanState(uiState = uiState, onCancel = viewModel::cancelScan)
                uiState.error != null ->
                    ErrorState(
                        error = uiState.error!!,
                        onRetry = {
                            if (viewModel.hasStoragePermission()) {
                                viewModel.startScan()
                            } else {
                                permissionLauncher.launch(permission)
                            }
                        },
                    )
                else ->
                    IdleState(
                        onStart = {
                            if (viewModel.hasStoragePermission()) {
                                viewModel.startScan()
                            } else {
                                permissionLauncher.launch(permission)
                            }
                        },
                    )
            }
        }
    }
}

@Composable
private fun IdleState(onStart: () -> Unit) {
    Icon(
        Icons.Default.VideoLibrary,
        null,
        modifier = Modifier.size(80.dp),
        tint = MaterialTheme.colorScheme.primary,
    )
    Spacer(Modifier.height(24.dp))
    Text(
        text = "Ready to Scan",
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
    )
    Spacer(Modifier.height(8.dp))
    Text(
        text = "Tap Start Scan to analyze all videos on your device and find duplicates.",
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(32.dp))
    Button(
        onClick = onStart,
        modifier =
            Modifier
                .fillMaxWidth()
                .height(56.dp),
    ) {
        Icon(Icons.Default.Search, null)
        Spacer(Modifier.width(8.dp))
        Text("Start Scan", style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun ActiveScanState(
    uiState: ScanProgressUiState,
    onCancel: () -> Unit,
) {
    CircularProgressIndicator(
        progress = { uiState.percentage / 100f },
        modifier = Modifier.size(120.dp),
        strokeWidth = 8.dp,
    )
    Spacer(Modifier.height(16.dp))
    Text(
        text = "${uiState.percentage.toInt()}%",
        style = MaterialTheme.typography.headlineLarge,
        fontWeight = FontWeight.Bold,
    )
    Spacer(Modifier.height(8.dp))
    Text(
        text = phaseLabel(uiState.phase),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
    )
    Spacer(Modifier.height(4.dp))
    Text(
        text = "${uiState.current} / ${uiState.total} files",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    if (uiState.currentFile.isNotEmpty()) {
        Spacer(Modifier.height(4.dp))
        Text(
            text = uiState.currentFile,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
    Spacer(Modifier.height(32.dp))
    OutlinedButton(onClick = onCancel) {
        Text("Cancel")
    }
}

@Composable
private fun CompleteScanState(
    stats: ScanProgress.Complete?,
    onViewResults: () -> Unit,
) {
    Icon(
        Icons.Default.CheckCircle,
        null,
        modifier = Modifier.size(80.dp),
        tint = MaterialTheme.colorScheme.primary,
    )
    Spacer(Modifier.height(24.dp))
    Text(
        text = "Scan Complete!",
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
    )
    Spacer(Modifier.height(16.dp))
    if (stats != null) {
        ScanResultItem("Total Videos", stats.totalVideos.toString())
        ScanResultItem("Exact Duplicates", stats.exactDuplicates.toString())
        ScanResultItem("Similar Videos", stats.similarVideos.toString())
        ScanResultItem("Recoverable", formatBytes(stats.recoverableBytes))
    }
    Spacer(Modifier.height(24.dp))
    Button(onClick = onViewResults) {
        Text("View Results")
    }
}

@Composable
private fun ScanResultItem(
    label: String,
    value: String,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun ErrorState(
    error: String,
    onRetry: () -> Unit,
) {
    Icon(
        Icons.Default.Error,
        null,
        modifier = Modifier.size(80.dp),
        tint = MaterialTheme.colorScheme.error,
    )
    Spacer(Modifier.height(16.dp))
    Text(
        text = "Scan Failed",
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.error,
    )
    Spacer(Modifier.height(8.dp))
    Text(
        text = error,
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(24.dp))
    Button(onClick = onRetry) {
        Text("Retry")
    }
}

private fun phaseLabel(phase: ScanPhase): String =
    when (phase) {
        ScanPhase.INDEXING -> "Indexing videos..."
        ScanPhase.GROUPING_BY_SIZE -> "Grouping by size..."
        ScanPhase.COMPUTING_HASHES -> "Computing hashes..."
        ScanPhase.ANALYZING_FRAMES -> "Analyzing frames..."
        ScanPhase.FINALIZING -> "Finalizing..."
    }

private fun formatBytes(bytes: Long): String =
    when {
        bytes >= 1_073_741_824 -> "%.1f GB".format(bytes / 1_073_741_824.0)
        bytes >= 1_048_576 -> "%.1f MB".format(bytes / 1_048_576.0)
        bytes >= 1_024 -> "%.1f KB".format(bytes / 1_024.0)
        else -> "$bytes B"
    }
