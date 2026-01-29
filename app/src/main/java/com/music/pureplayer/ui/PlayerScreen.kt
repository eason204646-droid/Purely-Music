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
package com.music.PurelyPlayer.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import coil.compose.AsyncImage
import com.music.PurelyPlayer.R
import com.music.PurelyPlayer.viewmodel.PlayerViewModel

@Composable
fun PlayerScreen(viewModel: PlayerViewModel, onBack: () -> Unit) {
    val currentSong = viewModel.currentSong
    val isPlaying = viewModel.isPlaying

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                // 使用 awaitEachGesture 确保每次进入页面及每次触摸都能正确触发逻辑
                awaitEachGesture {
                    // 等待第一次按下
                    val down = awaitFirstDown()
                    val startY = down.position.y

                    // 🚩 限制起始点在屏幕上半部分
                    if (startY < size.height / 2) {
                        var hasTriggered = false

                        while (true) {
                            val event = awaitPointerEvent()
                            val dragAmount = event.changes.firstOrNull()?.let { it.position - it.previousPosition } ?: androidx.compose.ui.geometry.Offset.Zero

                            // 累计向下位移判断
                            val currentY = event.changes.firstOrNull()?.position?.y ?: startY
                            val totalDragY = currentY - startY

                            // 🚩 向下滑动超过阈值 (150f) 且未触发过时执行返回
                            if (totalDragY > 150f && !hasTriggered) {
                                hasTriggered = true
                                event.changes.forEach { it.consume() }
                                onBack()
                                // 注意：这里不需要 break，让 gesture 完成生命周期防止残留
                            }

                            // 如果所有手指抬起，结束本次手势监听
                            if (event.changes.all { !it.pressed }) break
                        }
                    }
                }
            }
    ) {
        // 1. 背景层
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

        // 2. 遮罩层
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)))

        // --- 内容层 (层级 3) ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. 顶部栏 (固定高度)
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.KeyboardArrowDown, null, tint = Color.White, modifier = Modifier.size(32.dp))
                }
                Text("正在播放", modifier = Modifier.weight(1f), textAlign = TextAlign.Center, color = Color.White)
                Spacer(modifier = Modifier.width(48.dp))
            }

            // 2. 封面区 (使用 weight 动态收缩)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier
                        .sizeIn(maxWidth = 300.dp, maxHeight = 300.dp)
                        .aspectRatio(1f),
                    shape = RoundedCornerShape(16.dp),
                    shadowElevation = 12.dp
                ) {
                    AsyncImage(
                        model = currentSong?.coverUri ?: R.drawable.default_cover,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            // 3. 歌曲信息 (固定空间)
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = currentSong?.title ?: "未知曲目",
                    color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = currentSong?.artist ?: "未知艺术家",
                    color = Color.White.copy(alpha = 0.7f), fontSize = 18.sp
                )
            }

            // 4. 进度条 (固定空间)
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp)) {
                Slider(
                    value = viewModel.currentPosition.toFloat(),
                    onValueChange = { viewModel.seekTo(it) },
                    valueRange = 0f..(viewModel.duration.toFloat().coerceAtLeast(1f)),
                    colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.White)
                )
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(formatTime(viewModel.currentPosition), color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                    Spacer(modifier = Modifier.weight(1f))
                    Text(formatTime(viewModel.duration), color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                }
            }

            // 5. 控制按键区 (固定在底部)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { /* 模式 */ }) {
                    Icon(Icons.Default.Refresh, null, tint = Color.White.copy(alpha = 0.6f))
                }

                // 上一曲
                IconButton(onClick = { viewModel.playPrevious() }, modifier = Modifier.size(56.dp)) {
                    Icon(Icons.Default.SkipPrevious, null, tint = Color.White, modifier = Modifier.size(40.dp))
                }

                // 播放暂停
                Surface(
                    onClick = { viewModel.togglePlayPause() },
                    shape = CircleShape,
                    color = Color.White,
                    modifier = Modifier.size(72.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                // 下一曲
                IconButton(onClick = { viewModel.playNext() }, modifier = Modifier.size(56.dp)) {
                    Icon(Icons.Default.SkipNext, null, tint = Color.White, modifier = Modifier.size(40.dp))
                }

                IconButton(onClick = { /* 收藏 */ }) {
                    Icon(Icons.Default.FavoriteBorder, null, tint = Color.White.copy(alpha = 0.6f))
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}