package com.videocleaner.presentation.duplicates

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.videocleaner.data.local.entity.DuplicateGroupEntity
import com.videocleaner.data.local.entity.VideoEntity
import com.videocleaner.data.repository.DeleteResult
import com.videocleaner.data.repository.VideoRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ExactDuplicatesViewModelTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var videoRepository: VideoRepository
    private lateinit var viewModel: ExactDuplicatesViewModel

    private val video1 = VideoEntity(
        id = 1L,
        uri = "content://media/1",
        fileName = "video1.mp4",
        size = 1000L,
        duration = 5000L,
        width = 1920,
        height = 1080,
        dateCreated = 100L,
        dateModified = 200L,
        folder = "DCIM/",
        mimeType = "video/mp4"
    )

    private val video2 = VideoEntity(
        id = 2L,
        uri = "content://media/2",
        fileName = "video2.mp4",
        size = 2000L,
        duration = 5000L,
        width = 1280,
        height = 720,
        dateCreated = 100L,
        dateModified = 300L,
        folder = "DCIM/",
        mimeType = "video/mp4"
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        videoRepository = mockk(relaxed = true)

        coEvery { videoRepository.exactGroupsFlow } returns flowOf(
            listOf(DuplicateGroupEntity(groupId = "g1", type = "EXACT"))
        )
        coEvery { videoRepository.getVideosForGroup("g1") } returns listOf(video1, video2)

        viewModel = ExactDuplicatesViewModel(videoRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loads exact duplicates on initialization`() = runTest {
        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.isLoading).isFalse()
            assertThat(state.groups).hasSize(1)
            assertThat(state.groups[0].groupId).isEqualTo("g1")
            assertThat(state.groups[0].videos).hasSize(2)
        }
    }

    @Test
    fun `keepLargestForAll selects all videos except the largest`() = runTest {
        viewModel.keepLargestForAll()
        viewModel.uiState.test {
            val state = awaitItem()
            // video2 has size 2000L (largest), so video1 (size 1000L) should be selected.
            assertThat(state.selectedVideoIds).containsExactly(1L)
        }
    }

    @Test
    fun `keepNewestForAll selects all videos except the newest`() = runTest {
        viewModel.keepNewestForAll()
        viewModel.uiState.test {
            val state = awaitItem()
            // video2 has dateModified 300L (newest), so video1 (dateModified 200L) should be selected.
            assertThat(state.selectedVideoIds).containsExactly(1L)
        }
    }

    @Test
    fun `keepHDForAll selects all videos except the highest resolution`() = runTest {
        viewModel.keepHDForAll()
        viewModel.uiState.test {
            val state = awaitItem()
            // video1 has resolution 1920x1080 (HD), so video2 (1280x720) should be selected.
            assertThat(state.selectedVideoIds).containsExactly(2L)
        }
    }

    @Test
    fun `deleteSelected prompts permission when repository requires it`() = runTest {
        viewModel.toggleVideoSelection(1L)
        val intentSender = mockk<android.content.IntentSender>()
        coEvery { videoRepository.deleteVideos(listOf(1L)) } returns DeleteResult.RequiresPermission(intentSender, listOf(1L))

        viewModel.deleteSelected()

        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.pendingDeleteIntentSender).isEqualTo(intentSender)
            assertThat(state.pendingDeleteVideoIds).containsExactly(1L)
        }
    }
}
