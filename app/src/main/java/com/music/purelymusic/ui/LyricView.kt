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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
import com.music.purelymusic.model.LrcLine
import com.music.purelymusic.utils.ProfanityFilter
import com.music.purelymusic.viewmodel.PlayerViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.abs

private val LyricScrollEasing = CubicBezierEasing(0.24f, 0.58f, 0.24f, 1f)
private val LyricToolbarHeight = 48.dp

@OptIn(UnstableApi::class)
@Composable
fun LyricView(
    viewModel: PlayerViewModel,
    modifier: Modifier = Modifier
) {
    val lyrics = viewModel.lyricLines
    val currentIndex = viewModel.currentLyricIndex
    val showTranslation = viewModel.showTranslation
    val hasTranslation = remember(lyrics) { lyrics.any { !it.translation.isNullOrBlank() } }
    val chinese = viewModel.currentLanguage == "zh"
    val canShowTranslate = viewModel.canTranslate && lyrics.isNotEmpty()
    val translateError = viewModel.translateError

    Box(modifier.fillMaxSize()) {
        when {
            lyrics.isEmpty() -> Text(
                text = if (chinese) "暂无歌词" else "No lyrics available",
                color = Color.White.copy(alpha = 0.68f),
                modifier = Modifier.align(Alignment.Center)
            )
            viewModel.lyricStyle == "single" -> Box(
                Modifier.fillMaxSize().padding(top = if (canShowTranslate) LyricToolbarHeight else 0.dp)
            ) {
                SingleLineLyricView(
                    lyrics = lyrics,
                    currentIndex = currentIndex,
                    lyricGlowEnabled = viewModel.lyricGlowEnabled,
                    lyricFilterEnabled = viewModel.lyricFilterEnabled,
                    showTranslation = showTranslation
                )
            }
            else -> key(viewModel.currentSong?.id) {
                ScrollingLyrics(
                    lyrics = lyrics,
                    currentIndex = currentIndex,
                    showTranslation = showTranslation,
                    glowEnabled = viewModel.lyricGlowEnabled,
                    filterEnabled = viewModel.lyricFilterEnabled,
                    followLabel = if (chinese) "跟随歌词" else "Follow playback",
                    onSeek = { viewModel.seekTo(it.toFloat()) }
                )
            }
        }

        if (canShowTranslate) {
            val enabled = !viewModel.offlineMode || showTranslation || hasTranslation
            Box(
                modifier = Modifier.align(Alignment.TopEnd).size(LyricToolbarHeight),
                contentAlignment = Alignment.Center
            ) {
                if (viewModel.isTranslating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    IconButton(
                        onClick = {
                            if (showTranslation || hasTranslation) viewModel.toggleTranslation()
                            else viewModel.translateLyrics()
                        },
                        enabled = enabled,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = Color.Black.copy(alpha = 0.24f),
                            contentColor = if (showTranslation) Color(0xFFB4D9E8) else Color.White,
                            disabledContentColor = Color.White.copy(alpha = 0.35f)
                        )
                    ) {
                        Icon(
                            Icons.Default.Translate,
                            contentDescription = when {
                                !enabled && chinese -> "离线模式下歌词翻译已暂停"
                                !enabled -> "Translation paused in offline mode"
                                chinese -> "翻译"
                                else -> "Translate"
                            },
                            modifier = Modifier.size(21.dp)
                        )
                    }
                }
            }
        }

        // 翻译错误弹窗
        if (translateError != null) {
            GlassAlertDialog(
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

@Composable
private fun ScrollingLyrics(
    lyrics: List<LrcLine>,
    currentIndex: Int,
    showTranslation: Boolean,
    glowEnabled: Boolean,
    filterEnabled: Boolean,
    followLabel: String,
    onSeek: (Long) -> Unit
) {
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = currentIndex)
    val isDragged by listState.interactionSource.collectIsDraggedAsState()
    var browsing by remember { mutableStateOf(false) }

    LaunchedEffect(isDragged) {
        if (isDragged) {
            browsing = true
        } else if (browsing) {
            snapshotFlow { listState.isScrollInProgress }.collectLatest { scrolling ->
                if (!scrolling && browsing) {
                    delay(2400)
                    browsing = false
                }
            }
        }
    }

    LaunchedEffect(currentIndex, isDragged, browsing, showTranslation) {
        if (browsing || isDragged || currentIndex !in lyrics.indices) return@LaunchedEffect
        val offset = listState.layoutInfo.visibleItemsInfo
            .firstOrNull { it.index == currentIndex }?.offset

        // Large seeks settle directly. Nearby lines travel together with their measured spacing.
        when {
            abs(currentIndex - listState.firstVisibleItemIndex) > 5 -> listState.scrollToItem(currentIndex)
            offset == null -> listState.animateScrollToItem(currentIndex)
            offset != 0 -> listState.scroll {
                val duration = (330 + abs(offset).toFloat() /
                    listState.layoutInfo.viewportSize.height.coerceAtLeast(1) * 420).toInt().coerceAtMost(580)
                var previous = 0f
                animate(
                    initialValue = 0f,
                    targetValue = offset.toFloat(),
                    animationSpec = tween(duration, easing = LyricScrollEasing)
                ) { value, _ ->
                    scrollBy(value - previous)
                    previous = value
                }
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        BoxWithConstraints(Modifier.fillMaxSize().padding(top = LyricToolbarHeight)) {
            val focusTop = maxHeight * 0.24f
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().lyricViewportFade(),
                contentPadding = PaddingValues(top = focusTop, bottom = maxHeight - focusTop)
            ) {
                itemsIndexed(lyrics, key = { index, line -> "${line.time}_$index" }) { index, line ->
                    ScrollingLyricLine(
                        line = line,
                        distanceFromCurrent = index - currentIndex,
                        browsing = browsing || isDragged,
                        showTranslation = showTranslation,
                        glowEnabled = glowEnabled,
                        filterEnabled = filterEnabled,
                        onClick = {
                            onSeek(line.time)
                            browsing = false
                        }
                    )
                }
            }
        }

        if (browsing && !isDragged) {
            TextButton(
                onClick = { browsing = false },
                colors = ButtonDefaults.textButtonColors(
                    containerColor = Color.Black.copy(alpha = 0.35f),
                    contentColor = Color.White
                ),
                modifier = Modifier.align(Alignment.TopStart)
            ) {
                Text(followLabel)
            }
        }
    }
}

private fun Modifier.lyricViewportFade(): Modifier =
    graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawWithCache {
            val fade = Brush.verticalGradient(
                0f to Color.Transparent,
                0.14f to Color.Black,
                0.79f to Color.Black,
                1f to Color.Transparent
            )
            onDrawWithContent {
                drawContent()
                drawRect(fade, blendMode = BlendMode.DstIn)
            }
        }

@Composable
private fun ScrollingLyricLine(
    line: LrcLine,
    distanceFromCurrent: Int,
    browsing: Boolean,
    showTranslation: Boolean,
    glowEnabled: Boolean,
    filterEnabled: Boolean,
    onClick: () -> Unit
) {
    val active = distanceFromCurrent == 0
    val direction = LocalLayoutDirection.current
    val opacity = animateFloatAsState(
        targetValue = when {
            active -> 1f
            browsing -> 0.84f
            distanceFromCurrent in 1..2 -> 0.67f
            distanceFromCurrent < 0 -> 0.52f
            else -> 0.55f
        },
        animationSpec = tween(if (active) 240 else 360, easing = FastOutSlowInEasing),
        label = "Lyric opacity"
    )
    val scale = animateFloatAsState(
        targetValue = if (active || browsing) 1f else 0.97f,
        animationSpec = tween(340, easing = FastOutSlowInEasing),
        label = "Lyric emphasis"
    )

    Column(
        modifier = Modifier.fillMaxWidth()
            .clickable(interactionSource = null, indication = null, onClick = onClick)
            .semantics { selected = active }
            .padding(top = if (line.isContinuation) 2.dp else 10.dp, bottom = 7.dp, end = 4.dp)
            .graphicsLayer {
                alpha = opacity.value
                scaleX = scale.value
                scaleY = scale.value
                transformOrigin = TransformOrigin(if (direction == LayoutDirection.Ltr) 0f else 1f, 0.5f)
            }
    ) {
        val content = remember(line.content, filterEnabled) {
            if (filterEnabled) ProfanityFilter.filter(line.content) else line.content
        }
        Text(
            text = content,
            color = Color.White,
            fontSize = 25.sp,
            lineHeight = 33.sp,
            fontWeight = FontWeight.Bold,
            style = LocalTextStyle.current.copy(
                shadow = if (active && glowEnabled) Shadow(
                    color = Color.White.copy(alpha = 0.52f),
                    offset = Offset.Zero,
                    blurRadius = 12f
                ) else null
            )
        )

        val translation = line.translation
        if (showTranslation && !translation.isNullOrBlank()) {
            val display = remember(translation, filterEnabled) {
                if (filterEnabled) ProfanityFilter.filter(translation) else translation
            }
            Text(
                text = display,
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 15.sp,
                lineHeight = 21.sp,
                modifier = Modifier.padding(top = 3.dp)
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
