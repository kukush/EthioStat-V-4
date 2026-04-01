package com.ethiobalance.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont

val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = com.ethiobalance.app.R.array.com_google_android_gms_fonts_certs
)

val ManropeFamily = FontFamily(
    Font(googleFont = GoogleFont("Manrope"), fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = GoogleFont("Manrope"), fontProvider = provider, weight = FontWeight.SemiBold),
    Font(googleFont = GoogleFont("Manrope"), fontProvider = provider, weight = FontWeight.Bold),
    Font(googleFont = GoogleFont("Manrope"), fontProvider = provider, weight = FontWeight.ExtraBold),
    Font(googleFont = GoogleFont("Manrope"), fontProvider = provider, weight = FontWeight.Black),
)

private val LightColorScheme = lightColorScheme(
    primary = Blue600,
    onPrimary = Color.White,
    primaryContainer = Blue50,
    onPrimaryContainer = Blue600,
    secondary = Slate900,
    onSecondary = Color.White,
    background = Color.White,
    onBackground = Slate900,
    surface = Color.White,
    onSurface = Slate900,
    surfaceVariant = Slate50,
    onSurfaceVariant = Slate400,
    outline = Slate100,
    error = Rose600,
    onError = Color.White,
)

private val DarkColorScheme = darkColorScheme(
    primary = Blue400,
    onPrimary = Slate900,
    primaryContainer = Blue600,
    onPrimaryContainer = Color.White,
    secondary = Color.White,
    onSecondary = Slate900,
    background = Slate900,
    onBackground = Color.White,
    surface = Slate800,
    onSurface = Color.White,
    surfaceVariant = Slate700,
    onSurfaceVariant = Slate400,
    outline = Slate600,
    error = Rose500,
    onError = Color.White,
)

private val MidnightColorScheme = darkColorScheme(
    primary = Blue400,
    onPrimary = Blue950,
    primaryContainer = Blue600,
    onPrimaryContainer = Color.White,
    secondary = Color.White,
    onSecondary = Blue950,
    background = Blue950,
    onBackground = Color.White,
    surface = Color(0xFF1E3A5F),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF162D4A),
    onSurfaceVariant = Slate400,
    outline = Color(0xFF1E3A5F),
    error = Rose500,
    onError = Color.White,
)

private val ForestColorScheme = darkColorScheme(
    primary = Emerald500,
    onPrimary = Emerald950,
    primaryContainer = Emerald600,
    onPrimaryContainer = Color.White,
    secondary = Color.White,
    onSecondary = Emerald950,
    background = Emerald950,
    onBackground = Color.White,
    surface = Color(0xFF0A3D2A),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF083322),
    onSurfaceVariant = Slate400,
    outline = Color(0xFF0A3D2A),
    error = Rose500,
    onError = Color.White,
)

@Composable
fun EthioBalanceTheme(
    themeId: String = "light",
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeId) {
        "dark" -> DarkColorScheme
        "midnight" -> MidnightColorScheme
        "forest" -> ForestColorScheme
        else -> LightColorScheme
    }

    val typography = Typography().let { base ->
        base.copy(
            displayLarge = base.displayLarge.copy(fontFamily = ManropeFamily),
            displayMedium = base.displayMedium.copy(fontFamily = ManropeFamily),
            displaySmall = base.displaySmall.copy(fontFamily = ManropeFamily),
            headlineLarge = base.headlineLarge.copy(fontFamily = ManropeFamily),
            headlineMedium = base.headlineMedium.copy(fontFamily = ManropeFamily),
            headlineSmall = base.headlineSmall.copy(fontFamily = ManropeFamily),
            titleLarge = base.titleLarge.copy(fontFamily = ManropeFamily),
            titleMedium = base.titleMedium.copy(fontFamily = ManropeFamily),
            titleSmall = base.titleSmall.copy(fontFamily = ManropeFamily),
            bodyLarge = base.bodyLarge.copy(fontFamily = ManropeFamily),
            bodyMedium = base.bodyMedium.copy(fontFamily = ManropeFamily),
            bodySmall = base.bodySmall.copy(fontFamily = ManropeFamily),
            labelLarge = base.labelLarge.copy(fontFamily = ManropeFamily),
            labelMedium = base.labelMedium.copy(fontFamily = ManropeFamily),
            labelSmall = base.labelSmall.copy(fontFamily = ManropeFamily),
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        content = content
    )
}
