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
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
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
import androidx.compose.ui.platform.LocalContext
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
import com.music.purelymusic.ui.theme.*
import com.music.purelymusic.viewmodel.PlayerViewModel
import com.music.purelymusic.ui.utils.AppDimensions
import com.music.purelymusic.utils.PreferencesManager
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 配置 Coil 图片加载器（带缓存）
        val imageLoader = ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("coil_cache"))
                    .maxSizeBytes(50 * 1024 * 1024)
                    .build()
            }
            .okHttpClient {
                okhttp3.OkHttpClient.Builder()
                    .build()
            }
            .build()
        coil.Coil.setImageLoader(imageLoader)

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

    val isPrimaryRoute = currentRoute == "home" || currentRoute == "library" || currentRoute == "settings"
    var showWhatsNew by remember { mutableStateOf(com.music.purelymusic.utils.PreferencesManager.shouldShowReleaseNotes(ReleaseNotes.version)) }
    val context = LocalContext.current
    var showOnboarding by remember { mutableStateOf(PreferencesManager.shouldShowOnboarding(context)) }
    val navigationHazeState = remember { HazeState() }
    val glassMenuHost = remember { GlassMenuHostState() }

    if (showOnboarding) {
        OnboardingScreen(language = viewModel.currentLanguage) {
            PreferencesManager.markOnboardingCompleted()
            PreferencesManager.markReleaseNotesSeen(ReleaseNotes.version)
            showWhatsNew = false
            showOnboarding = false
        }
        return
    }

    CompositionLocalProvider(
        LocalLiquidGlassHazeState provides navigationHazeState,
        LocalGlassDialogHazeState provides navigationHazeState,
        LocalGlassMenuHost provides glassMenuHost
    ) {
    Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {

            NavHost(
                navController = navController,
                startDestination = "home",
                modifier = Modifier.hazeSource(navigationHazeState)
            ) {
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
                        bottomContentPadding = if (viewModel.currentSong != null) AppDimensions.miniPlayerHeight() + 100.dp else 88.dp,
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
                    .padding(bottom = if (isPrimaryRoute) 76.dp else AppDimensions.paddingSmall())
            ) {
                CompositionLocalProvider(LocalLiquidGlassHazeState provides navigationHazeState) {
                    MiniPlayer(
                        viewModel = viewModel,
                        onClick = { navController.navigate("player") }
                    )
                }
            }

            if (showWhatsNew) {
                GlassDialog(onDismissRequest = {
                    com.music.purelymusic.utils.PreferencesManager.markReleaseNotesSeen(ReleaseNotes.version)
                    showWhatsNew = false
                }) {
                    // The dialog shares the activity page source for live glass blur.
                    WhatsNewSheet(language = viewModel.currentLanguage, onDismiss = {
                        com.music.purelymusic.utils.PreferencesManager.markReleaseNotesSeen(ReleaseNotes.version)
                        showWhatsNew = false
                    })
                }
            }
        }
    }
    if (isPrimaryRoute) {
        CompositionLocalProvider(LocalLiquidGlassHazeState provides navigationHazeState) {
            LiquidGlassNavigationBar(
                currentRoute = currentRoute,
                language = viewModel.currentLanguage,
                onNavigate = { route ->
                    if (currentRoute != route) {
                        navController.navigate(route) { popUpTo("home") { inclusive = true } }
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            )
        }
    }
    CompositionLocalProvider(LocalLiquidGlassHazeState provides navigationHazeState) {
        GlassMenuHost(glassMenuHost)
    }
    }
    }
}

@Composable
private fun LiquidGlassNavigationBar(
    currentRoute: String?,
    language: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        (if (language == "zh") "主页" else "Home") to "home",
        (if (language == "zh") "资料库" else "Library") to "library",
        (if (language == "zh") "设置" else "Settings") to "settings"
    )
    LiquidGlass(
        modifier = modifier.fillMaxWidth().height(60.dp),
        shape = RoundedCornerShape(28.dp),
        opacity = 0.60f,
        highlightAlpha = 0.20f,
        edgeAlpha = 0.32f,
        shadowElevation = 0.dp
    ) {
        Row(modifier = Modifier.fillMaxSize().padding(5.dp), verticalAlignment = Alignment.CenterVertically) {
            items.forEach { (label, route) ->
                val selected = currentRoute == route
                GlassPressable(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    onClick = { onNavigate(route) }
                ) {
                    if (selected) {
                        Box(
                            modifier = Modifier.fillMaxSize().background(RedPrimary.copy(alpha = 0.16f), RoundedCornerShape(22.dp))
                        )
                    }
                    Text(
                        label,
                        fontSize = 12.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        color = if (selected) AppleRed else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
