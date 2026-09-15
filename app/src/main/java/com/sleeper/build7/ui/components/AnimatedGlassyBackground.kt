package com.sleeper.build7.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.math.cos
import kotlin.math.sin

/**
 * Premium Animated Material You Glassmorphic Background.
 * Provides a soft, slowly moving ambient mesh-like gradient that adapts
 * seamlessly to System Dark / Light themes:
 * - Dark Theme: Deep obsidian/slate canvas with neon green and cyan gradient pulses.
 * - Light Theme: Soft pastel slate canvas with gentle mint and sky-teal glows.
 */
@Composable
fun AnimatedGlassyBackground(
    modifier: Modifier = Modifier,
    isDark: Boolean = isSystemInDarkTheme(),
    content: @Composable BoxScope.() -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "AmbientBackgroundPulse")

    // Slow organic phase animations
    val phase1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 18000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase1"
    )

    val phase2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 24000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase2"
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    // Palette selection based on theme mode
    val baseBgColor = if (isDark) Color(0xFF0A1020) else Color(0xFFF6F8FC)
    val orb1Color = if (isDark) Color(0xFF10B981).copy(alpha = 0.22f) else Color(0xFF10B981).copy(alpha = 0.11f)
    val orb2Color = if (isDark) Color(0xFF06B6D4).copy(alpha = 0.20f) else Color(0xFF0284C7).copy(alpha = 0.10f)
    val orb3Color = if (isDark) Color(0xFF6366F1).copy(alpha = 0.14f) else Color(0xFFA7F3D0).copy(alpha = 0.14f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(baseBgColor)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Orb 1 coordinates (Greenish glow moving in gentle lissajous/ellipse curve)
            val orb1X = width * (0.30f + 0.25f * cos(phase1))
            val orb1Y = height * (0.25f + 0.18f * sin(phase1))
            val orb1Radius = (width.coerceAtLeast(height) * 0.55f) * pulseScale

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(orb1Color, Color.Transparent),
                    center = Offset(orb1X, orb1Y),
                    radius = orb1Radius
                ),
                radius = orb1Radius,
                center = Offset(orb1X, orb1Y)
            )

            // Orb 2 coordinates (Cyan glow moving around bottom-center)
            val orb2X = width * (0.70f + 0.22f * sin(phase2))
            val orb2Y = height * (0.72f + 0.16f * cos(phase2))
            val orb2Radius = (width.coerceAtLeast(height) * 0.60f) * (2f - pulseScale)

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(orb2Color, Color.Transparent),
                    center = Offset(orb2X, orb2Y),
                    radius = orb2Radius
                ),
                radius = orb2Radius,
                center = Offset(orb2X, orb2Y)
            )

            // Orb 3 coordinates (Subtle accent glow in middle-right)
            val orb3X = width * (0.50f + 0.30f * cos(phase1 + 1.2f))
            val orb3Y = height * (0.45f + 0.25f * sin(phase2 + 0.8f))
            val orb3Radius = width * 0.48f

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(orb3Color, Color.Transparent),
                    center = Offset(orb3X, orb3Y),
                    radius = orb3Radius
                ),
                radius = orb3Radius,
                center = Offset(orb3X, orb3Y)
            )
        }

        // Sub-screen and layout content rendered above dynamic animated canvas
        content()
    }
}
