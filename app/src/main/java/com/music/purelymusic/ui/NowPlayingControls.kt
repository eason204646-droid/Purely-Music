// Copyright (c) 2026 eason204646. Licensed under Mulan PSL v2.
package com.music.purelymusic.ui

import android.database.ContentObserver
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
import coil.compose.AsyncImage
import com.music.purelymusic.R
import com.music.purelymusic.ui.utils.CoverDepthAccent
import com.music.purelymusic.ui.utils.CoverDepthArt
import com.music.purelymusic.viewmodel.PlayerViewModel
import java.util.Locale
import kotlin.math.roundToInt

private val PlayerSecondaryText = Color.White.copy(alpha = 0.76f)

@OptIn(UnstableApi::class)
@Composable
internal fun NowPlayingControls(
    viewModel: PlayerViewModel,
    art: CoverDepthArt?,
    compact: Boolean,
    showThumbnail: Boolean,
    lyricsSelected: Boolean,
    lyricsEnabled: Boolean,
    queueSelected: Boolean,
    onLyricsClick: () -> Unit,
    onQueueClick: () -> Unit,
    onNavigateToEqualizer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val chinese = viewModel.currentLanguage == "zh"
    val accent = art?.accent ?: CoverDepthAccent
    val controlGap = if (compact) 2.dp else 10.dp

    Column(modifier.padding(bottom = if (compact) 2.dp else 12.dp)) {
        TrackHeading(viewModel, showThumbnail, compact, accent, onNavigateToEqualizer)
        Spacer(Modifier.height(controlGap))
        PlaybackProgress(viewModel, compact, accent)
        Spacer(Modifier.height(controlGap))
        TransportControls(viewModel, compact, accent)
        Spacer(Modifier.height(controlGap))
        MediaVolumeControl(chinese, compact, accent)
        Spacer(Modifier.height(controlGap))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val toolColors = IconButtonDefaults.iconToggleButtonColors(
                contentColor = PlayerSecondaryText,
                checkedContentColor = accent,
                checkedContainerColor = Color.White.copy(alpha = 0.17f)
            )
            IconToggleButton(
                checked = lyricsSelected,
                onCheckedChange = { onLyricsClick() },
                enabled = lyricsEnabled,
                colors = toolColors.copy(
                    disabledContentColor = accent,
                    disabledContainerColor = Color.White.copy(alpha = 0.17f)
                )
            ) {
                Icon(Icons.Default.Subtitles, if (chinese) "歌词" else "Lyrics")
            }
            PlayModeControl(viewModel, accent)
            IconToggleButton(
                checked = queueSelected,
                onCheckedChange = { onQueueClick() },
                colors = toolColors
            ) {
                Icon(Icons.AutoMirrored.Filled.PlaylistPlay, if (chinese) "播放队列" else "Queue")
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun TrackHeading(
    viewModel: PlayerViewModel,
    showThumbnail: Boolean,
    compact: Boolean,
    accent: Color,
    onNavigateToEqualizer: () -> Unit
) {
    val song = viewModel.currentSong
    val chinese = viewModel.currentLanguage == "zh"

    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        if (showThumbnail) {
            AsyncImage(
                model = song?.coverUri ?: R.drawable.default_cover,
                error = coil.compose.rememberAsyncImagePainter(R.drawable.default_cover),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(if (compact) 42.dp else 48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.10f))
            )
            Spacer(Modifier.width(10.dp))
        }

        Column(Modifier.weight(1f).padding(end = 4.dp)) {
            Text(
                text = song?.title ?: viewModel.textUnknownTrack,
                color = Color.White,
                fontSize = if (compact || showThumbnail) 24.sp else 32.sp,
                lineHeight = if (compact || showThumbnail) 28.sp else 36.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song?.artist ?: viewModel.textUnknownArtist,
                color = PlayerSecondaryText,
                fontSize = if (compact) 14.sp else 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            AnimatedVisibility(
                visible = viewModel.isAutoMixTransitioning,
                enter = fadeIn(tween(180)),
                exit = fadeOut(tween(180))
            ) {
                Text(
                    text = if (chinese) "智能混音" else "Smart Mix",
                    color = PlayerSecondaryText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        val favorite = song?.isFavorite == true
        IconToggleButton(
            checked = favorite,
            onCheckedChange = { song?.let(viewModel::toggleFavorite) },
            enabled = song != null,
            colors = IconButtonDefaults.iconToggleButtonColors(
                contentColor = PlayerSecondaryText,
                checkedContentColor = accent
            )
        ) {
            Icon(
                imageVector = if (favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = if (chinese) "收藏" else "Favorite"
            )
        }
        if (viewModel.equalizerEnabled) {
            IconButton(onClick = onNavigateToEqualizer) {
                Icon(Icons.Default.Equalizer, if (chinese) "均衡器" else "Equalizer", tint = PlayerSecondaryText)
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun PlaybackProgress(viewModel: PlayerViewModel, compact: Boolean, accent: Color) {
    val duration = viewModel.duration.coerceAtLeast(0)
    val position = viewModel.currentPosition.coerceIn(0L, duration)
    var scrubPosition by remember(viewModel.currentSong?.id) { mutableStateOf<Float?>(null) }
    val displayed = (scrubPosition?.toLong() ?: position).coerceIn(0L, duration)
    val clockStyle = TextStyle(fontSize = 12.sp, fontFeatureSettings = "tnum")

    PlayerSlider(
        value = displayed.toFloat(),
        onValueChange = { scrubPosition = it },
        onValueChangeFinished = {
            scrubPosition?.let { viewModel.seekTo(it.coerceIn(0f, duration.toFloat())) }
            scrubPosition = null
        },
        range = 0f..duration.toFloat().coerceAtLeast(1f),
        label = if (viewModel.currentLanguage == "zh") "播放进度" else "Playback position",
        accent = accent,
        enabled = duration > 0,
        modifier = Modifier.fillMaxWidth().height(if (compact) 24.dp else 30.dp)
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(formatPlayerTime(displayed), color = PlayerSecondaryText, style = clockStyle)
        Text("−" + formatPlayerTime(duration - displayed), color = PlayerSecondaryText, style = clockStyle)
    }
}

private fun formatPlayerTime(milliseconds: Long): String {
    val seconds = milliseconds / 1000
    return String.format(Locale.getDefault(), "%d:%02d", seconds / 60, seconds % 60)
}

@OptIn(UnstableApi::class)
@Composable
private fun TransportControls(viewModel: PlayerViewModel, compact: Boolean, accent: Color) {
    val chinese = viewModel.currentLanguage == "zh"
    val playing = viewModel.isActuallyPlaying

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = viewModel::playPrevious, modifier = Modifier.size(52.dp)) {
            Icon(
                Icons.Default.SkipPrevious,
                if (chinese) "上一首" else "Previous track",
                modifier = Modifier.size(34.dp),
                tint = Color.White
            )
        }

        FilledIconButton(
            onClick = viewModel::togglePlayPause,
            modifier = Modifier.size(if (compact) 60.dp else 72.dp),
            shape = CircleShape,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = accent,
                contentColor = Color(0xFF141C20)
            )
        ) {
            Icon(
                if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                if (playing) {
                    if (chinese) "暂停" else "Pause"
                } else {
                    if (chinese) "播放" else "Play"
                },
                modifier = Modifier.size(if (compact) 36.dp else 42.dp)
            )
        }

        IconButton(onClick = viewModel::playNext, modifier = Modifier.size(52.dp)) {
            Icon(
                Icons.Default.SkipNext,
                if (chinese) "下一首" else "Next track",
                modifier = Modifier.size(34.dp),
                tint = Color.White
            )
        }
    }
}

@Composable
private fun PlayModeControl(viewModel: PlayerViewModel, accent: Color) {
    val chinese = viewModel.currentLanguage == "zh"
    val mode = viewModel.playMode
    val (icon, description) = when (mode) {
        PlayerViewModel.PlayMode.SEQUENTIAL -> Icons.Default.Repeat to if (chinese) "顺序播放" else "Sequential play"
        PlayerViewModel.PlayMode.REPEAT_ONE -> Icons.Default.RepeatOne to if (chinese) "单曲循环" else "Repeat one"
        PlayerViewModel.PlayMode.SHUFFLE -> Icons.Default.Shuffle to if (chinese) "随机播放" else "Shuffle"
    }

    FilledTonalIconButton(
        onClick = {
            viewModel.playMode = when (mode) {
                PlayerViewModel.PlayMode.SEQUENTIAL -> PlayerViewModel.PlayMode.REPEAT_ONE
                PlayerViewModel.PlayMode.REPEAT_ONE -> PlayerViewModel.PlayMode.SHUFFLE
                PlayerViewModel.PlayMode.SHUFFLE -> PlayerViewModel.PlayMode.SEQUENTIAL
            }
        },
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = Color.Transparent,
            contentColor = if (mode == PlayerViewModel.PlayMode.SEQUENTIAL) PlayerSecondaryText else accent
        )
    ) {
        Icon(icon, "${viewModel.textPlayMode} · $description")
    }
}

@Composable
private fun MediaVolumeControl(chinese: Boolean, compact: Boolean, accent: Color) {
    val context = LocalContext.current
    val audio = remember(context) { context.getSystemService(AudioManager::class.java) }
    if (audio == null || audio.isVolumeFixed) return

    val stream = AudioManager.STREAM_MUSIC
    val min = remember(audio) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) audio.getStreamMinVolume(stream) else 0
    }
    val max = remember(audio) { audio.getStreamMaxVolume(stream) }
    if (min >= max) return
    var volume by remember(audio) { mutableIntStateOf(audio.getStreamVolume(stream)) }

    DisposableEffect(context, audio) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                volume = audio.getStreamVolume(stream)
            }
        }
        context.contentResolver.registerContentObserver(Settings.System.CONTENT_URI, true, observer)
        onDispose { context.contentResolver.unregisterContentObserver(observer) }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.AutoMirrored.Filled.VolumeDown,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = PlayerSecondaryText
        )
        PlayerSlider(
            value = volume.coerceIn(min, max).toFloat(),
            range = min.toFloat()..max.toFloat(),
            onValueChange = {
                val level = it.roundToInt()
                if (level != volume) {
                    audio.setStreamVolume(stream, level, 0)
                    volume = audio.getStreamVolume(stream)
                }
            },
            label = if (chinese) "媒体音量" else "Media volume",
            accent = accent,
            steps = max - min - 1,
            modifier = Modifier.weight(1f).height(if (compact) 28.dp else 36.dp)
        )
        Icon(
            Icons.AutoMirrored.Filled.VolumeUp,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = PlayerSecondaryText
        )
    }
}

