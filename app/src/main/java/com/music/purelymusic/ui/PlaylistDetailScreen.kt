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

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.media3.common.util.UnstableApi
import coil.compose.AsyncImage
import com.music.purelymusic.R
import com.music.purelymusic.model.Playlist
import com.music.purelymusic.model.Song
import com.music.purelymusic.viewmodel.PlayerViewModel
import com.music.purelymusic.ui.utils.AppDimensions
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    playlist: Playlist,
    viewModel: PlayerViewModel,
    onBack: () -> Unit,
    onNavigateToPlayer: () -> Unit
) {
    // 修复：按照 songIds 的顺序排列歌曲，而不是按照 libraryList 的顺序
    val playlistSongs = remember(playlist.songIds, viewModel.libraryList) {
        val songMap = viewModel.libraryList.associateBy { it.id.toLong() }
        playlist.songIds.mapNotNull { songId -> songMap[songId] }
    }

    val totalSongs = playlistSongs.size
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // 拖拽状态
    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    var targetIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var itemHeightPx by remember { mutableFloatStateOf(0f) }
    
    // 用于记录当前正在拖拽的歌曲列表（本地状态，用于实时显示拖拽效果）
    var currentSongList by remember { mutableStateOf(playlistSongs) }
    
    // 当 playlistSongs 变化时更新 currentSongList
    LaunchedEffect(playlistSongs) {
        currentSongList = playlistSongs
    }

    // 处理拖拽排序
    fun performReorder(fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex || fromIndex < 0 || toIndex < 0) return
        if (fromIndex >= currentSongList.size || toIndex >= currentSongList.size) return

        val newList = currentSongList.toMutableList()
        val moved = newList.removeAt(fromIndex)
        newList.add(toIndex, moved)
        currentSongList = newList

        val newSongIds = newList.map { it.id.toLong() }
        viewModel.updatePlaylistSongs(playlist.id.toString(), newSongIds)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(bottom = 100.dp),
                state = listState
            ) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(380.dp)) {
                        AsyncImage(
                            model = playlist.coverUri ?: R.drawable.default_cover,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(modifier = Modifier.fillMaxSize().background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Transparent,
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.4f),
                                    Color.Black.copy(alpha = 0.85f)
                                )
                            )
                        ))

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp)
                                .padding(bottom = 32.dp),
                            verticalArrangement = Arrangement.Bottom
                        ) {
                            Text(
                                text = playlist.name,
                                color = Color.White,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "$totalSongs 首歌曲",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Button(
                                    onClick = {
                                        viewModel.playPlaylist(playlist, isRandom = false)
                                        onNavigateToPlayer()
                                    },
                                    modifier = Modifier.weight(1f).height(48.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("播放全部", color = Color.White)
                                }
                                Button(
                                    onClick = {
                                        viewModel.playPlaylist(playlist, isRandom = true)
                                        onNavigateToPlayer()
                                    },
                                    modifier = Modifier.weight(1f).height(48.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.White.copy(alpha = 0.2f),
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("随机播放", color = Color.White)
                                }
                            }
                        }
                    }
                }

                itemsIndexed(currentSongList) { index, song ->
                    val isDragged = draggedIndex == index
                    val isTarget = targetIndex == index && draggedIndex != null
                    
                    // 计算项目的位移
                    val shiftDistance = if (itemHeightPx > 0f) itemHeightPx * 0.9f else 80f
                    val itemOffset by animateFloatAsState(
                        targetValue = when {
                            isDragged -> dragOffsetY
                            draggedIndex != null && targetIndex != null -> {
                                if (index > draggedIndex!! && index <= targetIndex!!) {
                                    -shiftDistance // 向上移动
                                } else if (index < draggedIndex!! && index >= targetIndex!!) {
                                    shiftDistance // 向下移动
                                } else {
                                    0f
                                }
                            }
                            else -> 0f
                        },
                        animationSpec = if (isDragged) {
                            snap()
                        } else {
                            spring(
                                dampingRatio = 0.85f,
                                stiffness = Spring.StiffnessMedium
                            )
                        },
                        label = "item_offset_$index"
                    )
                    
                    // 拖拽项目的缩放和阴影效果
                    val scale by animateFloatAsState(
                        targetValue = if (isDragged) 1.05f else 1f,
                        animationSpec = spring(stiffness = Spring.StiffnessVeryLow),
                        label = "scale_$index"
                    )
                    
                    val elevation by animateDpAsState(
                        targetValue = if (isDragged) 12.dp else 0.dp,
                        animationSpec = spring(stiffness = Spring.StiffnessVeryLow),
                        label = "elevation_$index"
                    )

                    DraggableSongItem(
                        song = song,
                        index = index,
                        isDragged = isDragged,
                        scale = scale,
                        elevation = elevation,
                        itemOffset = itemOffset,
                        onItemHeightChanged = { height ->
                            if (height > 0f) {
                                itemHeightPx = height
                            }
                        },
                        onDragStart = { 
                            draggedIndex = index
                            targetIndex = index
                            dragOffsetY = 0f
                        },
                        onDrag = { dragAmount ->
                            // 计算目标位置
                            val estimatedItemHeight = if (itemHeightPx > 0f) itemHeightPx else 80f
                            dragOffsetY += dragAmount
                            val indexChange = (dragOffsetY / estimatedItemHeight).roundToInt()
                            val newTargetIndex = (draggedIndex!! + indexChange)
                                .coerceIn(0, currentSongList.size - 1)
                            
                            if (newTargetIndex != targetIndex) {
                                targetIndex = newTargetIndex
                            }
                        },
                        onDragEnd = {
                            if (draggedIndex != null && targetIndex != null && draggedIndex != targetIndex) {
                                performReorder(draggedIndex!!, targetIndex!!)
                            }
                            draggedIndex = null
                            targetIndex = null
                            dragOffsetY = 0f
                        },
                        onClick = {
                            viewModel.playSong(song)
                            onNavigateToPlayer()
                        }
                    )
                }

                // 添加歌曲按钮
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .clickable {
                                viewModel.showAddSongDialog = true
                                viewModel.selectedPlaylistForAdd = playlist.id.toString()
                                viewModel.selectedSongsForAdd = emptySet()
                            },
                        shape = RoundedCornerShape(12.dp),
                        color = Color.Transparent
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "添加歌曲",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Surface(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(top = 12.dp, start = 8.dp)
                    .zIndex(1f),
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.3f),
                onClick = onBack
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "返回",
                    tint = Color.White,
                    modifier = Modifier.padding(8.dp).size(24.dp)
                )
            }
        }
    }

    // 添加歌曲对话框
    if (viewModel.showAddSongDialog) {
        AddSongsToPlaylistDialog(
            viewModel = viewModel,
            onDismiss = {
                viewModel.showAddSongDialog = false
                viewModel.selectedPlaylistForAdd = null
                viewModel.selectedSongsForAdd = emptySet()
            }
        )
    }
}

