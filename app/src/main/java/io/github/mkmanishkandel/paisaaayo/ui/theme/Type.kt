package io.github.mkmanishkandel.paisaaayo.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import io.github.mkmanishkandel.paisaaayo.R

@OptIn(ExperimentalTextApi::class)
private fun variableFamily(res: Int) = FontFamily(
    listOf(FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold).map { weight ->
        Font(res, weight = weight, variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)))
    },
)

/** IBM Plex Sans — fintech, trustworthy (design-system pick for English). */
val PlexSans = variableFamily(R.font.ibm_plex_sans)

/** Noto Sans Devanagari — full Nepali glyph coverage incl. conjuncts, plus Latin. */
val NotoDevanagari = variableFamily(R.font.noto_sans_devanagari)

/**
 * Type scale. Devanagari has tall ascenders (matras) and deep descenders, so Nepali gets
 * ~12% more line height to avoid clipping.
 */
fun paisaTypography(nepali: Boolean): Typography {
    val family = if (nepali) NotoDevanagari else PlexSans
    val lh = if (nepali) 1.12f else 1f
    fun style(size: Int, line: Int, weight: FontWeight, tracking: Double = 0.0) = TextStyle(
        fontFamily = family,
        fontWeight = weight,
        fontSize = size.sp,
        lineHeight = (line * lh).sp,
        // Devanagari is never tracked: negative spacing breaks its shaping (glyphs dropped or
        // wrapped, e.g. "रु ५२२" drawn as "रु ५२").
        letterSpacing = if (nepali) 0.em else tracking.em,
    )
    return Typography(
        displayLarge = style(44, 52, FontWeight.Bold, -0.02),
        displayMedium = style(36, 44, FontWeight.Bold, -0.02),
        displaySmall = style(30, 38, FontWeight.SemiBold, -0.01),
        headlineLarge = style(28, 36, FontWeight.SemiBold, -0.01),
        headlineMedium = style(24, 32, FontWeight.SemiBold),
        headlineSmall = style(20, 28, FontWeight.SemiBold),
        titleLarge = style(20, 28, FontWeight.SemiBold),
        titleMedium = style(16, 24, FontWeight.SemiBold),
        titleSmall = style(14, 20, FontWeight.Medium),
        bodyLarge = style(16, 22, FontWeight.Normal),
        bodyMedium = style(14, 20, FontWeight.Normal),
        bodySmall = style(13, 18, FontWeight.Normal),
        labelLarge = style(15, 20, FontWeight.SemiBold, 0.01),
        labelMedium = style(13, 18, FontWeight.Medium, 0.01),
        labelSmall = style(12, 16, FontWeight.Medium, 0.02),
    )
}

/** Tabular figures so amounts line up in lists. */
val TextStyle.tabular: TextStyle get() = copy(fontFeatureSettings = "tnum")
