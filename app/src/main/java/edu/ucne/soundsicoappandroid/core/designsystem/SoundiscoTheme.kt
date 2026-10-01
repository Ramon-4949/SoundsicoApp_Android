package edu.ucne.soundsicoappandroid.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFFC20017), onPrimary = Color.White,
    background = Color(0xFFF2F2F7), surface = Color.White,
    surfaceVariant = Color(0xFFEDEDF2), secondary = Color(0xFF686870)
)
private val DarkColors = darkColorScheme(
    primary = Color(0xFFFF5263), onPrimary = Color(0xFF490009),
    background = Color(0xFF101012), surface = Color(0xFF1C1C1E),
    surfaceVariant = Color(0xFF2C2C2E), secondary = Color(0xFFB8B8C0)
)

@Composable
fun SoundiscoTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors, content = content)
}
