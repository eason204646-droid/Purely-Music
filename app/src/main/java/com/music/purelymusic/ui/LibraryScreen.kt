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
import android.util.Log
import androidx.compose.ui.text.withStyle
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
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
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
import com.music.purelymusic.R
import com.music.purelymusic.model.Playlist
import com.music.purelymusic.model.Song
import com.music.purelymusic.viewmodel.PlayerViewModel
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.Description
import androidx.compose.runtime.Composable
import androidx.media3.common.util.UnstableApi
import com.music.purelymusic.ui.utils.AppDimensions

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryScreen(
    viewModel: PlayerViewModel,
    onPickFile: () -> Unit,
    onBatchPickFile: () -> Unit,
    onPickCover: () -> Unit,
    onNavigateToCreatePlaylist: () -> Unit,
    onNavigateToPlaylistDetail: (Playlist) -> Unit,
    onNavigateToAlbumDetail: (String) -> Unit,
    onNavigateToPlayer: () -> Unit,
    onPickLrc: () -> Unit
) {
    // 监听文件选择，尝试读取元数据
    LaunchedEffect(viewModel.tempMusicUri) {
        val uri = viewModel.tempMusicUri
        if (uri != null) {
            // 只有在开启自动获取元数据时才自动读取
            if (viewModel.autoFetchMetadata) {
                // 显示加载中弹窗
                viewModel.isProcessingImport = true
                
                // 尝试读取音频文件的元数据
                val (title, artist) = viewModel.readAudioMetadata(uri)
                
                // 如果元数据完整（有歌名和歌手），尝试自动获取信息并保存
                if (!title.isNullOrBlank() && !artist.isNullOrBlank()) {
                    try {
                        val (coverPath, lrcPath) = viewModel.fetchAllFromNetwork(title, artist)
                        
                        // 检查是否成功获取到信息
                        if (coverPath != null || lrcPath != null) {
                            // 至少获取到了封面或歌词，保存歌曲
                            if (coverPath != null) {
                                viewModel.tempCoverUri = android.net.Uri.parse(coverPath)
                            }
                            if (lrcPath != null) {
                                viewModel.tempLrcUri = android.net.Uri.parse("file://$lrcPath")
                            }
                            
                            // 保存歌曲
                            viewModel.saveSong(title, artist)
                            
                            // 保存成功后，关闭加载中弹窗并清除 tempMusicUri
                            viewModel.isProcessingImport = false
                            viewModel.tempMusicUri = null
                        } else {
                            // 没有获取到任何信息，显示补充信息弹窗
                            viewModel.isProcessingImport = false
                            // 保持 tempMusicUri 不变，ImportMusicDialog 会显示
                        }
                    } catch (e: Exception) {
                        // 自动获取失败，显示补充信息弹窗
                        viewModel.isProcessingImport = false
                        // 保持 tempMusicUri 不变，ImportMusicDialog 会显示
                    }
                } else {
                    // 元数据不完整，显示补充信息弹窗
                    viewModel.isProcessingImport = false
                    // 保持 tempMusicUri 不变，ImportMusicDialog 会显示
                }
            }
            // 如果关闭了自动获取元数据，直接显示弹窗让用户手动输入（不设置 isProcessingImport）
        }
    }

    if (viewModel.tempMusicUri != null) {
        ImportMusicDialog(
            viewModel = viewModel,
            onPickCover = onPickCover,
            onPickLrc = onPickLrc
        )
    }

    // 显示加载中弹窗（当正在处理导入时）
    if (viewModel.isProcessingImport) {
        ImportProcessingDialog(
            onDismiss = {
                // 加载中弹窗不允许手动关闭
            }
        )
    }

    // 批量导入进度弹窗
    if (viewModel.isBatchImporting && !viewModel.batchImportPaused) {
        BatchImportProgressDialog(
            progress = viewModel.batchImportProgress,
            total = viewModel.batchImportTotal,
            currentSong = viewModel.batchImportCurrentSong,
            currentLanguage = viewModel.currentLanguage
        )
    }

    // 批量导入暂停时的歌曲信息输入弹窗
    if (viewModel.batchImportPaused) {
        BatchImportPausedDialog(
            fileName = viewModel.batchImportPendingFileName ?: "",
            currentLanguage = viewModel.currentLanguage,
            onConfirm = { title, artist ->
                viewModel.continueBatchImport(title, artist)
            },
            onSkip = {
                viewModel.skipBatchImport()
            },
            onCancel = {
                viewModel.cancelBatchImport()
            }
        )
    }

    if (viewModel.editingSong != null) {
        EditSongDialog(
            viewModel = viewModel,
            onPickCover = onPickCover,
            onPickLrc = onPickLrc
        )
    }

    var showMenu by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = AppDimensions.paddingScreen())
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = AppDimensions.paddingScreen(), bottom = AppDimensions.paddingCard()),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (viewModel.currentLanguage == "zh") "我的资料库" else "My Library",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Box {
                    IconButton(
                        onClick = { showMenu = !showMenu },
                        modifier = Modifier.size(AppDimensions.iconButtonSizeM())
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = if (viewModel.currentLanguage == "zh") "添加" else "Add",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(AppDimensions.iconM())
                        )
                    }
                    
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier
                            .background(Color(0xFFF5F5F5), RoundedCornerShape(12.dp))
                            .clip(RoundedCornerShape(12.dp)),
                        offset = androidx.compose.ui.unit.DpOffset(0.dp, 8.dp)
                    ) {
                        DropdownMenuItem(
                            text = { 
                                Text(
                                    if (viewModel.currentLanguage == "zh") "导入歌曲" else "Import Song",
                                    color = Color.Black
                                ) 
                            },
                            leadingIcon = {
                                Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color.Black)
                            },
                            onClick = {
                                showMenu = false
                                onPickFile()
                            }
                        )
                        DropdownMenuItem(
                            text = { 
                                Text(
                                    if (viewModel.currentLanguage == "zh") "批量导入" else "Batch Import",
                                    color = Color.Black
                                ) 
                            },
                            leadingIcon = {
                                Icon(Icons.Default.LibraryMusic, contentDescription = null, tint = Color.Black)
                            },
                            onClick = {
                                showMenu = false
                                onBatchPickFile()
                            }
                        )
                        DropdownMenuItem(
                            text = { 
                                Text(
                                    if (viewModel.currentLanguage == "zh") "创建播放列表" else "Create Playlist",
                                    color = Color.Black
                                ) 
                            },
                            leadingIcon = {
                                Icon(Icons.Default.PlaylistAdd, contentDescription = null, tint = Color.Black)
                            },
                            onClick = {
                                showMenu = false
                                onNavigateToCreatePlaylist()
                            }
                        )
                    }
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = AppDimensions.paddingScreen()),
                horizontalArrangement = Arrangement.spacedBy(AppDimensions.libraryGridSpacing()),
                verticalArrangement = Arrangement.spacedBy(AppDimensions.libraryGridSpacing())
            ) {
                if (viewModel.playlists.isNotEmpty()) {
                    item(span = { GridItemSpan(2) }) {
                        Column {
                            Text(
                                text = if (viewModel.currentLanguage == "zh") "播放列表" else "Playlists",
                                fontSize = AppDimensions.homeSectionTitleSize().value.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = AppDimensions.paddingCard())
                            )
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(AppDimensions.spacingM()),
                                contentPadding = PaddingValues(end = AppDimensions.paddingCard())
                            ) {
                                items(viewModel.playlists) { playlist ->
                                    PlaylistItem(
                                        playlist = playlist,
                                        viewModel = viewModel,
                                        onClick = { onNavigateToPlaylistDetail(playlist) }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(AppDimensions.spacingS()))
                        }
                    }
                }

                if (viewModel.albums.isNotEmpty()) {
                    item(span = { GridItemSpan(2) }) {
                        Column {
                            Text(
                                text = if (viewModel.currentLanguage == "zh") "专辑" else "Albums",
                                fontSize = AppDimensions.homeSectionTitleSize().value.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = AppDimensions.paddingCard())
                            )
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(AppDimensions.spacingM()),
                                contentPadding = PaddingValues(end = AppDimensions.paddingCard())
                            ) {
                                items(viewModel.albums) { album ->
                                    AlbumItem(
                                        album = album,
                                        viewModel = viewModel,
                                        onClick = { onNavigateToAlbumDetail(album.id) }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(AppDimensions.paddingScreen()))
                        }
                    }
                }

                item(span = { GridItemSpan(2) }) {
                    Text(
                        text = if (viewModel.currentLanguage == "zh") "歌曲" else "Songs",
                        fontSize = AppDimensions.homeSectionTitleSize().value.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = AppDimensions.spacingXS())
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
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun ImportMusicDialog(
    viewModel: PlayerViewModel,
    onPickCover: () -> Unit,
    onPickLrc: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var title by remember { mutableStateOf("") }
    var artist by remember { mutableStateOf("") }
    var showManualImport by remember { mutableStateOf(false) }
    var showFetchErrorDialog by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    // 监听自动获取失败
    LaunchedEffect(viewModel.fetchAllError) {
        if (viewModel.fetchAllError != null && isSaving) {
            showFetchErrorDialog = true
        }
    }

    // 监听保存成功
    LaunchedEffect(viewModel.saveSongError) {
        if (viewModel.saveSongError == null && isSaving) {
            isSaving = false
        }
    }

    val saveSong: () -> Unit = {
        if (title.isNotBlank()) {
            isSaving = true
            viewModel.clearSaveSongError()
            viewModel.fetchAllError = null
            
            // 先尝试自动获取信息
            coroutineScope.launch {
                try {
                    if (title.isNotBlank() && artist.isNotBlank()) {
                        val (coverPath, lrcPath) = viewModel.fetchAllFromNetwork(title, artist)
                        if (coverPath != null) {
                            viewModel.tempCoverUri = android.net.Uri.parse(coverPath)
                        }
                        if (lrcPath != null) {
                            viewModel.tempLrcUri = android.net.Uri.parse("file://$lrcPath")
                        }
                        
                        // 自动获取成功，直接保存
                        viewModel.saveSong(title, artist)
                    } else {
                        // 没有歌名或歌手，直接保存
                        viewModel.saveSong(title, artist)
                    }
                } catch (e: Exception) {
                    // 自动获取失败，显示错误弹窗
                    showFetchErrorDialog = true
                    isSaving = false
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = {
            if (!isSaving) {
                viewModel.tempMusicUri = null
                viewModel.fetchAllError = null
                viewModel.saveSongError = null
            }
        },
        containerColor = Color.White,
        title = { Text(if (viewModel.currentLanguage == "zh") "补充歌曲信息" else "Add Song Info", color = Color.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AppDimensions.spacingS())) {
                TextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(if (viewModel.currentLanguage == "zh") "歌曲名称" else "Song Title") },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedIndicatorColor = Color(0xFFE53935),
                        unfocusedIndicatorColor = Color.Gray,
                        focusedLabelColor = Color(0xFFE53935),
                        unfocusedLabelColor = Color.Gray
                    )
                )
                TextField(
                    value = artist,
                    onValueChange = { artist = it },
                    label = { Text(if (viewModel.currentLanguage == "zh") "歌手" else "Artist") },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedIndicatorColor = Color(0xFFE53935),
                        unfocusedIndicatorColor = Color.Gray,
                        focusedLabelColor = Color(0xFFE53935),
                        unfocusedLabelColor = Color.Gray
                    )
                )
                
                Spacer(modifier = Modifier.height(AppDimensions.paddingCard()))

                // 手动导入区域（可展开/收起）
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showManualImport = !showManualImport }
                            .padding(vertical = AppDimensions.paddingSmall()),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (viewModel.currentLanguage == "zh") "手动导入" else "Manual Import",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Black
                        )
                        Icon(
                            imageVector = if (showManualImport) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = Color.Black.copy(alpha = 0.7f)
                        )
                    }
                    
                    if (showManualImport) {
                        Spacer(modifier = Modifier.height(AppDimensions.spacingS()))
                        
                        Column {
                            Text(
                                text = if (viewModel.currentLanguage == "zh") "歌曲封面" else "Album Cover",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Black.copy(alpha = 0.7f),
                                modifier = Modifier.padding(bottom = AppDimensions.spacingXS())
                            )
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(
                                    onClick = onPickCover,
                                    modifier = Modifier.height(AppDimensions.buttonHeightM())
                                ) {
                                    Text(if (viewModel.currentLanguage == "zh") "选择图片" else "Select Image")
                                }
                                Spacer(modifier = Modifier.width(AppDimensions.paddingCard()))
                                if (viewModel.tempCoverUri != null) {
                                    AsyncImage(
                                        model = viewModel.tempCoverUri,
                                        contentDescription = null,
                                        modifier = Modifier.size(AppDimensions.coverS()).clip(RoundedCornerShape(AppDimensions.cornerRadiusS())),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier.size(AppDimensions.coverS()).clip(RoundedCornerShape(AppDimensions.cornerRadiusS())).background(MaterialTheme.colorScheme.surfaceVariant),
                                        contentAlignment = Alignment.Center
                                    ) { Icon(Icons.Default.MusicNote, contentDescription = null, modifier = Modifier.size(AppDimensions.iconM())) }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(AppDimensions.spacingM()))

                        Column {
                            Text(
                                text = if (viewModel.currentLanguage == "zh") "歌词文件 (LRC)" else "Lyrics File (LRC)",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Black.copy(alpha = 0.7f),
                                modifier = Modifier.padding(bottom = AppDimensions.spacingXS())
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedButton(
                                    onClick = onPickLrc,
                                    modifier = Modifier.height(AppDimensions.buttonHeightM())
                                ) {
                                    Text(if (viewModel.currentLanguage == "zh") "选择文件" else "Select File")
                                }
                                Spacer(modifier = Modifier.width(AppDimensions.paddingCard()))
                                if (viewModel.tempLrcUri != null) {
                                    Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(AppDimensions.iconS()))
                                    Text(if (viewModel.currentLanguage == "zh") "已选择" else "Selected", style = MaterialTheme.typography.bodySmall)
                                }
                            }

                            val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
                            val annotatedString = androidx.compose.ui.text.buildAnnotatedString {
                                append(if (viewModel.currentLanguage == "zh") "可以从 " else "You can download from ")
                                pushStringAnnotation(tag = "URL", annotation = "https://xiaojiangclub.com/")
                                withStyle(
                                    style = androidx.compose.ui.text.SpanStyle(
                                        color = MaterialTheme.colorScheme.primary,
                                        textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                                    )
                                ) {
                                    append("https://xiaojiangclub.com/")
                                }
                                pop()
                                append(if (viewModel.currentLanguage == "zh") " 等网站下载" else " and other sites")
                            }

                            androidx.compose.foundation.text.ClickableText(
                                text = annotatedString,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color.Black.copy(alpha = 0.6f),
                                    fontSize = AppDimensions.textXS().value.sp
                                ),
                                modifier = Modifier.padding(top = AppDimensions.spacingXS(), start = AppDimensions.spacingXS()),
                                onClick = { offset ->
                                    annotatedString.getStringAnnotations(tag = "URL", start = offset, end = offset)
                                        .firstOrNull()?.let { annotation ->
                                            uriHandler.openUri(annotation.item)
                                        }
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = saveSong,
                modifier = Modifier.height(AppDimensions.buttonHeightM()),
                enabled = !isSaving
            ) {
                if (isSaving) {
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier.size(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.material3.CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    }
                } else {
                    Text(if (viewModel.currentLanguage == "zh") "保存" else "Save")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    if (!isSaving) {
                        viewModel.tempMusicUri = null
                        viewModel.saveSongError = null
                        viewModel.fetchAllError = null
                    }
                },
                modifier = Modifier.height(AppDimensions.buttonHeightM()),
                enabled = !isSaving
            ) {
                Text(if (viewModel.currentLanguage == "zh") "取消" else "Cancel")
            }
        }
    )

    // 自动获取失败弹窗
    if (showFetchErrorDialog) {
        AlertDialog(
            onDismissRequest = {
                showFetchErrorDialog = false
                isSaving = false
            },
            title = {
                Text(
                    if (viewModel.currentLanguage == "zh") "自动获取失败" else "Auto-fetch Failed",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        viewModel.fetchAllError ?: (if (viewModel.currentLanguage == "zh") "无法自动获取歌曲信息" else "Failed to fetch song information"),
                        color = Color.Black.copy(alpha = 0.8f),
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        if (viewModel.currentLanguage == "zh") "您可以选择重试或手动导入信息" else "You can retry or manually import the information",
                        color = Color.Black.copy(alpha = 0.6f),
                        fontSize = 12.sp
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showFetchErrorDialog = false
                        isSaving = false
                        showManualImport = true  // 展开手动导入区域
                    }
                ) {
                    Text(if (viewModel.currentLanguage == "zh") "手动导入" else "Manual Import", color = Color(0xFFE53935))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showFetchErrorDialog = false
                        isSaving = false
                        // 重试自动获取
                        saveSong()
                    }
                ) {
                    Text(if (viewModel.currentLanguage == "zh") "重试" else "Retry", color = Color(0xFFE53935))
                }
            },
            containerColor = Color.White
        )
    }
}

