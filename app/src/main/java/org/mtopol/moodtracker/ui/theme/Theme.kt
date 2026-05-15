package org.mtopol.moodtracker.ui.theme

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

private val DarkColors = darkColorScheme(
    primary = Indigo80,
    secondary = Teal80,
    tertiary = Slate80,
)
private val LightColors = lightColorScheme(
    primary = Indigo40,
    secondary = Teal40,
    tertiary = Slate40,
)

@Composable
fun MoodTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}

/** Fixed series/state colors, dark-aware but independent of the dynamic scheme. */
@Composable
fun anxietyColor(): Color = if (isSystemInDarkTheme()) AnxietyDark else AnxietyLight

@Composable
fun depressionColor(): Color = if (isSystemInDarkTheme()) DepressionDark else DepressionLight

@Composable
fun missedColor(): Color = if (isSystemInDarkTheme()) MissedDark else MissedLight

/** Green once the form is complete. */
@Composable
fun completeColor(): Color = if (isSystemInDarkTheme()) CompleteDark else CompleteLight
