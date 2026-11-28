// Copyright (c) 2026 eason204646. Licensed under Mulan PSL v2.
package com.music.purelymusic.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import com.music.purelymusic.model.Song
import com.music.purelymusic.model.artistKey
import com.music.purelymusic.model.findArtistAlbums
import com.music.purelymusic.ui.utils.AppDimensions
import com.music.purelymusic.viewmodel.PlayerViewModel

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun ArtistDetailScreen(
    artistKey: String,
    viewModel: PlayerViewModel,
    onBack: () -> Unit,
    onNavigateToAlbumDetail: (String) -> Unit,
    onNavigateToPlayer: () -> Unit
) {
    val library = viewModel.libraryList
    val albums = viewModel.albums.toList()
    val songs = remember(library, artistKey) {
        library.filter { it.artist.artistKey() == artistKey }
    }
    val artistAlbums = remember(artistKey, songs, albums) {
        findArtistAlbums(artistKey, songs, albums)
    }
    val chinese = viewModel.currentLanguage == "zh"
    val artistName = songs.firstOrNull()?.artist?.trim().orEmpty().ifEmpty {
        artistKey.ifEmpty { if (chinese) "未知歌手" else "Unknown artist" }
    }
    val cover = songs.firstNotNullOfOrNull { it.coverUri }
        ?: artistAlbums.firstNotNullOfOrNull { it.coverUri }
    val horizontalPadding = AppDimensions.paddingScreen()
    val bottomPadding = if (viewModel.currentSong != null) AppDimensions.miniPlayerHeight() + 24.dp else 24.dp

    fun play(shuffle: Boolean = false, song: Song? = null) {
        viewModel.playCollection(songs, isRandom = shuffle, startingSong = song)
        onNavigateToPlayer()
    }

    Box(Modifier.fillMaxSize()) {
        CollectionAtmosphere(cover, Modifier.matchParentSize())
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 54.dp, bottom = bottomPadding)
        ) {
            item {
                if (songs.isNotEmpty()) {
                    CollectionHeader(
                        cover = cover,
                        eyebrow = if (chinese) "歌手" else "Artist",
                        title = artistName,
                        subtitle = if (chinese) "${songs.size} 首歌曲" else "${songs.size} ${if (songs.size == 1) "song" else "songs"}",
                        playLabel = if (chinese) "播放全部" else "Play all",
                        shuffleLabel = if (chinese) "随机播放" else "Shuffle",
                        onPlay = { play() },
                        onShuffle = { play(shuffle = true) }
                    )
                } else {
                    Column(Modifier.padding(horizontal = horizontalPadding, vertical = 16.dp)) {
                        Text(artistName, style = MaterialTheme.typography.headlineMedium)
                        Text(
                            if (chinese) "资料库里没有这位歌手的歌曲" else "This artist has no songs in your library",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            if (artistAlbums.isNotEmpty()) {
                item {
                    LibraryShelf(
                        title = if (chinese) "专辑" else "Albums",
                        modifier = Modifier.padding(start = horizontalPadding, bottom = horizontalPadding)
                    ) {
                        items(artistAlbums, key = { it.id }) { album ->
                            AlbumItem(
                                album = album,
                                viewModel = viewModel,
                                onEditArtwork = null,
                                onClick = { onNavigateToAlbumDetail(album.id) }
                            )
                        }
                    }
                }
            }
            if (songs.isNotEmpty()) {
                item {
                    LibrarySectionTitle(
                        if (chinese) "歌曲" else "Songs",
                        Modifier.padding(horizontal = horizontalPadding)
                    )
                }
                items(songs, key = { it.id }) { song ->
                    Box(Modifier.padding(horizontal = horizontalPadding)) {
                        SongItem(song = song, onClick = { play(song = song) })
                    }
                }
            }
        }
        GlassControl(
            modifier = Modifier.padding(start = horizontalPadding, top = 8.dp).size(44.dp),
            shape = CircleShape,
            dark = isSystemInDarkTheme(),
            onClick = onBack
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = if (chinese) "返回" else "Back",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
