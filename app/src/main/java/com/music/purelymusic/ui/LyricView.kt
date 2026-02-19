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
package com.music.purelymusic.ui

import androidx.annotation.OptIn
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
import com.music.purelymusic.model.LrcLine
import com.music.purelymusic.viewmodel.PlayerViewModel
import kotlinx.coroutines.delay

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

    // 动画触发器：当歌词切换时,增加触发计数,重新触发波浪动画
    var animationTrigger by remember { mutableIntStateOf(0) }
    LaunchedEffect(currentIndex) {
        animationTrigger++
    }

    // 模糊参数
    val blurRadius = 3f  // 统一模糊度（可分辨文字）

    // 检测用户是否在手动滑动
    var isUserScrolling by remember { mutableStateOf(false) }
    var scrollInProgress by remember { mutableStateOf(false) }

    // 监听列表的交互状态和滚动状态
    LaunchedEffect(listState) {
        snapshotFlow { listState.interactionSource }
            .collect { interactionSource ->
                interactionSource.interactions.collect { interaction ->
                    when (interaction) {
                        is DragInteraction.Start -> {
                            isUserScrolling = true
                        }
                        is DragInteraction.Stop, is DragInteraction.Cancel -> {
                            delay(200)
                            isUserScrolling = false
                        }
                    }
                }
            }
    }

    // 监听滚动是否在进行中
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .collect { inProgress ->
                scrollInProgress = inProgress
            }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 0.dp),
        contentAlignment = Alignment.TopStart
    ) {
        val containerHeightPx = with(density) { maxHeight.toPx() }
        val containerHeightDp = maxHeight

        // 目标位置：屏幕上方，与播放器界面设计匹配
        val targetLinePx = containerHeightPx * 0.20f
        val targetLineDp = containerHeightDp * 0.20f

        // 使用固定的行高，避免因字体大小变化导致的行高差异
        // 设置更大的行高以容纳24sp的粗体字体
        val fixedLineHeight = 60.sp
        val fixedLineHeightPx = with(density) { fixedLineHeight.toPx() }
        val itemSpacingPx = with(density) { 8.dp.toPx() }

        // 使用丝滑的滚动动画 - 改进版本
        LaunchedEffect(currentIndex) {
            if (lyrics.isNotEmpty() && currentIndex in lyrics.indices && !isUserScrolling && !scrollInProgress) {
                // 使用带动画的滚动，但确保不会与属性动画冲突
                listState.animateScrollToItem(
                    index = currentIndex,
                    // 偏移计算：将当前行的中心对齐到目标位置
                    scrollOffset = (-targetLinePx + (fixedLineHeightPx / 2)).toInt()
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
                horizontalAlignment = Alignment.Start,
                contentPadding = PaddingValues(
                    top = targetLineDp,
                    bottom = containerHeightDp - targetLineDp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(
                    items = lyrics,
                    key = { index, line -> "${line.time}_$index" }
                ) { index, line ->
                    val isCurrent = index == currentIndex

                    // 计算与当前行的距离，用于波浪式动画延迟
                    val distanceFromCurrent = index - currentIndex

                    // 所有歌词使用统一的字体大小，确保排版完全一致
                    val fontSize = 24f

                    // 透明度动画也添加延迟，形成波浪效果
                    val textAlpha by animateFloatAsState(
                        targetValue = if (isCurrent) 1f else 0.4f,
                        label = "textAlpha_$index",
                        animationSpec = tween(
                            durationMillis = 350,
                            easing = EaseInOutCubic,
                            delayMillis = if (isCurrent) {
                                0
                            } else {
                                // 根据距离计算延迟,形成波浪效果
                                val absDistance = kotlin.math.abs(distanceFromCurrent)
                                (absDistance * 40).coerceAtMost(250)
                            }
                        )
                    )

                    val shadowBlur by animateFloatAsState(
                        targetValue = if (isCurrent) 12f else 0f,
                        label = "shadowBlur_$index",
                        animationSpec = tween(
                            durationMillis = 350,
                            easing = EaseInOutCubic,
                            delayMillis = if (isCurrent) {
                                0
                            } else {
                                // 根据距离计算延迟,形成波浪效果
                                val absDistance = kotlin.math.abs(distanceFromCurrent)
                                (absDistance * 40).coerceAtMost(250)
                            }
                        )
                    )

                    // 模糊动画也添加延迟，优化过渡效果
                    val blurAmount by animateFloatAsState(
                        targetValue = if (!isUserScrolling && !isCurrent) blurRadius else 0f,
                        label = "blurAmount_$index",
                        animationSpec = tween(
                            durationMillis = 400,
                            easing = FastOutSlowInEasing,
                            delayMillis = if (isCurrent) {
                                0
                            } else if (isUserScrolling) {
                                // 用户滚动时立即清除模糊，但保持平滑
                                0
                            } else {
                                // 根据距离计算延迟,形成波浪效果
                                val absDistance = kotlin.math.abs(distanceFromCurrent)
                                (absDistance * 40).coerceAtMost(250)
                            }
                        )
                    )

                    Text(
                        text = line.content,
                        fontSize = fontSize.sp,
                        lineHeight = fixedLineHeight,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Start,
                        style = LocalTextStyle.current.copy(
                            shadow = if (isCurrent) Shadow(
                                color = Color.White.copy(alpha = 0.6f),
                                offset = Offset(0f, 0f),
                                blurRadius = shadowBlur
                            ) else null
                        ),
                        modifier = Modifier
                            .alpha(textAlpha)
                            .blur(radius = blurAmount.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                // 点击歌词跳转到对应时间
                                viewModel.seekTo(line.time.toFloat())
                            }
                    )
                }
            }
        }
    }
}