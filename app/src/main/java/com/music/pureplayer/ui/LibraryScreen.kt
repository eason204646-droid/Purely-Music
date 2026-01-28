package com.music.PurelyPlayer.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.music.PurelyPlayer.R
import com.music.PurelyPlayer.model.Playlist
import com.music.PurelyPlayer.model.Song
import com.music.PurelyPlayer.viewmodel.PlayerViewModel
import androidx.compose.foundation.clickable

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryScreen(
    viewModel: PlayerViewModel,
    onPickFile: () -> Unit,
    onPickCover: () -> Unit,
    onNavigateToCreatePlaylist: () -> Unit,
    onNavigateToPlaylistDetail: (Playlist) -> Unit,
    onNavigateToPlayer: () -> Unit
) {
    // 🚩 修复：确保调用对话框逻辑正常
    if (viewModel.tempMusicUri != null) {
        ImportMusicDialog(viewModel, onPickCover)
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToCreatePlaylist,
                modifier = Modifier.padding(bottom = 65.dp),
                containerColor = Color(0xFFE53935), // 辅助色：浅红色 (Red 100)
                contentColor = Color(0xFFFFFFFF)    // 图标用深红，对比度更高
            ) {
                Icon(Icons.Default.PlaylistAdd, contentDescription = "组建播放列表")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "我的资料库",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                IconButton(onClick = onPickFile) {
                    Icon(Icons.Default.Add, contentDescription = "导入", tint = MaterialTheme.colorScheme.primary)
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 100.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                if (viewModel.playlists.isNotEmpty()) {
                    item(span = { GridItemSpan(2) }) {
                        Column {
                            Text(
                                text = "播放列表",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(end = 16.dp)
                            ) {
                                items(viewModel.playlists) { playlist ->
                                    // 🚩 修复点：在这里传入 viewModel
                                    PlaylistItem(
                                        playlist = playlist,
                                        viewModel = viewModel, // 传给它，它才能执行长按删除逻辑
                                        onClick = { onNavigateToPlaylistDetail(playlist) }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }

                item(span = { GridItemSpan(2) }) {
                    Text(
                        text = "歌曲",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                items(viewModel.libraryList) { song ->
                    SongGridItem(song, viewModel, onNavigateToPlayer)
                }
            }
        }
    }
}

// 🚩 补全：确保 ImportMusicDialog 函数存在，修复 contentCenter 错误
@Composable
fun ImportMusicDialog(viewModel: PlayerViewModel, onPickCover: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var artist by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { viewModel.tempMusicUri = null },
        title = { Text("补充歌曲信息") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextField(value = title, onValueChange = { title = it }, label = { Text("歌曲名称") }, singleLine = true)
                TextField(value = artist, onValueChange = { artist = it }, label = { Text("歌手") }, singleLine = true)
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Button(onClick = onPickCover) { Text("选择封面") }
                    Spacer(modifier = Modifier.width(16.dp))
                    if (viewModel.tempCoverUri != null) {
                        AsyncImage(model = viewModel.tempCoverUri, contentDescription = null, modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                    } else {
                        // 🚩 修正参数：contentAlignment
                        Box(
                            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.MusicNote, contentDescription = null)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { if (title.isNotBlank()) { viewModel.saveSong(title, artist); viewModel.tempMusicUri = null } }) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = { viewModel.tempMusicUri = null }) { Text("取消") }
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlaylistItem(playlist: Playlist, viewModel: PlayerViewModel, onClick: () -> Unit) {
    // 控制菜单显示状态
    var expanded by remember { mutableStateOf(false) }

    Box {
        Column(
            modifier = Modifier
                .width(120.dp)
                // 🚩 仅将 clickable 改为 combinedClickable 以支持长按
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { expanded = true }
                )
        ) {
            AsyncImage(
                model = playlist.coverUri ?: R.drawable.default_cover,
                contentDescription = null,
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop
            )
            Text(
                text = playlist.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // 🚩 长按弹出的删除选项
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("删除歌单", color = Color.Red) },
                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red) },
                onClick = {
                    viewModel.deletePlaylist(playlist)
                    expanded = false
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SongGridItem(song: Song, viewModel: PlayerViewModel, onNavigateToPlayer: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .combinedClickable(
                    onClick = {
                        viewModel.playSong(song)
                        onNavigateToPlayer()
                    },
                    onLongClick = { expanded = true }
                )
        ) {
            AsyncImage(
                model = song.coverUri ?: R.drawable.default_cover,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(song.title, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(song.artist, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("收藏") },
                leadingIcon = { Icon(Icons.Default.FavoriteBorder, null) },
                onClick = { expanded = false }
            )
            DropdownMenuItem(
                text = { Text("删除", color = Color.Red) },
                leadingIcon = { Icon(Icons.Default.Delete, null, tint = Color.Red) },
                onClick = {
                    viewModel.deleteSong(song)
                    expanded = false
                }
            )
        }
    }
}