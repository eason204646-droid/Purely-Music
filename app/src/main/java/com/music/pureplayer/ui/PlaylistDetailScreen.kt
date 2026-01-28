package com.music.PurelyPlayer.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.music.PurelyPlayer.R
import com.music.PurelyPlayer.model.Playlist
import com.music.PurelyPlayer.model.Song
import com.music.PurelyPlayer.viewmodel.PlayerViewModel

@Composable
fun PlaylistDetailScreen(
    playlist: Playlist,
    viewModel: PlayerViewModel,
    onBack: () -> Unit,
    onNavigateToPlayer: () -> Unit
) {
    // 获取当前歌单内的所有歌曲对象
    val playlistSongs = remember(playlist.songIds, viewModel.libraryList) {
        val idSet = playlist.songIds.map { it.toLong() }.toSet() // 统一转为 Long
        viewModel.libraryList.filter { song ->
            idSet.contains(song.id.toLong()) // 确保两边都是 Long
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            // 1. 顶部大封面区
            item {
                Box(modifier = Modifier.fillMaxWidth().height(360.dp)) {
                    AsyncImage(
                        model = playlist.coverUri ?: R.drawable.default_cover,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    // 渐变层，为了让标题更清晰
                    Box(modifier = Modifier.fillMaxSize().background(
                        Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)))
                    ))
                    // 返回按钮
                    IconButton(onClick = onBack, modifier = Modifier.padding(top = 12.dp, start = 8.dp)) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回", tint = Color.White)
                    }
                    // 歌单标题
                    Text(
                        text = playlist.name,
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.align(Alignment.BottomStart).padding(20.dp)
                    )
                }
            }

            // 2. 播放控制按钮
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { viewModel.playPlaylist(playlist, isRandom = false); onNavigateToPlayer() },
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE53935), // 主题色：标准红 (Red 600)
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Default.PlayArrow, null)
                        Spacer(Modifier.width(8.dp))
                        Text("顺序播放")
                    }
                    OutlinedButton(
                        onClick = { viewModel.playPlaylist(playlist, isRandom = true); onNavigateToPlayer() },
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFE53935)), // 主题红边框
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFE53935)            // 主题红文字
                        )
                    ) {
                        Icon(Icons.Default.Shuffle, null)
                        Spacer(Modifier.width(8.dp))
                        Text("随机播放")
                    }
                }
            }

            // 3. 歌曲列表项 (复用之前我们写过的 SongItem 逻辑)
            items(playlistSongs) { song ->
                SongItem(song = song, onClick = {
                    viewModel.playSong(song, updateInternalList = false) // 播放并保持歌单队列
                    onNavigateToPlayer()
                })
            }
        }
    }
}