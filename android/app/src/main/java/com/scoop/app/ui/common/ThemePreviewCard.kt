package com.scoop.app.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.scoop.app.ui.theme.Spacing

/**
 * A themed preview: a small illustration echoing the app's own moon/bird mark plus a mock
 * download-card row, so a Settings screen can show what the current accent palette actually
 * looks like against real components rather than just swatches. All text here is explicitly
 * sample/demo copy, not real app data.
 */
@Composable
fun ThemePreviewCard(sampleTitle: String, sampleSubtitle: String, sampleBadge: String, progress: Float, modifier: Modifier = Modifier) {
    val canopy = MaterialTheme.colorScheme.secondaryContainer
    val moon = MaterialTheme.colorScheme.primary
    val cutout = MaterialTheme.colorScheme.surfaceContainerHigh
    val accent = MaterialTheme.colorScheme.tertiary

    Card(modifier = modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(canopy)) {
            Canvas(modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)) {
                val w = size.width
                val h = size.height
                val moonRadius = h * 0.34f
                val moonCenter = Offset(w * 0.42f, h * 0.5f)
                drawCircle(color = moon, radius = moonRadius, center = moonCenter)
                drawCircle(color = cutout, radius = moonRadius * 0.82f, center = Offset(moonCenter.x + moonRadius * 0.42f, moonCenter.y - moonRadius * 0.12f))
                drawCircle(color = accent, radius = h * 0.09f, center = Offset(w * 0.78f, h * 0.28f))
            }
            Text(
                sampleBadge,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                modifier =
                    Modifier.align(Alignment.BottomEnd)
                        .padding(Spacing.sm)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(horizontal = Spacing.xs, vertical = 2.dp),
            )
        }
        Column(modifier = Modifier.fillMaxWidth().padding(Spacing.md)) {
            Text(sampleTitle, style = MaterialTheme.typography.titleSmall, maxLines = 1)
            Text(sampleSubtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Box(modifier = Modifier.fillMaxWidth().height(3.dp).background(MaterialTheme.colorScheme.surfaceContainerHigh)) {
            Box(modifier = Modifier.fillMaxWidth(progress).height(3.dp).background(MaterialTheme.colorScheme.primary))
        }
    }
}
