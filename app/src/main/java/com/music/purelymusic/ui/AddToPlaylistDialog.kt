package com.music.purelymusic.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.purelymusic.viewmodel.PlayerViewModel
import com.music.purelymusic.ui.utils.AppDimensions

@Composable
fun AddToPlaylistDialog(
    viewModel: PlayerViewModel,
    onDismiss: () -> Unit
) {
    val playlists = viewModel.playlists

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFFF5F5F5),
        title = {
            Text(
                if (viewModel.currentLanguage == "zh") "选择歌单" else "Select Playlist",
                color = Color.Black,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            if (playlists.isEmpty()) {
                Text(
                    if (viewModel.currentLanguage == "zh") "暂无歌单，请先创建歌单" else "No playlists yet. Create one first.",
                    color = Color.Gray
                )
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    playlists.forEach { playlist ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.addSongsToPlaylist(
                                        playlist.id.toString(),
                                        viewModel.selectedSongsForAdd.toList()
                                    )
                                    onDismiss()
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.PlaylistPlay,
                                    contentDescription = null,
                                    tint = Color(0xFFE53935),
                                    modifier = Modifier.size(AppDimensions.iconM())
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = playlist.name,
                                        color = Color.Black,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = if (viewModel.currentLanguage == "zh") "${playlist.songIds.size} 首" else "${playlist.songIds.size} songs",
                                        color = Color.Gray,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    if (viewModel.currentLanguage == "zh") "取消" else "Cancel",
                    color = Color(0xFFE53935)
                )
            }
        }
    )
}
