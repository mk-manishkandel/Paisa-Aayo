package io.github.mkmanishkandel.paisaaayo.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Design system (ui-ux-pro-max, "Fintech — gold trust"): gold primary on deep ink,
// Swiss/minimal style. Semantic tokens below; never use raw hex in screens.

private val Ink = Color(0xFF0F172A)
private val Gold = Color(0xFFF59E0B)
private val GoldSoft = Color(0xFFFBBF24)

val DarkScheme = darkColorScheme(
    primary = Gold,
    onPrimary = Ink,
    primaryContainer = Color(0xFF3B2A0C),
    onPrimaryContainer = Color(0xFFFCD34D),
    secondary = GoldSoft,
    onSecondary = Ink,
    background = Ink,
    onBackground = Color(0xFFF8FAFC),
    surface = Ink,
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF272F42),
    onSurfaceVariant = Color(0xFF94A3B8),
    surfaceContainerLowest = Color(0xFF0B1222),
    surfaceContainerLow = Color(0xFF182033),
    surfaceContainer = Color(0xFF1E2536),
    surfaceContainerHigh = Color(0xFF222735),
    surfaceContainerHighest = Color(0xFF272F42),
    outline = Color(0xFF475569),
    outlineVariant = Color(0xFF334155),
    error = Color(0xFFF87171),
    onError = Ink,
    inverseSurface = Color(0xFFF8FAFC),
    inverseOnSurface = Ink,
)

val LightScheme = lightColorScheme(
    primary = Gold,
    onPrimary = Ink,
    primaryContainer = Color(0xFFFEF3C7),
    onPrimaryContainer = Color(0xFF78350F),
    secondary = Color(0xFFB45309),
    onSecondary = Color.White,
    background = Color(0xFFF8FAFC),
    onBackground = Ink,
    surface = Color(0xFFF8FAFC),
    onSurface = Ink,
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF475569),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color(0xFFF1F5F9),
    surfaceContainerHighest = Color(0xFFE2E8F0),
    outline = Color(0xFF94A3B8),
    outlineVariant = Color(0xFFE2E8F0),
    error = Color(0xFFB91C1C),
    onError = Color.White,
    inverseSurface = Ink,
    inverseOnSurface = Color(0xFFF8FAFC),
)

/** Colours Material 3 has no slot for. */
@Immutable
data class PaisaColors(
    val credit: Color,
    val creditContainer: Color,
    val debit: Color,
    val debitContainer: Color,
    /** Gold that is readable as text (≥4.5:1) on the current background. */
    val accentText: Color,
    val heroGradientTop: Color,
    val heroGradientBottom: Color,
)

val DarkPaisaColors = PaisaColors(
    credit = Color(0xFF4ADE80),
    creditContainer = Color(0xFF12301F),
    debit = Color(0xFFF87171),
    debitContainer = Color(0xFF3A1A1F),
    accentText = GoldSoft,
    heroGradientTop = Color(0xFF1C2438),
    heroGradientBottom = Ink,
)

val LightPaisaColors = PaisaColors(
    credit = Color(0xFF15803D),
    creditContainer = Color(0xFFDCFCE7),
    debit = Color(0xFFB91C1C),
    debitContainer = Color(0xFFFEE2E2),
    accentText = Color(0xFFB45309),
    heroGradientTop = Color(0xFFFFF7E6),
    heroGradientBottom = Color(0xFFF8FAFC),
)

val LocalPaisaColors = staticCompositionLocalOf { DarkPaisaColors }
