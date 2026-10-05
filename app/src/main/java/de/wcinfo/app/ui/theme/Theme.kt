package de.wcinfo.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryPurpleDark,
    onPrimary = Color.White,
    primaryContainer = PrimaryPurpleContainerDark,
    onPrimaryContainer = Color(0xFFF3E8FF),
    secondary = AccentMagentaDark,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF4A154B),
    onSecondaryContainer = Color(0xFFFDF4FF),
    tertiary = PrimaryPurpleDark,
    onTertiary = Color.White,
    background = BackgroundAppDark,
    onBackground = Color(0xFFE4E4E7),
    surface = CardBackgroundDark,
    onSurface = Color(0xFFF4F4F5),
    surfaceVariant = SecondaryBackgroundDark,
    onSurfaceVariant = Color(0xFFA1A1AA),
    outline = Color(0xFF52525B),
    outlineVariant = BorderSubtleDark
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryPurple,
    onPrimary = Color.White,
    primaryContainer = PrimaryPurpleLight,
    onPrimaryContainer = PrimaryPurpleHover,
    secondary = AccentMagenta,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFDF4FF),
    onSecondaryContainer = Color(0xFF86198F),
    tertiary = PrimaryPurpleLight,
    onTertiary = PrimaryPurpleHover,
    background = BackgroundApp,
    onBackground = Color(0xFF1F2937),
    surface = CardBackground,
    onSurface = Color(0xFF111827),
    surfaceVariant = SecondaryBackground,
    onSurfaceVariant = Color(0xFF6B7280),
    outline = Color(0xFF9CA3AF),
    outlineVariant = BorderSubtle
)

val MaterialTheme.statusOpen: Color
    @Composable
    get() = if (isSystemInDarkTheme()) StatusOpenDark else StatusOpen

val MaterialTheme.statusUrgent: Color
    @Composable
    get() = if (isSystemInDarkTheme()) StatusUrgentDark else StatusUrgent

val MaterialTheme.statusWarning: Color
    @Composable
    get() = if (isSystemInDarkTheme()) StatusWarningDark else StatusWarning

val MaterialTheme.statusClosed: Color
    @Composable
    get() = if (isSystemInDarkTheme()) StatusClosedDark else StatusClosed

@Composable
fun WCinfoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
