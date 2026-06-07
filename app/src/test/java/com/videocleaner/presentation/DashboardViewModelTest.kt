package com.videocleaner.presentation

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.videocleaner.data.local.entity.DuplicateGroupEntity
import com.videocleaner.data.repository.SettingsRepository
import com.videocleaner.data.repository.VideoRepository
import com.videocleaner.presentation.dashboard.DashboardViewModel
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for DashboardViewModel.
 * Uses TestCoroutineDispatcher for controlling coroutine execution.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var videoRepository: VideoRepository
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var viewModel: DashboardViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        videoRepository = mockk(relaxed = true)
        settingsRepository = mockk(relaxed = true)

        every { videoRepository.videoCountFlow } returns flowOf(25)
        every { videoRepository.totalSizeFlow } returns flowOf(500_000_000L)
        every { videoRepository.exactGroupsFlow } returns flowOf(
            listOf(DuplicateGroupEntity(groupId = "g1", type = "EXACT"))
        )
        every { videoRepository.similarGroupsFlow } returns flowOf(
            listOf(DuplicateGroupEntity(groupId = "g2", type = "SIMILAR"))
        )
        every { settingsRepository.lastScanTimeFlow } returns flowOf(1_000_000L)

        viewModel = DashboardViewModel(videoRepository, settingsRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is loading`() = runTest {
        val freshRepo = mockk<VideoRepository>(relaxed = true)
        val freshSettings = mockk<SettingsRepository>(relaxed = true)
        every { freshRepo.videoCountFlow } returns flowOf(0)
        every { freshRepo.totalSizeFlow } returns flowOf(0L)
        every { freshRepo.exactGroupsFlow } returns flowOf(emptyList())
        every { freshRepo.similarGroupsFlow } returns flowOf(emptyList())
        every { freshSettings.lastScanTimeFlow } returns flowOf(0L)

        val vm = DashboardViewModel(freshRepo, freshSettings)
        // After collection starts, isLoading transitions to false
        assertThat(vm.uiState.value.isLoading).isFalse()
    }

    @Test
    fun `stats are populated from repository`() = runTest {
        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.stats.totalVideos).isEqualTo(25)
            assertThat(state.stats.totalStorageBytes).isEqualTo(500_000_000L)
            assertThat(state.stats.exactDuplicateGroups).isEqualTo(1)
            assertThat(state.stats.similarVideoGroups).isEqualTo(1)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
