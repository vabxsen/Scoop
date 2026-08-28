package com.scoop.app.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp

/**
 * An original flat-vector illustration for empty states: a tree with a small accent circle
 * tucked close to its canopy. Composed from primitive shapes (not traced from any reference
 * artwork). All stroke widths and positions are proportional to the canvas size, since DrawScope
 * operates in raw pixels and a fixed px stroke width would render as a near-invisible hairline on
 * higher-density screens.
 */
@Composable
fun EmptyDownloadsIllustration(modifier: Modifier = Modifier) {
    val canopy = MaterialTheme.colorScheme.secondaryContainer
    val canopyShadow = MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f)
    val trunk = MaterialTheme.colorScheme.onSurfaceVariant
    val ground = MaterialTheme.colorScheme.outlineVariant
    val accentBg = MaterialTheme.colorScheme.primaryContainer

    Box(modifier = modifier.width(210.dp).height(200.dp)) {
        Canvas(modifier = Modifier.width(220.dp).height(200.dp)) {
            val w = size.width
            val h = size.height

            // Ground line
            drawLine(
                color = ground,
                start = Offset(0f, h * 0.86f),
                end = Offset(w * 0.82f, h * 0.86f),
                strokeWidth = w * 0.012f,
                cap = StrokeCap.Round,
            )

            // Tree trunk
            val trunkX = w * 0.32f
            drawLine(
                color = trunk,
                start = Offset(trunkX, h * 0.20f),
                end = Offset(trunkX, h * 0.86f),
                strokeWidth = w * 0.028f,
                cap = StrokeCap.Round,
            )

            // Tree canopy (two overlapping circles for a soft, organic shape)
            drawCircle(color = canopyShadow, radius = w * 0.24f, center = Offset(trunkX - w * 0.07f, h * 0.26f))
            drawCircle(color = canopy, radius = w * 0.27f, center = Offset(trunkX + w * 0.05f, h * 0.22f))
        }

        // Decorative accent circle with a download glyph, tucked close to the canopy
        Box(modifier = Modifier.width(72.dp).height(72.dp).align(Alignment.CenterEnd)) {
            Canvas(modifier = Modifier.width(72.dp).height(72.dp)) { drawCircle(color = accentBg) }
            Icon(
                Icons.Outlined.Download,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.width(28.dp).height(28.dp).align(Alignment.Center),
            )
        }
    }
}
