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

import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
import coil.compose.AsyncImage
import com.music.purelymusic.R
import com.music.purelymusic.viewmodel.PlayerViewModel

@OptIn(UnstableApi::class)
@Composable
fun CreatePlaylistScreen(
    viewModel: PlayerViewModel,
    onPickPlaylistCover: () -> Unit,
    onBack: () -> Unit,
    onFinish: () -> Unit
) {
    var isNamingStage by remember { mutableStateOf(false) } // 是否处于命名阶段
    var playlistName by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // 顶部栏
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = { if (isNamingStage) isNamingStage = false else onBack() }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "返回")
            }
            Text(
                text = if (isNamingStage) "歌单信息" else "选择歌曲 (${viewModel.selectedSongsForPlaylist.size})",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            TextButton(
                onClick = {
                    if (!isNamingStage) {
                        isNamingStage = true
                    } else if (playlistName.isNotBlank()) {
                        viewModel.savePlaylist(playlistName)
                        onFinish()
                    }
                },
                enabled = if (isNamingStage) playlistName.isNotBlank() else viewModel.selectedSongsForPlaylist.isNotEmpty()
            ) {
                Text(if (isNamingStage) "完成" else "下一步")
            }
        }

        if (!isNamingStage) {
            // 第一步：勾选歌曲列表
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(viewModel.libraryList) { song ->
                    val isSelected = viewModel.selectedSongsForPlaylist.any { it.id == song.id }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (isSelected) {
                                    viewModel.selectedSongsForPlaylist.removeAll { it.id == song.id }
                                } else {
                                    viewModel.selectedSongsForPlaylist.add(song)
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = song.coverUri ?: R.drawable.default_cover,
                            contentDescription = null,
                            modifier = Modifier.size(50.dp).clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Column(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
                            Text(song.title, fontWeight = FontWeight.Bold)
                            Text(song.artist, fontSize = 12.sp, color = Color.Gray)
                        }
                        Icon(
                            imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                    }
                }
            }
        } else {
            // 第二步：输入名字和选封面 (逻辑参考导入歌曲对话框)
            Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { onPickPlaylistCover() },
                    contentAlignment = Alignment.Center
                ) {
                    if (viewModel.tempPlaylistCoverUri != null) {
                        AsyncImage(model = viewModel.tempPlaylistCoverUri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    } else {
                        Text("点击上传封面", color = MaterialTheme.colorScheme.primary)
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
                TextField(
                    value = playlistName,
                    onValueChange = { playlistName = it },
                    label = { Text("歌单名称") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        }
    }
}