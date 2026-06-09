package com.videocleaner.data.repository

import android.app.RecoverableSecurityException
import android.content.Context
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.videocleaner.data.local.dao.DuplicateGroupDao
import com.videocleaner.data.local.dao.VideoDao
import com.videocleaner.data.local.entity.DuplicateGroupEntity
import com.videocleaner.data.local.entity.GroupVideoEntity
import com.videocleaner.data.local.entity.VideoEntity
import com.videocleaner.domain.model.*
import com.videocleaner.util.HashUtils
import com.videocleaner.util.MediaStoreUtils
import com.videocleaner.util.VideoFrameExtractor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository that orchestrates video scanning, hashing, and duplicate detection.
 *
 * This is the single source of truth for all video data, following the
 * repository pattern from Android architecture guidelines.
 */
@Singleton
class VideoRepository
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val videoDao: VideoDao,
        private val duplicateGroupDao: DuplicateGroupDao,
    ) {
        // ──────────────────────── Observables ────────────────────────

        val allVideosFlow: Flow<List<VideoEntity>> get() = videoDao.getAllFlow()
        val videoCountFlow: Flow<Int> get() = videoDao.getCountFlow()
        val totalSizeFlow: Flow<Long?> get() = videoDao.getTotalSizeFlow()
        val exactGroupsFlow: Flow<List<DuplicateGroupEntity>> get() = duplicateGroupDao.getExactDuplicateGroupsFlow()
        val similarGroupsFlow: Flow<List<DuplicateGroupEntity>> get() = duplicateGroupDao.getSimilarGroupsFlow()

        fun getAllVideosPaged(): Flow<PagingData<VideoEntity>> =
            Pager(
                config = PagingConfig(pageSize = 50, enablePlaceholders = false),
                pagingSourceFactory = { videoDao.getAllPaged() },
            ).flow

        fun getExactDuplicatesPaged(): Flow<PagingData<DuplicateGroupEntity>> =
            Pager(
                config = PagingConfig(pageSize = 20, enablePlaceholders = false),
                pagingSourceFactory = { duplicateGroupDao.getExactDuplicateGroupsPaged() },
            ).flow

        fun getSimilarVideosPaged(): Flow<PagingData<DuplicateGroupEntity>> =
            Pager(
                config = PagingConfig(pageSize = 20, enablePlaceholders = false),
                pagingSourceFactory = { duplicateGroupDao.getSimilarGroupsPaged() },
            ).flow

        // ──────────────────────── Scanning ────────────────────────

        /**
         * Main scan entry point. Emits [ScanProgress] updates throughout.
         *
         * Stages:
         * 1. Query MediaStore for all videos
         * 2. Store in Room database
         * 3. Detect exact duplicates (size → partial hash → SHA-256)
         * 4. Detect similar videos (perceptual hash)
         */
        suspend fun scanVideos(
            settings: ScanSettings,
            onProgress: suspend (ScanProgress) -> Unit,
        ) {
            onProgress(ScanProgress.Progress(0, 100, ScanPhase.INDEXING))

            // Stage 1: Index MediaStore
            val videos =
                MediaStoreUtils.queryAllVideos(
                    context = context,
                    excludeFolders = settings.excludeFolders,
                    minSize = settings.minFileSize,
                    onProgress = { current, total ->
                        onProgress(ScanProgress.Progress(current, total, ScanPhase.INDEXING))
                    },
                )

            // Stage 2: Persist to database (clear old data first)
            videoDao.deleteAll()
            duplicateGroupDao.deleteAllGroups()
            duplicateGroupDao.deleteAllGroupVideos()

            // Batch insert for performance
            videos.chunked(100).forEach { chunk ->
                videoDao.insertAll(chunk)
            }

            onProgress(ScanProgress.Progress(0, videos.size, ScanPhase.GROUPING_BY_SIZE))

            // Stage 3: Exact duplicate detection
            val exactGroups = detectExactDuplicates(videos, onProgress)

            // Stage 4: Similar video detection
            val similarGroups = detectSimilarVideos(videos, settings.similarityThreshold, onProgress)

            // Compute stats
            val recoverableBytes =
                exactGroups.sumOf { group ->
                    group.sumOf { it.size } - (group.maxOfOrNull { it.size } ?: 0L)
                } +
                    similarGroups.sumOf { (group, _) ->
                        group.sumOf { it.size } - (group.maxOfOrNull { it.size } ?: 0L)
                    }

            onProgress(
                ScanProgress.Complete(
                    totalVideos = videos.size,
                    exactDuplicates = exactGroups.size,
                    similarVideos = similarGroups.size,
                    recoverableBytes = recoverableBytes,
                ),
            )
        }

        /**
         * Detects exact duplicates using a 3-stage approach:
         * 1. Group by file size
         * 2. Filter candidates with partial hash
         * 3. Confirm with full SHA-256
         */
        private suspend fun detectExactDuplicates(
            videos: List<VideoEntity>,
            onProgress: suspend (ScanProgress) -> Unit,
        ): List<List<VideoEntity>> {
            val exactGroups = mutableListOf<List<VideoEntity>>()

            // Stage 1: Group by size
            val sizeGroups = videos.groupBy { it.size }.filter { (_, group) -> group.size > 1 }
            val total = sizeGroups.size
            var processed = 0

            for ((_, sizeGroup) in sizeGroups) {
                onProgress(ScanProgress.Progress(processed, total, ScanPhase.GROUPING_BY_SIZE))

                // Stage 2: Compute partial hashes
                val partialHashGroups = mutableMapOf<String, MutableList<VideoEntity>>()
                sizeGroup.forEach { video ->
                    val uri = Uri.parse(video.uri)
                    val partialHash = HashUtils.computePartialHash(context, uri) ?: return@forEach
                    partialHashGroups.getOrPut(partialHash) { mutableListOf() }.add(video)
                }

                // Stage 3: Full SHA-256 for partial hash matches
                for ((_, candidates) in partialHashGroups.filter { it.value.size > 1 }) {
                    onProgress(ScanProgress.Progress(processed, total, ScanPhase.COMPUTING_HASHES))

                    val fullHashGroups = mutableMapOf<String, MutableList<VideoEntity>>()
                    candidates.forEach { video ->
                        val uri = Uri.parse(video.uri)
                        val fullHash = HashUtils.computeFullSha256(context, uri) ?: return@forEach
                        fullHashGroups.getOrPut(fullHash) { mutableListOf() }.add(video)
                        videoDao.updateHash(video.id, fullHash, "")
                    }

                    fullHashGroups.values
                        .filter { it.size > 1 }
                        .forEach { group ->
                            exactGroups.add(group)
                            saveExactDuplicateGroup(group)
                        }
                }
                processed++
            }

            return exactGroups
        }

        private suspend fun saveExactDuplicateGroup(videos: List<VideoEntity>) {
            val groupId = UUID.randomUUID().toString()
            duplicateGroupDao.insertGroup(
                DuplicateGroupEntity(groupId = groupId, type = "EXACT"),
            )
            duplicateGroupDao.insertGroupVideos(
                videos.map { GroupVideoEntity(groupId = groupId, videoId = it.id) },
            )
        }

        /**
         * Detects similar videos using perceptual hashing.
         * Extracts frames at 5 key positions and computes pHash for each.
         */
        private suspend fun detectSimilarVideos(
            videos: List<VideoEntity>,
            similarityThreshold: Int,
            onProgress: suspend (ScanProgress) -> Unit,
        ): List<Pair<List<VideoEntity>, Float>> {
            val similarGroups = mutableListOf<Pair<List<VideoEntity>, Float>>()
            val total = videos.size

            // Compute hashes for all videos
            val videoHashes = mutableMapOf<Long, LongArray>()
            videos.forEachIndexed { index, video ->
                onProgress(ScanProgress.Progress(index, total, ScanPhase.ANALYZING_FRAMES))
                val uri = Uri.parse(video.uri)
                val hash = VideoFrameExtractor.computeVideoHash(context, uri, video.duration)
                if (hash != null) videoHashes[video.id] = hash
            }

            // Compare all pairs (n*(n-1)/2 comparisons)
            val processed = mutableSetOf<Long>()
            val videoList = videos.filter { videoHashes.containsKey(it.id) }

            for (i in videoList.indices) {
                if (videoList[i].id in processed) continue
                val group = mutableListOf(videoList[i])
                var totalSimilarity = 0f
                var comparisons = 0

                for (j in i + 1 until videoList.size) {
                    if (videoList[j].id in processed) continue
                    val similarity =
                        VideoFrameExtractor.computeSimilarity(
                            videoHashes[videoList[i].id]!!,
                            videoHashes[videoList[j].id]!!,
                        )
                    if (similarity >= similarityThreshold) {
                        group.add(videoList[j])
                        processed.add(videoList[j].id)
                        totalSimilarity += similarity
                        comparisons++
                    }
                }

                if (group.size > 1) {
                    val avgSimilarity = if (comparisons > 0) totalSimilarity / comparisons else 100f
                    similarGroups.add(Pair(group, avgSimilarity))
                    saveSimilarGroup(group, avgSimilarity)
                    processed.add(videoList[i].id)
                }
            }

            return similarGroups
        }

        private suspend fun saveSimilarGroup(
            videos: List<VideoEntity>,
            similarity: Float,
        ) {
            val groupId = UUID.randomUUID().toString()
            duplicateGroupDao.insertGroup(
                DuplicateGroupEntity(groupId = groupId, type = "SIMILAR", similarityScore = similarity),
            )
            duplicateGroupDao.insertGroupVideos(
                videos.map { GroupVideoEntity(groupId = groupId, videoId = it.id) },
            )
        }

        // ──────────────────────── Get Group Details ────────────────────────

        suspend fun getVideosForGroup(groupId: String): List<VideoEntity> {
            val ids = duplicateGroupDao.getVideoIdsForGroup(groupId)
            return videoDao.getVideosByIds(ids)
        }

        // ──────────────────────── Delete ────────────────────────

        /**
         * Deletes videos from device storage using MediaStore API.
         * Supports scoped storage (Android 10+).
         *
         * @param videoIds IDs of videos to delete
         * @return DeleteResult containing the outcome of the deletion
         */
        suspend fun deleteVideos(videoIds: List<Long>): DeleteResult {
            val contentResolver = context.contentResolver
            val videos = videoIds.mapNotNull { id -> videoDao.getById(id) }
            val uris = videos.map { Uri.parse(it.uri) }

            if (uris.isEmpty()) return DeleteResult.Success(0)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                try {
                    val pendingIntent = MediaStore.createDeleteRequest(contentResolver, uris)
                    return DeleteResult.RequiresPermission(pendingIntent.intentSender, videoIds)
                } catch (e: Exception) {
                    return DeleteResult.Error(e.message ?: "Failed to create delete request")
                }
            } else {
                var deletedCount = 0
                for (video in videos) {
                    val uri = Uri.parse(video.uri)
                    try {
                        contentResolver.delete(uri, null, null)
                        videoDao.deleteById(video.id)
                        deletedCount++
                    } catch (e: SecurityException) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && e is RecoverableSecurityException) {
                            return DeleteResult.RequiresPermission(e.userAction.actionIntent.intentSender, listOf(video.id))
                        }
                        // Skip if other security issues
                    } catch (e: Exception) {
                        // Skip
                    }
                }
                return DeleteResult.Success(deletedCount)
            }
        }

        /**
         * Deletes video records from the local database.
         * Call this after external deletion (e.g. via MediaStore intent result) completes.
         */
        suspend fun deleteVideosFromDb(videoIds: List<Long>) {
            videoIds.forEach { id ->
                videoDao.deleteById(id)
            }
        }

        // ──────────────────────── Dashboard Stats ────────────────────────

        fun getDashboardStatsFlow(): Flow<DashboardStats> {
            return combine(
                videoCountFlow,
                totalSizeFlow,
                duplicateGroupDao.getExactGroupCountFlow(),
                duplicateGroupDao.getSimilarGroupCountFlow(),
                exactGroupsFlow,
            ) { count, totalSize, exactCount, similarCount, exactGroups ->
                DashboardStats(
                    totalVideos = count,
                    totalStorageBytes = totalSize ?: 0L,
                    exactDuplicateGroups = exactCount,
                    // computed separately
                    exactDuplicateVideos = 0,
                    similarVideoGroups = similarCount,
                    // computed from groups
                    recoverableBytes = 0L,
                )
            }
        }
    }

sealed class DeleteResult {
    data class Success(val deletedCount: Int) : DeleteResult()
    data class RequiresPermission(val intentSender: IntentSender, val videoIds: List<Long>) : DeleteResult()
    data class Error(val message: String) : DeleteResult()
}
