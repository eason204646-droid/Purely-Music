//Copyright (c) [2026] [eason204646]
//[purelymusic] is licensed under Mulan PSL v2.
//You can use this software according to the terms and conditions of the Mulan
//PSL v2.
//You may obtain a copy ，of Mulan PSL v2 at:
//         http://license.coscl.org.cn/MulanPSL2
//THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY
//KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
//NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
//See the Mulan PSL v2 for more details.
//
//Mulan Permissive Software License，Version 2
//
//Mulan Permissive Software License，Version 2
//
//January 2020 http://license.coscl.org.cn/MulanPSL2
package com.music.purelymusic.ui

import androidx.annotation.OptIn
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
import coil.compose.AsyncImage
import com.music.purelymusic.R
import com.music.purelymusic.viewmodel.PlayerViewModel
import com.music.purelymusic.ui.utils.AppDimensions

@OptIn(UnstableApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(viewModel: PlayerViewModel, onBack: () -> Unit) {
    val currentSong = viewModel.currentSong
    val isPlaying = viewModel.isPlaying
    var showLyrics by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A1A1A),
                        Color(0xFF0D0D0D),
                        Color(0xFF000000)
                    )
                )
            )
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    val startY = down.position.y
                    if (startY < size.height / 2) {
                        var hasTriggered = false
                        while (true) {
                            val event = awaitPointerEvent()
                            val currentY = event.changes.firstOrNull()?.position?.y ?: startY
                            val totalDragY = currentY - startY
                            if (totalDragY > 150f && !hasTriggered) {
                                hasTriggered = true
                                event.changes.forEach { it.consume() }
                                onBack()
                            }
                            if (event.changes.all { !it.pressed }) break
                        }
                    }
                }
            }
    ) {
        // --- 背景层 (Crossfade 模糊图) ---
        Crossfade(targetState = viewModel.blurredBackground, label = "Blur") { bitmap ->
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(modifier = Modifier.fillMaxSize().background(Color(0xFF1A1A1A)))
            }
        }

        // --- 遮罩层 ---
        Box(modifier = Modifier.fillMaxSize().background(
            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                colors = listOf(Color.Black.copy(alpha = 0.4f), Color.Black.copy(alpha = 0.85f))
            )
        ))

        // --- 内容层 ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 顶部提示条
            Box(modifier = Modifier.fillMaxWidth().padding(top = AppDimensions.spacingXS()), contentAlignment = Alignment.Center) {
                Box(modifier = Modifier.width(AppDimensions.iconM()).height(AppDimensions.spacingXS()).background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(AppDimensions.spacingXS())))
            }

            // 1. 歌词/封面显示区域 (保持较大权重)
            Box(
                modifier = Modifier
                    .weight(3.0f)
                    .fillMaxWidth()
                    .padding(horizontal = AppDimensions.playerCoverPadding()),
                contentAlignment = Alignment.Center
            ) {
                Crossfade(targetState = viewModel.showPlaylist, label = "ContentSwitch") { showPlaylist ->
                    if (showPlaylist) {
                        PlaylistView(viewModel = viewModel, modifier = Modifier.fillMaxSize())
                    } else {
                        Crossfade(targetState = showLyrics, label = "LyricSwitch") { isLyrics ->
                            if (isLyrics) {
                                LyricView(viewModel = viewModel, modifier = Modifier.fillMaxSize())
                            } else {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(0.8f).aspectRatio(1f).clickable { showLyrics = true },
                                    shape = RoundedCornerShape(AppDimensions.cornerRadiusL()),
                                    shadowElevation = AppDimensions.elevationL()
                                ) {
                                    AsyncImage(
                                        model = currentSong?.coverUri ?: R.drawable.default_cover,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. 歌曲信息区域 + 模式切换按钮
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = AppDimensions.playerCoverPadding(), vertical = AppDimensions.spacingS()),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = currentSong?.title ?: "未知曲目", color = Color.White, fontSize = AppDimensions.textXXL().value.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(text = currentSong?.artist ?: "未知艺术家", color = Color.White.copy(alpha = 0.8f), fontSize = AppDimensions.textM().value.sp)
                }

                IconButton(
                    onClick = {
                        if (viewModel.showPlaylist) {
                            viewModel.showPlaylist = false
                            showLyrics = true
                        } else {
                            showLyrics = !showLyrics
                        }
                    },
                    modifier = Modifier.size(AppDimensions.iconButtonSizeS())
                ) {
                    Icon(
                        imageVector = if (showLyrics) Icons.Default.Album else Icons.Default.Notes,
                        contentDescription = "模式切换",
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(AppDimensions.iconM())
                    )
                }
            }

            // 3. 进度条区域
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = AppDimensions.playerCoverPadding())) {
                Slider(
                    value = viewModel.currentPosition.toFloat(),
                    onValueChange = { viewModel.seekTo(it) },
                    valueRange = 0f..(viewModel.duration.toFloat().coerceAtLeast(1f)),
                    colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.White, inactiveTrackColor = Color.White.copy(alpha = 0.3f))
                )
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(formatTime(viewModel.currentPosition), color = Color.White.copy(alpha = 0.6f), fontSize = AppDimensions.textS().value.sp)
                    Spacer(modifier = Modifier.weight(1f))
                    Text(formatTime(viewModel.duration), color = Color.White.copy(alpha = 0.6f), fontSize = AppDimensions.textS().value.sp)
                }
            }

            // 4. 底部控制区
            Box(
                modifier = Modifier
                    .weight(1.5f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = AppDimensions.paddingScreen()),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            viewModel.playMode = when (viewModel.playMode) {
                                PlayerViewModel.PlayMode.SEQUENTIAL -> PlayerViewModel.PlayMode.REPEAT_ONE
                                PlayerViewModel.PlayMode.REPEAT_ONE -> PlayerViewModel.PlayMode.SEQUENTIAL
                            }
                        },
                        modifier = Modifier.size(AppDimensions.iconButtonSizeM())
                    ) {
                        Icon(
                            imageVector = if (viewModel.playMode == PlayerViewModel.PlayMode.REPEAT_ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                            contentDescription = "播放模式",
                            tint = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.size(AppDimensions.iconL())
                        )
                    }

                    IconButton(onClick = { viewModel.playPrevious() }, modifier = Modifier.size(AppDimensions.iconButtonSizeXL())) {
                        Icon(Icons.Default.SkipPrevious, null, tint = Color.White, modifier = Modifier.size(AppDimensions.playerControlIconSize()))
                    }

                    Surface(
                        onClick = { viewModel.togglePlayPause() },
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.15f),
                        modifier = Modifier.size(AppDimensions.playerPlayButtonSize())
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(AppDimensions.playerPlayIconSize())
                            )
                        }
                    }

                    IconButton(onClick = { viewModel.playNext() }, modifier = Modifier.size(AppDimensions.iconButtonSizeXL())) {
                        Icon(Icons.Default.SkipNext, null, tint = Color.White, modifier = Modifier.size(AppDimensions.playerControlIconSize()))
                    }

                    IconButton(onClick = { viewModel.showPlaylist = !viewModel.showPlaylist }, modifier = Modifier.size(AppDimensions.iconButtonSizeM())) {
                        Icon(Icons.Default.PlaylistPlay, null, tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(AppDimensions.iconL()))
                    }
                }
            }

            Spacer(modifier = Modifier.height(AppDimensions.spacingL()))
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return java.lang.String.format(java.util.Locale.getDefault(), "%02d:%02d", minutes, seconds)
}

