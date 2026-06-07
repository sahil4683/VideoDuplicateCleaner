package com.videocleaner.util

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.videocleaner.data.local.entity.VideoEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Utility for querying MediaStore to discover videos on device storage.
 * Handles both modern (Android 10+) and legacy storage APIs.
 */
object MediaStoreUtils {
    private val VIDEO_COLLECTION: Uri =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

    private val PROJECTION =
        arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.DATE_MODIFIED,
            MediaStore.Video.Media.RELATIVE_PATH,
            MediaStore.Video.Media.MIME_TYPE,
        )

    /**
     * Queries MediaStore for all video files.
     * Streams results as a list to avoid loading everything into memory at once.
     *
     * @param context Application context
     * @param excludeFolders List of folder paths to exclude
     * @param minSize Minimum file size in bytes (0 = no minimum)
     * @param onProgress Callback invoked with (scanned, total) progress
     */
    suspend fun queryAllVideos(
        context: Context,
        excludeFolders: List<String> = emptyList(),
        minSize: Long = 0L,
        onProgress: suspend (current: Int, total: Int) -> Unit = { _, _ -> },
    ): List<VideoEntity> =
        withContext(Dispatchers.IO) {
            val videos = mutableListOf<VideoEntity>()
            val contentResolver: ContentResolver = context.contentResolver

            // First, count total for progress reporting
            val totalCount = countVideos(contentResolver, minSize)

            contentResolver.query(
                VIDEO_COLLECTION,
                PROJECTION,
                if (minSize > 0) "${MediaStore.Video.Media.SIZE} >= ?" else null,
                if (minSize > 0) arrayOf(minSize.toString()) else null,
                "${MediaStore.Video.Media.DATE_MODIFIED} DESC",
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
                val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)
                val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
                val dateModifiedCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)
                val pathCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.RELATIVE_PATH)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)

                var index = 0
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val folder = cursor.getString(pathCol) ?: ""

                    // Skip excluded folders
                    if (excludeFolders.any { folder.contains(it, ignoreCase = true) }) {
                        index++
                        continue
                    }

                    val contentUri = ContentUris.withAppendedId(VIDEO_COLLECTION, id)

                    videos.add(
                        VideoEntity(
                            id = id,
                            uri = contentUri.toString(),
                            fileName = cursor.getString(nameCol) ?: "Unknown",
                            size = cursor.getLong(sizeCol),
                            duration = cursor.getLong(durationCol),
                            width = cursor.getInt(widthCol),
                            height = cursor.getInt(heightCol),
                            dateCreated = cursor.getLong(dateAddedCol),
                            dateModified = cursor.getLong(dateModifiedCol),
                            folder = folder,
                            mimeType = cursor.getString(mimeCol) ?: "video/*",
                        ),
                    )

                    index++
                    if (index % 50 == 0) {
                        onProgress(index, totalCount)
                    }
                }
                onProgress(index, totalCount)
            }

            videos
        }

    private fun countVideos(
        contentResolver: ContentResolver,
        minSize: Long,
    ): Int {
        return contentResolver.query(
            VIDEO_COLLECTION,
            arrayOf(MediaStore.Video.Media._ID),
            if (minSize > 0) "${MediaStore.Video.Media.SIZE} >= ?" else null,
            if (minSize > 0) arrayOf(minSize.toString()) else null,
            null,
        )?.use { it.count } ?: 0
    }
}
