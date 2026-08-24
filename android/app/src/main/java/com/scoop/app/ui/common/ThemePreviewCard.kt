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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.scoop.app.ui.theme.Spacing
import kotlin.math.cos
import kotlin.math.sin

private val SkyTop = Color(0xFF7EC8F2)
private val SkyBottom = Color(0xFFCDEEFF)
private val Sun = Color(0xFFFFD54F)
private val SunGlow = Color(0xFFFFF3C4)
private val GrassBack = Color(0xFF8BC34A)
private val GrassFront = Color(0xFF689F38)
private val Stem = Color(0xFF4C7A2E)
private val FlowerCenter = Color(0xFFFFC107)

private data class Bloom(val xFrac: Float, val heightFrac: Float, val sizeFrac: Float, val petalColor: Color)

private val Blooms =
    listOf(
        Bloom(0.10f, 0.30f, 0.075f, Color(0xFFE91E63)),
        Bloom(0.24f, 0.42f, 0.09f, Color(0xFFFFFFFF)),
        Bloom(0.38f, 0.24f, 0.065f, Color(0xFFFF7043)),
        Bloom(0.52f, 0.46f, 0.10f, Color(0xFF9C27B0)),
        Bloom(0.66f, 0.28f, 0.07f, Color(0xFFFFEE58)),
        Bloom(0.80f, 0.40f, 0.085f, Color(0xFFEC407A)),
        Bloom(0.91f, 0.22f, 0.06f, Color(0xFF7E57C2)),
    )

/**
 * A themed preview: a small original illustration (colorful flowers in a garden, daylight) plus
 * a mock download-card row, so a Settings screen can show what the current accent palette looks
 * like against real components rather than just swatches. All text here is explicitly sample/
 * demo copy, not real app data. The garden scene uses fixed, literal colors — it's a picture, not
 * a theme-driven graphic — while the badge, title, and progress bar below still follow the
 * currently selected accent palette.
 */
@Composable
fun ThemePreviewCard(sampleTitle: String, sampleSubtitle: String, sampleBadge: String, progress: Float, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)) {
            Canvas(modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)) {
                val w = size.width
                val h = size.height

                drawRect(brush = Brush.verticalGradient(listOf(SkyTop, SkyBottom), endY = h * 0.78f))

                val sunCenter = Offset(w * 0.84f, h * 0.22f)
                drawCircle(color = SunGlow, radius = h * 0.28f, center = sunCenter)
                drawCircle(color = Sun, radius = h * 0.14f, center = sunCenter)

                val grassTop = h * 0.62f
                drawRect(color = GrassBack, topLeft = Offset(0f, grassTop), size = androidx.compose.ui.geometry.Size(w, h - grassTop))
                val frontTop = h * 0.74f
                drawRect(color = GrassFront, topLeft = Offset(0f, frontTop), size = androidx.compose.ui.geometry.Size(w, h - frontTop))

                Blooms.forEach { bloom ->
                    val bx = w * bloom.xFrac
                    val groundY = if (bloom.heightFrac > 0.35f) frontTop else grassTop
                    val flowerY = groundY - h * bloom.heightFrac
                    val r = h * bloom.sizeFrac

                    drawLine(color = Stem, start = Offset(bx, groundY), end = Offset(bx, flowerY), strokeWidth = w * 0.006f)

                    val petalCount = 6
                    for (i in 0 until petalCount) {
                        val angle = (2 * Math.PI * i / petalCount).toFloat()
                        val petalCenter = Offset(bx + cos(angle) * r * 0.9f, flowerY + sin(angle) * r * 0.9f)
                        drawCircle(color = bloom.petalColor, radius = r * 0.62f, center = petalCenter)
                    }
                    drawCircle(color = FlowerCenter, radius = r * 0.5f, center = Offset(bx, flowerY))
                }
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
