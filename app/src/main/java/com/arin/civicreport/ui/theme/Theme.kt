package com.arin.civicreport.ui.theme

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
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

private val Green = Color(0xFF0F6E4B)
private val GreenLight = Color(0xFF2E9D71)
private val GreenDark = Color(0xFF0A4A33)
private val Sand = Color(0xFFD9B26A)
private val Ink = Color(0xFF141A17)

private val Light = lightColorScheme(
    primary = Green,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB7EBD4),
    onPrimaryContainer = Color(0xFF002014),
    secondary = Sand,
    onSecondary = Ink,
    secondaryContainer = Color(0xFFF2E2C0),
    onSecondaryContainer = Color(0xFF2A1F00),
    tertiary = Color(0xFF1F5C8B),
    onTertiary = Color.White,
    background = Color(0xFFF7F9F7),
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Color(0xFFDDE5E0),
    onSurfaceVariant = Color(0xFF404943),
    outline = Color(0xFF707973),
    error = Color(0xFFB3261E),
    onError = Color.White,
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B)
)

private val Dark = darkColorScheme(
    primary = GreenLight,
    onPrimary = Color(0xFF002014),
    primaryContainer = GreenDark,
    onPrimaryContainer = Color(0xFFB7EBD4),
    secondary = Sand,
    onSecondary = Ink,
    secondaryContainer = Color(0xFF554100),
    onSecondaryContainer = Color(0xFFF2E2C0),
    tertiary = Color(0xFF8FC6F0),
    onTertiary = Color(0xFF00233A),
    background = Color(0xFF101513),
    onBackground = Color(0xFFE1E6E2),
    surface = Color(0xFF171D1A),
    onSurface = Color(0xFFE1E6E2),
    surfaceVariant = Color(0xFF404943),
    onSurfaceVariant = Color(0xFFBFC9C2),
    outline = Color(0xFF8A938D),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC)
)

val AppTypography = Typography(
    headlineLarge = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.Bold),
    headlineSmall = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium),
    bodyLarge = TextStyle(fontSize = 16.sp),
    bodyMedium = TextStyle(fontSize = 14.sp),
    labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium)
)

@Composable
fun CivicReportTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val scheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val ctx = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        }
        darkTheme -> Dark
        else -> Light
    }
    val view = LocalContext.current as? Activity
    SideEffect {
        view?.window?.let { w ->
            WindowCompat.getInsetsController(w, w.decorView).isAppearanceLightStatusBars = !darkTheme
        }
    }
    MaterialTheme(colorScheme = scheme, typography = AppTypography, content = content)
}
