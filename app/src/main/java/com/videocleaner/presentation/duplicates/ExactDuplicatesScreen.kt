package com.videocleaner.presentation.duplicates

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.videocleaner.domain.model.DuplicateGroup
import com.videocleaner.domain.model.VideoFile
import com.videocleaner.presentation.components.ConfirmDeleteDialog
import com.videocleaner.presentation.components.VideoCard

/**
 * Screen showing groups of exact duplicate videos.
 * Supports smart selection helpers and batch deletion.
 */
@Composable
fun ExactDuplicatesScreen(
    onVideoClick: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: ExactDuplicatesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.showDeleteSuccessSnackbar) {
        if (uiState.showDeleteSuccessSnackbar) {
            snackbarHostState.showSnackbar("Deleted ${uiState.deletedCount} video(s)")
            viewModel.clearSnackbar()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Exact Duplicates") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (uiState.selectedVideoIds.isNotEmpty()) {
                        IconButton(onClick = viewModel::showDeleteConfirmation) {
                            Icon(
                                Icons.Default.Delete,
                                "Delete selected",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (uiState.selectedVideoIds.isNotEmpty()) {
                BottomAppBar {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${uiState.selectedVideoIds.size} selected",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Button(
                            onClick = viewModel::showDeleteConfirmation,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(Icons.Default.Delete, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Delete")
                        }
                    }
                }
            }
        }
    ) { padding ->
        when {
            uiState.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            uiState.groups.isEmpty() -> {
                EmptyState(message = "No exact duplicates found.\nTap 'Scan Now' on the dashboard to start.")
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = uiState.groups,
                        key = { it.groupId }
                    ) { group ->
                        DuplicateGroupCard(
                            group = group,
                            selectedIds = uiState.selectedVideoIds,
                            onVideoSelect = { id, _ -> viewModel.toggleVideoSelection(id) },
                            onVideoClick = { uri -> onVideoClick(uri) },
                            onSelectExceptLargest = { viewModel.selectAllExceptLargest(group) },
                            onSelectExceptNewest = { viewModel.selectAllExceptNewest(group) },
                            onSelectExceptHighestRes = { viewModel.selectAllExceptHighestRes(group) }
                        )
                    }
                }
            }
        }
    }

    if (uiState.showDeleteDialog) {
        val selectedSize = uiState.selectedSize
        ConfirmDeleteDialog(
            fileCount = uiState.selectedVideoIds.size,
            totalSize = formatBytes(selectedSize),
            onConfirm = viewModel::deleteSelected,
            onDismiss = viewModel::dismissDeleteConfirmation
        )
    }
}

@Composable
private fun DuplicateGroupCard(
    group: DuplicateGroup,
    selectedIds: Set<Long>,
    onVideoSelect: (Long, Boolean) -> Unit,
    onVideoClick: (String) -> Unit,
    onSelectExceptLargest: () -> Unit,
    onSelectExceptNewest: () -> Unit,
    onSelectExceptHighestRes: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Group header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "${group.count} identical videos",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Saves ${formatBytes(group.recoverableSize)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand"
                    )
                }
            }

            if (expanded) {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))

                // Smart selection helpers
                Text(
                    text = "Smart Select",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = false,
                        onClick = onSelectExceptLargest,
                        label = { Text("Keep Largest", style = MaterialTheme.typography.labelMedium) }
                    )
                    FilterChip(
                        selected = false,
                        onClick = onSelectExceptNewest,
                        label = { Text("Keep Newest", style = MaterialTheme.typography.labelMedium) }
                    )
                    FilterChip(
                        selected = false,
                        onClick = onSelectExceptHighestRes,
                        label = { Text("Keep HD", style = MaterialTheme.typography.labelMedium) }
                    )
                }
                Spacer(Modifier.height(8.dp))

                // Video list
                group.videos.forEach { video ->
                    VideoCard(
                        video = video,
                        isSelected = video.id in selectedIds,
                        selectionEnabled = true,
                        onSelect = { selected -> onVideoSelect(video.id, selected) },
                        onThumbnailClick = { onVideoClick(video.uri.toString()) },
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyState(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.CheckCircle,
                null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1_073_741_824 -> "%.1f GB".format(bytes / 1_073_741_824.0)
    bytes >= 1_048_576 -> "%.1f MB".format(bytes / 1_048_576.0)
    bytes >= 1_024 -> "%.1f KB".format(bytes / 1_024.0)
    else -> "$bytes B"
}
