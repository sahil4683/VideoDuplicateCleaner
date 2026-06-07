package com.videocleaner.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.videocleaner.data.repository.SettingsRepository
import com.videocleaner.data.repository.VideoRepository
import com.videocleaner.domain.model.ScanProgress
import com.videocleaner.presentation.MainActivity
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * WorkManager worker for background video scanning.
 * Uses HiltWorker for dependency injection.
 *
 * This worker runs periodic scans and shows a notification on completion.
 */
@HiltWorker
class VideoScanWorker
    @AssistedInject
    constructor(
        @Assisted context: Context,
        @Assisted workerParams: WorkerParameters,
        private val videoRepository: VideoRepository,
        private val settingsRepository: SettingsRepository,
    ) : CoroutineWorker(context, workerParams) {
        companion object {
            const val WORK_NAME = "video_scan_worker"
            const val CHANNEL_ID = "scan_notifications"
            const val NOTIFICATION_ID = 1001

            /**
             * Schedules a periodic scan based on user settings.
             * Cancels any existing scheduled work before scheduling new.
             */
            fun schedule(
                context: Context,
                intervalHours: Long,
            ) {
                val workRequest =
                    PeriodicWorkRequestBuilder<VideoScanWorker>(
                        intervalHours,
                        TimeUnit.HOURS,
                    )
                        .setConstraints(
                            Constraints.Builder()
                                .setRequiresBatteryNotLow(true)
                                .setRequiresStorageNotLow(false)
                                .build(),
                        )
                        .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
                        .build()

                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    workRequest,
                )
            }

            fun cancel(context: Context) {
                WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            }
        }

        override suspend fun doWork(): Result {
            createNotificationChannel()
            showProgressNotification("Scanning for duplicate videos...")

            return try {
                val settings = settingsRepository.scanSettingsFlow.first()
                var scanComplete: ScanProgress.Complete? = null

                videoRepository.scanVideos(settings) { progress ->
                    when (progress) {
                        is ScanProgress.Progress -> {
                            showProgressNotification(
                                "Scanning... ${progress.percentage.toInt()}%",
                            )
                        }
                        is ScanProgress.Complete -> {
                            scanComplete = progress
                        }
                        else -> {}
                    }
                }

                settingsRepository.updateLastScanTime(System.currentTimeMillis())

                scanComplete?.let { complete ->
                    showCompletionNotification(
                        totalVideos = complete.totalVideos,
                        exactDuplicates = complete.exactDuplicates,
                        similarVideos = complete.similarVideos,
                    )
                }

                Result.success()
            } catch (e: Exception) {
                showErrorNotification(e.message ?: "Scan failed")
                if (runAttemptCount < 3) Result.retry() else Result.failure()
            }
        }

        private fun createNotificationChannel() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel =
                    NotificationChannel(
                        CHANNEL_ID,
                        "Video Scan",
                        NotificationManager.IMPORTANCE_LOW,
                    ).apply {
                        description = "Notifications for background video scanning"
                    }
                val manager = applicationContext.getSystemService(NotificationManager::class.java)
                manager.createNotificationChannel(channel)
            }
        }

        private fun showProgressNotification(message: String) {
            val notification =
                NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                    .setContentTitle("Video Duplicate Cleaner")
                    .setContentText(message)
                    .setSmallIcon(android.R.drawable.ic_popup_sync)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setOngoing(true)
                    .build()

            setForegroundAsync(ForegroundInfo(NOTIFICATION_ID, notification))
        }

        private fun showCompletionNotification(
            totalVideos: Int,
            exactDuplicates: Int,
            similarVideos: Int,
        ) {
            val intent = Intent(applicationContext, MainActivity::class.java)
            val pendingIntent =
                PendingIntent.getActivity(
                    applicationContext,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )

            val manager = applicationContext.getSystemService(NotificationManager::class.java)
            val notification =
                NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                    .setContentTitle("Scan Complete")
                    .setContentText("Found $exactDuplicates exact duplicates and $similarVideos similar videos")
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .build()

            manager.notify(NOTIFICATION_ID + 1, notification)
        }

        private fun showErrorNotification(message: String) {
            val manager = applicationContext.getSystemService(NotificationManager::class.java)
            val notification =
                NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                    .setContentTitle("Scan Failed")
                    .setContentText(message)
                    .setSmallIcon(android.R.drawable.ic_dialog_alert)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true)
                    .build()

            manager.notify(NOTIFICATION_ID + 2, notification)
        }
    }
