package com.videocleaner.presentation.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// ────────────── Color Tokens ──────────────

private val PrimaryBlue = Color(0xFF1565C0)
private val PrimaryBlueLight = Color(0xFF42A5F5)
private val SecondaryTeal = Color(0xFF00897B)
private val ErrorRed = Color(0xFFD32F2F)
private val WarningOrange = Color(0xFFF57C00)
private val SuccessGreen = Color(0xFF388E3C)

private val DarkColorScheme =
    darkColorScheme(
        primary = PrimaryBlueLight,
        onPrimary = Color(0xFF003087),
        primaryContainer = Color(0xFF00428A),
        onPrimaryContainer = Color(0xFFD5E3FF),
        secondary = Color(0xFF80CBC4),
        onSecondary = Color(0xFF00312D),
        secondaryContainer = Color(0xFF004F47),
        onSecondaryContainer = Color(0xFF9CF0E8),
        tertiary = Color(0xFFB0C4FF),
        error = Color(0xFFFFB4AB),
        errorContainer = Color(0xFF93000A),
        background = Color(0xFF1A1C1E),
        onBackground = Color(0xFFE2E2E6),
        surface = Color(0xFF1A1C1E),
        onSurface = Color(0xFFE2E2E6),
        surfaceVariant = Color(0xFF42474E),
        onSurfaceVariant = Color(0xFFC2C7CF),
    )

private val LightColorScheme =
    lightColorScheme(
        primary = PrimaryBlue,
        onPrimary = Color.White,
        primaryContainer = Color(0xFFD5E3FF),
        onPrimaryContainer = Color(0xFF001947),
        secondary = SecondaryTeal,
        onSecondary = Color.White,
        secondaryContainer = Color(0xFF9CF0E8),
        onSecondaryContainer = Color(0xFF00201D),
        tertiary = Color(0xFF355CA8),
        error = ErrorRed,
        errorContainer = Color(0xFFFFDAD6),
        background = Color(0xFFFAFAFF),
        onBackground = Color(0xFF1A1C1E),
        surface = Color(0xFFFAFAFF),
        onSurface = Color(0xFF1A1C1E),
        surfaceVariant = Color(0xFFE0E2EC),
        onSurfaceVariant = Color(0xFF42474E),
    )

@Composable
fun VideoDuplicateCleanerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Material You on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme =
        when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }
            darkTheme -> DarkColorScheme
            else -> LightColorScheme
        }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content,
    )
}
