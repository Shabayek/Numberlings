package com.vividoai.numberlings.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

private val Scheme = lightColorScheme(
    primary = Berry,
    onPrimary = Paper,
    secondary = Sunflower,
    onSecondary = Cocoa,
    tertiary = Leaf,
    background = Cream,
    onBackground = Cocoa,
    surface = Paper,
    onSurface = Cocoa,
    surfaceVariant = Color(0xFFFDF1DC),
    onSurfaceVariant = CocoaSoft
)

private val KidTypography = Typography().let {
    it.copy(
        headlineLarge = it.headlineLarge.copy(fontWeight = FontWeight.ExtraBold),
        headlineMedium = it.headlineMedium.copy(fontWeight = FontWeight.Bold),
        titleLarge = it.titleLarge.copy(fontWeight = FontWeight.Bold),
        bodyLarge = it.bodyLarge.copy(fontWeight = FontWeight.Medium)
    )
}

@Composable
fun NumberlingsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = Scheme,
        typography = KidTypography,
        // No custom font files: system sans only (web-safe, offline).
        content = content
    )
}

// Referencing FontFamily keeps future font swaps local to this file.
private val FutureFont = FontFamily.Default