// 编辑歌曲信息对话框
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun EditSongDialog(
    viewModel: PlayerViewModel,
    onPickCover: () -> Unit,
    onPickLrc: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = {
            viewModel.cancelEditSong()
        },
        containerColor = Color(0xFFF5F5F5),
        title = { Text(if (viewModel.currentLanguage == "zh") "编辑歌曲信息" else "Edit Song Info", fontWeight = FontWeight.Bold, color = Color.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AppDimensions.paddingCard())) {
                TextField(
                    value = viewModel.editTitle,
                    onValueChange = { viewModel.editTitle = it },
                    label = { Text(if (viewModel.currentLanguage == "zh") "歌曲名称" else "Song Title") },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedIndicatorColor = Color(0xFFE53935),
                        unfocusedIndicatorColor = Color.Gray,
                        focusedLabelColor = Color(0xFFE53935),
                        unfocusedLabelColor = Color.Gray
                    )
                )
                TextField(
                    value = viewModel.editArtist,
                    onValueChange = { viewModel.editArtist = it },
                    label = { Text(if (viewModel.currentLanguage == "zh") "歌手" else "Artist") },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedIndicatorColor = Color(0xFFE53935),
                        unfocusedIndicatorColor = Color.Gray,
                        focusedLabelColor = Color(0xFFE53935),
                        unfocusedLabelColor = Color.Gray
                    )
                )
                Spacer(modifier = Modifier.height(AppDimensions.spacingS()))

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Button(onClick = onPickCover, modifier = Modifier.height(AppDimensions.buttonHeightM())) { Text("更换封面") }
                    Spacer(modifier = Modifier.width(AppDimensions.paddingCard()))
                    if (viewModel.editCoverUri != null) {
                        AsyncImage(
                            model = viewModel.editCoverUri,
                            contentDescription = null,
                            modifier = Modifier.size(AppDimensions.coverS()).clip(RoundedCornerShape(AppDimensions.cornerRadiusS())),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier.size(AppDimensions.coverS()).clip(RoundedCornerShape(AppDimensions.cornerRadiusS())).background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.Default.MusicNote, contentDescription = null, modifier = Modifier.size(AppDimensions.iconM())) }
                    }
                }

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(onClick = onPickLrc, modifier = Modifier.height(AppDimensions.buttonHeightM())) { Text("手动导入") }
                        Spacer(modifier = Modifier.width(AppDimensions.paddingCard()))
                        if (viewModel.editLrcUri != null) {
                            Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(AppDimensions.iconS()))
                            Text("已选择", style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
                    val annotatedString = androidx.compose.ui.text.buildAnnotatedString {
                        append("可以从 ")
                        pushStringAnnotation(tag = "URL", annotation = "https://xiaojiangclub.com/")
                        withStyle(
                            style = androidx.compose.ui.text.SpanStyle(
                                color = MaterialTheme.colorScheme.primary,
                                textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                            )
                        ) {
                            append("https://xiaojiangclub.com/")
                        }
                        pop()
                        append(" 等网站下载")
                    }

                    androidx.compose.foundation.text.ClickableText(
                        text = annotatedString,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            fontSize = AppDimensions.textXS().value.sp
                        ),
                        modifier = Modifier.padding(top = AppDimensions.spacingXS(), start = AppDimensions.spacingXS()),
                        onClick = { offset ->
                            annotatedString.getStringAnnotations(tag = "URL", start = offset, end = offset)
                                .firstOrNull()?.let { annotation ->
                                    uriHandler.openUri(annotation.item)
                                }
                        }
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (viewModel.editTitle.isNotBlank()) {
                    viewModel.saveEditedSong()
                }
            }, modifier = Modifier.height(AppDimensions.buttonHeightM())) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = { viewModel.cancelEditSong() }, modifier = Modifier.height(AppDimensions.buttonHeightM())) { Text("取消") }
        }
    )
}

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlaylistItem(playlist: Playlist, viewModel: PlayerViewModel, onClick: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var isHovered by remember { mutableStateOf(false) }

    Box {
        Column(
            modifier = Modifier
                .width(AppDimensions.libraryPlaylistWidth())
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { expanded = true }
                )
        ) {
            Box(
                modifier = Modifier
                    .size(AppDimensions.libraryPlaylistWidth())
                    .clip(RoundedCornerShape(AppDimensions.cornerRadiusL()))
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.linearGradient(
                            colors = listOf(
                                com.music.purelymusic.ui.theme.RedLight,
                                com.music.purelymusic.ui.theme.RedPrimary
                            )
                        )
                    )
            ) {
                AsyncImage(
                    model = playlist.coverUri ?: R.drawable.default_cover,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(AppDimensions.cornerRadiusL())),
                    contentScale = ContentScale.Crop
                )

                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(AppDimensions.spacingS()),
                    shape = RoundedCornerShape(AppDimensions.cornerRadiusS()),
                    color = Color.Black.copy(alpha = 0.6f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = AppDimensions.spacingS(), vertical = AppDimensions.spacingXS()),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(AppDimensions.iconXS())
                        )
                        Spacer(modifier = Modifier.width(AppDimensions.spacingXS()))
                        Text(
                            text = "${playlist.songIds.size}",
                            color = Color.White,
                            fontSize = AppDimensions.textS().value.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Text(
                text = playlist.name,
                fontSize = AppDimensions.textM().value.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = AppDimensions.paddingCard()),
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        DropdownMenu(

                    expanded = expanded,

                    onDismissRequest = { expanded = false },

                    modifier = Modifier

                        .background(Color(0xFFF5F5F5), RoundedCornerShape(12.dp))

                        .clip(RoundedCornerShape(12.dp)),

                    offset = androidx.compose.ui.unit.DpOffset(0.dp, 8.dp)

                ) {
            DropdownMenuItem(
                text = { Text(if (viewModel.currentLanguage == "zh") "删除歌单" else "Delete Playlist", color = Color.Red) },
                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red) },
                onClick = {
                    viewModel.deletePlaylist(playlist)
                    expanded = false
                }
            )
        }
    }
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SongGridItem(song: Song, viewModel: PlayerViewModel, onNavigateToPlayer: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(AppDimensions.cornerRadiusL()))
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
                    .clip(RoundedCornerShape(AppDimensions.cornerRadiusL()))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(AppDimensions.spacingS()))
            Text(song.title, fontSize = AppDimensions.textM().value.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(song.artist, fontSize = AppDimensions.textS().value.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(Color(0xFFF5F5F5), RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp)),
            offset = androidx.compose.ui.unit.DpOffset(0.dp, 8.dp)
        ) {
            DropdownMenuItem(
                text = { Text(if (viewModel.currentLanguage == "zh") "收藏" else "Favorite", color = Color.Black) },
                leadingIcon = { Icon(Icons.Default.FavoriteBorder, null, tint = Color.Black, modifier = Modifier.size(AppDimensions.iconS())) },
                onClick = { expanded = false }
            )
            DropdownMenuItem(
                text = { Text(if (viewModel.currentLanguage == "zh") "编辑" else "Edit", color = Color.Black) },
                leadingIcon = { Icon(Icons.Default.Edit, null, tint = Color.Black, modifier = Modifier.size(AppDimensions.iconS())) },
                onClick = {
                    viewModel.startEditSong(song)
                    expanded = false
                }
            )
            DropdownMenuItem(
                text = { Text(if (viewModel.currentLanguage == "zh") "删除" else "Delete", color = Color.Red) },
                leadingIcon = { Icon(Icons.Default.Delete, null, tint = Color.Red, modifier = Modifier.size(AppDimensions.iconS())) },
                onClick = {
                    viewModel.deleteSong(song)
                    expanded = false
                }
            )
        }
    }
}

