package com.videocleaner.domain.model

/**
 * Represents a group of duplicate or similar videos.
 *
 * @param groupId Unique identifier for this duplicate group
 * @param type    Whether these are exact duplicates or similar videos
 * @param videos  The list of videos in this group
 * @param similarityScore For similar video groups, the percentage similarity (0-100)
 */
data class DuplicateGroup(
    val groupId: String,
    val type: DuplicateType,
    val videos: List<VideoFile>,
    val similarityScore: Float = 100f,
) {
    /** Total size of all videos in this group */
    val totalSize: Long get() = videos.sumOf { it.size }

    /** Recoverable storage = total size minus the largest file */
    val recoverableSize: Long get() = totalSize - (videos.maxOfOrNull { it.size } ?: 0L)

    /** Number of videos in this group */
    val count: Int get() = videos.size
}

enum class DuplicateType {
    EXACT, // SHA-256 hash match
    SIMILAR, // Perceptual hash similarity > threshold
}
