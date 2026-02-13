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
package com.music.purelymusic

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.music.purelymusic.ui.*
import com.music.purelymusic.ui.theme.AMPlayerTheme
import com.music.purelymusic.viewmodel.PlayerViewModel
import com.music.purelymusic.ui.utils.AppDimensions

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 配置 Coil 以支持 HTTP 协议
        coil.ImageLoader.Builder(this)
            .okHttpClient {
                okhttp3.OkHttpClient.Builder()
                    .build()
            }
            .build()

        setContent {
            AMPlayerTheme {
                val playerViewModel: PlayerViewModel = viewModel()
                MainScreen(playerViewModel)
            }
        }
    }
}

@Composable
fun MainScreen(viewModel: PlayerViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> uri?.let { viewModel.tempMusicUri = it } }

    val coverPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> uri?.let { viewModel.tempCoverUri = it } }

    val playlistCoverPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> uri?.let { viewModel.tempPlaylistCoverUri = it } }
    val lrcPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.tempLrcUri = it }
    }

    Scaffold(
        containerColor = Color.White,
        bottomBar = {
            if (currentRoute == "home" || currentRoute == "library") {
                NavigationBar(
                    containerColor = Color.White,
                    tonalElevation = 0.dp
                ) {
                    val itemColors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFFE53935),
                        selectedTextColor = Color(0xFFE53935),
                        indicatorColor = Color(0xFFFFCDD2),
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    )

                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Home, null, modifier = Modifier.size(AppDimensions.navigationBarIconSize())) },
                        label = { Text("主页") },
                        selected = currentRoute == "home",
                        colors = itemColors,
                        onClick = {
                            if (currentRoute != "home") {
                                navController.navigate("home") { popUpTo("home") { inclusive = true } }
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.LibraryMusic, null, modifier = Modifier.size(AppDimensions.navigationBarIconSize())) },
                        label = { Text("资料库") },
                        selected = currentRoute == "library",
                        colors = itemColors,
                        onClick = { if (currentRoute != "library") navController.navigate("library") }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {

            NavHost(navController = navController, startDestination = "home") {
                composable("home") {
                    HomeScreen(
                        viewModel = viewModel,
                        onNavigateToPlayer = { navController.navigate("player") }
                    )
                }

                composable("library") {
                    LibraryScreen(
                        viewModel = viewModel,
                        onPickFile = { filePickerLauncher.launch("audio/*") },
                        onPickCover = { coverPickerLauncher.launch("image/*") },
                        onNavigateToCreatePlaylist = { navController.navigate("create_playlist") },
                        onNavigateToPlaylistDetail = { playlist ->
                            navController.navigate("playlist_detail/${playlist.id}")
                        },
                        onNavigateToAlbumDetail = { albumId ->
                            navController.navigate("album_detail/$albumId")
                        },
                        onNavigateToPlayer = { navController.navigate("player") },
                        onPickLrc = {
                            lrcPickerLauncher.launch("*/*")
                        }
                    )
                }

                composable("create_playlist") {
                    CreatePlaylistScreen(
                        viewModel = viewModel,
                        onPickPlaylistCover = { playlistCoverPickerLauncher.launch("image/*") },
                        onBack = { navController.popBackStack() },
                        onFinish = { navController.popBackStack() }
                    )
                }

                composable(
                    route = "playlist_detail/{playlistId}",
                    arguments = listOf(navArgument("playlistId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val playlistId = backStackEntry.arguments?.getString("playlistId")
                    val playlist = viewModel.playlists.find { it.id == playlistId }
                    if (playlist != null) {
                        PlaylistDetailScreen(
                            playlist = playlist,
                            viewModel = viewModel,
                            onBack = { navController.popBackStack() },
                            onNavigateToPlayer = { navController.navigate("player") }
                        )
                    }
                }

                composable(

                                    route = "album_detail/{albumId}",

                                    arguments = listOf(navArgument("albumId") { type = NavType.StringType })

                                ) { backStackEntry ->

                                    val albumId = backStackEntry.arguments?.getString("albumId")

                                    val album = viewModel.albums.find { it.id == albumId }

                                    if (album != null) {

                                        AlbumDetailScreen(

                                            album = album,

                                            viewModel = viewModel,

                                            onBack = { navController.popBackStack() },

                                            onNavigateToPlayer = { navController.navigate("player") }

                                        )

                                    }

                                }
                composable(
                    route = "edit_playlist/{playlistId}",
                    arguments = listOf(navArgument("playlistId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val playlistId = backStackEntry.arguments?.getString("playlistId") ?: ""

                    EditPlaylistScreen(
                        playlistId = playlistId,
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() }
                    )
                }

                composable("player") {
                    PlayerScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
                }
            }

            AnimatedVisibility(
                visible = viewModel.currentSong != null && currentRoute != "player",
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = if (currentRoute == "home" || currentRoute == "library") AppDimensions.spacingS() else AppDimensions.paddingSmall())
            ) {
                MiniPlayer(
                    viewModel = viewModel,
                    onClick = { navController.navigate("player") }
                )
            }
        }
    }
}