@Composable
fun AlbumItem(album: com.music.purelymusic.model.Album, viewModel: PlayerViewModel, onClick: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    // 计算该专辑的歌曲数量
    val albumSongCount = remember(album.name, viewModel.libraryList) {
        viewModel.libraryList.count { it.album == album.name }
    }

    Box {
        Column(
            modifier = Modifier
                .width(AppDimensions.libraryPlaylistWidth())
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { expanded = true }
                )
        ) {
            Box(
                modifier = Modifier
                    .size(AppDimensions.libraryPlaylistWidth())
                    .clip(RoundedCornerShape(AppDimensions.cornerRadiusL()))
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.linearGradient(
                            colors = listOf(
                                com.music.purelymusic.ui.theme.RedLight,
                                com.music.purelymusic.ui.theme.RedPrimary
                            )
                        )
                    )
            ) {
                AsyncImage(
                    model = album.coverUri ?: R.drawable.default_cover,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(AppDimensions.cornerRadiusL())),
                    contentScale = ContentScale.Crop
                )

                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(AppDimensions.spacingS()),
                    shape = RoundedCornerShape(AppDimensions.cornerRadiusS()),
                    color = Color.Black.copy(alpha = 0.6f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = AppDimensions.spacingS(), vertical = AppDimensions.spacingXS()),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(AppDimensions.iconS())
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$albumSongCount",
                            color = Color.White,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(AppDimensions.spacingXS()))
            Text(
                text = album.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            Text(
                text = album.artist,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            offset = androidx.compose.ui.unit.DpOffset(0.dp, 8.dp)
        ) {
            DropdownMenuItem(
                text = { Text("删除") },
                leadingIcon = { Icon(Icons.Default.Delete, null, modifier = Modifier.size(AppDimensions.iconS())) },
                onClick = {
                    viewModel.deleteAlbum(album)
                    expanded = false
                }
            )
        }
    }
}

