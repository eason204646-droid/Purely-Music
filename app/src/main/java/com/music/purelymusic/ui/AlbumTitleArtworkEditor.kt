// Copyright (c) 2026 eason204646. Licensed under Mulan PSL v2.
package com.music.purelymusic.ui

import android.graphics.Bitmap
import android.graphics.Point
import android.graphics.Rect as BitmapRect
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Colorize
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.util.UnstableApi
import com.music.purelymusic.model.Album
import com.music.purelymusic.ui.utils.ExtractedAlbumTitle
import com.music.purelymusic.ui.utils.extractAlbumTitle
import com.music.purelymusic.ui.utils.fillAlbumTitleOnCover
import com.music.purelymusic.ui.utils.loadAlbumBitmap
import com.music.purelymusic.ui.utils.prepareImportedAlbumTitle
import com.music.purelymusic.viewmodel.PlayerViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private enum class ArtworkEditorPage { Sources, Import, Extract }

private data class ArtworkDraft(
    val title: Bitmap,
    val cover: Bitmap? = null,
    val extraction: ExtractedAlbumTitle? = null
)

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
internal fun AlbumTitleArtworkEditor(
    album: Album,
    coverSource: String?,
    viewModel: PlayerViewModel,
    onDismiss: () -> Unit
) {
    val chinese = viewModel.currentLanguage == "zh"
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var page by rememberSaveable { mutableStateOf(ArtworkEditorPage.Sources) }
    var importedUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    var saving by remember { mutableStateOf(false) }
    var saveFailed by remember { mutableStateOf(false) }
    var draft by remember(page) { mutableStateOf<ArtworkDraft?>(null) }
    val hasArtwork = album.titleArtworkPath != null
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            saveFailed = false
            importedUri = uri
            page = ArtworkEditorPage.Import
        }
    }

    fun back() {
        if (saving) return
        saveFailed = false
        if (page == ArtworkEditorPage.Sources) onDismiss() else page = ArtworkEditorPage.Sources
    }

    fun persist(close: Boolean, work: suspend () -> Boolean) {
        if (saving) return
        saving = true
        saveFailed = false
        scope.launch {
            try {
                val saved = work()
                saveFailed = !saved
                if (saved && close) onDismiss()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                saveFailed = true
            } finally {
                saving = false
            }
        }
    }

    Dialog(
        onDismissRequest = ::back,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = !saving)
    ) {
        Surface(
            modifier = Modifier.padding(horizontal = 18.dp).widthIn(max = 500.dp).fillMaxWidth()
                .heightIn(max = LocalConfiguration.current.screenHeightDp.dp * 0.90f),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 4.dp
        ) {
            Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 10.dp)) {
                Text(
                    when (page) {
                        ArtworkEditorPage.Sources -> if (chinese) "专辑艺术字" else "Album title artwork"
                        ArtworkEditorPage.Import -> if (chinese) "预览导入的艺术字" else "Preview imported artwork"
                        ArtworkEditorPage.Extract -> if (chinese) "从封面提取" else "Extract from cover"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(12.dp))
                Column(
                    Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    when (page) {
                        ArtworkEditorPage.Sources -> {
                            Text(album.name, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (hasArtwork) {
                                AlbumArtworkPreview(album.titleArtworkPath)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        if (chinese) "在此专辑显示艺术字" else "Show artwork for this album",
                                        modifier = Modifier.weight(1f)
                                    )
                                    Switch(
                                        checked = album.titleArtworkEnabled,
                                        enabled = !saving,
                                        onCheckedChange = { enabled ->
                                            persist(close = false) { viewModel.setAlbumTitleArtworkEnabled(album.id, enabled) }
                                        }
                                    )
                                }
                            }
                            OutlinedButton(
                                onClick = { picker.launch("image/*") },
                                enabled = !saving,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.FileUpload, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(if (chinese) "导入透明图片" else "Import a transparent image")
                            }
                            OutlinedButton(
                                onClick = {
                                    saveFailed = false
                                    page = ArtworkEditorPage.Extract
                                },
                                enabled = !saving && coverSource != null,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.CropFree, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(if (chinese) "从封面框选提取" else "Select and extract from cover")
                            }
                            if (hasArtwork) {
                                TextButton(
                                    onClick = { persist(close = false) { viewModel.saveAlbumTitleArtwork(album.id, null) } },
                                    enabled = !saving
                                ) {
                                    Text(if (chinese) "移除艺术字" else "Remove artwork", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                        ArtworkEditorPage.Import -> ImportedArtworkPreview(importedUri, chinese) { bitmap ->
                            SideEffect { draft = bitmap?.let { ArtworkDraft(it) } }
                        }
                        ArtworkEditorPage.Extract -> CoverArtworkExtraction(coverSource, chinese, saving) { cover, extraction ->
                            SideEffect {
                                draft = if (cover != null && extraction != null) {
                                    ArtworkDraft(extraction.artwork, cover, extraction)
                                } else {
                                    null
                                }
                            }
                        }
                    }
                }
                if (saveFailed) {
                    Text(
                        if (chinese) "保存失败，请重试。" else "Couldn’t save the artwork. Try again.",
                        modifier = Modifier.padding(top = 8.dp),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (saving) LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 8.dp))
                Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = ::back, enabled = !saving) {
                        Text(
                            if (page == ArtworkEditorPage.Sources) {
                                if (chinese) "完成" else "Done"
                            } else {
                                if (chinese) "返回" else "Back"
                            }
                        )
                    }
                    if (page != ArtworkEditorPage.Sources) {
                        Button(
                            enabled = draft != null && !saving,
                            onClick = {
                                val selected = draft ?: return@Button
                                persist(close = true) {
                                    val displayCover = if (selected.cover != null && selected.extraction != null) {
                                        fillAlbumTitleOnCover(selected.cover, selected.extraction)
                                    } else {
                                        null
                                    }
                                    viewModel.saveAlbumTitleArtwork(album.id, selected.title, displayCover)
                                }
                            }
                        ) {
                            Text(if (chinese) "应用到专辑" else "Apply to album")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AlbumArtworkPreview(path: String?) {
    val context = LocalContext.current
    val bitmap by produceState<Bitmap?>(null, path) {
        value = path?.let { loadAlbumBitmap(context, it, size = 480) }
    }
    bitmap?.let { ArtworkCheckerboard(it) }
}

@Composable
private fun ImportedArtworkPreview(uri: Uri?, chinese: Boolean, onPreview: @Composable (Bitmap?) -> Unit) {
    val context = LocalContext.current
    var loading by remember(uri) { mutableStateOf(true) }
    val bitmap by produceState<Bitmap?>(null, uri) {
        loading = true
        value = uri?.let { loadAlbumBitmap(context, it) }?.let { prepareImportedAlbumTitle(it) }
        loading = false
    }
    onPreview(bitmap.takeUnless { loading })
    if (loading) {
        LinearProgressIndicator(Modifier.fillMaxWidth())
    } else if (bitmap == null) {
        Text(
            if (chinese) "请选择能打开且含透明背景的图片，例如透明 PNG。" else
                "Choose a readable image with a transparent background, such as a transparent PNG.",
            color = MaterialTheme.colorScheme.error
        )
    } else {
        ArtworkCheckerboard(checkNotNull(bitmap))
    }
}

@Composable
private fun CoverArtworkExtraction(
    coverSource: String?,
    chinese: Boolean,
    saving: Boolean,
    onPreview: @Composable (Bitmap?, ExtractedAlbumTitle?) -> Unit
) {
    val context = LocalContext.current
    var loading by remember(coverSource) { mutableStateOf(true) }
    val cover by produceState<Bitmap?>(null, coverSource) {
        loading = true
        value = coverSource?.let { loadAlbumBitmap(context, it) }
        loading = false
    }
    var selectionCoordinates by rememberSaveable(coverSource) { mutableStateOf<FloatArray?>(null) }
    var sampleCoordinates by rememberSaveable(coverSource) { mutableStateOf<FloatArray?>(null) }
    val selection = selectionCoordinates?.let { Rect(it[0], it[1], it[2], it[3]) }
    val sample = sampleCoordinates?.let { Offset(it[0], it[1]) }
    var sampling by remember { mutableStateOf(false) }
    var precision by rememberSaveable { mutableFloatStateOf(0.5f) }
    val image = cover
    if (image == null || loading) {
        onPreview(null, null)
        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth()) else {
            Text(
                if (chinese) "无法读取这张专辑的封面。" else "This album’s cover could not be opened.",
                color = MaterialTheme.colorScheme.error
            )
        }
        return
    }
    var extracting by remember(image, selection, precision, sample) { mutableStateOf(false) }
    val extraction by key(image, selection, precision, sample) {
        produceState<ExtractedAlbumTitle?>(null) {
            val region = selection ?: return@produceState
            extracting = true
            delay(100)
            val bounds = BitmapRect(
                (region.left * image.width).roundToInt(), (region.top * image.height).roundToInt(),
                (region.right * image.width).roundToInt(), (region.bottom * image.height).roundToInt()
            )
            val point = sample?.let {
                Point((it.x * image.width).toInt(), (it.y * image.height).toInt())
            }
            value = extractAlbumTitle(image, bounds, precision, point)
            extracting = false
        }
    }
    onPreview(image, extraction.takeUnless { extracting })
    Text(
        if (sampling) {
            if (chinese) "点一下框内最能代表艺术字的颜色。" else "Tap a representative letter color inside the selection."
        } else {
            if (chinese) "框住完整艺术字，四周稍留空隙。可拖动四角微调。" else
                "Select the full title with a little space around it. Drag the corners to refine."
        },
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    SelectableAlbumCover(
        cover = image,
        selection = selection,
        sample = sample,
        sampling = sampling,
        enabled = !saving,
        label = if (chinese) "原始专辑封面" else "Original album cover",
        onSelection = { rect ->
            selectionCoordinates = floatArrayOf(rect.left, rect.top, rect.right, rect.bottom)
            if (sample?.let { !rect.contains(it) } == true) sampleCoordinates = null
        },
        onSample = {
            sampleCoordinates = floatArrayOf(it.x, it.y)
            sampling = false
        }
    )
    if (selection != null) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = sampling,
                enabled = !saving,
                onClick = { sampling = !sampling },
                label = { Text(if (chinese) "点选字色" else "Sample letter color") },
                leadingIcon = { Icon(Icons.Default.Colorize, null, Modifier.size(17.dp)) }
            )
            if (sample != null) {
                TextButton(
                    onClick = {
                        sampleCoordinates = null
                        sampling = false
                    },
                    enabled = !saving
                ) {
                    Text(if (chinese) "清除取色" else "Clear sample")
                }
            }
        }
        Text(if (chinese) "背景去除强度" else "Background removal strength", style = MaterialTheme.typography.labelLarge)
        Slider(value = precision, onValueChange = { precision = it }, enabled = !saving)
        val preview = extraction?.artwork
        when {
            extracting -> LinearProgressIndicator(Modifier.fillMaxWidth())
            preview != null -> ArtworkCheckerboard(preview)
            else -> Text(
                if (chinese) "未提取到完整字形。可缩小选区、点选字色或调节强度。" else
                    "No distinct lettering found. Tighten the selection, sample a color, or adjust the strength.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun ArtworkCheckerboard(bitmap: Bitmap) {
    Box(
        Modifier.fillMaxWidth().height(86.dp).clip(RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.matchParentSize()) {
            val tile = 10.dp.toPx()
            val rows = (size.height / tile).toInt()
            val columns = (size.width / tile).toInt()
            val light = Color(0xFFB1B6BB)
            val dark = Color(0xFF899098)
            drawRect(light)
            for (row in 0..rows) {
                for (column in row % 2..columns step 2) {
                    drawRect(dark, Offset(column * tile, row * tile), Size(tile, tile))
                }
            }
        }
        Image(
            bitmap = remember(bitmap) { bitmap.asImageBitmap() },
            contentDescription = null,
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
private fun SelectableAlbumCover(
    cover: Bitmap,
    selection: Rect?,
    sample: Offset?,
    sampling: Boolean,
    enabled: Boolean,
    label: String,
    onSelection: (Rect) -> Unit,
    onSample: (Offset) -> Unit
) {
    var draft by remember(cover) { mutableStateOf<Rect?>(null) }
    val currentSelection by rememberUpdatedState(selection)
    val select by rememberUpdatedState(onSelection)
    val sampleColor by rememberUpdatedState(onSample)
    val aspect = cover.width.toFloat() / cover.height
    val maxHeight = minOf(295.dp, LocalConfiguration.current.screenHeightDp.dp * 0.39f)
    BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val width = minOf(maxWidth, maxHeight * aspect)
        val shape = RoundedCornerShape(10.dp)
        Box(Modifier.width(width).height(width / aspect).clip(shape)) {
            Image(
                bitmap = remember(cover) { cover.asImageBitmap() },
                contentDescription = label,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.FillBounds
            )
            Canvas(
                Modifier.matchParentSize().pointerInput(cover, sampling, enabled) {
                    if (!enabled) return@pointerInput
                    try {
                        awaitEachGesture {
                            val down = awaitFirstDown()
                            val start = Offset(
                                (down.position.x / size.width).coerceIn(0f, 1f),
                                (down.position.y / size.height).coerceIn(0f, 1f)
                            )
                            val existing = currentSelection
                            down.consume()
                            if (sampling) {
                                if (existing?.contains(start) == true) sampleColor(start)
                                return@awaitEachGesture
                            }
                            val corners = existing?.corners().orEmpty()
                            val closest = corners.indexOfFirst { corner ->
                                val delta = Offset(
                                    (corner.x - start.x) * size.width,
                                    (corner.y - start.y) * size.height
                                )
                                delta.getDistance() <= 24.dp.toPx()
                            }
                            val anchor = if (closest >= 0) corners[(closest + 2) % 4] else start
                            while (true) {
                                val event = awaitPointerEvent()
                                val pointer = event.changes.firstOrNull { it.id == down.id } ?: break
                                if (pointer.isConsumed) break
                                if (pointer.position != down.position || draft != null) {
                                    val end = Offset(
                                        (pointer.position.x / size.width).coerceIn(0f, 1f),
                                        (pointer.position.y / size.height).coerceIn(0f, 1f)
                                    )
                                    draft = Rect(min(anchor.x, end.x), min(anchor.y, end.y), max(anchor.x, end.x), max(anchor.y, end.y))
                                }
                                if (!pointer.pressed) {
                                    draft?.takeIf { it.width * size.width >= 12.dp.toPx() && it.height * size.height >= 9.dp.toPx() }
                                        ?.let(select)
                                    break
                                }
                                pointer.consume()
                            }
                            draft = null
                        }
                    } finally {
                        draft = null
                    }
                }
            ) {
                val rect = (draft ?: selection)?.let {
                    Rect(it.left * size.width, it.top * size.height, it.right * size.width, it.bottom * size.height)
                } ?: return@Canvas
                val dim = Color.Black.copy(alpha = 0.56f)
                drawRect(dim, size = Size(size.width, rect.top))
                drawRect(dim, Offset(0f, rect.bottom), Size(size.width, size.height - rect.bottom))
                drawRect(dim, Offset(0f, rect.top), Size(rect.left, rect.height))
                drawRect(dim, Offset(rect.right, rect.top), Size(size.width - rect.right, rect.height))
                drawRect(Color.Black.copy(alpha = 0.55f), rect.topLeft, rect.size, style = Stroke(4.dp.toPx()))
                drawRect(Color.White, rect.topLeft, rect.size, style = Stroke(2.dp.toPx()))
                rect.corners().forEach { drawCircle(Color.White, radius = 4.dp.toPx(), center = it) }
                sample?.let {
                    val center = Offset(it.x * size.width, it.y * size.height)
                    drawCircle(Color.Black, radius = 7.dp.toPx(), center = center)
                    drawCircle(Color.White, radius = 5.dp.toPx(), center = center, style = Stroke(2.dp.toPx()))
                }
            }
        }
    }
}

private fun Rect.corners() = listOf(topLeft, topRight, bottomRight, bottomLeft)
