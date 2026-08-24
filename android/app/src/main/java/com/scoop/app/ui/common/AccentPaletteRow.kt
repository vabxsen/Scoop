package com.scoop.app.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.scoop.app.core.model.AccentPalette
import com.scoop.app.ui.theme.Spacing
import com.scoop.app.ui.theme.swatchColors

@Composable
fun AccentPaletteRow(
    selected: AccentPalette,
    onSelect: (AccentPalette) -> Unit,
    label: @Composable (AccentPalette) -> String,
    modifier: Modifier = Modifier,
) {
    LazyRow(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
        items(AccentPalette.entries) { palette ->
            val (top, bottom) = palette.swatchColors()
            PaletteSwatch(
                topColor = top,
                bottomColor = bottom,
                isSelected = palette == selected,
                label = label(palette),
                onClick = { onSelect(palette) },
            )
        }
    }
}

@Composable
private fun PaletteSwatch(topColor: Color, bottomColor: Color, isSelected: Boolean, label: String, onClick: () -> Unit) {
    Box(
        modifier =
            Modifier.size(56.dp)
                .clip(CircleShape)
                .clickable(onClickLabel = label, onClick = onClick)
                .border(
                    width = if (isSelected) 2.dp else 0.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                    shape = CircleShape,
                ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(56.dp)) {
            drawRect(color = topColor, size = Size(size.width, size.height / 2f))
            drawRect(color = bottomColor, topLeft = androidx.compose.ui.geometry.Offset(0f, size.height / 2f), size = Size(size.width, size.height / 2f))
        }
        if (isSelected) {
            Box(
                modifier = Modifier.size(24.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(16.dp))
            }
        }
    }
}
