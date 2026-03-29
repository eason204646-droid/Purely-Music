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
package com.music.purelymusic.ui // 1. 确保包名一致

import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PauseCircleFilled
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
import coil.compose.AsyncImage
import com.music.purelymusic.model.Song
import com.music.purelymusic.viewmodel.PlayerViewModel
import com.music.purelymusic.R
import com.music.purelymusic.ui.utils.AppDimensions

@OptIn(UnstableApi::class)
@Composable
fun HomeScreen(
    viewModel: PlayerViewModel,
    onNavigateToPlayer: () -> Unit,
    onPickFile: () -> Unit,
    onBatchPickFile: () -> Unit,
    onPickCover: () -> Unit,
    onPickLrc: () -> Unit,
    onNavigateToCreatePlaylist: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
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
                text = if (viewModel.currentLanguage == "zh") "主页" else "Home",
                fontSize = AppDimensions.textXXXL().value.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.Black
            )
            Box {
                IconButton(
                    onClick = { showMenu = !showMenu },
                    modifier = Modifier.size(AppDimensions.iconButtonSizeM())
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = if (viewModel.currentLanguage == "zh") "添加" else "Add",
                        tint = Color(0xFFE53935),
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

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = AppDimensions.miniPlayerHeight() + AppDimensions.paddingScreen())
        ) {
            if (viewModel.recentSongs.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.padding(vertical = AppDimensions.spacingS())) {
                        Text(
                            text = if (viewModel.currentLanguage == "zh") "最近播放" else "Recently Played",
                            fontSize = AppDimensions.homeSectionTitleSize().value.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            modifier = Modifier.padding(bottom = AppDimensions.paddingCard())
                        )

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(AppDimensions.spacingM())
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

            item {
                Text(
                    text = if (viewModel.currentLanguage == "zh") "所有歌曲" else "All Songs",
                    fontSize = AppDimensions.homeSectionTitleSize().value.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.padding(top = AppDimensions.paddingScreen(), bottom = AppDimensions.paddingCard())
                )
            }

            items(viewModel.libraryList) { song ->
                SongItem(song = song, onClick = { viewModel.playSong(song) })
            }
        }

        // 监听文件选择，尝试读取元数据
        LaunchedEffect(viewModel.tempMusicUri) {
            val uri = viewModel.tempMusicUri
            if (uri != null) {
                // 只有在开启自动获取元数据时才自动读取
                if (viewModel.autoFetchMetadata) {
                    // 尝试读取音频文件的元数据
                    val (title, artist) = viewModel.readAudioMetadata(uri)
                    
                    // 如果元数据完整（有歌名和歌手），自动获取信息并保存
                    if (!title.isNullOrBlank() && !artist.isNullOrBlank()) {
                        try {
                            val (coverPath, lrcPath) = viewModel.fetchAllFromNetwork(title, artist)
                            if (coverPath != null) {
                                viewModel.tempCoverUri = android.net.Uri.parse(coverPath)
                            }
                            if (lrcPath != null) {
                                viewModel.tempLrcUri = android.net.Uri.parse("file://$lrcPath")
                            }
                            // 保存歌曲
                            viewModel.saveSong(title, artist)
                        } catch (e: Exception) {
                            // 自动获取失败，清除 tempMusicUri，不显示对话框
                            viewModel.tempMusicUri = null
                        }
                    }
                    // 如果元数据不完整，保持 tempMusicUri 不变，弹窗会显示
                }
                // 如果关闭了自动获取元数据，保持 tempMusicUri 不变，弹窗会显示让用户手动输入
            }
        }

        // 显示导入歌曲对话框（只有当没有成功保存时才显示）
        if (viewModel.tempMusicUri != null) {
            HomeImportMusicDialog(
                viewModel = viewModel,
                onPickCover = onPickCover,
                onPickLrc = onPickLrc
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
    }
}

@Composable
fun RecentSongItem(song: Song, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(AppDimensions.homeRecentCardWidth())
            .clickable { onClick() }
    ) {
        AsyncImage(
            model = song.coverUri ?: R.drawable.default_cover,
            contentDescription = null,
            modifier = Modifier
                .size(AppDimensions.homeRecentCardWidth())
                .clip(RoundedCornerShape(AppDimensions.cornerRadiusM())),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(AppDimensions.spacingS()))
        Text(
            text = song.title,
            color = Color.Black,
            fontSize = AppDimensions.textM().value.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = song.artist,
            color = Color.Gray,
            fontSize = AppDimensions.textS().value.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun SongItem(song: Song, onClick: () -> Unit) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(vertical = AppDimensions.spacingM()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = song.coverUri ?: R.drawable.default_cover,
                contentDescription = null,
                modifier = Modifier
                    .size(AppDimensions.coverM())
                    .clip(RoundedCornerShape(AppDimensions.cornerRadiusS())),
                contentScale = ContentScale.Crop
            )
            Column(
                modifier = Modifier
                    .padding(start = AppDimensions.spacingM())
                    .weight(1f)
            ) {
                Text(
                    song.title,
                    color = Color.Black,
                    fontSize = AppDimensions.textL().value.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(AppDimensions.spacingXS()))
                Text(
                    song.artist,
                    color = Color.Gray,
                    fontSize = AppDimensions.textM().value.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        androidx.compose.material3.Divider(
            color = Color(0xFFF5F5F5),
            thickness = 1.dp
        )
    }
}

@OptIn(UnstableApi::class)
@Composable
fun MiniPlayer(viewModel: PlayerViewModel, onClick: () -> Unit) {
    val currentSong = viewModel.currentSong ?: return
    Surface(
        modifier = Modifier.fillMaxWidth().height(AppDimensions.miniPlayerHeight()).padding(horizontal = AppDimensions.miniPlayerPaddingH()),
        shape = RoundedCornerShape(28.dp),
        color = Color.White,
        shadowElevation = AppDimensions.elevationL()
    ) {
        Row(
            modifier = Modifier.fillMaxSize().clickable { onClick() }.padding(horizontal = AppDimensions.miniPlayerPaddingH()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = currentSong.coverUri ?: R.drawable.default_cover,
                contentDescription = null,
                modifier = Modifier.size(AppDimensions.coverS()).clip(RoundedCornerShape(AppDimensions.cornerRadiusS())),
                contentScale = ContentScale.Crop
            )
            Column(modifier = Modifier.weight(1f).padding(start = AppDimensions.paddingCard())) {
                Text(currentSong.title, color = Color.Black, fontWeight = FontWeight.Bold, fontSize = AppDimensions.textM().value.sp, maxLines = 1)
                Text(currentSong.artist, color = Color.Gray, fontSize = AppDimensions.textS().value.sp, maxLines = 1)
            }
            IconButton(onClick = { viewModel.togglePlayPause() }, modifier = Modifier.size(AppDimensions.iconButtonSizeM())) {
                Icon(
                    imageVector = if (viewModel.isPlaying) Icons.Default.PauseCircleFilled else Icons.Default.PlayCircleFilled,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(AppDimensions.iconXL())
                )
            }
        }
    }
}

@Composable
fun HomeImportMusicDialog(
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
        containerColor = Color(0xFFF5F5F5),
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

                            val uriHandler = LocalUriHandler.current
                            val annotatedString = buildAnnotatedString {
                                append(if (viewModel.currentLanguage == "zh") "可以从 " else "You can download from ")
                                pushStringAnnotation(tag = "URL", annotation = "https://xiaojiangclub.com/")
                                withStyle(
                                    style = SpanStyle(
                                        color = MaterialTheme.colorScheme.primary,
                                        textDecoration = TextDecoration.Underline
                                    )
                                ) {
                                    append("https://xiaojiangclub.com/")
                                }
                                pop()
                                append(if (viewModel.currentLanguage == "zh") " 等网站下载" else " and other sites")
                            }

                            ClickableText(
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
                    Box(
                        modifier = Modifier.size(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
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
            containerColor = Color(0xFFF5F5F5)
        )
    }
}

// 批量导入进度弹窗
@Composable
fun BatchImportProgressDialog(
    progress: Int,
    total: Int,
    currentSong: String?,
    currentLanguage: String
) {
    AlertDialog(
        onDismissRequest = { /* 不允许手动关闭 */ },
        containerColor = Color.White,
        title = {
            Text(
                text = if (currentLanguage == "zh") "批量导入中" else "Batch Importing",
                color = Color.Black,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(AppDimensions.spacingM()),
                modifier = Modifier.fillMaxWidth()
            ) {
                CircularProgressIndicator(
                    progress = { if (total > 0) progress.toFloat() / total.toFloat() else 0f },
                    modifier = Modifier.size(64.dp),
                    strokeWidth = 6.dp,
                    color = Color(0xFFE53935),
                    trackColor = Color(0xFFE0E0E0)
                )
                
                Text(
                    text = "$progress / $total",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                
                if (currentSong != null) {
                    Text(
                        text = if (currentLanguage == "zh") "正在处理: $currentSong" else "Processing: $currentSong",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        confirmButton = { },
        dismissButton = { }
    )
}

// BatchImportPausedDialog 定义在 LibraryScreen.kt 中，同包名可共享