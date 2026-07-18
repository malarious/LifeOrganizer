package com.geepscontrol.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors =
    lightColorScheme(
        primary = Color(0xFF315C49),
        secondary = Color(0xFF4D6358),
        tertiary = Color(0xFF3D6374),
    )

private val DarkColors =
    darkColorScheme(
        primary = Color(0xFF99D2B5),
        secondary = Color(0xFFB4CCBF),
        tertiary = Color(0xFFA5CDDF),
    )

@Composable
fun GeepsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(colorScheme = if (darkTheme) DarkColors else LightColors, content = content)
}
