//Copyright (c) [2026] [eason204646]
//[purelymusic] is licensed under Mulan PSL v2.
//You can use this software according to the terms and conditions of the Mulan
//PSL v2.
//You may obtain a copy of Mulan PSL v2 at:
//         http://license.coscl.org.cn/MulanPSL2
//THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY
//KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
//NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
//See the Mulan PSL v2 for more details.
//
//Mulan Permissive Software License，Version 2
//
//Mulan Permissive Software License，Version 2 (Mulan PSL v2)
//
//January 2020 http://license.coscl.org.cn/MulanPSL2
package com.music.purelymusic.ui

import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
import coil.compose.AsyncImage
import com.music.purelymusic.R
import com.music.purelymusic.ui.utils.AppDimensions
import com.music.purelymusic.ui.utils.CoverDepthArt
import com.music.purelymusic.ui.utils.CoverDepthSurface
import com.music.purelymusic.ui.utils.buildCoverDepthArt
import com.music.purelymusic.ui.utils.loadAlbumBitmap
import com.music.purelymusic.viewmodel.PlayerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private enum class PlayerPane { Cover, Lyrics, Queue }

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel,
    onBack: () -> Unit,
    onNavigateToEqualizer: () -> Unit
) {
    var showLyrics by rememberSaveable { mutableStateOf(false) }
    val showQueue = viewModel.showPlaylist
    val context = LocalContext.current
    val cover = viewModel.currentSong?.coverUri
    val art by produceState<CoverDepthArt?>(null, context, cover) {
        val bitmap = cover?.let { loadAlbumBitmap(context, it) }
            ?: loadAlbumBitmap(context, R.drawable.default_cover)
        value = bitmap?.let { withContext(Dispatchers.Default) { buildCoverDepthArt(it) } }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 760.dp || (maxWidth >= 560.dp && maxWidth > maxHeight)
        val compact = maxHeight < (if (wide) 500.dp else 700.dp)
        val pane = when {
            showQueue -> PlayerPane.Queue
            wide || showLyrics -> PlayerPane.Lyrics
            else -> PlayerPane.Cover
        }
        val sidePadding = if (maxWidth < 360.dp) 20.dp else 28.dp

        BackHandler(enabled = showQueue || (!wide && showLyrics)) {
            if (showQueue) viewModel.showPlaylist = false else showLyrics = false
        }

        PlayerCoverBackdrop(
            art = art,
            wide = wide,
            detailsOpen = !wide && pane != PlayerPane.Cover,
            modifier = Modifier.matchParentSize()
        )

        val controls: @Composable () -> Unit = {
            NowPlayingControls(
                viewModel = viewModel,
                art = art,
                compact = compact,
                showThumbnail = !wide && pane != PlayerPane.Cover,
                lyricsSelected = pane == PlayerPane.Lyrics,
                lyricsEnabled = !wide || showQueue,
                queueSelected = showQueue,
                onLyricsClick = {
                    if (showQueue) {
                        viewModel.showPlaylist = false
                        showLyrics = true
                    } else if (!wide) {
                        showLyrics = !showLyrics
                    }
                },
                onQueueClick = { viewModel.showPlaylist = !showQueue },
                onNavigateToEqualizer = onNavigateToEqualizer,
                modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth()
            )
        }

        Column(
            modifier = Modifier.fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = sidePadding)
        ) {
            NowPlayingHeader(viewModel, onBack)

            if (wide) {
                Row(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(sidePadding + 8.dp)
                ) {
                    Column(
                        modifier = Modifier.weight(0.44f).fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(Modifier.weight(1f))
                        controls()
                    }
                    PlayerDetails(
                        pane = pane,
                        viewModel = viewModel,
                        modifier = Modifier.weight(0.56f).fillMaxHeight()
                    )
                }
            } else {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    if (pane == PlayerPane.Cover) {
                        val collapse by rememberUpdatedState(onBack)
                        val label = if (viewModel.currentLanguage == "zh") "显示歌词" else "Show lyrics"
                        Box(
                            Modifier.fillMaxSize()
                                .pointerInput(Unit) {
                                    var drag = 0f
                                    detectVerticalDragGestures(
                                        onDragStart = { drag = 0f },
                                        onVerticalDrag = { _, amount -> drag += amount },
                                        onDragEnd = { if (drag > 64.dp.toPx()) collapse() }
                                    )
                                }
                                .clickable(
                                    interactionSource = null,
                                    indication = null,
                                    role = Role.Button,
                                    onClickLabel = label,
                                    onClick = { showLyrics = true }
                                )
                                .semantics { contentDescription = label }
                        )
                    }
                    PlayerDetails(
                        pane = pane,
                        viewModel = viewModel,
                        modifier = Modifier.widthIn(max = 560.dp).fillMaxSize()
                    )
                }

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    controls()
                }
            }
        }
    }
}

