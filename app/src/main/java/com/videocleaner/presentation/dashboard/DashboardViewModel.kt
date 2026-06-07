package com.videocleaner.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.videocleaner.data.repository.SettingsRepository
import com.videocleaner.data.repository.VideoRepository
import com.videocleaner.domain.model.DashboardStats
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

/**
 * ViewModel for the Dashboard screen.
 * Aggregates stats from the repository and exposes them as StateFlow.
 */
@HiltViewModel
class DashboardViewModel
    @Inject
    constructor(
        private val videoRepository: VideoRepository,
        private val settingsRepository: SettingsRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(DashboardUiState())
        val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

        init {
            observeStats()
        }

        private fun observeStats() {
            combine(
                videoRepository.videoCountFlow,
                videoRepository.totalSizeFlow,
                videoRepository.exactGroupsFlow,
                videoRepository.similarGroupsFlow,
                settingsRepository.lastScanTimeFlow,
            ) { count, totalSize, exactGroups, similarGroups, lastScan ->
                val recoverableBytes =
                    exactGroups.sumOf { group ->
                        // This is approximate; full calculation needs video sizes
                        0L
                    }
                DashboardUiState(
                    stats =
                        DashboardStats(
                            totalVideos = count,
                            totalStorageBytes = totalSize ?: 0L,
                            exactDuplicateGroups = exactGroups.size,
                            similarVideoGroups = similarGroups.size,
                            lastScanTime = lastScan,
                        ),
                    isLoading = false,
                )
            }
                .catch { e ->
                    _uiState.update { it.copy(errorMessage = e.message) }
                }
                .onEach { state ->
                    _uiState.update { state }
                }
                .launchIn(viewModelScope)
        }
    }

data class DashboardUiState(
    val stats: DashboardStats = DashboardStats(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)
