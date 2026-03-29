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

import android.os.Build
import android.util.Log
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.zIndex
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
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

@RequiresApi(Build.VERSION_CODES.S_V2)
@OptIn(UnstableApi::class)
@Composable
fun LyricView(
    viewModel: PlayerViewModel,
    modifier: Modifier = Modifier
) {
    val lyrics = viewModel.lyricLines
    val currentIndex = viewModel.currentLyricIndex
    val showTranslation = viewModel.showTranslation
    val isTranslating = viewModel.isTranslating
    val canTranslate = viewModel.canTranslate
    val translateError = viewModel.translateError
    val translateLogs = viewModel.translateLogs
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    
    // 歌词设置
    val lyricStyle = viewModel.lyricStyle
    val lyricGlowEnabled = viewModel.lyricGlowEnabled
    val lyricFilterEnabled = viewModel.lyricFilterEnabled

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
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "暂无歌词",
                    color = Color.Gray.copy(alpha = 0.5f)
                )
            }
        }

        // 翻译按钮（仅在非中文歌词时显示）
        if (canTranslate && lyrics.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .zIndex(10f) // 确保在最上层
            ) {
                if (isTranslating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    IconButton(
                        onClick = {
                            Log.d("LyricView", "翻译按钮被点击")
                            if (showTranslation) {
                                // 取消翻译
                                viewModel.showTranslation = false
                            } else {
                                // 开始翻译
                                viewModel.translateLyrics()
                            }
                        },
                        modifier = Modifier
                            .background(
                                Color.White.copy(alpha = 0.15f),
                                RoundedCornerShape(50)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Translate,
                            contentDescription = "翻译",
                            tint = if (showTranslation) Color(0xFF4FC3F7) else Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        if (lyrics.isNotEmpty()) {
            // 根据歌词样式选择渲染方式
            if (lyricStyle == "single") {
                // 单行歌词样式
                SingleLineLyricView(
                    lyrics = lyrics,
                    currentIndex = currentIndex,
                    lyricGlowEnabled = lyricGlowEnabled,
                    lyricFilterEnabled = lyricFilterEnabled,
                    showTranslation = showTranslation
                )
            } else {
                // 多行歌词样式（原有样式）
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.Start,
                    contentPadding = PaddingValues(
                        top = targetLineDp,
                        bottom = containerHeightDp - targetLineDp
                    )
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
                    val translationFontSize = 16f // 翻译字体更小

                    // 根据是否为续行调整行高：续行使用更小的行高，其他行使用较大行高
                    val actualLineHeight = if (line.isContinuation) 28.sp else fixedLineHeight

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

                    Column(
                        modifier = Modifier
                            .padding(top = if (line.isContinuation) 0.dp else 8.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                // 点击歌词跳转到对应时间
                                viewModel.seekTo(line.time.toFloat())
                            }
                    ) {
                        // 原文（应用脏字过滤）
                        val displayContent = if (lyricFilterEnabled) {
                            com.music.purelymusic.utils.ProfanityFilter.filter(line.content)
                        } else {
                            line.content
                        }
                        
                        Text(
                            text = displayContent,
                            fontSize = fontSize.sp,
                            lineHeight = actualLineHeight,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Start,
                            style = LocalTextStyle.current.copy(
                                shadow = if (isCurrent && lyricGlowEnabled) Shadow(
                                    color = Color.White.copy(alpha = 0.6f),
                                    offset = Offset(0f, 0f),
                                    blurRadius = shadowBlur
                                ) else null
                            ),
                            modifier = Modifier
                                .alpha(textAlpha)
                                .blur(radius = blurAmount.dp)
                        )

                        // 翻译文本（如果启用翻译且有翻译内容）
                        if (showTranslation && !line.translation.isNullOrEmpty()) {
                            val displayTranslation = if (lyricFilterEnabled) {
                                com.music.purelymusic.utils.ProfanityFilter.filter(line.translation)
                            } else {
                                line.translation
                            }
                            
                            Text(
                                text = displayTranslation,
                                fontSize = translationFontSize.sp,
                                lineHeight = (translationFontSize * 1.4f).sp,
                                fontWeight = FontWeight.Normal,
                                color = Color.White.copy(alpha = 0.7f),
                                textAlign = TextAlign.Start,
                                modifier = Modifier
                                    .padding(top = 4.dp)
                                    .alpha(textAlpha)
                                    .blur(radius = blurAmount.dp)
                            )
                        }
                    }
                }
            }
        }
        }

        // 翻译错误弹窗
        if (translateError != null) {
            var showDetails by remember { mutableStateOf(false) }

            AlertDialog(
                onDismissRequest = { viewModel.translateError = null },
                title = {
                    Text(
                        "翻译失败",
                        color = Color.White,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                },
                text = {
                    Column {
                        Text(
                            translateError,
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "提示：\n1. 请检查网络连接\n2. 可能翻译服务暂时不可用\n3. 可以查看Logcat日志获取详细信息",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 12.sp
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.translateError = null }) {
                        Text("确定", color = Color(0xFF4FC3F7))
                    }
                },
                containerColor = Color(0xFF2A2A2A)
            )
        }
    }
}

/**
 * 单行歌词样式组件
 * 只显示当前播放的一句歌词，带有渐变过渡动画
 */
@Composable
fun SingleLineLyricView(
    lyrics: List<LrcLine>,
    currentIndex: Int,
    lyricGlowEnabled: Boolean,
    lyricFilterEnabled: Boolean,
    showTranslation: Boolean
) {
    val currentLine = if (currentIndex in lyrics.indices) lyrics[currentIndex] else null
    
    // 动画：透明度和缩放
    val alpha by animateFloatAsState(
        targetValue = if (currentLine != null) 1f else 0f,
        animationSpec = tween(
            durationMillis = 300,
            easing = EaseInOutCubic
        ),
        label = "singleLineAlpha"
    )
    
    val scale by animateFloatAsState(
        targetValue = if (currentLine != null) 1f else 0.95f,
        animationSpec = tween(
            durationMillis = 300,
            easing = EaseInOutCubic
        ),
        label = "singleLineScale"
    )
    
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.CenterStart
    ) {
        if (currentLine != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Center
            ) {
                // 主歌词
                val displayContent = if (lyricFilterEnabled) {
                    com.music.purelymusic.utils.ProfanityFilter.filter(currentLine.content)
                } else {
                    currentLine.content
                }
                
                Text(
                    text = displayContent,
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Start,
                    lineHeight = 56.sp,
                    maxLines = 4,
                    style = androidx.compose.ui.text.TextStyle(
                        shadow = if (lyricGlowEnabled) Shadow(
                            color = Color.White.copy(alpha = 0.5f),
                            offset = Offset(0f, 0f),
                            blurRadius = 16f
                        ) else null
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(alpha)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                )
                
                // 翻译文本
                if (showTranslation && !currentLine.translation.isNullOrEmpty()) {
                    val displayTranslation = if (lyricFilterEnabled) {
                        com.music.purelymusic.utils.ProfanityFilter.filter(currentLine.translation)
                    } else {
                        currentLine.translation
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = displayTranslation,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = TextAlign.Start,
                        lineHeight = 32.sp,
                        maxLines = 3,
                        modifier = Modifier
                            .fillMaxWidth()
                            .alpha(alpha)
                    )
                }
            }
        }
    }
}