package com.videocleaner.domain.model

import kotlinx.serialization.Serializable

/**
 * User-configurable settings for the video scanner.
 */
@Serializable
data class ScanSettings(
    /** Minimum similarity percentage to flag as "similar" (0-100) */
    val similarityThreshold: Int = 90,
    /** Folders to include in scan (empty = all folders) */
    val includeFolders: List<String> = emptyList(),
    /** Folders to exclude from scan */
    val excludeFolders: List<String> = emptyList(),
    /** Scheduled auto-scan frequency */
    val autoScanSchedule: ScanSchedule = ScanSchedule.DISABLED,
    /** Minimum file size to consider (bytes), 0 = no minimum */
    val minFileSize: Long = 0L,
)

enum class ScanSchedule(val displayName: String) {
    DISABLED("Disabled"),
    DAILY("Daily"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly"),
}
