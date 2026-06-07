package com.videocleaner.data.local.dao

import androidx.paging.PagingSource
import androidx.room.*
import com.videocleaner.data.local.entity.DuplicateGroupEntity
import com.videocleaner.data.local.entity.GroupVideoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DuplicateGroupDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: DuplicateGroupEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroupVideos(links: List<GroupVideoEntity>)

    @Delete
    suspend fun deleteGroup(group: DuplicateGroupEntity)

    @Query("DELETE FROM duplicate_groups")
    suspend fun deleteAllGroups()

    @Query("DELETE FROM group_videos")
    suspend fun deleteAllGroupVideos()

    @Query("SELECT * FROM duplicate_groups WHERE type = 'EXACT' ORDER BY createdAt DESC")
    fun getExactDuplicateGroupsPaged(): PagingSource<Int, DuplicateGroupEntity>

    @Query("SELECT * FROM duplicate_groups WHERE type = 'SIMILAR' ORDER BY similarityScore DESC")
    fun getSimilarGroupsPaged(): PagingSource<Int, DuplicateGroupEntity>

    @Query("SELECT * FROM duplicate_groups WHERE type = 'EXACT'")
    fun getExactDuplicateGroupsFlow(): Flow<List<DuplicateGroupEntity>>

    @Query("SELECT * FROM duplicate_groups WHERE type = 'SIMILAR'")
    fun getSimilarGroupsFlow(): Flow<List<DuplicateGroupEntity>>

    @Query("SELECT videoId FROM group_videos WHERE groupId = :groupId")
    suspend fun getVideoIdsForGroup(groupId: String): List<Long>

    @Query("SELECT COUNT(*) FROM duplicate_groups WHERE type = 'EXACT'")
    fun getExactGroupCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM duplicate_groups WHERE type = 'SIMILAR'")
    fun getSimilarGroupCountFlow(): Flow<Int>

    @Query("SELECT groupId FROM group_videos WHERE videoId = :videoId")
    suspend fun getGroupIdForVideo(videoId: Long): String?
}
