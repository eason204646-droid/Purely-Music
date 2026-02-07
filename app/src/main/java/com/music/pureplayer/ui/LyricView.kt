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
package com.music.PurelyPlayer.ui

import androidx.annotation.OptIn
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
// 导入对应的包
import com.music.PurelyPlayer.viewmodel.PlayerViewModel
import com.music.PurelyPlayer.model.LrcLine
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow

@OptIn(UnstableApi::class)
@Composable
fun LyricView(
    viewModel: PlayerViewModel,
    modifier: Modifier = Modifier
) {
    val lyrics = viewModel.lyricLines
    val currentIndex = viewModel.currentLyricIndex
    val listState = rememberLazyListState()
    val density = LocalDensity.current

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 0.dp),
        contentAlignment = Alignment.TopStart
    ) {
        val containerHeightPx = with(density) { maxHeight.toPx() }
        val containerHeightDp = maxHeight

        // 🚩 目标位置：屏幕上方 1/4 (25%) 处
        val targetLinePx = containerHeightPx * 0.25f
        val targetLineDp = containerHeightDp * 0.25f

        // 估算单行行高（包含文字和间距）用于平滑偏移
        val lineHeightPx = with(density) { 56.dp.toPx() }

        LaunchedEffect(currentIndex) {
            if (lyrics.isNotEmpty() && currentIndex in lyrics.indices) {
                listState.animateScrollToItem(
                    index = currentIndex,
                    // 🚩 偏移计算：将当前项的顶部对齐到 1/4 处，并向上修正半行高度实现垂直居中于该线
                    scrollOffset = (-targetLinePx + (lineHeightPx / 2)).toInt()
                )
            }
        }

        if (lyrics.isEmpty()) {
            Text(
                text = "暂无歌词",
                color = Color.Gray.copy(alpha = 0.5f),
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.Start, // 靠左对齐
                // 🚩 重点：顶部留 1/4，底部留 3/4。
                contentPadding = PaddingValues(
                    top = targetLineDp,
                    bottom = containerHeightDp - targetLineDp
                ),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                itemsIndexed(
                    items = lyrics,
                    key = { index, line -> "${line.time}_$index" }
                ) { index, line ->
                    val isCurrent = index == currentIndex

                    val fontSize by animateFloatAsState(targetValue = if (isCurrent) 24f else 18f, label = "fontSize")
                    val textAlpha by animateFloatAsState(targetValue = if (isCurrent) 1f else 0.4f, label = "textAlpha")

                    // 🚩 定义一个发光模糊半径的动画，当前行会有 12f 的光晕，非当前行无光晕
                    val shadowBlur by animateFloatAsState(
                        targetValue = if (isCurrent) 12f else 0f,
                        label = "shadowBlur"
                    )

                    Text(
                        text = line.content,
                        fontSize = fontSize.sp,
                        lineHeight = (fontSize * 1.4f).sp,
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                        color = Color.White,
                        textAlign = TextAlign.Start,
                        style = LocalTextStyle.current.copy(
                            // 🚩 核心：添加发光阴影效果
                            shadow = if (isCurrent) Shadow(
                                color = Color.White.copy(alpha = 0.6f), // 发光颜色：带透明度的白
                                offset = Offset(0f, 0f),                // 偏移为0，光晕均匀向四周扩散
                                blurRadius = shadowBlur                 // 模糊半径
                            ) else null
                        ),
                        modifier = Modifier
                            .alpha(textAlpha)
                            .padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}