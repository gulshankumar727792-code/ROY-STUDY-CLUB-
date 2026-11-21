package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val RoyLightColorScheme = lightColorScheme(
    primary = RoyalBluePrimary,
    onPrimary = Color.White,
    primaryContainer = RoyalBlueContainer,
    onPrimaryContainer = OnRoyalBlueContainer,
    secondary = RoyalBlueLight,
    onSecondary = Color.White,
    secondaryContainer = SurfaceVariantBlue,
    onSecondaryContainer = TextPrimaryNavy,
    tertiary = CrownGold,
    onTertiary = OnCrownGoldContainer,
    tertiaryContainer = CrownGoldContainer,
    onTertiaryContainer = OnCrownGoldContainer,
    background = BackgroundIce,
    onBackground = TextPrimaryNavy,
    surface = SurfaceWhite,
    onSurface = TextPrimaryNavy,
    surfaceVariant = SurfaceVariantBlue,
    onSurfaceVariant = TextSecondarySlate,
    outline = OutlineBlue,
    outlineVariant = Color(0xFFDCE7FA),
    error = ErrorCrimson,
    onError = Color.White,
    errorContainer = ErrorCrimsonContainer,
    onErrorContainer = Color(0xFF7F1D1D)
)

private val RoyShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun RoyStudyClubTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = RoyLightColorScheme,
        typography = Typography,
        shapes = RoyShapes,
        content = content
    )
}
