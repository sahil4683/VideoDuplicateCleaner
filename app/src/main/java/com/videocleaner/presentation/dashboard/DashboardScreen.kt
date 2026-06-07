package com.videocleaner.presentation.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.videocleaner.domain.model.DashboardStats
import java.text.SimpleDateFormat
import java.util.*

/**
 * Dashboard screen - the home screen of the app.
 * Shows aggregated storage statistics and quick-action buttons.
 */
@Composable
fun DashboardScreen(
    onNavigateToExactDuplicates: () -> Unit,
    onNavigateToSimilarVideos: () -> Unit,
    onNavigateToScan: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAbout: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Video Duplicate Cleaner") },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, "Settings")
                    }
                    IconButton(onClick = onNavigateToAbout) {
                        Icon(Icons.Default.Info, "About")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToScan,
                icon = { Icon(Icons.Default.Search, "Scan") },
                text = { Text("Scan Now") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                StorageOverviewCard(stats = uiState.stats)
                DuplicatesSummaryCard(
                    stats = uiState.stats,
                    onExactClick = onNavigateToExactDuplicates,
                    onSimilarClick = onNavigateToSimilarVideos
                )
                LastScanCard(lastScanTime = uiState.stats.lastScanTime)
                QuickActionsCard(
                    onScanClick = onNavigateToScan,
                    onExactClick = onNavigateToExactDuplicates,
                    onSimilarClick = onNavigateToSimilarVideos
                )
            }
        }
    }
}

@Composable
private fun StorageOverviewCard(stats: DashboardStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Storage Overview",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    label = "Total Videos",
                    value = stats.totalVideos.toString(),
                    icon = Icons.Default.VideoLibrary
                )
                StatItem(
                    label = "Storage Used",
                    value = formatBytes(stats.totalStorageBytes),
                    icon = Icons.Default.Storage
                )
                StatItem(
                    label = "Recoverable",
                    value = formatBytes(stats.recoverableBytes),
                    icon = Icons.Default.CleaningServices
                )
            }
        }
    }
}

@Composable
private fun DuplicatesSummaryCard(
    stats: DashboardStats,
    onExactClick: () -> Unit,
    onSimilarClick: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Duplicates Found",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))

            // Exact duplicates row
            DuplicateRow(
                icon = Icons.Default.FileCopy,
                label = "Exact Duplicates",
                count = stats.exactDuplicateGroups,
                description = "groups with identical content",
                onClick = onExactClick
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Similar videos row
            DuplicateRow(
                icon = Icons.Default.VideoFile,
                label = "Similar Videos",
                count = stats.similarVideoGroups,
                description = "groups of visually similar videos",
                onClick = onSimilarClick
            )
        }
    }
}

@Composable
private fun DuplicateRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    count: Int,
    description: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.titleMedium)
            Text(
                text = "$count $description",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (count > 0) {
            FilledTonalButton(onClick = onClick) {
                Text("View")
            }
        }
    }
}

@Composable
private fun LastScanCard(lastScanTime: Long) {
    val dateText = if (lastScanTime == 0L) {
        "Never scanned"
    } else {
        val sdf = SimpleDateFormat("MMM d, yyyy 'at' HH:mm", Locale.getDefault())
        "Last scan: ${sdf.format(Date(lastScanTime))}"
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.AccessTime, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Text(text = dateText, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun QuickActionsCard(
    onScanClick: () -> Unit,
    onExactClick: () -> Unit,
    onSimilarClick: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Quick Actions",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onScanClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Scan")
                }
                OutlinedButton(
                    onClick = onExactClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.FileCopy, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Exact")
                }
                OutlinedButton(
                    onClick = onSimilarClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.VideoFile, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Similar")
                }
            }
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(28.dp)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1_073_741_824 -> "%.1f GB".format(bytes / 1_073_741_824.0)
    bytes >= 1_048_576 -> "%.1f MB".format(bytes / 1_048_576.0)
    bytes >= 1_024 -> "%.1f KB".format(bytes / 1_024.0)
    else -> "$bytes B"
}
