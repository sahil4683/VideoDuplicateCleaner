package com.videocleaner.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing a video file stored in the local database.
 * Indexed on size and sha256 for fast duplicate grouping queries.
 */
@Entity(
    tableName = "videos",
    indices = [
        Index(value = ["size"]),
        Index(value = ["sha256"]),
        Index(value = ["partialHash"]),
    ],
)
data class VideoEntity(
    @PrimaryKey
    val id: Long,
    val uri: String,
    val fileName: String,
    val size: Long,
    val duration: Long,
    val width: Int,
    val height: Int,
    val dateCreated: Long,
    val dateModified: Long,
    val folder: String,
    val mimeType: String,
    val sha256: String? = null,
    val partialHash: String? = null,
    val isHashed: Boolean = false,
    val scanTimestamp: Long = System.currentTimeMillis(),
)