@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    range: ClosedFloatingPointRange<Float>,
    label: String,
    accent: Color,
    modifier: Modifier,
    enabled: Boolean = true,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null
) {
    val interaction = remember { MutableInteractionSource() }
    val dragged by interaction.collectIsDraggedAsState()
    val focused by interaction.collectIsFocusedAsState()
    Slider(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        valueRange = range,
        steps = steps,
        onValueChangeFinished = onValueChangeFinished,
        interactionSource = interaction,
        modifier = modifier.semantics { contentDescription = label },
        thumb = { Spacer(Modifier.width(0.dp).height(18.dp)) },
        track = { slider ->
            Box(Modifier.fillMaxWidth().height(if (dragged || focused) 8.dp else 5.dp).drawWithCache {
                val rounding = CornerRadius(size.height / 2)
                onDrawBehind {
                    drawRoundRect(Color.White.copy(alpha = if (enabled) 0.27f else 0.14f), cornerRadius = rounding)
                    val fraction = (slider.value - range.start) / (range.endInclusive - range.start)
                    val fillWidth = size.width * fraction.coerceIn(0f, 1f)
                    if (enabled && fillWidth > 0f) {
                        drawRoundRect(
                            color = accent,
                            topLeft = Offset(if (layoutDirection == LayoutDirection.Rtl) size.width - fillWidth else 0f, 0f),
                            size = Size(fillWidth, size.height),
                            cornerRadius = rounding
                        )
                    }
                }
            })
        }
    )
}
