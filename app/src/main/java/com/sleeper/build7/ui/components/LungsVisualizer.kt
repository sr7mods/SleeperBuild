package com.sleeper.build7.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sleeper.build7.R
import kotlin.math.*

/**
 * Clean anatomical lungs and cigarette visualization.
 * Rendered using crisp, scalable Android vector XMLs with
 * natural breathing and smoke animations.
 */
@Composable
fun LungsVisualizer(
    isDemonic: Boolean,
    daysClean: Int = 0,
    totalSmoked: Int = 0,
    flareTrigger: Long = 0L,
    modifier: Modifier = Modifier
) {
    // Smooth natural breathing motion
    val infiniteTransition = rememberInfiniteTransition(label = "lungs_respiration")
    val breathScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breath_scale"
    )

    // Pulsing ember glow at the cigarette tip
    val emberPulse by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ember_pulse"
    )

    // Ambient particle and smoke flow
    val particlePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28318f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "particle_phase"
    )

    // Quick burst effect when logging a cigarette
    val flareAnim = remember { Animatable(0f) }
    LaunchedEffect(flareTrigger) {
        if (flareTrigger > 0L) {
            flareAnim.snapTo(1f)
            flareAnim.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing)
            )
        }
    }

    // Cigarette gradually fades away as clean streak grows (faded out at 30 days)
    val cigAlpha = remember(daysClean) {
        (1f - (daysClean / 30f)).coerceIn(0.08f, 1f)
    }

    val themeAccent = if (isDemonic) Color(0xFFEF4444) else Color(0xFF10B981)
    val containerBg = if (isDemonic) {
        Brush.verticalGradient(
            listOf(
                Color(0xFF150808),
                Color(0xFF0F0505),
                Color(0xFF180808)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color(0x2810B981),
                Color(0x1506B6D4),
                Color(0x2238BDF8)
            )
        )
    }

    val containerBorder = if (isDemonic) {
        Brush.horizontalGradient(
            listOf(
                Color(0xFFDC2626).copy(alpha = 0.8f),
                Color(0xFFF59E0B).copy(alpha = 0.7f),
                Color(0xFF991B1B).copy(alpha = 0.8f)
            )
        )
    } else {
        Brush.horizontalGradient(
            listOf(
                Color(0xFF10B981).copy(alpha = 0.7f),
                Color(0xFF06B6D4).copy(alpha = 0.7f),
                Color(0xFF3B82F6).copy(alpha = 0.7f)
            )
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(containerBg)
            .border(1.2.dp, containerBorder, RoundedCornerShape(22.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Status Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(themeAccent)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isDemonic) "SMOKING IMPACT" else "LUNG HEALTH",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp,
                    color = if (isDemonic) Color(0xFFF87171) else Color(0xFF34D399)
                )
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isDemonic) Color(0x35DC2626) else Color(0x2510B981),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isDemonic) Color(0x60EF4444) else Color(0x5010B981)
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isDemonic) Icons.Default.LocalFireDepartment else Icons.Default.Spa,
                        contentDescription = null,
                        tint = if (isDemonic) Color(0xFFFCA5A5) else Color(0xFF6EE7B7),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (isDemonic) "Active Smoker" else "Smoke Free",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDemonic) Color(0xFFFCA5A5) else Color(0xFF6EE7B7)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 1. ANATOMICAL LUNGS VIEWPORT (Vector XML + Smooth Breathing Animation)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF070709))
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            // Scalable Vector XML Anatomical Lungs with harmonic breathing scale
            Image(
                painter = painterResource(
                    id = if (isDemonic) R.drawable.ic_lungs_demonic else R.drawable.ic_lungs_healthy
                ),
                contentDescription = if (isDemonic) "Smoker Lungs" else "Healthy Lungs",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp)
                    .scale(breathScale),
                contentScale = ContentScale.Fit
            )

            // Dynamic Particle & Flare Animation Layer
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val flare = flareAnim.value

                if (isDemonic) {
                    // Rising ember sparks drifting upward from lungs
                    val emberCount = 16
                    for (i in 0 until emberCount) {
                        val progress = ((particlePhase + i * 0.65f) % 6.28318f) / 6.28318f
                        val pY = h * (1f - progress * 1.05f)
                        val pX = w * 0.2f + (w * 0.6f) * ((sin(i * 2.1f + particlePhase) + 1f) / 2f)
                        val pRadius = 1.5f + (sin(i * 1.4f + particlePhase) + 1f) * 1.2f + flare * 2f
                        val pAlpha = (sin(progress * Math.PI.toFloat()) * (0.5f + flare * 0.5f)).coerceIn(0f, 1f)

                        drawCircle(
                            brush = Brush.radialGradient(
                                listOf(
                                    Color(0xFFFDE047).copy(alpha = pAlpha),
                                    Color(0xFFEF4444).copy(alpha = pAlpha * 0.7f),
                                    Color.Transparent
                                ),
                                center = Offset(pX, pY),
                                radius = pRadius * 2f
                            ),
                            radius = pRadius * 2f,
                            center = Offset(pX, pY)
                        )
                    }

                    // Combustion burst when logging a cigarette
                    if (flare > 0.02f) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                listOf(
                                    Color(0xFFEF4444).copy(alpha = flare * 0.4f),
                                    Color(0xFFF59E0B).copy(alpha = flare * 0.2f),
                                    Color.Transparent
                                ),
                                center = Offset(w / 2f, h / 2f),
                                radius = (w / 2f) * (0.6f + flare * 0.4f)
                            ),
                            radius = (w / 2f) * (0.6f + flare * 0.4f),
                            center = Offset(w / 2f, h / 2f)
                        )
                    }
                } else {
                    // Fresh oxygen particles softly rising
                    val particleCount = 12
                    for (i in 0 until particleCount) {
                        val progress = ((particlePhase * 0.7f + i * 0.85f) % 6.28318f) / 6.28318f
                        val pY = h * 0.2f + h * 0.65f * (1f - progress)
                        val pX = w * 0.2f + w * 0.6f * ((sin(i * 1.9f + particlePhase * 0.6f) + 1f) / 2f)
                        val pRadius = 1.3f + (sin(particlePhase + i) + 1f) * 1.0f
                        val pAlpha = (0.25f + 0.35f * sin(progress * Math.PI.toFloat())).coerceIn(0f, 0.75f)

                        drawCircle(
                            color = Color(0xFF6EE7B7).copy(alpha = pAlpha),
                            radius = pRadius,
                            center = Offset(pX, pY)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 2. CIGARETTE STAGE (Vector XML + Dynamic Smoke & Cherry Ember Animation)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF070709))
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            // Stage Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isDemonic) "CURRENT CIGARETTE" else "CLEAN PROGRESS",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 0.8.sp,
                    color = if (isDemonic) Color(0xFFF87171) else Color(0xFF34D399)
                )

                Text(
                    text = if (isDemonic) "Burning" else if (daysClean >= 30) "Smoke free" else "Day $daysClean clean",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Cigarette Display Box
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(82.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF040405)),
                contentAlignment = Alignment.Center
            ) {
                val boxWidth = maxWidth
                val boxHeight = maxHeight

                if (isDemonic) {
                    // Burning cigarette Vector XML
                    Image(
                        painter = painterResource(id = R.drawable.ic_cigarette_burning),
                        contentDescription = "Burning Cigarette",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        contentScale = ContentScale.Fit
                    )

                    // Dynamic live burning ember and rising smoke plumes
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        val flare = flareAnim.value

                        // The burning cherry is located at approximately 85% width, centered vertically
                        val cherryX = w * 0.84f
                        val cherryY = h * 0.50f
                        val cherryCenter = Offset(cherryX, cherryY)

                        // Pulsing incandescent halo over the cherry
                        val glowRadius = 14f + (emberPulse * 7f) + (flare * 18f)
                        drawCircle(
                            brush = Brush.radialGradient(
                                listOf(
                                    Color(0xFFFFFBEB).copy(alpha = 0.7f + flare * 0.3f),
                                    Color(0xFFF59E0B).copy(alpha = 0.55f + flare * 0.3f),
                                    Color(0xFFEF4444).copy(alpha = 0.35f),
                                    Color.Transparent
                                ),
                                center = cherryCenter,
                                radius = glowRadius
                            ),
                            radius = glowRadius,
                            center = cherryCenter
                        )

                        // Smooth curling smoke plumes rising from the burning tip
                        val smokePuffs = 7
                        for (i in 0 until smokePuffs) {
                            val progress = ((particlePhase * 1.3f + i * 1.1f) % 5f) / 5f
                            val sY = cherryY - (progress * (h * 0.7f))
                            val wave = sin(particlePhase * 2.2f + i * 1.4f)
                            val sX = cherryX + wave * (8f + progress * 20f)
                            val sRadius = 4f + progress * 14f + flare * 6f
                            val sAlpha = ((1f - progress) * (0.35f + flare * 0.25f)).coerceIn(0f, 0.6f)

                            drawCircle(
                                brush = Brush.radialGradient(
                                    listOf(
                                        Color(0xFFE2E8F0).copy(alpha = sAlpha),
                                        Color(0xFF94A3B8).copy(alpha = sAlpha * 0.45f),
                                        Color.Transparent
                                    ),
                                    center = Offset(sX, sY),
                                    radius = sRadius
                                ),
                                radius = sRadius,
                                center = Offset(sX, sY)
                            )
                        }

                        // Crackling sparks popping off the ember
                        val sparkCount = if (flare > 0.05f) 8 else 3
                        for (i in 0 until sparkCount) {
                            val angle = (i * 0.9f) + sin(particlePhase * 2f + i) * 0.4f
                            val dist = 8f + ((particlePhase * 10f + i * 7f) % 22f) + flare * 16f
                            val sparkOffset = Offset(
                                cherryCenter.x + cos(angle) * dist,
                                cherryCenter.y - abs(sin(angle)) * dist
                            )
                            val sparkAlpha = (1f - (dist / 38f)).coerceIn(0f, 1f)
                            drawCircle(
                                color = if (i % 2 == 0) Color(0xFFFFFBEB).copy(alpha = sparkAlpha) else Color(0xFFF59E0B).copy(alpha = sparkAlpha),
                                radius = 1.6f,
                                center = sparkOffset
                            )
                        }
                    }
                } else {
                    // Extinguished half-smoked cigarette Vector XML
                    Image(
                        painter = painterResource(id = R.drawable.ic_cigarette_extinguished),
                        contentDescription = "Extinguished Cigarette",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp)
                            .alpha(cigAlpha),
                        contentScale = ContentScale.Fit
                    )

                    // Clean particles gently fading away
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        for (i in 0..5) {
                            val stardustX = w * 0.25f + (w * 0.5f) * ((sin(particlePhase + i * 1.3f) + 1f) / 2f)
                            val stardustY = h * 0.2f + (h * 0.6f) * ((cos(particlePhase * 0.7f + i * 1.2f) + 1f) / 2f)
                            val stardustAlpha = (0.2f + 0.25f * ((sin(particlePhase * 2f + i) + 1f) / 2f)).coerceIn(0f, 0.5f)
                            drawCircle(
                                color = Color(0xFF6EE7B7).copy(alpha = stardustAlpha * cigAlpha),
                                radius = 1.3f,
                                center = Offset(stardustX, stardustY)
                            )
                        }
                    }
                }
            }
        }
    }
}
