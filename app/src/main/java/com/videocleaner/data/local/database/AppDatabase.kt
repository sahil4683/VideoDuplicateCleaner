package com.videocleaner.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.videocleaner.data.local.dao.DuplicateGroupDao
import com.videocleaner.data.local.dao.VideoDao
import com.videocleaner.data.local.entity.DuplicateGroupEntity
import com.videocleaner.data.local.entity.GroupVideoEntity
import com.videocleaner.data.local.entity.VideoEntity

/**
 * Main Room database for Video Duplicate Cleaner.
 *
 * Version history:
 * 1 - Initial schema with videos, duplicate_groups, group_videos tables
 */
@Database(
    entities = [
        VideoEntity::class,
        DuplicateGroupEntity::class,
        GroupVideoEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun videoDao(): VideoDao
    abstract fun duplicateGroupDao(): DuplicateGroupDao

    companion object {
        const val DATABASE_NAME = "video_cleaner_db"
    }
}
