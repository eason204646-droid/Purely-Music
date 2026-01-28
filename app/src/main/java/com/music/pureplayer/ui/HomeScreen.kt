package com.music.PurelyPlayer.ui // 1. 确保包名一致

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PauseCircleFilled
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.music.PurelyPlayer.model.Song       // 2. 这里的 model 路径也要改
import com.music.PurelyPlayer.viewmodel.PlayerViewModel // 3. 这里的 viewmodel 路径也要改
import com.music.PurelyPlayer.R                // 4. 资源文件引用

@Composable
fun HomeScreen(viewModel: PlayerViewModel, onNavigateToPlayer: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Text(
            text = "主页",
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.Black,
            modifier = Modifier.padding(start = 20.dp, top = 24.dp, bottom = 12.dp)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            // 🚩 接入点：最近播放栏 (按顺序横向显示)
            if (viewModel.recentSongs.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.padding(vertical = 10.dp)) {
                        Text(
                            text = "最近播放",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            modifier = Modifier.padding(start = 20.dp, bottom = 12.dp)
                        )

                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(viewModel.recentSongs) { song ->
                                RecentSongItem(song = song, onClick = {
                                    viewModel.playSong(song)
                                    onNavigateToPlayer()
                                })
                            }
                        }
                    }
                }
            }

            // 所有歌曲标题
            item {
                Text(
                    text = "所有歌曲",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 12.dp)
                )
            }

            // 所有歌曲列表
            items(viewModel.libraryList) { song ->
                SongItem(song = song, onClick = { viewModel.playSong(song) })
            }
        }
    }
}

// 横向卡片组件
@Composable
fun RecentSongItem(song: Song, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(140.dp)
            .clickable { onClick() }
    ) {
        AsyncImage(
            model = song.coverUri ?: R.drawable.default_cover,
            contentDescription = null,
            modifier = Modifier
                .size(140.dp)
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = song.title,
            color = Color.Black,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = song.artist,
            color = Color.Gray,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// 纵向列表项组件
@Composable
fun SongItem(song: Song, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = song.coverUri ?: R.drawable.default_cover,
            contentDescription = null,
            modifier = Modifier.size(52.dp).clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )
        Column(modifier = Modifier.padding(start = 16.dp).weight(1f)) {
            Text(song.title, color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(song.artist, color = Color.Gray, fontSize = 13.sp, maxLines = 1)
        }
    }
}

// MiniPlayer 组件 (由 MainActivity 调用)
@Composable
fun MiniPlayer(viewModel: PlayerViewModel, onClick: () -> Unit) {
    val currentSong = viewModel.currentSong ?: return
    Surface(
        modifier = Modifier.fillMaxWidth().height(72.dp).padding(horizontal = 12.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 12.dp
    ) {
        Row(
            modifier = Modifier.fillMaxSize().clickable { onClick() }.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = currentSong.coverUri ?: R.drawable.default_cover,
                contentDescription = null,
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                Text(currentSong.title, color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
                Text(currentSong.artist, color = Color.Gray, fontSize = 12.sp, maxLines = 1)
            }
            IconButton(onClick = { viewModel.togglePlayPause() }) {
                Icon(
                    imageVector = if (viewModel.isPlaying) Icons.Default.PauseCircleFilled else Icons.Default.PlayCircleFilled,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(40.dp)
                )
            }
        }
    }
}