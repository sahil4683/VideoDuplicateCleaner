package com.videocleaner.presentation.duplicates

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.videocleaner.data.repository.VideoRepository
import com.videocleaner.domain.model.DuplicateGroup
import com.videocleaner.domain.model.DuplicateType
import com.videocleaner.util.toDomain
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Exact Duplicates screen.
 * Loads groups from DB, supports multi-select and deletion.
 */
@HiltViewModel
class ExactDuplicatesViewModel
    @Inject
    constructor(
        private val videoRepository: VideoRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(ExactDuplicatesUiState())
        val uiState: StateFlow<ExactDuplicatesUiState> = _uiState.asStateFlow()

        init {
            loadGroups()
        }

        private fun loadGroups() {
            videoRepository.exactGroupsFlow
                .onEach { groupEntities ->
                    val groups =
                        groupEntities.map { entity ->
                            val videos =
                                videoRepository.getVideosForGroup(entity.groupId)
                                    .map { it.toDomain() }
                            DuplicateGroup(
                                groupId = entity.groupId,
                                type = DuplicateType.EXACT,
                                videos = videos,
                            )
                        }.filter { it.videos.size > 1 }

                    _uiState.update { it.copy(groups = groups, isLoading = false) }
                }
                .catch { e -> _uiState.update { it.copy(errorMessage = e.message, isLoading = false) } }
                .launchIn(viewModelScope)
        }

        fun toggleVideoSelection(videoId: Long) {
            _uiState.update { state ->
                val current = state.selectedVideoIds.toMutableSet()
                if (videoId in current) current.remove(videoId) else current.add(videoId)
                state.copy(selectedVideoIds = current)
            }
        }

        /**
         * Selects all videos in a group except the largest (keep the original).
         */
        fun selectAllExceptLargest(group: DuplicateGroup) {
            val largest = group.videos.maxByOrNull { it.size }
            _uiState.update { state ->
                val newSelected = state.selectedVideoIds.toMutableSet()
                group.videos.forEach { video ->
                    if (video.id != largest?.id) newSelected.add(video.id)
                }
                state.copy(selectedVideoIds = newSelected)
            }
        }

        /**
         * Selects all videos in a group except the newest (keep the most recent).
         */
        fun selectAllExceptNewest(group: DuplicateGroup) {
            val newest = group.videos.maxByOrNull { it.dateModified }
            _uiState.update { state ->
                val newSelected = state.selectedVideoIds.toMutableSet()
                group.videos.forEach { video ->
                    if (video.id != newest?.id) newSelected.add(video.id)
                }
                state.copy(selectedVideoIds = newSelected)
            }
        }

        /**
         * Selects all except highest resolution video.
         */
        fun selectAllExceptHighestRes(group: DuplicateGroup) {
            val highestRes = group.videos.maxByOrNull { it.width * it.height }
            _uiState.update { state ->
                val newSelected = state.selectedVideoIds.toMutableSet()
                group.videos.forEach { video ->
                    if (video.id != highestRes?.id) newSelected.add(video.id)
                }
                state.copy(selectedVideoIds = newSelected)
            }
        }

        fun showDeleteConfirmation() {
            _uiState.update { it.copy(showDeleteDialog = true) }
        }

        fun dismissDeleteConfirmation() {
            _uiState.update { it.copy(showDeleteDialog = false) }
        }

        fun deleteSelected() {
            val ids = _uiState.value.selectedVideoIds.toList()
            if (ids.isEmpty()) return

            viewModelScope.launch {
                _uiState.update { it.copy(isDeleting = true, showDeleteDialog = false) }
                val deleted = videoRepository.deleteVideos(ids)
                _uiState.update { state ->
                    state.copy(
                        isDeleting = false,
                        selectedVideoIds = emptySet(),
                        deletedCount = deleted,
                        showDeleteSuccessSnackbar = true,
                    )
                }
            }
        }

        fun clearSnackbar() {
            _uiState.update { it.copy(showDeleteSuccessSnackbar = false, deletedCount = 0) }
        }
    }

data class ExactDuplicatesUiState(
    val groups: List<DuplicateGroup> = emptyList(),
    val selectedVideoIds: Set<Long> = emptySet(),
    val isLoading: Boolean = true,
    val isDeleting: Boolean = false,
    val showDeleteDialog: Boolean = false,
    val showDeleteSuccessSnackbar: Boolean = false,
    val deletedCount: Int = 0,
    val errorMessage: String? = null,
) {
    val selectedSize: Long
        get() =
            groups.flatMap { it.videos }
                .filter { it.id in selectedVideoIds }
                .sumOf { it.size }
}
