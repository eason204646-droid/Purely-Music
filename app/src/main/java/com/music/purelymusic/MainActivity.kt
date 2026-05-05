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
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    // 批量导入文件选择器
    val batchFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.batchImportSongs(uris)
        }
    }

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
            if (currentRoute == "home" || currentRoute == "library" || currentRoute == "settings") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp),
                        shape = RoundedCornerShape(32.dp),
                        color = Color.White,
                        shadowElevation = 8.dp,
                        tonalElevation = 0.dp
                    ) {
                        NavigationBar(
                            containerColor = Color.Transparent,
                            tonalElevation = 0.dp
                        ) {
                            val itemColors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color(0xFFE53935),
                                selectedTextColor = Color(0xFFE53935),
                                indicatorColor = Color.Transparent,
                                unselectedIconColor = Color(0xFFBDBDBD),
                                unselectedTextColor = Color(0xFFBDBDBD)
                            )

                            val navItems: List<Pair<String, String>> = listOf(
                                Pair(if (viewModel.currentLanguage == "zh") "主页" else "Home", "home"),
                                Pair(if (viewModel.currentLanguage == "zh") "资料库" else "Library", "library"),
                                Pair(if (viewModel.currentLanguage == "zh") "设置" else "Settings", "settings")
                            )

                            // 使用Box作为容器，用于放置背景指示器
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                            ) {
                                // 背景指示器
                                val density = LocalDensity.current
                                var containerWidth by remember { mutableStateOf(0f) }
                                
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .onSizeChanged { size ->
                                            containerWidth = size.width.toFloat()
                                        }
                                ) {
                                    // 背景指示器层 - 只显示一个
                                    val selectedIndex = navItems.indexOfFirst { it.second == currentRoute }
                                    if (selectedIndex >= 0 && containerWidth > 0) {
                                        val itemWidth = containerWidth / navItems.size
                                        val offsetX by animateDpAsState(
                                            targetValue = with(density) { (selectedIndex * itemWidth).toDp() },
                                            animationSpec = tween(durationMillis = 300, easing = EaseInOutCubic),
                                            label = "navOffset"
                                        )
                                        
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .width(with(density) { itemWidth.toDp() })
                                                .offset(x = offsetX)
                                                .padding(vertical = 12.dp, horizontal = 16.dp)
                                                .background(
                                                    color = Color(0xFFE0E0E0),
                                                    shape = RoundedCornerShape(20.dp)
                                                )
                                        )
                                    }
                                    
                                    // 导航项层
                                    Row(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalArrangement = Arrangement.SpaceEvenly,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        navItems.forEachIndexed { index, item ->
                                            val isSelected = currentRoute == item.second
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .fillMaxHeight(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                // 可点击的视觉区域 - 只在文字和背景指示器区域
                                                Box(
                                                    modifier = Modifier
                                                        .width(with(density) { (containerWidth / navItems.size).toDp() })
                                                        .fillMaxHeight()
                                                        .padding(vertical = 12.dp, horizontal = 16.dp)
                                                        .clip(RoundedCornerShape(20.dp))
                                                        .clickable {
                                                            if (currentRoute != item.second) {
                                                                navController.navigate(item.second) { popUpTo("home") { inclusive = true } }
                                                            }
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        item.first,
                                                        fontSize = 14.sp,
                                                        color = if (isSelected) Color(0xFFE53935) else Color(0xFFBDBDBD),
                                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {

            NavHost(navController = navController, startDestination = "home") {
                composable("home") {
                    HomeScreen(
                        viewModel = viewModel,
                        onNavigateToPlayer = { navController.navigate("player") },
                        onPickFile = { filePickerLauncher.launch("audio/*") },
                        onBatchPickFile = { batchFilePickerLauncher.launch(arrayOf("audio/*")) },
                        onPickCover = { coverPickerLauncher.launch("image/*") },
                        onPickLrc = { lrcPickerLauncher.launch("*/*") },
                        onNavigateToCreatePlaylist = { navController.navigate("create_playlist") }
                    )
                }

                composable("library") {
                    LibraryScreen(
                        viewModel = viewModel,
                        onPickFile = { filePickerLauncher.launch("audio/*") },
                        onBatchPickFile = { batchFilePickerLauncher.launch(arrayOf("audio/*")) },
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
                    PlayerScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() },
                        onNavigateToEqualizer = { navController.navigate("equalizer") }
                    )
                }

                composable("equalizer") {
                    EqualizerScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() }
                    )
                }

                composable("settings") {
                    SettingsScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
            }

            AnimatedVisibility(
                visible = viewModel.currentSong != null && currentRoute != "player" && currentRoute != "equalizer",
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