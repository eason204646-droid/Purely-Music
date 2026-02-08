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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PauseCircleFilled
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
fun HomeScreen(viewModel: PlayerViewModel, onNavigateToPlayer: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Text(
            text = "主页",
            fontSize = AppDimensions.textXXXL().value.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.Black,
            modifier = Modifier.padding(start = AppDimensions.homeHeaderPadding(), top = AppDimensions.paddingScreen(), bottom = AppDimensions.spacingM())
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = AppDimensions.miniPlayerHeight() + AppDimensions.paddingScreen())
        ) {
            if (viewModel.recentSongs.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.padding(vertical = AppDimensions.spacingS())) {
                        Text(
                            text = "最近播放",
                            fontSize = AppDimensions.homeSectionTitleSize().value.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            modifier = Modifier.padding(start = AppDimensions.homeHeaderPadding(), bottom = AppDimensions.spacingM())
                        )

                        LazyRow(
                            contentPadding = PaddingValues(horizontal = AppDimensions.homeHeaderPadding()),
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
                    text = "所有歌曲",
                    fontSize = AppDimensions.homeSectionTitleSize().value.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.padding(start = AppDimensions.homeHeaderPadding(), top = AppDimensions.paddingScreen(), bottom = AppDimensions.spacingM())
                )
            }

            items(viewModel.libraryList) { song ->
                SongItem(song = song, onClick = { viewModel.playSong(song) })
            }
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
                .padding(horizontal = AppDimensions.paddingScreen(), vertical = AppDimensions.spacingM()),
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
            thickness = 1.dp,
            modifier = Modifier.padding(horizontal = AppDimensions.paddingScreen())
        )
    }
}

@OptIn(UnstableApi::class)
@Composable
fun MiniPlayer(viewModel: PlayerViewModel, onClick: () -> Unit) {
    val currentSong = viewModel.currentSong ?: return
    Surface(
        modifier = Modifier.fillMaxWidth().height(AppDimensions.miniPlayerHeight()).padding(horizontal = AppDimensions.miniPlayerPaddingH()),
        shape = RoundedCornerShape(AppDimensions.cornerRadiusL()),
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