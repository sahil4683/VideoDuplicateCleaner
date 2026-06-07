package com.videocleaner.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.videocleaner.domain.model.ScanSchedule
import com.videocleaner.domain.model.ScanSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * Repository for user preferences using Jetpack DataStore.
 * Provides reactive Flow-based access to settings.
 */
@Singleton
class SettingsRepository
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        private object Keys {
            val SIMILARITY_THRESHOLD = intPreferencesKey("similarity_threshold")
            val INCLUDE_FOLDERS = stringPreferencesKey("include_folders")
            val EXCLUDE_FOLDERS = stringPreferencesKey("exclude_folders")
            val AUTO_SCAN_SCHEDULE = stringPreferencesKey("auto_scan_schedule")
            val MIN_FILE_SIZE = longPreferencesKey("min_file_size")
            val HAS_SEEN_ONBOARDING = booleanPreferencesKey("has_seen_onboarding")
            val LAST_SCAN_TIME = longPreferencesKey("last_scan_time")
        }

        val scanSettingsFlow: Flow<ScanSettings> =
            context.dataStore.data
                .catch { emit(emptyPreferences()) }
                .map { prefs ->
                    ScanSettings(
                        similarityThreshold = prefs[Keys.SIMILARITY_THRESHOLD] ?: 90,
                        includeFolders =
                            prefs[Keys.INCLUDE_FOLDERS]
                                ?.let { Json.decodeFromString<List<String>>(it) } ?: emptyList(),
                        excludeFolders =
                            prefs[Keys.EXCLUDE_FOLDERS]
                                ?.let { Json.decodeFromString<List<String>>(it) } ?: emptyList(),
                        autoScanSchedule =
                            prefs[Keys.AUTO_SCAN_SCHEDULE]
                                ?.let { runCatching { ScanSchedule.valueOf(it) }.getOrDefault(ScanSchedule.DISABLED) }
                                ?: ScanSchedule.DISABLED,
                        minFileSize = prefs[Keys.MIN_FILE_SIZE] ?: 0L,
                    )
                }

        val hasSeenOnboardingFlow: Flow<Boolean> =
            context.dataStore.data
                .catch { emit(emptyPreferences()) }
                .map { prefs -> prefs[Keys.HAS_SEEN_ONBOARDING] ?: false }

        val lastScanTimeFlow: Flow<Long> =
            context.dataStore.data
                .catch { emit(emptyPreferences()) }
                .map { prefs -> prefs[Keys.LAST_SCAN_TIME] ?: 0L }

        suspend fun updateSimilarityThreshold(threshold: Int) {
            context.dataStore.edit { it[Keys.SIMILARITY_THRESHOLD] = threshold }
        }

        suspend fun updateExcludeFolders(folders: List<String>) {
            context.dataStore.edit { it[Keys.EXCLUDE_FOLDERS] = Json.encodeToString(folders) }
        }

        suspend fun updateAutoScanSchedule(schedule: ScanSchedule) {
            context.dataStore.edit { it[Keys.AUTO_SCAN_SCHEDULE] = schedule.name }
        }

        suspend fun markOnboardingComplete() {
            context.dataStore.edit { it[Keys.HAS_SEEN_ONBOARDING] = true }
        }

        suspend fun updateLastScanTime(timestamp: Long) {
            context.dataStore.edit { it[Keys.LAST_SCAN_TIME] = timestamp }
        }
    }
