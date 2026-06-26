package com.recover.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val NavyDark   = Color(0xFF0A0F24)
val NavyMedium = Color(0xFF111827)
val CardBg     = Color(0xFF1C2333)
val CardBorder = Color(0xFF2A3347)
val AccentBlue = Color(0xFF3B82F6)
val AccentGreen= Color(0xFF34C759)
val TextSec    = Color(0xFF8E9BB5)

private val DarkColors = darkColorScheme(
    primary         = AccentBlue,
    secondary       = AccentGreen,
    background      = NavyDark,
    surface         = CardBg,
    onBackground    = Color.White,
    onSurface       = Color.White,
    onPrimary       = Color.White,
    surfaceVariant  = Color(0xFF243047),
    outline         = CardBorder
)

@Composable
fun RecovErTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkColors, content = content)
}
