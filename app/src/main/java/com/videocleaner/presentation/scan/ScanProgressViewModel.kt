package com.videocleaner.presentation.scan

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.videocleaner.data.repository.SettingsRepository
import com.videocleaner.data.repository.VideoRepository
import com.videocleaner.domain.model.ScanPhase
import com.videocleaner.domain.model.ScanProgress
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ScanProgressViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val videoRepository: VideoRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScanProgressUiState())
    val uiState: StateFlow<ScanProgressUiState> = _uiState.asStateFlow()

    fun startScan() {
        if (_uiState.value.isScanning) return

        viewModelScope.launch {
            _uiState.update { it.copy(isScanning = true, error = null) }
            val settings = settingsRepository.scanSettingsFlow.first()

            try {
                videoRepository.scanVideos(settings) { progress ->
                    when (progress) {
                        is ScanProgress.Progress -> {
                            _uiState.update { state ->
                                state.copy(
                                    current = progress.current,
                                    total = progress.total,
                                    percentage = progress.percentage,
                                    phase = progress.phase,
                                    currentFile = progress.currentFile
                                )
                            }
                        }
                        is ScanProgress.Complete -> {
                            settingsRepository.updateLastScanTime(System.currentTimeMillis())
                            _uiState.update { state ->
                                state.copy(
                                    isScanning = false,
                                    isComplete = true,
                                    completedStats = progress
                                )
                            }
                        }
                        is ScanProgress.Error -> {
                            _uiState.update { it.copy(isScanning = false, error = progress.message) }
                        }
                        else -> {}
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isScanning = false, error = e.message ?: "Unknown error") }
            }
        }
    }

    fun cancelScan() {
        // Cancellation handled by ViewModel scope cancellation
        viewModelScope.coroutineContext[kotlinx.coroutines.Job]?.cancel()
        _uiState.update { it.copy(isScanning = false) }
    }

    fun hasStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.READ_MEDIA_VIDEO
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
}

data class ScanProgressUiState(
    val isScanning: Boolean = false,
    val isComplete: Boolean = false,
    val current: Int = 0,
    val total: Int = 0,
    val percentage: Float = 0f,
    val phase: ScanPhase = ScanPhase.INDEXING,
    val currentFile: String = "",
    val error: String? = null,
    val completedStats: ScanProgress.Complete? = null
)
