package com.videocleaner.presentation.similar

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
import com.videocleaner.presentation.components.ConfirmDeleteDialog
import com.videocleaner.presentation.components.VideoCard

/**
 * Screen showing groups of visually similar (but not identical) videos.
 * Each group shows a similarity percentage badge.
 */
@Composable
fun SimilarVideosScreen(
    onVideoClick: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: SimilarVideosViewModel = hiltViewModel()
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
                title = { Text("Similar Videos") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (uiState.selectedVideoIds.isNotEmpty()) {
                        IconButton(onClick = viewModel::showDeleteConfirmation) {
                            Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        when {
            uiState.isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            uiState.groups.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(72.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(16.dp))
                        Text("No similar videos found.", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.groups, key = { it.groupId }) { group ->
                        SimilarGroupCard(
                            group = group,
                            selectedIds = uiState.selectedVideoIds,
                            onVideoSelect = { id, _ -> viewModel.toggleVideoSelection(id) },
                            onVideoClick = { uri -> onVideoClick(uri) }
                        )
                    }
                }
            }
        }
    }

    if (uiState.showDeleteDialog) {
        ConfirmDeleteDialog(
            fileCount = uiState.selectedVideoIds.size,
            totalSize = "selected files",
            onConfirm = viewModel::deleteSelected,
            onDismiss = viewModel::dismissDeleteConfirmation
        )
    }
}

@Composable
private fun SimilarGroupCard(
    group: DuplicateGroup,
    selectedIds: Set<Long>,
    onVideoSelect: (Long, Boolean) -> Unit,
    onVideoClick: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "${group.count} similar videos",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SimilarityBadge(percent = group.similarityScore)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "similarity",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        null
                    )
                }
            }

            if (expanded) {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
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
private fun SimilarityBadge(percent: Float) {
    val color = when {
        percent >= 95 -> MaterialTheme.colorScheme.error
        percent >= 85 -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }
    Surface(
        shape = MaterialTheme.shapes.small,
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text = "%.0f%%".format(percent),
            style = MaterialTheme.typography.labelLarge,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
