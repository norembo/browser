package com.nutrisnap.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Brand palette
val Greenleaf = Color(0xFF2E7D5B)
val GreenleafDark = Color(0xFF1B5E40)
val Carrot = Color(0xFFE8743B)
val ProteinBlue = Color(0xFF3B82F6)
val CarbAmber = Color(0xFFF59E0B)
val FatPurple = Color(0xFF8B5CF6)

private val LightColors = lightColorScheme(
    primary = Greenleaf,
    secondary = Carrot,
    tertiary = ProteinBlue,
)
private val DarkColors = darkColorScheme(
    primary = GreenleafDark,
    secondary = Carrot,
    tertiary = ProteinBlue,
)

@Composable
fun NutriSnapTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colors = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = colors, typography = Typography(), content = content)
}
