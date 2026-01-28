package com.music.PurelyPlayer.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.PurelyPlayer.model.Song
import com.music.PurelyPlayer.viewmodel.PlayerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPlaylistScreen(
    playlistId: String, // 🚩 类型改为 String
    viewModel: PlayerViewModel,
    onBack: () -> Unit
) {
    // 1. 获取当前歌单对象 (字符串匹配)
    val playlist = remember(playlistId, viewModel.playlists) {
        viewModel.playlists.find { it.id == playlistId }
    }

    // 🚩 空保护：如果找不到歌单，显示加载中，防止 NPE
    if (playlist == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFFE53935))
        }
        return
    }

    // 2. 状态列表 (记录选中的歌曲 ID)
    // 注意：如果你的歌曲 ID 也是 UUID，请把 Long 改为 String
    val selectedIds = remember {
        mutableStateListOf<Long>().apply {
            playlist.songIds.forEach { id ->
                id.toString().toLongOrNull()?.let { add(it) }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("编辑歌单", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            // 🚩 这里调用的 ViewModel 函数也需要支持 String 类型的 playlistId
                            viewModel.updatePlaylistSongs(playlistId, selectedIds.toList())
                            onBack()
                        }
                    ) {
                        Text("保存", color = Color(0xFFE53935), fontWeight = FontWeight.Bold)
                    }
                }
            )
        },
        containerColor = Color.White
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            items(viewModel.libraryList) { song: Song ->
                val songId = song.id.toLong() // 假设歌曲 ID 确实是数字
                val isChecked = selectedIds.contains(songId)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (isChecked) selectedIds.remove(songId)
                            else selectedIds.add(songId)
                        }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isChecked,
                        onCheckedChange = null,
                        colors = CheckboxDefaults.colors(checkedColor = Color(0xFFE53935))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(text = song.title, fontSize = 16.sp)
                        Text(text = song.artist, fontSize = 12.sp, color = Color.Gray)
                    }
                }
            }
        }
    }
}