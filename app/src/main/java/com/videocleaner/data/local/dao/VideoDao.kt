package com.videocleaner.data.local.dao

import androidx.paging.PagingSource
import androidx.room.*
import com.videocleaner.data.local.entity.VideoEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for video entities.
 * All queries are suspend functions or return Flow for reactive updates.
 */
@Dao
interface VideoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(videos: List<VideoEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(video: VideoEntity)

    @Update
    suspend fun update(video: VideoEntity)

    @Delete
    suspend fun delete(video: VideoEntity)

    @Query("DELETE FROM videos WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM videos")
    suspend fun deleteAll()

    @Query("SELECT * FROM videos WHERE id = :id")
    suspend fun getById(id: Long): VideoEntity?

    @Query("SELECT * FROM videos ORDER BY size DESC")
    fun getAllPaged(): PagingSource<Int, VideoEntity>

    @Query("SELECT * FROM videos ORDER BY size DESC")
    fun getAllFlow(): Flow<List<VideoEntity>>

    @Query("SELECT COUNT(*) FROM videos")
    fun getCountFlow(): Flow<Int>

    @Query("SELECT SUM(size) FROM videos")
    fun getTotalSizeFlow(): Flow<Long?>

    /**
     * Groups videos by size and returns only sizes that have more than one video.
     * This is Stage 1 of exact duplicate detection.
     */
    @Query("""
        SELECT size FROM videos 
        GROUP BY size 
        HAVING COUNT(*) > 1
    """)
    suspend fun getSizesWithDuplicates(): List<Long>

    /**
     * Gets all videos matching a specific file size.
     * Used in Stage 2 of exact duplicate detection.
     */
    @Query("SELECT * FROM videos WHERE size = :size")
    suspend fun getVideosBySize(size: Long): List<VideoEntity>

    /**
     * Gets all un-hashed videos for computing SHA-256.
     */
    @Query("SELECT * FROM videos WHERE isHashed = 0")
    suspend fun getUnhashedVideos(): List<VideoEntity>

    @Query("SELECT * FROM videos WHERE sha256 = :hash")
    suspend fun getVideosByHash(hash: String): List<VideoEntity>

    /**
     * Updates only the hash fields to avoid re-fetching entire entity.
     */
    @Query("""
        UPDATE videos 
        SET sha256 = :sha256, partialHash = :partialHash, isHashed = 1 
        WHERE id = :id
    """)
    suspend fun updateHash(id: Long, sha256: String, partialHash: String)

    @Query("SELECT * FROM videos WHERE id IN (:ids)")
    suspend fun getVideosByIds(ids: List<Long>): List<VideoEntity>
}