@Composable
private fun NowPlayingHeader(viewModel: PlayerViewModel, onBack: () -> Unit) {
    val chinese = viewModel.currentLanguage == "zh"
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilledTonalIconButton(
            onClick = onBack,
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = Color.Black.copy(alpha = 0.28f),
                contentColor = Color.White
            )
        ) {
            Icon(Icons.Default.ExpandMore, if (chinese) "收起播放器" else "Collapse player")
        }
        Spacer(Modifier.width(6.dp))
        Text(
            text = viewModel.textNowPlaying,
            color = Color.White.copy(alpha = 0.90f),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            style = TextStyle(shadow = Shadow(Color.Black.copy(alpha = 0.8f), blurRadius = 9f)),
            modifier = Modifier.weight(1f)
        )
        if (viewModel.sleepTimerActive && viewModel.sleepTimerDisplay.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.28f), RoundedCornerShape(24.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Timer,
                    contentDescription = if (chinese) "睡眠定时器" else "Sleep timer",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(viewModel.sleepTimerDisplay, color = Color.White, fontSize = 12.sp)
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun PlayerDetails(pane: PlayerPane, viewModel: PlayerViewModel, modifier: Modifier) {
    Crossfade(
        targetState = pane,
        modifier = modifier,
        animationSpec = tween(240),
        label = "Player details"
    ) { active ->
        when (active) {
            PlayerPane.Cover -> Spacer(Modifier.fillMaxSize())
            PlayerPane.Lyrics -> LyricView(viewModel, Modifier.fillMaxSize())
            PlayerPane.Queue -> PlaylistView(viewModel, Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun PlayerCoverBackdrop(
    art: CoverDepthArt?,
    wide: Boolean,
    detailsOpen: Boolean,
    modifier: Modifier = Modifier
) {
    val detailsShade = animateFloatAsState(
        targetValue = if (detailsOpen) 0.73f else 0f,
        animationSpec = tween(260),
        label = "Player details shade"
    )

    Crossfade(
        targetState = art,
        modifier = modifier.background(CoverDepthSurface),
        animationSpec = tween(360),
        label = "Player artwork"
    ) { artwork ->
        val surface = artwork?.surface ?: CoverDepthSurface
        BoxWithConstraints(Modifier.fillMaxSize().background(surface)) {
            if (artwork != null) {
                Image(
                    bitmap = artwork.soft,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().graphicsLayer { alpha = 0.28f }
                )

                val artworkWidth = if (wide) maxWidth * 0.60f else maxWidth
                val artworkHeight = if (wide) maxHeight else
                    (maxWidth * 1.22f).coerceIn(maxHeight * 0.57f, maxHeight * 0.70f)
                Image(
                    bitmap = artwork.sharp,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.TopCenter,
                    modifier = Modifier
                        .size(artworkWidth, artworkHeight)
                        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                        .drawWithCache {
                            val verticalFade = Brush.verticalGradient(
                                0f to Color.Black,
                                (if (wide) 0.28f else 0.62f) to Color.Black,
                                (if (wide) 0.68f else 0.83f) to Color.Black.copy(alpha = 0.50f),
                                1f to Color.Transparent
                            )
                            val sideFade = Brush.horizontalGradient(
                                0.58f to Color.Black,
                                1f to Color.Transparent
                            )
                            onDrawWithContent {
                                drawContent()
                                drawRect(verticalFade, blendMode = BlendMode.DstIn)
                                if (wide) drawRect(sideFade, blendMode = BlendMode.DstIn)
                            }
                        }
                )
            }

            Box(Modifier.matchParentSize().drawWithCache {
                val scrim = if (wide) {
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.42f),
                        0.18f to surface.copy(alpha = 0.21f),
                        0.40f to surface.copy(alpha = 0.63f),
                        0.74f to surface.copy(alpha = 0.86f),
                        1f to surface
                    )
                } else {
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.46f),
                        0.15f to Color.Black.copy(alpha = 0.07f),
                        0.40f to Color.Transparent,
                        0.66f to surface.copy(alpha = 0.30f),
                        0.86f to surface.copy(alpha = 0.76f),
                        1f to surface
                    )
                }
                val lyricsScrim = Brush.horizontalGradient(
                    0.42f to Color.Transparent,
                    0.65f to surface.copy(alpha = 0.80f),
                    1f to surface.copy(alpha = 0.90f)
                )
                onDrawBehind {
                    drawRect(scrim)
                    if (wide) drawRect(lyricsScrim)
                    val shade = detailsShade.value
                    if (shade > 0f) drawRect(surface.copy(alpha = shade))
                }
            })
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun PlaylistView(viewModel: PlayerViewModel, modifier: Modifier = Modifier) {
    val playingQueue = viewModel.getPlayingQueue()
    val currentSong = viewModel.currentSong
    val chinese = viewModel.currentLanguage == "zh"

    Column(modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (chinese) "播放队列" else "Queue",
                color = Color.White,
                fontSize = 21.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            if (playingQueue.isNotEmpty()) {
                TextButton(onClick = { viewModel.clearPlayingList() }) {
                    Text(
                        if (chinese) "清空队列" else "Clear queue",
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }
            IconButton(
                onClick = { viewModel.showPlaylist = false },
                modifier = Modifier.size(AppDimensions.iconButtonSizeS())
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = viewModel.textClose,
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.size(AppDimensions.iconM())
                )
            }
        }

        if (playingQueue.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = viewModel.textQueueEmpty,
                    color = Color.White.copy(alpha = 0.72f),
                    fontSize = 15.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) {
                items(playingQueue) { song ->
                    PlaylistItem(
                        song = song,
                        isPlaying = currentSong?.id == song.id,
                        onClick = { viewModel.jumpToSong(song) },
                        onLongClick = { viewModel.removeSongFromPlayingList(song) },
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

@kotlin.OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlaylistItem(
    song: com.music.purelymusic.model.Song,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    viewModel: PlayerViewModel
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { expanded = true }
                )
                .padding(vertical = AppDimensions.paddingSmall(), horizontal = AppDimensions.paddingCard()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = song.coverUri ?: R.drawable.default_cover,
                contentDescription = null,
                modifier = Modifier
                    .size(AppDimensions.coverS())
                    .clip(RoundedCornerShape(AppDimensions.cornerRadiusS())),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(AppDimensions.spacingM()))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    color = Color.White,
                    fontSize = AppDimensions.textM().value.sp,
                    fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(AppDimensions.spacingXS()))
                Text(
                    text = song.artist,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = AppDimensions.textS().value.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (isPlaying) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = viewModel.textNowPlaying,
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.size(AppDimensions.iconS())
                )
            }
        }
        HorizontalDivider(
            color = Color.White.copy(alpha = 0.14f),
            modifier = Modifier.padding(horizontal = AppDimensions.paddingCard())
        )

        GlassDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            GlassMenuItem(
                label = viewModel.textDelete,
                icon = Icons.Default.Delete,
                destructive = true,
                onClick = {
                    onLongClick()
                    expanded = false
                }
            )
        }
    }
}
