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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import com.music.purelymusic.R
import com.music.purelymusic.model.Playlist
import com.music.purelymusic.model.Song
import com.music.purelymusic.viewmodel.PlayerViewModel
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.Description
import androidx.media3.common.util.UnstableApi
import com.music.purelymusic.ui.utils.AppDimensions

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryScreen(
    viewModel: PlayerViewModel,
    onPickFile: () -> Unit,
    onPickCover: () -> Unit,
    onNavigateToCreatePlaylist: () -> Unit,
    onNavigateToPlaylistDetail: (Playlist) -> Unit,
    onNavigateToPlayer: () -> Unit,
    onPickLrc: () -> Unit
) {
    if (viewModel.tempMusicUri != null) {
        ImportMusicDialog(
            viewModel = viewModel,
            onPickCover = onPickCover,
            onPickLrc = onPickLrc
        )
    }

    if (viewModel.editingSong != null) {
        EditSongDialog(
            viewModel = viewModel,
            onPickCover = onPickCover,
            onPickLrc = onPickLrc
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToCreatePlaylist,
                modifier = Modifier.padding(bottom = AppDimensions.miniPlayerHeight()),
                containerColor = Color(0xFFE53935),
                contentColor = Color(0xFFFFFFFF)
            ) {
                Icon(Icons.Default.PlaylistAdd, contentDescription = "组建播放列表", modifier = Modifier.size(AppDimensions.iconM()))
            }
        },
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
                    text = "我的资料库",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                IconButton(onClick = onPickFile, modifier = Modifier.size(AppDimensions.iconButtonSizeM())) {
                    Icon(Icons.Default.Add, contentDescription = "导入", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(AppDimensions.iconM()))
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = AppDimensions.miniPlayerHeight() + AppDimensions.paddingScreen()),
                horizontalArrangement = Arrangement.spacedBy(AppDimensions.libraryGridSpacing()),
                verticalArrangement = Arrangement.spacedBy(AppDimensions.libraryGridSpacing())
            ) {
                if (viewModel.playlists.isNotEmpty()) {
                    item(span = { GridItemSpan(2) }) {
                        Column {
                            Text(
                                text = "播放列表",
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
                            Spacer(modifier = Modifier.height(AppDimensions.paddingScreen()))
                        }
                    }
                }

                item(span = { GridItemSpan(2) }) {
                    Text(
                        text = "歌曲",
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
    var title by remember { mutableStateOf("") }
    var artist by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { viewModel.tempMusicUri = null },
        title = { Text("补充歌曲信息") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AppDimensions.spacingS())) {
                TextField(value = title, onValueChange = { title = it }, label = { Text("歌曲名称") }, singleLine = true)
                TextField(value = artist, onValueChange = { artist = it }, label = { Text("歌手") }, singleLine = true)
                Spacer(modifier = Modifier.height(AppDimensions.paddingCard()))

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Button(onClick = onPickCover, modifier = Modifier.height(AppDimensions.buttonHeightM())) { Text("选择封面") }
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

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Button(onClick = onPickLrc, modifier = Modifier.height(AppDimensions.buttonHeightM())) { Text("选择歌词 (LRC)") }
                        Spacer(modifier = Modifier.width(AppDimensions.paddingCard()))
                        if (viewModel.tempLrcUri != null) {
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
                if (title.isNotBlank()) {
                    viewModel.saveSong(title, artist)
                    viewModel.tempMusicUri = null
                }
            }, modifier = Modifier.height(AppDimensions.buttonHeightM())) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = { viewModel.tempMusicUri = null }, modifier = Modifier.height(AppDimensions.buttonHeightM())) { Text("取消") }
        }
    )
}

// 编辑歌曲信息对话框
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun EditSongDialog(
    viewModel: PlayerViewModel,
    onPickCover: () -> Unit,
    onPickLrc: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { viewModel.cancelEditSong() },
        title = { Text("编辑歌曲信息", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AppDimensions.paddingCard())) {
                TextField(
                    value = viewModel.editTitle,
                    onValueChange = { viewModel.editTitle = it },
                    label = { Text("歌曲名称") },
                    singleLine = true
                )
                TextField(
                    value = viewModel.editArtist,
                    onValueChange = { viewModel.editArtist = it },
                    label = { Text("歌手") },
                    singleLine = true
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
                        Button(onClick = onPickLrc, modifier = Modifier.height(AppDimensions.buttonHeightM())) { Text("更换歌词 (LRC)") }
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
            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
        ) {
            DropdownMenuItem(
                text = { Text("删除歌单", color = com.music.purelymusic.ui.theme.RedPrimary) },
                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = com.music.purelymusic.ui.theme.RedPrimary) },
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

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("收藏") },
                leadingIcon = { Icon(Icons.Default.FavoriteBorder, null, modifier = Modifier.size(AppDimensions.iconS())) },
                onClick = { expanded = false }
            )
            DropdownMenuItem(
                text = { Text("编辑") },
                leadingIcon = { Icon(Icons.Default.Edit, null, modifier = Modifier.size(AppDimensions.iconS())) },
                onClick = {
                    viewModel.startEditSong(song)
                    expanded = false
                }
            )
            DropdownMenuItem(
                text = { Text("删除", color = Color.Red) },
                leadingIcon = { Icon(Icons.Default.Delete, null, tint = Color.Red, modifier = Modifier.size(AppDimensions.iconS())) },
                onClick = {
                    viewModel.deleteSong(song)
                    expanded = false
                }
            )
        }
    }
}