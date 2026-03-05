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

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
import com.music.purelymusic.viewmodel.PlayerViewModel
import kotlinx.coroutines.delay
import kotlin.math.*

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(UnstableApi::class)
@Composable
fun ImmersiveSurroundView(
    viewModel: PlayerViewModel,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    @Suppress("OPT_IN_USAGE")
    val isPlaying = viewModel.isPlaying

    // 浅蓝色主题色
    val themeLightBlue = Color(0xFF4FC3F7)
    val themeBlue = Color(0xFF29B6F6)
    val themeWhite = Color.White

    // 动画时间
    var rotationAngle by remember { mutableFloatStateOf(0f) }

    // 当播放时旋转
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (true) {
                rotationAngle = (rotationAngle + 1f) % 360f
                delay(16) // ~60fps
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1B2A3A), // 深蓝色背景
                        Color(0xFF0D1A24),
                        Color(0xFF050C10)
                    )
                )
            )
    ) {
        // 标题和关闭按钮
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .statusBarsPadding(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "沉浸立体音",
                color = themeWhite,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
            IconButton(onClick = {
                viewModel.surroundMode = PlayerViewModel.SurroundMode.NONE
                viewModel.isSurroundEnabled = false
                onClose()
            }) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "关闭",
                    tint = themeWhite
                )
            }
        }

        // 中心可视化区域
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .aspectRatio(1f)
            ) {
                val size = size
                val centerX = size.width / 2
                val centerY = size.height / 2
                val radius = min(centerX, centerY) * 0.8f

                // 绘制中心圆
                drawCircle(
                    color = themeLightBlue.copy(alpha = 0.2f),
                    radius = radius * 0.3f,
                    center = androidx.compose.ui.geometry.Offset(centerX, centerY)
                )

                // 绘制四个方向的声源图标
                val directions = listOf(0f, 90f, 180f, 270f) // 前、右、后、左
                val icons = listOf("前", "右", "后", "左")

                directions.forEachIndexed { index, angle ->
                    val angleRad = Math.toRadians(angle.toDouble())
                    val iconX = centerX + (radius * 0.6f) * cos(angleRad).toFloat()
                    val iconY = centerY + (radius * 0.6f) * sin(angleRad).toFloat()

                    // 绘制方向圆圈
                    drawCircle(
                        color = themeBlue.copy(alpha = 0.5f),
                        radius = radius * 0.15f,
                        center = androidx.compose.ui.geometry.Offset(iconX, iconY)
                    )

                    // 绘制波纹效果
                    val waveRadius = radius * 0.15f + (sin((rotationAngle + angle * 4) * PI / 180).toFloat() + 1) * radius * 0.05f
                    drawCircle(
                        color = themeLightBlue.copy(alpha = 0.3f),
                        radius = waveRadius,
                        center = androidx.compose.ui.geometry.Offset(iconX, iconY),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }

                // 绘制环绕线
                val path = Path().apply {
                    for (angle in 0..360 step 2) {
                        val angleRad = Math.toRadians(angle.toDouble())
                        val wave = 1 + 0.1f * sin((angle * 8 + rotationAngle * 2) * PI / 180).toFloat()
                        val x = centerX + radius * wave * cos(angleRad).toFloat()
                        val y = centerY + radius * wave * sin(angleRad).toFloat()
                        if (angle == 0) {
                            moveTo(x, y)
                        } else {
                            lineTo(x, y)
                        }
                    }
                    close()
                }
                drawPath(
                    path = path,
                    color = themeLightBlue.copy(alpha = 0.4f),
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }

        // 底部信息卡片
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF2A3A4A).copy(alpha = 0.8f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = "全方位沉浸体验",
                    color = themeWhite,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "多声道环绕 • 四面八方传来",
                    color = themeWhite.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Divider(
                    color = themeLightBlue.copy(alpha = 0.3f),
                    thickness = 1.dp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Text(
                    text = "技术原理",
                    color = themeWhite.copy(alpha = 0.9f),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = "• 模拟前、后、左、右四个虚拟声源\n• 智能混合多声道信号\n• 增强空间混响和反射效果\n• 营造全方位沉浸式听感",
                    color = themeWhite.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodySmall,
                    lineHeight = 20.sp
                )
            }
        }
    }
}