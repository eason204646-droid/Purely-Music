//Copyright (c) [2026] [eason204646]
//[purelyplayer] is licensed under Mulan PSL v2.
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
package com.music.PurelyPlayer

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
import com.music.PurelyPlayer.ui.*
import com.music.PurelyPlayer.ui.theme.AMPlayerTheme
import com.music.PurelyPlayer.viewmodel.PlayerViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

    // 音频文件选择器
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> uri?.let { viewModel.tempMusicUri = it } }

    // 歌曲封面选择器
    val coverPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> uri?.let { viewModel.tempCoverUri = it } }

    // 🚩 新增：歌单封面选择器
    val playlistCoverPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> uri?.let { viewModel.tempPlaylistCoverUri = it } }
    // 🚩 新增：歌词文件选择器
    val lrcPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.tempLrcUri = it } // 将选中的 Uri 存入 ViewModel
    }

    Scaffold(
        containerColor = Color.White,
        bottomBar = {
            // 只有在主页和资料库显示导航栏（创建页面和详情页不显示，让界面更沉浸）
            if (currentRoute == "home" || currentRoute == "library") {
                NavigationBar(
                    containerColor = Color.White,
                    tonalElevation = 0.dp
                ) {
                    // 🚩 定义红色系配置
                    val itemColors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFFE53935),    // 选中图标：标准红
                        selectedTextColor = Color(0xFFE53935),    // 选中文字：标准红
                        indicatorColor = Color(0xFFFFCDD2),       // 选中时的圆形底座：浅红色
                        unselectedIconColor = Color.Gray,         // 未选中图标：灰色
                        unselectedTextColor = Color.Gray          // 未选中文字：灰色
                    )

                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Home, null) },
                        label = { Text("主页") },
                        selected = currentRoute == "home",
                        colors = itemColors, // 🚩 应用颜色
                        onClick = {
                            if (currentRoute != "home") {
                                navController.navigate("home") { popUpTo("home") { inclusive = true } }
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.LibraryMusic, null) },
                        label = { Text("资料库") },
                        selected = currentRoute == "library",
                        colors = itemColors, // 🚩 应用颜色
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

                // 🚩 修改：完善资料库的回调
                composable("library") {
                    LibraryScreen(
                        viewModel = viewModel,
                        onPickFile = { filePickerLauncher.launch("audio/*") },
                        onPickCover = { coverPickerLauncher.launch("image/*") },
                        onNavigateToCreatePlaylist = { navController.navigate("create_playlist") },
                        onNavigateToPlaylistDetail = { playlist ->
                            navController.navigate("playlist_detail/${playlist.id}")
                        },
                        onNavigateToPlayer = { navController.navigate("player") },
                        onPickLrc = {
                            // GetContent 契约只接受 String
                            lrcPickerLauncher.launch("*/*")
                        }
                    )
                }

                // 🚩 新增：创建歌单页面
                composable("create_playlist") {
                    CreatePlaylistScreen(
                        viewModel = viewModel,
                        onPickPlaylistCover = { playlistCoverPickerLauncher.launch("image/*") },
                        onBack = { navController.popBackStack() },
                        onFinish = { navController.popBackStack() }
                    )
                }

                // 🚩 新增：歌单详情页面
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
                    route = "edit_playlist/{playlistId}",
                    arguments = listOf(navArgument("playlistId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val playlistId = backStackEntry.arguments?.getString("playlistId") ?: ""

                    // 🚩 修复点：直接传递 String，不再尝试转 Long
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

            // MiniPlayer 逻辑
            AnimatedVisibility(
                visible = viewModel.currentSong != null && currentRoute != "player",
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = if (currentRoute == "home" || currentRoute == "library") 8.dp else 12.dp)
            ) {
                MiniPlayer(
                    viewModel = viewModel,
                    onClick = { navController.navigate("player") }
                )
            }
        }
    }
}