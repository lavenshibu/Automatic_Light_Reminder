// Package for app theme customization
package com.example.lhtkotapp.ui.theme

// --- Required Imports ---
import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

// --- Define Static Dark Theme Colors ---
private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

// --- Define Static Light Theme Colors ---
private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

// --- Custom Theme Composable ---
@Composable
fun LhtkotAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(), // Default: match system theme
    dynamicColor: Boolean = true,               // Enable Material You dynamic color (Android 12+)
    content: @Composable () -> Unit             // Composable UI content block
) {
    // Determine appropriate color scheme based on OS version and theme mode
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    // Apply the MaterialTheme with chosen color scheme and typography
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
