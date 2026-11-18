package com.dssrkh.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** 对照官方 APK 资源：浅色主色 #426EFE，夜间 #507BF2。 */
val DeepBlue = Color(0xFF426EFE)

private val LightColors = lightColorScheme(
    primary = DeepBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8EDFF),
    onPrimaryContainer = Color(0xFF0A1B66),
    secondaryContainer = Color(0xFFE8EDFF),
    onSecondaryContainer = Color(0xFF0A1B66),
    tertiary = DeepBlue,
    tertiaryContainer = Color(0xFFE8EDFF),
    onTertiaryContainer = Color(0xFF0A1B66),
    background = Color.White,
    onBackground = Color(0xFF1A1A1A),
    surface = Color.White,
    onSurface = Color(0xFF1A1A1A),
    surfaceVariant = Color(0xFFF3F4F6),
    onSurfaceVariant = Color(0xFF6B6F76),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFAFBFC),
    surfaceContainer = Color(0xFFF5F6F8),
    surfaceContainerHigh = Color(0xFFF1F2F4),
    surfaceContainerHighest = Color(0xFFECEDEF),
    outline = Color(0xFFE3E5E8),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF507BF2),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF22346E),
    onPrimaryContainer = Color(0xFFDDE4FF),
    secondaryContainer = Color(0xFF22346E),
    onSecondaryContainer = Color(0xFFDDE4FF),
    tertiary = Color(0xFF507BF2),
    tertiaryContainer = Color(0xFF22346E),
    onTertiaryContainer = Color(0xFFDDE4FF),
    background = Color(0xFF111111),
    onBackground = Color(0xFFEDEDED),
    surface = Color(0xFF111111),
    onSurface = Color(0xFFEDEDED),
    surfaceVariant = Color(0xFF1F1F1F),
    onSurfaceVariant = Color(0xFFA8ACB3),
    surfaceContainerLowest = Color(0xFF0C0C0D),
    surfaceContainerLow = Color(0xFF161617),
    surfaceContainer = Color(0xFF1C1C1E),
    surfaceContainerHigh = Color(0xFF242426),
    surfaceContainerHighest = Color(0xFF2C2C2E),
    outline = Color(0xFF2E2F31),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
)

@Composable
fun DssrkhTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography(),
        content = content,
    )
}
