package com.archieapps.exercises.ui.design

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val IronColorScheme = darkColorScheme(
    primary = IronPrimary,
    onPrimary = IronBackground,
    primaryContainer = IronPrimaryFaint,
    onPrimaryContainer = IronPrimary,
    secondary = IronRecord,
    onSecondary = IronBackground,
    background = IronBackground,
    onBackground = IronTextPrimary,
    surface = IronSurface,
    onSurface = IronTextPrimary,
    surfaceVariant = IronSurfaceHigh,
    onSurfaceVariant = IronTextSecondary,
    outline = IronBorder,
    outlineVariant = IronBorderStrong,
    error = IronNegative,
    onError = IronBackground,
)

private val IronShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

object Spacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
}

@Composable
fun ExercisesTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = IronColorScheme,
        typography = IronTypography,
        shapes = IronShapes,
        content = content,
    )
}
