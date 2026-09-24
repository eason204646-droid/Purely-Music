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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.media3.common.util.UnstableApi
import coil.compose.AsyncImage
import com.music.purelymusic.R
import com.music.purelymusic.model.Album
import com.music.purelymusic.model.Song
import com.music.purelymusic.viewmodel.PlayerViewModel
import com.music.purelymusic.ui.utils.AppDimensions

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumDetailScreen(
    album: Album,
    viewModel: PlayerViewModel,
    onBack: () -> Unit,
    onNavigateToPlayer: () -> Unit
) {
    val albumSongs = remember(album.name, viewModel.libraryList) {
        viewModel.libraryList.filter { song ->
            song.albumId == album.id || (song.albumId == null && song.album == album.name)
        }
    }

    val totalSongs = albumSongs.size
    val isChinese = viewModel.currentLanguage == "zh"

    fun playAlbum(random: Boolean) {
        if (albumSongs.isNotEmpty()) {
            viewModel.playPlaylist(
                com.music.purelymusic.model.Playlist(
                    id = album.id,
                    name = album.name,
                    coverUri = album.coverUri,
                    songIds = albumSongs.map { it.id },
                    description = null,
                    createdAt = album.createdAt,
                    updatedAt = album.createdAt
                ),
                isRandom = random
            )
            onNavigateToPlayer()
        }
    }

    Scaffold(
        containerColor = Color.Transparent
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            CollectionAtmosphere(cover = album.coverUri, modifier = Modifier.fillMaxSize())
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
            item {
                CollectionHeader(
                    cover = album.coverUri,
                    eyebrow = if (isChinese) "专辑" else "Album",
                    title = album.name,
                    subtitle = "${album.artist} · ${if (isChinese) "$totalSongs 首歌曲" else "$totalSongs song${if (totalSongs == 1) "" else "s"}"}",
                    playLabel = if (isChinese) "播放全部" else "Play all",
                    shuffleLabel = if (isChinese) "随机播放" else "Shuffle",
                    onPlay = { playAlbum(false) },
                    onShuffle = { playAlbum(true) }
                )
            }

            item {
                Text(
                    text = if (isChinese) "歌曲" else "Songs",
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

                items(albumSongs) { song ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = AppDimensions.paddingScreen())
                    ) {
                        SongItem(
                            song = song,
                            onClick = {
                                viewModel.playSong(song)
                                onNavigateToPlayer()
                            }
                        )
                    }
                }
            }

            GlassControl(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(top = 12.dp, start = 8.dp)
                    .size(44.dp)
                    .zIndex(1f),
                shape = CircleShape,
                dark = false,
                onClick = onBack
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = if (viewModel.currentLanguage == "zh") "返回" else "Back",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(23.dp)
                )
            }
        }
    }
}
