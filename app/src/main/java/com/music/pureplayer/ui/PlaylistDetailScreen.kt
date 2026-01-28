package com.music.PurelyPlayer.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    playlist: Playlist,
    viewModel: PlayerViewModel,
    onBack: () -> Unit,
    onNavigateToPlayer: () -> Unit,
    // 🚩 关键修改：参数类型由 Long 改为 String
    onNavigateToEditPlaylist: (String) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    // 🚩 修复 ID 匹配逻辑：全部使用 toString() 比较，确保 UUID 字符串也能正常匹配
    val playlistSongs = remember(playlist.songIds, viewModel.libraryList) {
        val idSet = playlist.songIds.map { it.toString() }.toSet()
        viewModel.libraryList.filter { song ->
            idSet.contains(song.id.toString())
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            item {
                Box(modifier = Modifier.fillMaxWidth().height(360.dp)) {
                    AsyncImage(
                        model = playlist.coverUri ?: R.drawable.default_cover,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(modifier = Modifier.fillMaxSize().background(
                        Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)))
                    ))

                    // 返回按钮
                    IconButton(onClick = onBack, modifier = Modifier.padding(top = 12.dp, start = 8.dp)) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回", tint = Color.White)
                    }

                    // 更多操作按钮
                    Box(modifier = Modifier.align(Alignment.TopEnd).padding(top = 12.dp, end = 8.dp)) {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "更多",
                                tint = Color.White
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier.background(Color.White)
                        ) {
                            DropdownMenuItem(
                                text = { Text("修改歌单", color = Color.Black) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = null,
                                        tint = Color(0xFFE53935)
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    // 🚩 核心修复点：直接传递字符串 ID，绝对不要调用 .toLong()
                                    onNavigateToEditPlaylist(playlist.id.toString())
                                }
                            )
                        }
                    }

                    Text(
                        text = playlist.name,
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.align(Alignment.BottomStart).padding(20.dp)
                    )
                }
            }

            // 控制按钮保持原样
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { viewModel.playPlaylist(playlist, isRandom = false); onNavigateToPlayer() },
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                    ) {
                        Icon(Icons.Default.PlayArrow, null)
                        Spacer(Modifier.width(8.dp))
                        Text("顺序播放")
                    }
                    OutlinedButton(
                        onClick = { viewModel.playPlaylist(playlist, isRandom = true); onNavigateToPlayer() },
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFE53935)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE53935))
                    ) {
                        Icon(Icons.Default.Shuffle, null)
                        Spacer(Modifier.width(8.dp))
                        Text("随机播放")
                    }
                }
            }

            items(playlistSongs) { song ->
                SongItem(song = song, onClick = {
                    viewModel.playSong(song, updateInternalList = false)
                    onNavigateToPlayer()
                })
            }
        }
    }
}