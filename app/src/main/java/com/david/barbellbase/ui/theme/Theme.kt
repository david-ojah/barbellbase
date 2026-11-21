package com.david.barbellbase.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.david.barbellbase.data.prefs.ThemeMode

private val Lime = Color(0xFFB6F09C)
private val LimeDim = Color(0xFF7FBF6A)
private val Ink = Color(0xFF0E0F0D)
private val Paper = Color(0xFFF7F8F4)

private val DarkScheme = darkColorScheme(
    primary = Lime,
    onPrimary = Color(0xFF0B1A05),
    primaryContainer = Color(0xFF2A4A1E),
    onPrimaryContainer = Color(0xFFD6FFC2),
    secondary = Color(0xFFC7C9BF),
    onSecondary = Color(0xFF1A1C17),
    secondaryContainer = Color(0xFF2A2D26),
    onSecondaryContainer = Color(0xFFE3E5DA),
    tertiary = Color(0xFFFFD27A),
    onTertiary = Color(0xFF241A00),
    background = Ink,
    onBackground = Color(0xFFE6E8E0),
    surface = Color(0xFF141613),
    onSurface = Color(0xFFE6E8E0),
    surfaceVariant = Color(0xFF23261F),
    onSurfaceVariant = Color(0xFFB9BDB0),
    surfaceContainer = Color(0xFF1A1D18),
    surfaceContainerHigh = Color(0xFF21241E),
    surfaceContainerHighest = Color(0xFF282C25),
    surfaceContainerLow = Color(0xFF171A15),
    surfaceContainerLowest = Color(0xFF0B0C0A),
    outline = Color(0xFF6C7264),
    outlineVariant = Color(0xFF34382F),
    error = Color(0xFFFFB4A8),
    onError = Color(0xFF5C120A),
)

private val LightScheme = lightColorScheme(
    primary = Color(0xFF3F6B2A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC0F2A6),
    onPrimaryContainer = Color(0xFF0B2000),
    secondary = Color(0xFF55624C),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD9E7CE),
    onSecondaryContainer = Color(0xFF131F0D),
    tertiary = Color(0xFF7A5900),
    onTertiary = Color.White,
    background = Paper,
    onBackground = Color(0xFF1A1C17),
    surface = Color(0xFFFDFDF8),
    onSurface = Color(0xFF1A1C17),
    surfaceVariant = Color(0xFFE1E4D8),
    onSurfaceVariant = Color(0xFF454940),
    surfaceContainer = Color(0xFFF0F2EA),
    surfaceContainerHigh = Color(0xFFE9ECE2),
    surfaceContainerHighest = Color(0xFFE3E6DC),
    surfaceContainerLow = Color(0xFFF6F8F1),
    surfaceContainerLowest = Color.White,
    outline = Color(0xFF757A6C),
    outlineVariant = Color(0xFFC5C9BB),
    error = Color(0xFFB3261E),
    onError = Color.White,
)

private val MonoDark = darkColorScheme(
    primary = Color(0xFFF2F2F2),
    onPrimary = Color(0xFF0A0A0A),
    primaryContainer = Color(0xFF2A2A2A),
    onPrimaryContainer = Color(0xFFF7F7F7),
    secondary = Color(0xFFBDBDBD),
    onSecondary = Color(0xFF111111),
    secondaryContainer = Color(0xFF262626),
    onSecondaryContainer = Color(0xFFEDEDED),
    background = Color(0xFF000000),
    onBackground = Color(0xFFF0F0F0),
    surface = Color(0xFF0B0B0B),
    onSurface = Color(0xFFF0F0F0),
    surfaceVariant = Color(0xFF1E1E1E),
    onSurfaceVariant = Color(0xFFB5B5B5),
    surfaceContainer = Color(0xFF121212),
    surfaceContainerHigh = Color(0xFF191919),
    surfaceContainerHighest = Color(0xFF202020),
    surfaceContainerLow = Color(0xFF0D0D0D),
    surfaceContainerLowest = Color(0xFF000000),
    outline = Color(0xFF6E6E6E),
    outlineVariant = Color(0xFF2E2E2E),
    error = Color(0xFFFF8A80),
    onError = Color(0xFF3A0905),
)

private val MonoLight = lightColorScheme(
    primary = Color(0xFF111111),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE2E2E2),
    onPrimaryContainer = Color(0xFF0C0C0C),
    secondary = Color(0xFF4A4A4A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8E8E8),
    onSecondaryContainer = Color(0xFF141414),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF111111),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF111111),
    surfaceVariant = Color(0xFFEDEDED),
    onSurfaceVariant = Color(0xFF4C4C4C),
    surfaceContainer = Color(0xFFF2F2F2),
    surfaceContainerHigh = Color(0xFFECECEC),
    surfaceContainerHighest = Color(0xFFE6E6E6),
    surfaceContainerLow = Color(0xFFF7F7F7),
    surfaceContainerLowest = Color.White,
    outline = Color(0xFF7A7A7A),
    outlineVariant = Color(0xFFD5D5D5),
    error = Color(0xFFB3261E),
    onError = Color.White,
)

fun isDarkTheme(mode: ThemeMode, systemDark: Boolean): Boolean = when (mode) {
    ThemeMode.DARK -> true
    ThemeMode.LIGHT -> false
    else -> systemDark
}

@Composable
fun BarbellBaseTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val dark = isDarkTheme(themeMode, systemDark)
    val context = LocalContext.current

    val scheme = when (themeMode) {
        ThemeMode.DYNAMIC ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } else {
                if (dark) DarkScheme else LightScheme
            }

        ThemeMode.MONO -> if (dark) MonoDark else MonoLight
        ThemeMode.DARK -> DarkScheme
        ThemeMode.LIGHT -> LightScheme
    }

    MaterialTheme(colorScheme = scheme, content = content)
}

/** Accent used for charts and rings so they stay readable in every scheme. */
val ChartAccent: Color
    @Composable get() = MaterialTheme.colorScheme.primary

val ChartAccentSoft: Color
    @Composable get() = if (isSystemInDarkTheme()) LimeDim else LimeDim
