package com.tuppersoft.signaturepad.sample.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * Android-specific theme wrapper with dynamic color support (Android 12+).
 *
 * This extends the common SignaturePadTheme with Android-specific features
 * like dynamic colors based on the system wallpaper.
 *
 * @param darkTheme Whether to use dark theme. Defaults to system setting.
 * @param dynamicColor Whether to use dynamic colors on Android 12+. Defaults to true.
 * @param content The composable content to be themed.
 */
@Composable
public fun AndroidSignaturePadTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> {
            // Fallback to common theme
            return SignaturePadTheme(darkTheme = darkTheme, content = content)
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
