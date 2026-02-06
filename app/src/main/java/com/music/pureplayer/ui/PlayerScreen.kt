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
//Mulan Permissive Software License，Version 2
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
//Affiliates means entities that control, are controlled by, or are under
//common control with the acting entity under this License, 'control' means
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
//Contribution where such patent license is only limited to the patent claims
//owned or controlled by such Contributor now or in future which will be
//necessarily infringed by its Contribution alone, or by combination of the
//Contribution with the Software to which the Contribution was contributed.
//
//The patent license shall not apply to any modification of the Contribution,
//and any other combination which includes the Contribution in it, or to any
//other combination which includes the Contribution in it.
//
//3. No Trademark License
//
//No trademark license is granted to use the trade names, trademarks, service
//marks, or product names of Contributor, except as required for fulfillment of
//notice requirements in section 4.
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
//KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
//NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
//See the Mulan PSL v2 for more details.
//
//6. Language
//
//THIS LICENSE IS WRITTEN IN BOTH CHINESE AND ENGLISH, AND THE CHINESE VERSION
//AND ENGLISH VERSION SHALL HAVE THE SAME LEGAL EFFECT. IN THE CASE OF
//DIVERGENCE BETWEEN THE CHINESE VERSION AND ENGLISH VERSION, THE CHINESE
//VERSION SHALL PREVAIL.
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
package com.music.PurelyPlayer.ui

import androidx.annotation.OptIn
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
import coil.compose.AsyncImage
import com.music.PurelyPlayer.R
import com.music.PurelyPlayer.viewmodel.PlayerViewModel

@OptIn(UnstableApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(viewModel: PlayerViewModel, onBack: () -> Unit) {
    val currentSong = viewModel.currentSong
    val isPlaying = viewModel.isPlaying
    var showLyrics by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A1A1A),
                        Color(0xFF0D0D0D),
                        Color(0xFF000000)
                    )
                )
            )
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    val startY = down.position.y
                    if (startY < size.height / 2) {
                        var hasTriggered = false
                        while (true) {
                            val event = awaitPointerEvent()
                            val currentY = event.changes.firstOrNull()?.position?.y ?: startY
                            val totalDragY = currentY - startY
                            if (totalDragY > 150f && !hasTriggered) {
                                hasTriggered = true
                                event.changes.forEach { it.consume() }
                                onBack()
                            }
                            if (event.changes.all { !it.pressed }) break
                        }
                    }
                }
            }
    ) {
        // --- 背景层 (Crossfade 模糊图) ---
        Crossfade(targetState = viewModel.blurredBackground, label = "Blur") { bitmap ->
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(modifier = Modifier.fillMaxSize().background(Color(0xFF1A1A1A)))
            }
        }

        // --- 遮罩层 ---
        Box(modifier = Modifier.fillMaxSize().background(
            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                colors = listOf(Color.Black.copy(alpha = 0.4f), Color.Black.copy(alpha = 0.85f))
            )
        ))

        // --- 内容层 ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 顶部提示条
            Box(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), contentAlignment = Alignment.Center) {
                Box(modifier = Modifier.width(60.dp).height(4.dp).background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(2.dp)))
            }

            // 1. 歌词/封面显示区域 (保持较大权重)
            Box(
                modifier = Modifier
                    .weight(3.0f) // 稍微增加权重
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Crossfade(targetState = showLyrics, label = "ContentSwitch") { isLyrics ->
                    if (isLyrics) {
                        LyricView(viewModel = viewModel, modifier = Modifier.fillMaxSize())
                    } else {
                        // ... 保持原有封面 Surface 代码不变 ...
                        Surface(
                            modifier = Modifier.fillMaxWidth(0.8f).aspectRatio(1f).clickable { showLyrics = true },
                            shape = RoundedCornerShape(20.dp),
                            shadowElevation = 16.dp
                        ) {
                            AsyncImage(
                                model = currentSong?.coverUri ?: R.drawable.default_cover,
                                contentDescription = null,
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }

            // 2. 歌曲信息区域 + 模式切换按钮 (将切换键移到这里)
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = currentSong?.title ?: "未知曲目", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(text = currentSong?.artist ?: "未知艺术家", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                }

                // 🚩 原本底部的歌词键上移到此处
                IconButton(onClick = { showLyrics = !showLyrics }) {
                    Icon(
                        imageVector = if (showLyrics) Icons.Default.Album else Icons.Default.Notes,
                        contentDescription = "模式切换",
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // 3. 进度条区域
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
                Slider(
                    value = viewModel.currentPosition.toFloat(),
                    onValueChange = { viewModel.seekTo(it) },
                    valueRange = 0f..(viewModel.duration.toFloat().coerceAtLeast(1f)),
                    colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.White, inactiveTrackColor = Color.White.copy(alpha = 0.3f))
                )
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(formatTime(viewModel.currentPosition), color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                    Spacer(modifier = Modifier.weight(1f))
                    Text(formatTime(viewModel.duration), color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                }
            }

            // 4. 底部控制区 (居中悬浮逻辑)
            Box(
                modifier = Modifier
                    .weight(1.5f) // 这里的权重负责进度条与底部的间距
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 收藏键
                    IconButton(onClick = { /* 收藏逻辑 */ }, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.FavoriteBorder, null, tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(26.dp))
                    }

                    // 上一首
                    IconButton(onClick = { viewModel.playPrevious() }, modifier = Modifier.size(70.dp)) {
                        Icon(Icons.Default.SkipPrevious, null, tint = Color.White, modifier = Modifier.size(42.dp))
                    }

                    // 播放/暂停 (绝对居中)
                    Surface(
                        onClick = { viewModel.togglePlayPause() },
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.15f), // 给播放按钮加一个淡淡的底圈，更美观
                        modifier = Modifier.size(90.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(56.dp)
                            )
                        }
                    }

                    // 下一首
                    IconButton(onClick = { viewModel.playNext() }, modifier = Modifier.size(70.dp)) {
                        Icon(Icons.Default.SkipNext, null, tint = Color.White, modifier = Modifier.size(42.dp))
                    }

                    // 更多/列表键 (填补收藏键对称位置，或者保留空白)
                    IconButton(onClick = { /* 其他逻辑 */ }, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.MoreHoriz, null, tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(26.dp))
                    }
                }
            }

            // 底部留出一小段空白，让按钮不至于贴着导航栏
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return java.lang.String.format(java.util.Locale.getDefault(), "%02d:%02d", minutes, seconds)
}