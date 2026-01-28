package com.music.PurelyPlayer.ui


import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.music.PurelyPlayer.R
import com.music.PurelyPlayer.viewmodel.PlayerViewModel

@Composable
fun PlayerDetailScreen(viewModel: PlayerViewModel, onBack: () -> Unit) {
    // 获取当前播放的歌曲，如果没有则直接返回（防崩溃）
    val song = viewModel.currentSong ?: return

    Box(modifier = Modifier.fillMaxSize()) {
        // --- 1. 背景层：高斯模糊 (参考图三沉浸感) ---
        AsyncImage(
            model = song.coverUri ?: R.drawable.default_cover, // 这里的逻辑是：如果 coverUri 有值就用，没有才用默认图
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            // 关键：增加这两行来强制刷新图片
            error = painterResource(R.drawable.default_cover),
            placeholder = painterResource(R.drawable.default_cover)
        )


        // 黑色半透明遮罩，确保白色文字清晰
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f))
        )

        // --- 2. 内容层 ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 顶部下拉关闭条 (参考图三最上方)
            Box(
                modifier = Modifier
                    .padding(vertical = 20.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.4f))
                    .clickable { onBack() }
            )

            Spacer(modifier = Modifier.weight(0.6f))

            // 主封面卡片 (参考图三：大圆角 + 深度阴影)
            Card(
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 30.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            ) {
                AsyncImage(
                    model = song.coverUri ?: R.drawable.default_cover,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    error = painterResource(R.drawable.default_cover)
                )
            }

            Spacer(modifier = Modifier.weight(0.8f))

            // 歌曲信息栏 (参考图三：左对齐歌曲名，右侧星标)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1
                    )
                    Text(
                        text = song.artist,
                        fontSize = 18.sp,
                        color = Color.White.copy(alpha = 0.6f),
                        maxLines = 1
                    )
                }
                // 图三中的五角星收藏按钮
                Icon(
                    imageVector = Icons.Default.StarBorder,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 进度条 (参考图三：纯白简约风格)
            Slider(
                value = 0.3f, // 后续需对接 viewModel 中的播放进度
                onValueChange = { /* 拖动进度逻辑 */ },
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.White,
                    inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                )
            )

            // 播放控制按钮 (参考图三：极简线性图标)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.playPrevious() }) {
                    Icon(
                        Icons.Default.SkipPrevious,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(45.dp)
                    )
                }

                // 播放/暂停大按钮
                IconButton(
                    onClick = { viewModel.togglePlayPause() },
                    modifier = Modifier.size(85.dp)
                ) {
                    Icon(
                        imageVector = if (viewModel.isPlaying) Icons.Default.PauseCircleFilled else Icons.Default.PlayCircleFilled,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                IconButton(onClick = { viewModel.playNext() }) {
                    Icon(
                        Icons.Default.SkipNext,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(45.dp)
                    )
                }
            }

            // 底部音量调节栏 (参考图三最下方)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 40.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.VolumeDown, null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
                Slider(
                    value = 0.5f,
                    onValueChange = { /* 音量调节 */ },
                    modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = Color.Transparent, // 隐藏滑块小圆点更显高级
                        activeTrackColor = Color.White.copy(alpha = 0.8f),
                        inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                    )
                )
                Icon(Icons.Default.VolumeUp, null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
            }
        }
    }
}