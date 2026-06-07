package com.videocleaner.presentation

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.videocleaner.presentation.dashboard.DashboardScreen
import com.videocleaner.presentation.duplicates.ExactDuplicatesScreen
import com.videocleaner.presentation.onboarding.OnboardingScreen
import com.videocleaner.presentation.player.VideoPlayerScreen
import com.videocleaner.presentation.scan.ScanProgressScreen
import com.videocleaner.presentation.settings.SettingsScreen
import com.videocleaner.presentation.similar.SimilarVideosScreen
import com.videocleaner.presentation.about.AboutScreen

/**
 * Navigation destinations for the app.
 * Uses type-safe route strings for clarity.
 */
object Routes {
    const val ONBOARDING = "onboarding"
    const val DASHBOARD = "dashboard"
    const val EXACT_DUPLICATES = "exact_duplicates"
    const val SIMILAR_VIDEOS = "similar_videos"
    const val SCAN_PROGRESS = "scan_progress"
    const val SETTINGS = "settings"
    const val ABOUT = "about"
    const val VIDEO_PLAYER = "video_player/{videoUri}"

    fun videoPlayer(videoUri: String): String =
        "video_player/${java.net.URLEncoder.encode(videoUri, "UTF-8")}"
}

@Composable
fun AppNavigation(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Routes.ONBOARDING,
        modifier = modifier
    ) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onComplete = {
                    navController.navigate(Routes.DASHBOARD) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.DASHBOARD) {
            DashboardScreen(
                onNavigateToExactDuplicates = { navController.navigate(Routes.EXACT_DUPLICATES) },
                onNavigateToSimilarVideos = { navController.navigate(Routes.SIMILAR_VIDEOS) },
                onNavigateToScan = { navController.navigate(Routes.SCAN_PROGRESS) },
                onNavigateToSettings = { navController.navigate(Routes.SETTINGS) },
                onNavigateToAbout = { navController.navigate(Routes.ABOUT) }
            )
        }

        composable(Routes.EXACT_DUPLICATES) {
            ExactDuplicatesScreen(
                onVideoClick = { uri ->
                    navController.navigate(Routes.videoPlayer(uri))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.SIMILAR_VIDEOS) {
            SimilarVideosScreen(
                onVideoClick = { uri ->
                    navController.navigate(Routes.videoPlayer(uri))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.SCAN_PROGRESS) {
            ScanProgressScreen(
                onComplete = {
                    navController.navigate(Routes.DASHBOARD) {
                        popUpTo(Routes.DASHBOARD) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.ABOUT) {
            AboutScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.VIDEO_PLAYER) { backStackEntry ->
            val encodedUri = backStackEntry.arguments?.getString("videoUri") ?: ""
            val decodedUri = java.net.URLDecoder.decode(encodedUri, "UTF-8")
            VideoPlayerScreen(
                videoUri = decodedUri,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
