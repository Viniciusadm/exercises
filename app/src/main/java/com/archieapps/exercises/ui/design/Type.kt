package com.archieapps.exercises.ui.design

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.archieapps.exercises.R

val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
)

val Condensed = FontFamily(
    Font(R.font.barlow_condensed_semibold, FontWeight.SemiBold),
    Font(R.font.barlow_condensed_bold, FontWeight.Bold),
)

val IronTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = Condensed,
        fontWeight = FontWeight.Bold,
        fontSize = 76.sp,
        lineHeight = 72.sp,
        letterSpacing = (-2).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = Condensed,
        fontWeight = FontWeight.Bold,
        fontSize = 44.sp,
        lineHeight = 44.sp,
        letterSpacing = (-1).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.4).sp,
    ),
    titleLarge = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.2).sp,
    ),
    titleMedium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.2.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.SemiBold,
        fontSize = 10.sp,
        lineHeight = 12.sp,
        letterSpacing = 1.4.sp,
    ),
)

object IronText {
    val metric = TextStyle(
        fontFamily = Condensed,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.6).sp,
    )

    val metricSmall = TextStyle(
        fontFamily = Condensed,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 22.sp,
    )

    val tabular = TextStyle(
        fontFamily = Condensed,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 18.sp,
        textAlign = TextAlign.End,
    )
}
