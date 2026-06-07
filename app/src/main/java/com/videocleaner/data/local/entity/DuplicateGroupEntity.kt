package com.videocleaner.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a group of duplicate or similar videos.
 */
@Entity(tableName = "duplicate_groups")
data class DuplicateGroupEntity(
    @PrimaryKey
    val groupId: String,
    /** "EXACT" or "SIMILAR" */
    val type: String,
    val similarityScore: Float = 100f,
    val createdAt: Long = System.currentTimeMillis(),
)