// 可拖拽的歌曲项
@Composable
fun DraggableSongItem(
    song: Song,
    index: Int,
    isDragged: Boolean,
    scale: Float,
    elevation: Dp,
    itemOffset: Float,
    onItemHeightChanged: (Float) -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .onSizeChanged { size -> onItemHeightChanged(size.height.toFloat()) }
            .graphicsLayer {
                translationY = itemOffset
                scaleX = scale
                scaleY = scale
                shadowElevation = elevation.value
            }
            .shadow(
                elevation = if (isDragged) 8.dp else 0.dp,
                shape = RoundedCornerShape(12.dp),
                ambientColor = Color.Black.copy(alpha = 0.12f),
                spotColor = Color.Black.copy(alpha = 0.12f)
            )
            .background(
                if (isDragged) Color(0xFFF2F2F2)
                else Color.Transparent
            )
            .padding(start = AppDimensions.paddingScreen())
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 歌曲项主体
            SongItem(
                song = song,
                onClick = onClick,
                showDragHandle = true,
                isDragging = isDragged,
                dragHandleModifier = Modifier.pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = {
                            onDragStart()
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            onDrag(dragAmount.y)
                        },
                        onDragEnd = {
                            onDragEnd()
                        },
                        onDragCancel = {
                            onDragEnd()
                        }
                    )
                }
            )
        }
    }
}

// 添加歌曲到歌单的对话框
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun AddSongsToPlaylistDialog(
    viewModel: PlayerViewModel,
    onDismiss: () -> Unit
) {
    // 用于记录选择顺序
    var selectedSongsInOrder by remember { mutableStateOf<List<Long>>(emptyList()) }
    
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.8f),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
            color = Color.White,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // 标题栏
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "添加歌曲",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Text(
                            text = "选择要添加到歌单的歌曲",
                            fontSize = 14.sp,
                            color = Color.Black.copy(alpha = 0.6f)
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "关闭", tint = Color.Black)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                androidx.compose.material3.Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                Spacer(modifier = Modifier.height(16.dp))

                // 歌曲列表
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(viewModel.libraryList.size) { index ->
                        val song = viewModel.libraryList[index]
                        val isSelected = selectedSongsInOrder.contains(song.id.toLong())
                        
                        SelectableSongItem(
                            song = song,
                            isSelected = isSelected,
                            selectionOrder = if (isSelected) selectedSongsInOrder.indexOf(song.id.toLong()) + 1 else null,
                            onClick = {
                                val songId = song.id.toLong()
                                if (isSelected) {
                                    // 取消选择：从列表中移除
                                    selectedSongsInOrder = selectedSongsInOrder.filter { it != songId }
                                } else {
                                    // 添加选择：添加到列表末尾（保持用户选择顺序）
                                    selectedSongsInOrder = selectedSongsInOrder + songId
                                }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 底部按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = Color(0xFFE53935)
                        )
                    ) {
                        Text("取消", fontSize = 15.sp)
                    }
                    Button(
                        onClick = {
                            viewModel.selectedPlaylistForAdd?.let { playlistId ->
                                // 按照用户选择的顺序添加歌曲
                                viewModel.addSongsToPlaylist(
                                    playlistId,
                                    selectedSongsInOrder
                                )
                            }
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        enabled = selectedSongsInOrder.isNotEmpty()
                    ) {
                        Text(
                            "添加 ${selectedSongsInOrder.size} 首歌曲",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

// 可选择的歌曲项（带选择顺序显示）
@Composable
fun SelectableSongItem(
    song: Song,
    isSelected: Boolean,
    selectionOrder: Int? = null,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        } else {
            Color.Black.copy(alpha = 0.05f)
        },
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
        } else {
            null
        },
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 选择指示器（显示选择顺序）
            Surface(
                modifier = Modifier.size(24.dp),
                shape = CircleShape,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    Color.Gray.copy(alpha = 0.3f)
                },
                border = if (!isSelected) {
                    androidx.compose.foundation.BorderStroke(2.dp, Color.Gray)
                } else {
                    null
                }
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected && selectionOrder != null) {
                        Text(
                            text = selectionOrder.toString(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 封面
            AsyncImage(
                model = song.coverUri ?: R.drawable.default_cover,
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            // 歌曲信息
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = song.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = song.artist,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = Color.Black.copy(alpha = 0.6f)
                )
            }
        }
    }
}



