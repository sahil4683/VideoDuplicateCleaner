package com.videocleaner.domain.model

/**
 * Aggregated statistics displayed on the dashboard screen.
 */
data class DashboardStats(
    val totalVideos: Int = 0,
    val totalStorageBytes: Long = 0L,
    val exactDuplicateGroups: Int = 0,
    val exactDuplicateVideos: Int = 0,
    val similarVideoGroups: Int = 0,
    val similarVideoCount: Int = 0,
    val recoverableBytes: Long = 0L,
    val lastScanTime: Long = 0L, // epoch millis, 0 = never scanned
)
