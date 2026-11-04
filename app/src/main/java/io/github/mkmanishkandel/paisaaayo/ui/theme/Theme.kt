package io.github.mkmanishkandel.paisaaayo.ui.theme

import android.provider.Settings
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/** 4/8 dp spacing rhythm. */
object Spacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
    val xxxl = 48.dp
}

/** Shared motion durations (ms): exits faster than entrances. */
object Motion {
    const val SHORT = 150
    const val MEDIUM = 260
    const val LONG = 420
}

/** True when the user turned animations off in Android accessibility settings. */
val LocalReducedMotion = staticCompositionLocalOf { false }

/** True when the UI is in Nepali (drives fonts and Devanagari digits). */
val LocalNepali = staticCompositionLocalOf { false }

private val PaisaShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun PaisaAayoTheme(
    nepali: Boolean,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val reducedMotion = remember(context) {
        runCatching {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        }.getOrDefault(false)
    }
    CompositionLocalProvider(
        LocalPaisaColors provides if (darkTheme) DarkPaisaColors else LightPaisaColors,
        LocalReducedMotion provides reducedMotion,
        LocalNepali provides nepali,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkScheme else LightScheme,
            typography = remember(nepali) { paisaTypography(nepali) },
            shapes = PaisaShapes,
            content = content,
        )
    }
}
