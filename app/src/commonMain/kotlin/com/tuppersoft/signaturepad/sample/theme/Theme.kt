package com.tuppersoft.signaturepad.sample.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

/**
 * Signature Pad theme for all platforms (Android, Desktop, iOS, Web).
 *
 * This is a simplified Material 3 theme without platform-specific features
 * like Android's dynamic colors.
 *
 * @param darkTheme Whether to use dark theme colors. Defaults to false (light theme).
 * @param content The composable content to be themed.
 */
@Composable
public fun SignaturePadTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
