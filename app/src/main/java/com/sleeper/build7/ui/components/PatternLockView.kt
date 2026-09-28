package com.sleeper.build7.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.sqrt

/**
 * High-performance 3x3 Pattern Lock drawing view for Jetpack Compose.
 * Outputs pattern as comma-separated node indices (e.g. "0,1,2,5,8").
 */
@Composable
fun PatternLockView(
    modifier: Modifier = Modifier,
    size: Dp = 280.dp,
    dotColor: Color = Color.White.copy(alpha = 0.5f),
    activeColor: Color = MaterialTheme.colorScheme.primary,
    lineColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
    onPatternComplete: (String) -> Unit
) {
    var selectedNodes by remember { mutableStateOf<List<Int>>(emptyList()) }
    var currentTouchPosition by remember { mutableStateOf<Offset?>(null) }

    Box(
        modifier = modifier
            .size(size)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        selectedNodes = emptyList()
                        currentTouchPosition = offset
                        val node = findHitNode(offset, size.toPx())
                        if (node != null) {
                            selectedNodes = listOf(node)
                        }
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        currentTouchPosition = change.position
                        val node = findHitNode(change.position, size.toPx())
                        if (node != null && node !in selectedNodes) {
                            selectedNodes = selectedNodes + node
                        }
                    },
                    onDragEnd = {
                        currentTouchPosition = null
                        if (selectedNodes.isNotEmpty()) {
                            onPatternComplete(selectedNodes.joinToString(","))
                        }
                    },
                    onDragCancel = {
                        currentTouchPosition = null
                        selectedNodes = emptyList()
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val totalSize = size.toPx()
            val step = totalSize / 4f
            val nodeRadius = 14f
            val outerRingRadius = 32f

            // Calculate center points for all 9 nodes (index 0..8)
            val nodeCenters = (0..8).map { index ->
                val row = index / 3
                val col = index % 3
                Offset((col + 1) * step, (row + 1) * step)
            }

            // 1. Draw connecting lines between already-selected nodes
            for (i in 0 until selectedNodes.size - 1) {
                val start = nodeCenters[selectedNodes[i]]
                val end = nodeCenters[selectedNodes[i + 1]]
                drawLine(
                    color = lineColor,
                    start = start,
                    end = end,
                    strokeWidth = 8f,
                    cap = StrokeCap.Round
                )
            }

            // 2. Draw live line from the last selected node to current touch pointer
            if (selectedNodes.isNotEmpty() && currentTouchPosition != null) {
                val lastCenter = nodeCenters[selectedNodes.last()]
                drawLine(
                    color = lineColor.copy(alpha = 0.6f),
                    start = lastCenter,
                    end = currentTouchPosition!!,
                    strokeWidth = 6f,
                    cap = StrokeCap.Round
                )
            }

            // 3. Draw the 9 nodes
            nodeCenters.forEachIndexed { index, center ->
                val isSelected = index in selectedNodes
                val color = if (isSelected) activeColor else dotColor

                // Outer faint ring if selected
                if (isSelected) {
                    drawCircle(
                        color = activeColor.copy(alpha = 0.25f),
                        radius = outerRingRadius,
                        center = center
                    )
                    drawCircle(
                        color = activeColor.copy(alpha = 0.7f),
                        radius = outerRingRadius,
                        center = center,
                        style = Stroke(width = 2.5f)
                    )
                } else {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.08f),
                        radius = outerRingRadius,
                        center = center
                    )
                }

                // Inner solid dot
                drawCircle(
                    color = color,
                    radius = if (isSelected) nodeRadius * 1.2f else nodeRadius,
                    center = center
                )
            }
        }
    }
}

private fun findHitNode(touch: Offset, totalSize: Float): Int? {
    val step = totalSize / 4f
    val hitThreshold = step * 0.45f

    for (index in 0..8) {
        val row = index / 3
        val col = index % 3
        val center = Offset((col + 1) * step, (row + 1) * step)
        val dx = touch.x - center.x
        val dy = touch.y - center.y
        if (sqrt(dx * dx + dy * dy) <= hitThreshold) {
            return index
        }
    }
    return null
}
