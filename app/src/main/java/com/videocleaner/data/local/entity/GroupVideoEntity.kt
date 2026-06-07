package com.videocleaner.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Junction table linking videos to duplicate groups.
 * Uses foreign keys to maintain referential integrity.
 */
@Entity(
    tableName = "group_videos",
    primaryKeys = ["groupId", "videoId"],
    foreignKeys = [
        ForeignKey(
            entity = DuplicateGroupEntity::class,
            parentColumns = ["groupId"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = VideoEntity::class,
            parentColumns = ["id"],
            childColumns = ["videoId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["groupId"]),
        Index(value = ["videoId"])
    ]
)
data class GroupVideoEntity(
    val groupId: String,
    val videoId: Long
)
