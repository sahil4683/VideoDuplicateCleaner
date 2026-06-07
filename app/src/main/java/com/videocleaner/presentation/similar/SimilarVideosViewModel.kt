package com.videocleaner.presentation.similar

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

@HiltViewModel
class SimilarVideosViewModel @Inject constructor(
    private val videoRepository: VideoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SimilarVideosUiState())
    val uiState: StateFlow<SimilarVideosUiState> = _uiState.asStateFlow()

    init {
        loadSimilarGroups()
    }

    private fun loadSimilarGroups() {
        videoRepository.similarGroupsFlow
            .onEach { groupEntities ->
                val groups = groupEntities.map { entity ->
                    val videos = videoRepository.getVideosForGroup(entity.groupId)
                        .map { it.toDomain() }
                    DuplicateGroup(
                        groupId = entity.groupId,
                        type = DuplicateType.SIMILAR,
                        videos = videos,
                        similarityScore = entity.similarityScore
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

    fun showDeleteConfirmation() {
        _uiState.update { it.copy(showDeleteDialog = true) }
    }

    fun dismissDeleteConfirmation() {
        _uiState.update { it.copy(showDeleteDialog = false) }
    }

    fun deleteSelected() {
        val ids = _uiState.value.selectedVideoIds.toList()
        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true, showDeleteDialog = false) }
            val deleted = videoRepository.deleteVideos(ids)
            _uiState.update { state ->
                state.copy(
                    isDeleting = false,
                    selectedVideoIds = emptySet(),
                    deletedCount = deleted,
                    showDeleteSuccessSnackbar = true
                )
            }
        }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(showDeleteSuccessSnackbar = false, deletedCount = 0) }
    }
}

data class SimilarVideosUiState(
    val groups: List<DuplicateGroup> = emptyList(),
    val selectedVideoIds: Set<Long> = emptySet(),
    val isLoading: Boolean = true,
    val isDeleting: Boolean = false,
    val showDeleteDialog: Boolean = false,
    val showDeleteSuccessSnackbar: Boolean = false,
    val deletedCount: Int = 0,
    val errorMessage: String? = null
)