@OptIn(UnstableApi::class)
@Composable
fun PlaylistView(viewModel: PlayerViewModel, modifier: Modifier = Modifier) {
    val playingQueue = viewModel.getPlayingQueue()
    val currentSong = viewModel.currentSong

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(AppDimensions.paddingCard())
    ) {
        // 关闭按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(
                onClick = { viewModel.showPlaylist = false },
                modifier = Modifier.size(AppDimensions.iconButtonSizeS())
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "关闭",
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.size(AppDimensions.iconM())
                )
            }
        }

        Spacer(modifier = Modifier.height(AppDimensions.spacingM()))

        if (playingQueue.isEmpty()) {

                        Box(

                            modifier = Modifier.fillMaxWidth().weight(1f),

                            contentAlignment = Alignment.Center

                        ) {

                            Text(

                                text = "播放清单为空",

                                color = Color.White.copy(alpha = 0.6f),

                                fontSize = AppDimensions.textM().value.sp

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
                        onLongClick = { viewModel.removeSongFromPlayingList(song) }
                    )
                }
            }
        }
    }
}

@Composable
fun PlaylistItem(
    song: com.music.purelymusic.model.Song,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
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
                    imageVector = Icons.Default.VolumeUp,
                    contentDescription = "正在播放",
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.size(AppDimensions.iconS())
                )
            }
        }
        androidx.compose.material3.Divider(
            color = Color(0xFFE0E0E0),
            thickness = 1.dp,
            modifier = Modifier.padding(horizontal = AppDimensions.paddingCard())
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Color.Black.copy(alpha = 0.9f))
        ) {
            DropdownMenuItem(
                text = { Text("删除", color = Color.White) },
                leadingIcon = {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        tint = Color.White
                    )
                },
                onClick = {
                    onLongClick()
                    expanded = false
                }
            )
        }
    }
}