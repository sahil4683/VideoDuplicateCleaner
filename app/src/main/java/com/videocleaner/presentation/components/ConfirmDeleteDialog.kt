package com.videocleaner.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable

/**
 * Confirmation dialog displayed before deleting videos.
 * Shows the number of files and recoverable storage amount.
 */
@Composable
fun ConfirmDeleteDialog(
    fileCount: Int,
    totalSize: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = { Text("Delete $fileCount Video${if (fileCount > 1) "s" else ""}?") },
        text = {
            Text(
                "This will permanently delete $fileCount video file${if (fileCount > 1) "s" else ""} " +
                "and free up $totalSize of storage.\n\nThis action cannot be undone."
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
