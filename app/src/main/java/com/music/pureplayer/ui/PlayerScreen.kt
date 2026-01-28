package com.music.PurelyPlayer.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.music.PurelyPlayer.R
import com.music.PurelyPlayer.viewmodel.PlayerViewModel

@Composable
fun PlayerScreen(viewModel: PlayerViewModel, onBack: () -> Unit) {
    val currentSong = viewModel.currentSong
    val isPlaying = viewModel.isPlaying

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                // 使用 awaitEachGesture 确保每次进入页面及每次触摸都能正确触发逻辑
                awaitEachGesture {
                    // 等待第一次按下
                    val down = awaitFirstDown()
                    val startY = down.position.y

                    // 🚩 限制起始点在屏幕上半部分
                    if (startY < size.height / 2) {
                        var hasTriggered = false

                        while (true) {
                            val event = awaitPointerEvent()
                            val dragAmount = event.changes.firstOrNull()?.let { it.position - it.previousPosition } ?: androidx.compose.ui.geometry.Offset.Zero

                            // 累计向下位移判断
                            val currentY = event.changes.firstOrNull()?.position?.y ?: startY
                            val totalDragY = currentY - startY

                            // 🚩 向下滑动超过阈值 (150f) 且未触发过时执行返回
                            if (totalDragY > 150f && !hasTriggered) {
                                hasTriggered = true
                                event.changes.forEach { it.consume() }
                                onBack()
                                // 注意：这里不需要 break，让 gesture 完成生命周期防止残留
                            }

                            // 如果所有手指抬起，结束本次手势监听
                            if (event.changes.all { !it.pressed }) break
                        }
                    }
                }
            }
    ) {
        // 1. 背景层
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

        // 2. 遮罩层
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)))

        // --- 内容层 (层级 3) ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. 顶部栏 (固定高度)
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.KeyboardArrowDown, null, tint = Color.White, modifier = Modifier.size(32.dp))
                }
                Text("正在播放", modifier = Modifier.weight(1f), textAlign = TextAlign.Center, color = Color.White)
                Spacer(modifier = Modifier.width(48.dp))
            }

            // 2. 封面区 (使用 weight 动态收缩)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier
                        .sizeIn(maxWidth = 300.dp, maxHeight = 300.dp)
                        .aspectRatio(1f),
                    shape = RoundedCornerShape(16.dp),
                    shadowElevation = 12.dp
                ) {
                    AsyncImage(
                        model = currentSong?.coverUri ?: R.drawable.default_cover,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            // 3. 歌曲信息 (固定空间)
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = currentSong?.title ?: "未知曲目",
                    color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = currentSong?.artist ?: "未知艺术家",
                    color = Color.White.copy(alpha = 0.7f), fontSize = 18.sp
                )
            }

            // 4. 进度条 (固定空间)
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp)) {
                Slider(
                    value = viewModel.currentPosition.toFloat(),
                    onValueChange = { viewModel.seekTo(it) },
                    valueRange = 0f..(viewModel.duration.toFloat().coerceAtLeast(1f)),
                    colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.White)
                )
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(formatTime(viewModel.currentPosition), color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                    Spacer(modifier = Modifier.weight(1f))
                    Text(formatTime(viewModel.duration), color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                }
            }

            // 5. 控制按键区 (固定在底部)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { /* 模式 */ }) {
                    Icon(Icons.Default.Refresh, null, tint = Color.White.copy(alpha = 0.6f))
                }

                // 上一曲
                IconButton(onClick = { viewModel.playPrevious() }, modifier = Modifier.size(56.dp)) {
                    Icon(Icons.Default.SkipPrevious, null, tint = Color.White, modifier = Modifier.size(40.dp))
                }

                // 播放暂停
                Surface(
                    onClick = { viewModel.togglePlayPause() },
                    shape = CircleShape,
                    color = Color.White,
                    modifier = Modifier.size(72.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                // 下一曲
                IconButton(onClick = { viewModel.playNext() }, modifier = Modifier.size(56.dp)) {
                    Icon(Icons.Default.SkipNext, null, tint = Color.White, modifier = Modifier.size(40.dp))
                }

                IconButton(onClick = { /* 收藏 */ }) {
                    Icon(Icons.Default.FavoriteBorder, null, tint = Color.White.copy(alpha = 0.6f))
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}