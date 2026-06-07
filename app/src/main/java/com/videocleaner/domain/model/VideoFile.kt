package com.videocleaner.domain.model

import android.net.Uri

/**
 * Domain model representing a video file on device storage.
 * Immutable data class to ensure thread safety.
 */
data class VideoFile(
    val id: Long,
    val uri: Uri,
    val fileName: String,
    val size: Long, // bytes
    val duration: Long, // milliseconds
    val width: Int,
    val height: Int,
    val dateCreated: Long, // epoch seconds
    val dateModified: Long, // epoch seconds
    val folder: String,
    val mimeType: String,
    val sha256: String? = null,
    val partialHash: String? = null,
) {
    /** Human-readable file size */
    val formattedSize: String get() = formatBytes(size)

    /** Human-readable duration (mm:ss or hh:mm:ss) */
    val formattedDuration: String get() {
        val totalSeconds = duration / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            "%02d:%02d:%02d".format(hours, minutes, seconds)
        } else {
            "%02d:%02d".format(minutes, seconds)
        }
    }

    /** Resolution string e.g. "1920×1080" */
    val resolution: String get() = "$width×$height"

    private fun formatBytes(bytes: Long): String {
        return when {
            bytes >= 1_073_741_824 -> "%.1f GB".format(bytes / 1_073_741_824.0)
            bytes >= 1_048_576 -> "%.1f MB".format(bytes / 1_048_576.0)
            bytes >= 1_024 -> "%.1f KB".format(bytes / 1_024.0)
            else -> "$bytes B"
        }
    }
}
