package com.videocleaner.presentation.settings

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
import com.videocleaner.domain.model.ScanSchedule

/**
 * Settings screen for configuring scan behavior, thresholds, and scheduling.
 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
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
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Similarity threshold section
            SectionCard(title = "Duplicate Detection") {
                Column {
                    Text(
                        text = "Similarity Threshold: ${settings.similarityThreshold}%",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = "Videos above this threshold are flagged as similar",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    Slider(
                        value = settings.similarityThreshold.toFloat(),
                        onValueChange = { viewModel.updateSimilarityThreshold(it.toInt()) },
                        valueRange = 70f..99f,
                        steps = 28,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("70% (loose)", style = MaterialTheme.typography.labelSmall)
                        Text("99% (strict)", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            // Auto-scan schedule section
            SectionCard(title = "Auto Scan Schedule") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ScanSchedule.entries.forEach { schedule ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = settings.autoScanSchedule == schedule,
                                onClick = { viewModel.updateAutoScanSchedule(schedule) },
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = schedule.displayName,
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                                if (schedule != ScanSchedule.DISABLED) {
                                    Text(
                                        text =
                                            when (schedule) {
                                                ScanSchedule.DAILY -> "Runs once per day when battery is not low"
                                                ScanSchedule.WEEKLY -> "Runs once per week"
                                                ScanSchedule.MONTHLY -> "Runs once per month"
                                                else -> ""
                                            },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Excluded folders section
            SectionCard(title = "Excluded Folders") {
                Column {
                    if (settings.excludeFolders.isEmpty()) {
                        Text(
                            text = "No folders excluded. All video folders will be scanned.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        settings.excludeFolders.forEach { folder ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = folder,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f),
                                )
                                IconButton(
                                    onClick = {
                                        val updated = settings.excludeFolders.filter { it != folder }
                                        viewModel.updateExcludeFolders(updated)
                                    },
                                ) {
                                    Icon(Icons.Default.Close, "Remove folder")
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { /* TODO: folder picker */ },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.Add, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Add Excluded Folder")
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}
