package com.videocleaner.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.videocleaner.data.repository.SettingsRepository
import com.videocleaner.domain.model.ScanSchedule
import com.videocleaner.domain.model.ScanSettings
import com.videocleaner.worker.VideoScanWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val settings: StateFlow<ScanSettings> = settingsRepository.scanSettingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ScanSettings())

    fun updateSimilarityThreshold(threshold: Int) {
        viewModelScope.launch {
            settingsRepository.updateSimilarityThreshold(threshold)
        }
    }

    fun updateExcludeFolders(folders: List<String>) {
        viewModelScope.launch {
            settingsRepository.updateExcludeFolders(folders)
        }
    }

    fun updateAutoScanSchedule(schedule: ScanSchedule) {
        viewModelScope.launch {
            settingsRepository.updateAutoScanSchedule(schedule)
            when (schedule) {
                ScanSchedule.DISABLED -> VideoScanWorker.cancel(context)
                ScanSchedule.DAILY -> VideoScanWorker.schedule(context, 24)
                ScanSchedule.WEEKLY -> VideoScanWorker.schedule(context, 24 * 7)
                ScanSchedule.MONTHLY -> VideoScanWorker.schedule(context, 24 * 30)
            }
        }
    }
}
