package com.scoop.app.ui.screen.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.ImageLoader
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import com.scoop.app.R
import com.scoop.app.core.model.ImageCollection
import com.scoop.app.ui.theme.Spacing
import com.scoop.app.extractor.forImages
import okhttp3.OkHttpClient

@Composable
fun ImageConfigureForm(viewModel: HomeViewModel, collection: ImageCollection, onDismiss: () -> Unit, onOpenDownloads: () -> Unit) {
    val selected = viewModel.selectedImageUrls
    val context = LocalContext.current
    val imageLoader = remember(context) {
        ImageLoader.Builder(context).okHttpClient(OkHttpClient().forImages()).components { add(SvgDecoder.Factory()) }.build()
    }
    DisposableEffect(imageLoader) { onDispose { imageLoader.shutdown() } }
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Text(collection.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(stringResource(R.string.image_original_quality), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.image_selection_count, selected.size, collection.images.size), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = { viewModel.selectAllImages(selected.size != collection.images.size) }) {
                Text(stringResource(if (selected.size == collection.images.size) R.string.image_deselect_all else R.string.image_select_all))
            }
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(if (collection.images.size == 1) 1 else 2),
            modifier = Modifier.fillMaxWidth().heightIn(max = (LocalConfiguration.current.screenHeightDp * 0.42f).dp),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            items(collection.images, key = { it.url }) { image ->
                val checked = image.url in selected
                Column(modifier = Modifier.clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .border(if (checked) 2.dp else 1.dp, if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                    .toggleable(checked, role = Role.Checkbox, onValueChange = { viewModel.toggleImage(image.url) })) {
                    Box(modifier = Modifier.fillMaxWidth().aspectRatio(if (collection.images.size == 1) 1.7f else 1f)
                        .background(Color(0xFFE5E5E5)), contentAlignment = Alignment.Center) {
                        var previewFailed by remember(image.url) { mutableStateOf(false) }
                        val request = remember(image.url, image.headers) {
                            ImageRequest.Builder(context).data(image.url).apply { image.headers.forEach { (key, value) -> addHeader(key, value) } }.build()
                        }
                        if (previewFailed) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Outlined.Image, contentDescription = null, tint = Color.DarkGray)
                                Text(stringResource(R.string.image_preview_unavailable), style = MaterialTheme.typography.bodySmall, color = Color.DarkGray)
                            }
                        } else {
                            AsyncImage(model = request, imageLoader = imageLoader, contentDescription = null, contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize(), onError = { previewFailed = true })
                        }
                        Checkbox(checked = checked, onCheckedChange = null,
                            modifier = Modifier.align(Alignment.TopEnd).background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f), RoundedCornerShape(12.dp)))
                    }
                    Text(image.title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs))
                    val metadata = listOfNotNull(
                        if (image.width > 0 && image.height > 0) "${image.width} × ${image.height}" else null,
                        image.mimeType?.substringAfter('/')?.substringBefore('+')?.uppercase(),
                    ).joinToString(" · ")
                    if (metadata.isNotEmpty()) Text(metadata, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs))
                }
            }
        }
        collection.notice?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        Button(onClick = {
            if (viewModel.confirmImagesDownload() > 1) { onDismiss(); onOpenDownloads() }
        }, enabled = selected.isNotEmpty(), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(50)) {
            Text(stringResource(R.string.image_download_selected, selected.size))
        }
    }
}
