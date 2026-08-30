package com.example.ui.theme

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

private val DarkColorScheme = darkColorScheme(
    primary = HblDarkPrimary,
    onPrimary = Color(0xFF00382D),
    primaryContainer = HblDarkPrimaryContainer,
    onPrimaryContainer = Color(0xFF8CF4D6),
    secondary = HblLime,
    onSecondary = HblOnLime,
    secondaryContainer = Color(0xFF2C3800),
    onSecondaryContainer = Color(0xFFF3F9CE),
    tertiary = HblLime,
    onTertiary = HblOnLime,
    background = HblDarkBackground,
    onBackground = HblDarkOnSurface,
    surface = HblDarkSurface,
    onSurface = HblDarkOnSurface,
    surfaceVariant = HblDarkSurfaceVariant,
    onSurfaceVariant = HblDarkOnSurfaceVariant,
    outline = Color(0xFF899A93),
    outlineVariant = Color(0xFF3F504B)
)

private val LightColorScheme = lightColorScheme(
    primary = HblPrimary,
    onPrimary = Color.White,
    primaryContainer = HblPrimaryContainer,
    onPrimaryContainer = HblOnPrimaryContainer,
    secondary = HblLime,
    onSecondary = HblOnLime,
    secondaryContainer = HblLimeContainer,
    onSecondaryContainer = HblOnLimeContainer,
    tertiary = HblTertiaryLime,
    onTertiary = HblOnLime,
    tertiaryContainer = HblTertiaryContainer,
    onTertiaryContainer = HblOnTertiaryContainer,
    background = HblBackground,
    onBackground = HblOnSurface,
    surface = HblSurface,
    onSurface = HblOnSurface,
    surfaceVariant = HblSurfaceVariant,
    onSurfaceVariant = HblOnSurfaceVariant,
    outline = HblOutline,
    outlineVariant = HblOutlineVariant
)

@Composable
fun HblTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MyApplicationTheme(
        darkTheme = darkTheme,
        dynamicColor = dynamicColor,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Keep HBL brand identity consistent by default
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
