package org.thewealthgapresolutionalgorithm.pdfseal.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColors = darkColorScheme(
    primary = SealGold,
    onPrimary = InkBlack,
    primaryContainer = SealGoldDeep,
    onPrimaryContainer = InkOnDark,
    secondary = SealGoldBright,
    onSecondary = InkBlack,
    secondaryContainer = InkSurfaceHigh,
    onSecondaryContainer = InkOnDark,
    background = InkBlack,
    onBackground = InkOnDark,
    surface = InkSurface,
    onSurface = InkOnDark,
    surfaceVariant = InkSurfaceHigh,
    onSurfaceVariant = InkOnDark,
    outline = InkOutline,
    outlineVariant = InkOutline,
)

private val LightColors = lightColorScheme(
    primary = SealGoldDeep,
    onPrimary = Cream,
    primaryContainer = SealGoldBright,
    onPrimaryContainer = InkOnLight,
    secondary = SealGoldDeep,
    onSecondary = Cream,
    secondaryContainer = CreamSurfaceHigh,
    onSecondaryContainer = InkOnLight,
    background = Cream,
    onBackground = InkOnLight,
    surface = CreamSurface,
    onSurface = InkOnLight,
    surfaceVariant = CreamSurfaceHigh,
    onSurfaceVariant = InkOnLight,
    outline = CreamOutline,
    outlineVariant = CreamOutline,
)

/**
 * App-wide theme. Explicit light + dark gold-seal schemes; dynamic color is
 * intentionally NOT used so the brand look is consistent on every device.
 */
@Composable
fun PdfSealTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = PdfSealTypography,
        shapes = PdfSealShapes,
        content = content,
    )
}
