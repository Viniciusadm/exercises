package com.archieapps.exercises.ui.design

import androidx.compose.ui.graphics.Color

val IronBackground = Color(0xFF0B0D0C)
val IronSurface = Color(0xFF14171A)
val IronSurfaceHigh = Color(0xFF1B2026)
val IronBorder = Color(0xFF252B33)
val IronBorderStrong = Color(0xFF334050)

val IronPrimary = Color(0xFF349EF4)
val IronPrimaryPressed = Color(0xFF2483D2)
val IronPrimaryGlow = Color(0x33349EF4)
val IronPrimaryFaint = Color(0x14349EF4)

val IronTextPrimary = Color(0xFFF2F5F7)
val IronTextSecondary = Color(0xFFA8B2BC)
val IronTextMuted = Color(0xFF6F7A85)

val IronRecord = Color(0xFFFFC24B)
val IronPositive = Color(0xFF3ED598)
val IronNegative = Color(0xFFFF5C5C)

/**
 * Cor estavel por identidade. Indexar a paleta pela ordem da lista faz a cor de um
 * grupo mudar quando o ranking muda entre periodos.
 */
fun muscleColorFor(key: String): Color =
    MuscleGroupColors[kotlin.math.abs(key.hashCode()) % MuscleGroupColors.size]

val MuscleGroupColors = listOf(
    IronPrimary,
    Color(0xFF5FD0E8),
    Color(0xFF8C7BF0),
    Color(0xFF3ED598),
    Color(0xFFFFC24B),
    Color(0xFFFF8A5C),
    Color(0xFFE86A9B),
    Color(0xFF9AA7B4),
)
