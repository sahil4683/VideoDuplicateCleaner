package com.videocleaner.data

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.videocleaner.data.local.dao.DuplicateGroupDao
import com.videocleaner.data.local.dao.VideoDao
import com.videocleaner.data.local.entity.DuplicateGroupEntity
import com.videocleaner.data.local.entity.VideoEntity
import com.videocleaner.data.repository.VideoRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for VideoRepository.
 * Uses MockK to mock DAOs and Context.
 */
class VideoRepositoryTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var context: Context
    private lateinit var videoDao: VideoDao
    private lateinit var duplicateGroupDao: DuplicateGroupDao
    private lateinit var repository: VideoRepository

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        videoDao = mockk(relaxed = true)
        duplicateGroupDao = mockk(relaxed = true)
        repository = VideoRepository(context, videoDao, duplicateGroupDao)
    }

    @Test
    fun `videoCountFlow emits from dao`() =
        runTest {
            val expected = 42
            coEvery { videoDao.getCountFlow() } returns flowOf(expected)

            repository.videoCountFlow.test {
                assertThat(awaitItem()).isEqualTo(expected)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `exactGroupsFlow emits from dao`() =
        runTest {
            val group = DuplicateGroupEntity(groupId = "g1", type = "EXACT")
            coEvery { duplicateGroupDao.getExactDuplicateGroupsFlow() } returns flowOf(listOf(group))

            repository.exactGroupsFlow.test {
                val result = awaitItem()
                assertThat(result).hasSize(1)
                assertThat(result[0].groupId).isEqualTo("g1")
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `getVideosForGroup returns mapped videos`() =
        runTest {
            val videoIds = listOf(1L, 2L)
            val videos =
                videoIds.map { id ->
                    VideoEntity(
                        id = id,
                        uri = "content://media/$id",
                        fileName = "video_$id.mp4",
                        size = 1000L * id,
                        duration = 5000L,
                        width = 1920, height = 1080,
                        dateCreated = 0L, dateModified = 0L,
                        folder = "DCIM/", mimeType = "video/mp4",
                    )
                }

            coEvery { duplicateGroupDao.getVideoIdsForGroup("group1") } returns videoIds
            coEvery { videoDao.getVideosByIds(videoIds) } returns videos

            val result = repository.getVideosForGroup("group1")
            assertThat(result).hasSize(2)
            assertThat(result.map { it.id }).containsExactly(1L, 2L)
        }
}
