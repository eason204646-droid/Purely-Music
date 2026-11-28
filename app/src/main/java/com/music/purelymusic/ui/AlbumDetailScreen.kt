// Copyright (c) 2026 eason204646. Licensed under Mulan PSL v2.
package com.music.purelymusic.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
import com.music.purelymusic.R
import com.music.purelymusic.model.Album
import com.music.purelymusic.model.Song
import com.music.purelymusic.ui.utils.loadAlbumBitmap
import com.music.purelymusic.viewmodel.PlayerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val AlbumInk = Color(0xFF0C1216)
private val AlbumWhite = Color(0xFFF5F8F8)
private val AlbumMuted = Color(0xFFBDC8CC)
private val AlbumRule = Color.White.copy(alpha = 0.13f)

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun AlbumDetailScreen(
    album: Album,
    viewModel: PlayerViewModel,
    onBack: () -> Unit,
    onNavigateToPlayer: () -> Unit
) {
    val library = viewModel.libraryList
    val songs = remember(album.id, album.name, library) {
        library.filter { song ->
            song.albumId == album.id || (song.albumId == null && song.album == album.name)
        }.sortedWith(compareBy<Song> { it.createdTime }.thenBy { it.id })
    }
    val chinese = viewModel.currentLanguage == "zh"
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val cover = album.coverUri ?: songs.firstNotNullOfOrNull { it.coverUri }
    val artworkPath = album.titleArtworkPath.takeIf {
        viewModel.albumTitleArtworkEnabled && album.titleArtworkEnabled
    }
    val title by key(artworkPath) {
        produceState<Bitmap?>(null) {
            value = artworkPath?.let { loadAlbumBitmap(context, it) }
        }
    }
    val displayCover = album.titleArtworkCoverPath.takeIf { title != null }
    val art by produceState<AlbumHeroArt?>(null, displayCover, cover) {
        val bitmap = displayCover?.let { loadAlbumBitmap(context, it) }
            ?: cover?.let { loadAlbumBitmap(context, it) }
            ?: loadAlbumBitmap(context, R.drawable.default_cover)
        value = bitmap?.let { withContext(Dispatchers.Default) { buildAlbumHeroArt(it) } }
    }
    val canvasColor = art?.surface ?: AlbumInk
    val highlight = art?.accent ?: AlbumWhite
    val statusInsets = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navInsets = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val trackCount = songs.size
    val countText = when {
        chinese -> "$trackCount 首歌曲"
        trackCount == 1 -> "1 track"
        else -> "$trackCount tracks"
    }
    var showPlaylists by rememberSaveable(album.id) { mutableStateOf(false) }
    val linePadding = 22.dp

    fun play(shuffle: Boolean = false, song: Song? = null) {
        viewModel.playCollection(songs, isRandom = shuffle, startingSong = song)
        onNavigateToPlayer()
    }

    if (showPlaylists) {
        AddToPlaylistDialog(
            viewModel = viewModel,
            songIds = songs.map { it.id },
            onDismiss = { showPlaylists = false }
        )
    }

    Box(
        modifier = Modifier.fillMaxSize().background(canvasColor),
        contentAlignment = Alignment.TopCenter
    ) {
        BoxWithConstraints(Modifier.widthIn(max = 710.dp).fillMaxHeight().fillMaxWidth()) {
            val pageWidth = maxWidth
            val heroHeight = (pageWidth * 1.20f).coerceIn(260.dp, 595.dp)
                .coerceAtMost((maxHeight * 0.88f).coerceAtLeast(220.dp))
            val parallaxLimit = with(LocalDensity.current) { 80.dp.toPx() }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                contentPadding = PaddingValues(bottom = navInsets + 100.dp)
            ) {
                item(key = "album-header") {
                    Box(Modifier.fillMaxWidth()) {
                        AlbumCoverDepth(
                            art = art,
                            surface = canvasColor,
                            height = heroHeight,
                            scrollOffset = {
                                if (listState.firstVisibleItemIndex == 0) {
                                    (listState.firstVisibleItemScrollOffset * 0.22f).coerceAtMost(parallaxLimit)
                                } else 0f
                            }
                        )
                        Column(
                            Modifier.fillMaxWidth()
                                .padding(top = maxOf(heroHeight * 0.73f, statusInsets + 90.dp))
                                .padding(horizontal = linePadding),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (title != null) {
                                val bitmap = checkNotNull(title)
                                val aspect = bitmap.width.toFloat() / bitmap.height
                                val width = pageWidth * 0.72f
                                val height = (width / aspect).coerceIn(28.dp, minOf(114.dp, pageWidth * 0.25f))
                                Image(
                                    bitmap = remember(bitmap) { bitmap.asImageBitmap() },
                                    contentDescription = album.name,
                                    modifier = Modifier.width(width).height(height).semantics { heading() },
                                    contentScale = ContentScale.Fit
                                )
                            } else {
                                Text(
                                    album.name,
                                    modifier = Modifier.semantics { heading() },
                                    color = AlbumWhite,
                                    textAlign = TextAlign.Center,
                                    fontSize = if (pageWidth < 380.dp) 33.sp else 39.sp,
                                    lineHeight = if (pageWidth < 380.dp) 36.sp else 42.sp,
                                    letterSpacing = (-0.95).sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 4,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (album.artist.isNotBlank()) {
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    album.artist,
                                    color = AlbumWhite,
                                    textAlign = TextAlign.Center,
                                    fontSize = 20.sp,
                                    lineHeight = 24.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(Modifier.height(5.dp))
                            Text(countText, color = AlbumMuted, fontSize = 13.sp)
                            Spacer(Modifier.height(22.dp))
                            Row(
                                modifier = Modifier.widthIn(max = 370.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AlbumIconButton(
                                    icon = Icons.Default.Shuffle,
                                    label = if (chinese) "随机播放" else "Shuffle",
                                    enabled = songs.isNotEmpty(),
                                    size = 52.dp,
                                    onClick = { play(shuffle = true) }
                                )
                                Button(
                                    onClick = { play() },
                                    enabled = songs.isNotEmpty(),
                                    modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                                    shape = RoundedCornerShape(19.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = highlight,
                                        contentColor = AlbumInk,
                                        disabledContainerColor = Color.White.copy(alpha = 0.09f),
                                        disabledContentColor = AlbumMuted
                                    ),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                                    Spacer(Modifier.width(6.dp))
                                    Text(if (chinese) "播放" else "Play", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                }
                                AlbumIconButton(
                                    icon = Icons.Default.Add,
                                    label = if (chinese) "添加专辑到歌单" else "Add album to playlist",
                                    enabled = songs.isNotEmpty(),
                                    size = 52.dp,
                                    onClick = { showPlaylists = true }
                                )
                            }
                            Spacer(Modifier.height(25.dp))
                            HorizontalDivider(color = AlbumRule)
                        }
                    }
                }
                if (songs.isEmpty()) {
                    item(key = "album-empty") {
                        Text(
                            if (chinese) "这张专辑还没有歌曲" else "There are no tracks in this album yet",
                            modifier = Modifier.padding(horizontal = linePadding, vertical = 22.dp),
                            color = AlbumMuted
                        )
                    }
                }
                itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
                    AlbumTrack(
                        song = song,
                        index = index,
                        albumArtist = album.artist,
                        currentTrack = song.id == viewModel.currentSong?.id,
                        playing = viewModel.isActuallyPlaying,
                        playingLabel = if (chinese) "正在播放" else "Now playing",
                        highlight = highlight,
                        modifier = Modifier.padding(horizontal = linePadding),
                        onClick = { play(song = song) }
                    )
                }
            }
            Box(Modifier.statusBarsPadding().padding(horizontal = 16.dp, vertical = 9.dp)) {
                AlbumIconButton(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    if (chinese) "返回" else "Back",
                    overCover = true,
                    onClick = onBack
                )
            }
        }
    }
}

private data class AlbumHeroArt(
    val sharp: Bitmap,
    val soft: Bitmap,
    val surface: Color,
    val accent: Color
)

private fun buildAlbumHeroArt(bitmap: Bitmap): AlbumHeroArt {
    val scale = 24f / maxOf(bitmap.width, bitmap.height)
    val soft = Bitmap.createScaledBitmap(
        bitmap, (bitmap.width * scale).toInt().coerceAtLeast(1),
        (bitmap.height * scale).toInt().coerceAtLeast(1), true
    )
    var hue = Color(soft.getPixel(soft.width / 2, soft.height / 2)).copy(alpha = 1f)
    for (step in 1..4) {
        val color = Color(soft.getPixel((soft.width - 1) * step / 5, (soft.height - 1) * 3 / 4))
        hue = lerp(hue, color.copy(alpha = 1f), 0.20f)
    }
    return AlbumHeroArt(bitmap, soft, lerp(AlbumInk, hue, 0.16f), lerp(AlbumWhite, hue, 0.15f))
}

@Composable
private fun AlbumCoverDepth(art: AlbumHeroArt?, surface: Color, height: Dp, scrollOffset: () -> Float) {
    Box(Modifier.fillMaxWidth().height(height).clipToBounds()) {
        if (art != null) {
            Image(
                bitmap = remember(art) { art.soft.asImageBitmap() },
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopCenter,
                modifier = Modifier.fillMaxSize().graphicsLayer {
                    scaleX = 1.12f
                    scaleY = 1.12f
                    translationY = scrollOffset() * 0.55f
                }
            )
            Image(
                bitmap = remember(art) { art.sharp.asImageBitmap() },
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopCenter,
                modifier = Modifier.fillMaxSize()
                    .graphicsLayer {
                        compositingStrategy = CompositingStrategy.Offscreen
                        translationY = scrollOffset()
                    }
                    .drawWithCache {
                        val focus = Brush.radialGradient(
                            0f to Color.Black,
                            0.59f to Color.Black,
                            1f to Color.Transparent,
                            center = Offset(size.width * 0.50f, size.height * 0.37f),
                            radius = size.width * 0.85f
                        )
                        onDrawWithContent {
                            drawContent()
                            drawRect(brush = focus, blendMode = BlendMode.DstIn)
                        }
                    }
            )
        }
        Box(
            Modifier.fillMaxSize().drawWithCache {
                val shade = Brush.verticalGradient(
                    0f to Color.Black.copy(alpha = 0.37f),
                    0.23f to Color.Transparent,
                    0.48f to Color.Transparent,
                    0.67f to surface.copy(alpha = 0.57f),
                    0.83f to surface.copy(alpha = 0.92f),
                    1f to surface
                )
                onDrawBehind { drawRect(shade) }
            }
        )
    }
}

@Composable
private fun AlbumIconButton(
    icon: ImageVector,
    label: String,
    enabled: Boolean = true,
    size: Dp = 48.dp,
    overCover: Boolean = false,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(17.dp)
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = shape,
        color = if (overCover) AlbumInk.copy(alpha = 0.72f) else Color.White.copy(alpha = 0.075f),
        contentColor = if (enabled) AlbumWhite else AlbumMuted.copy(alpha = 0.46f),
        modifier = Modifier.size(size).border(1.dp, AlbumRule, shape)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun AlbumTrack(
    song: Song,
    index: Int,
    albumArtist: String,
    currentTrack: Boolean,
    playing: Boolean,
    playingLabel: String,
    highlight: Color,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Column(modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 55.dp).clickable(onClick = onClick)
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.width(36.dp), contentAlignment = Alignment.CenterStart) {
                if (currentTrack && playing) {
                    Icon(Icons.Default.GraphicEq, playingLabel, tint = highlight, modifier = Modifier.size(20.dp))
                } else {
                    Text((index + 1).toString(), color = if (currentTrack) highlight else AlbumMuted, fontSize = 14.sp)
                }
            }
            Column(Modifier.weight(1f)) {
                Text(
                    song.title, color = if (currentTrack) highlight else AlbumWhite,
                    fontSize = 16.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium,
                    maxLines = 2, overflow = TextOverflow.Ellipsis
                )
                if (song.artist.isNotBlank() && !song.artist.equals(albumArtist, ignoreCase = true)) {
                    Text(song.artist, color = AlbumMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            if (song.duration > 0) {
                val seconds = song.duration / 1000
                Text(
                    "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}",
                    modifier = Modifier.padding(start = 14.dp, end = 2.dp),
                    color = AlbumMuted,
                    fontSize = 12.sp
                )
            }
        }
        HorizontalDivider(modifier = Modifier.padding(start = 36.dp), color = AlbumRule)
    }
}