// 加载中弹窗组件
@Composable
fun ImportProcessingDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFFF5F5F5),
        title = {
            Text(
                text = "处理中",
                color = Color.Black,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(32.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    strokeWidth = 4.dp,
                    color = Color(0xFFE53935)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "正在读取音频文件信息...",
                    color = Color.Black.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )
            }
        },
        confirmButton = { },
        dismissButton = { }
    )
}

// 批量导入暂停时的歌曲信息输入弹窗
@Composable
fun BatchImportPausedDialog(
    fileName: String,
    currentLanguage: String,
    onConfirm: (String, String) -> Unit,
    onSkip: () -> Unit,
    onCancel: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var artist by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { /* 不允许手动关闭 */ },
        containerColor = Color.White,
        title = {
            Text(
                text = if (currentLanguage == "zh") "补充歌曲信息" else "Add Song Info",
                color = Color.Black,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(AppDimensions.spacingM())
            ) {
                // 显示文件名
                Text(
                    text = if (currentLanguage == "zh") "文件: $fileName" else "File: $fileName",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(if (currentLanguage == "zh") "歌名" else "Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = artist,
                    onValueChange = { artist = it },
                    label = { Text(if (currentLanguage == "zh") "歌手" else "Artist") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onConfirm(title, artist.ifBlank { "Unknown Artist" })
                    }
                },
                enabled = title.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE53935)
                )
            ) {
                Text(if (currentLanguage == "zh") "确定" else "OK", color = Color.White)
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onSkip) {
                    Text(if (currentLanguage == "zh") "跳过" else "Skip", color = Color.Gray)
                }
                TextButton(onClick = onCancel) {
                    Text(if (currentLanguage == "zh") "取消全部" else "Cancel All", color = Color(0xFFE53935))
                }
            }
        }
    )
}
