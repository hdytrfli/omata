package dev.hdytrfli.omata.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val OmataColors =
    darkColorScheme(
        primary = Color(0xFF9FCAFF),
        onPrimary = Color(0xFF003257),
        primaryContainer = Color(0xFF00497C),
        onPrimaryContainer = Color(0xFFD1E4FF),
        background = Color(0xFF0B0D0F),
        onBackground = Color(0xFFE2E2E6),
        surface = Color(0xFF121417),
        onSurface = Color(0xFFE2E2E6),
        surfaceVariant = Color(0xFF42474E),
        onSurfaceVariant = Color(0xFFC2C7CF),
        outline = Color(0xFF8C9199),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
    )

@Composable
fun OmataTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = OmataColors, content = content)
}
