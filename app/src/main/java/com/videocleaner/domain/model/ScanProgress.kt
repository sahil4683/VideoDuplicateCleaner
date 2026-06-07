package com.videocleaner.domain.model

/**
 * Represents the current state of a video scan operation.
 */
sealed class ScanProgress {
    object Idle : ScanProgress()

    object Scanning : ScanProgress()

    data class Progress(
        val current: Int,
        val total: Int,
        val phase: ScanPhase,
        val currentFile: String = "",
    ) : ScanProgress() {
        val percentage: Float get() = if (total > 0) (current.toFloat() / total) * 100f else 0f
    }

    data class Complete(
        val totalVideos: Int,
        val exactDuplicates: Int,
        val similarVideos: Int,
        val recoverableBytes: Long,
    ) : ScanProgress()

    data class Error(val message: String, val throwable: Throwable? = null) : ScanProgress()
}

enum class ScanPhase {
    INDEXING, // Scanning MediaStore
    GROUPING_BY_SIZE, // Grouping by file size
    COMPUTING_HASHES, // Computing SHA-256 hashes
    ANALYZING_FRAMES, // Extracting frames for similarity
    FINALIZING, // Storing results
}
