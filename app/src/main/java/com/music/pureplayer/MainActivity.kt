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
//
//Your reproduction, use, modification and distribution of the Software shall
//be subject to Mulan PSL v2 (this License) with the following terms and
//conditions:
//
//0. Definition
//
//Software means the program and related documents which are licensed under
//this License and comprise all Contribution(s).
//
//Contribution means the copyrightable work licensed by a particular
//Contributor under this License.
//
//Contributor means the Individual or Legal Entity who licenses its
//copyrightable work under this License.
//
//Legal Entity means the entity making a Contribution and all its
//Affiliates.
//
//Affiliates means entities that control, are controlled by, or are under
//common control with the acting entity under this License, ‘control’ means
//direct or indirect ownership of at least fifty percent (50%) of the voting
//power, capital or other securities of controlled or commonly controlled
//entity.
//
//1. Grant of Copyright License
//
//Subject to the terms and conditions of this License, each Contributor hereby
//grants to you a perpetual, worldwide, royalty-free, non-exclusive,
//irrevocable copyright license to reproduce, use, modify, or distribute its
//Contribution, with modification or not.
//
//2. Grant of Patent License
//
//Subject to the terms and conditions of this License, each Contributor hereby
//grants to you a perpetual, worldwide, royalty-free, non-exclusive,
//irrevocable (except for revocation under this Section) patent license to
//make, have made, use, offer for sale, sell, import or otherwise transfer its
//Contribution, where such patent license is only limited to the patent claims
//owned or controlled by such Contributor now or in future which will be
//necessarily infringed by its Contribution alone, or by combination of the
//Contribution with the Software to which the Contribution was contributed.
//The patent license shall not apply to any modification of the Contribution,
//and any other combination which includes the Contribution. If you or your
//Affiliates directly or indirectly institute patent litigation (including a
//cross claim or counterclaim in a litigation) or other patent enforcement
//activities against any individual or entity by alleging that the Software or
//any Contribution in it infringes patents, then any patent license granted to
//you under this License for the Software shall terminate as of the date such
//litigation or activity is filed or taken.
//
//3. No Trademark License
//
//No trademark license is granted to use the trade names, trademarks, service
//marks, or product names of Contributor, except as required to fulfill notice
//requirements in section 4.
//
//4. Distribution Restriction
//
//You may distribute the Software in any medium with or without modification,
//whether in source or executable forms, provided that you provide recipients
//with a copy of this License and retain copyright, patent, trademark and
//disclaimer statements in the Software.
//
//5. Disclaimer of Warranty and Limitation of Liability
//
//THE SOFTWARE AND CONTRIBUTION IN IT ARE PROVIDED WITHOUT WARRANTIES OF ANY
//KIND, EITHER EXPRESS OR IMPLIED. IN NO EVENT SHALL ANY CONTRIBUTOR OR
//COPYRIGHT HOLDER BE LIABLE TO YOU FOR ANY DAMAGES, INCLUDING, BUT NOT
//LIMITED TO ANY DIRECT, OR INDIRECT, SPECIAL OR CONSEQUENTIAL DAMAGES ARISING
//FROM YOUR USE OR INABILITY TO USE THE SOFTWARE OR THE CONTRIBUTION IN IT, NO
//MATTER HOW IT’S CAUSED OR BASED ON WHICH LEGAL THEORY, EVEN IF ADVISED OF
//THE POSSIBILITY OF SUCH DAMAGES.
//
//6. Language
//
//THIS LICENSE IS WRITTEN IN BOTH CHINESE AND ENGLISH, AND THE CHINESE VERSION
//AND ENGLISH VERSION SHALL HAVE THE SAME LEGAL EFFECT. IN THE CASE OF
//DIVERGENCE BETWEEN THE CHINESE AND ENGLISH VERSIONS, THE CHINESE VERSION
//SHALL PREVAIL.
//
//END OF THE TERMS AND CONDITIONS
//
//How to Apply the Mulan Permissive Software License，Version 2
//(Mulan PSL v2) to Your Software
//
//To apply the Mulan PSL v2 to your work, for easy identification by
//recipients, you are suggested to complete following three steps:
//
//i. Fill in the blanks in following statement, including insert your software
//name, the year of the first publication of your software, and your name
//identified as the copyright owner;
//
//ii. Create a file named "LICENSE" which contains the whole context of this
//License in the first directory of your software package;
//
//iii. Attach the statement to the appropriate annotated syntax at the
//beginning of each source file.
//
//Copyright (c) [Year] [name of copyright holder]
//[Software Name] is licensed under Mulan PSL v2.
//You can use this software according to the terms and conditions of the Mulan
//PSL v2.
//You may obtain a copy of Mulan PSL v2 at:
//         http://license.coscl.org.cn/MulanPSL2
//THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY
//KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
//NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
//See the Mulan PSL v2 for more details.
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
                            onNavigateToPlayer = { navController.navigate("player") },
                            // 🚩 修复点 1：在这里传入跳转逻辑
                            onNavigateToEditPlaylist = { id ->
                                navController.navigate("edit_playlist/$id")
                            }
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