//Copyright (c) [2026] [eason204646]
//[purelymusic] is licensed under Mulan PSL v2.
//You can use this software according to the terms and conditions of the Mulan
//PSL v2.
//You may obtain a copy of Mulan PSL v2 at:
//         http://license.coscl.org.cn/MulanPSL2
//THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY
//KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT LIMITED TO
//NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
//See the Mulan PSL v2 for more details.
//
//Mulan Permissive Software License，Version 2
//
//Mulan Permissive Software License，Version 2
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
fun ThreeDSurroundView(
    viewModel: PlayerViewModel,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    @Suppress("OPT_IN_USAGE")
    val isPlaying = viewModel.isPlaying
    val radius by remember { derivedStateOf { viewModel.surroundRadius } }
    val speed by remember { derivedStateOf { viewModel.surroundSpeed } }

    // 浅红色主题色
    val themeLightRed = Color(0xFFFF9999)
    val themeRed = Color(0xFFE57373)
    val themeWhite = Color.White

    // 动画时间
    var rotationAngle by remember { mutableFloatStateOf(0f) }
    val animatedRadius by animateFloatAsState(
        targetValue = radius,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 300f),
        label = "radius"
    )
    val animatedSpeed by animateFloatAsState(
        targetValue = speed,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 300f),
        label = "speed"
    )

    // 当播放时旋转
    LaunchedEffect(isPlaying, animatedSpeed) {
        if (isPlaying) {
            while (true) {
                rotationAngle = (rotationAngle + animatedSpeed) % 360f
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
                        Color(0xFF2C1B1B), // 深红色背景
                        Color(0xFF1A1010),
                        Color(0xFF0F0808)
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
                text = "HRTF 3D环绕立体声",
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

        // 3D环绕可视化
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(32.dp)
            ) {
                val centerX = size.width / 2
                val centerY = size.height / 2

                // 绘制多个音源点模拟环绕效果
                val soundSourceCount = 8
                val circleRadius = animatedRadius * (min(size.width, size.height) / 500f)

                // 绘制外围圆环轨迹
                drawCircle(
                    color = themeLightRed.copy(alpha = 0.3f),
                    radius = circleRadius,
                    center = androidx.compose.ui.geometry.Offset(centerX, centerY),
                    style = Stroke(width = 2.dp.toPx())
                )

                // 绘制移动的音源点
                for (i in 0 until soundSourceCount) {
                    val angleRad = Math.toRadians((rotationAngle + (i * 360f / soundSourceCount)).toDouble())
                    val x = centerX + cos(angleRad).toFloat() * circleRadius
                    val y = centerY + sin(angleRad).toFloat() * circleRadius

                    // 音源点
                    drawCircle(
                        color = themeRed.copy(alpha = 0.8f),
                        radius = 12.dp.toPx() + (i * 1.5f).dp.toPx(),
                        center = androidx.compose.ui.geometry.Offset(x, y)
                    )

                    // 音源点外圈光晕
                    drawCircle(
                        color = themeLightRed.copy(alpha = 0.4f),
                        radius = (18.dp.toPx() + (i * 2f).dp.toPx()),
                        center = androidx.compose.ui.geometry.Offset(x, y),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }

                // 中心区域
                drawCircle(
                    color = themeWhite.copy(alpha = 0.1f),
                    radius = circleRadius * 0.3f,
                    center = androidx.compose.ui.geometry.Offset(centerX, centerY)
                )

                // 中心点（听者位置）
                drawCircle(
                    color = themeWhite,
                    radius = 8.dp.toPx(),
                    center = androidx.compose.ui.geometry.Offset(centerX, centerY)
                )

                // 连接线（从中心到各个音源）
                for (i in 0 until soundSourceCount) {
                    val angleRad = Math.toRadians((rotationAngle + (i * 360f / soundSourceCount)).toDouble())
                    val x = centerX + cos(angleRad).toFloat() * circleRadius
                    val y = centerY + sin(angleRad).toFloat() * circleRadius

                    val path = Path().apply {
                        moveTo(centerX, centerY)
                        lineTo(x, y)
                    }

                    drawPath(
                        path = path,
                        color = themeLightRed.copy(alpha = 0.2f),
                        style = Stroke(width = 1.dp.toPx())
                    )
                }
            }

            // 控制面板
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
                    .padding(bottom = 32.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF3D2B2B).copy(alpha = 0.8f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "环绕立体声设置",
                        color = themeWhite,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "基于HRTF原理 • 模拟真实听感",
                        color = themeWhite.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // 圆周半径调节
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "圆周半径",
                                color = themeWhite.copy(alpha = 0.9f),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "${radius.toInt()}",
                                color = themeRed,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Slider(
                            value = radius,
                            onValueChange = { viewModel.surroundRadius = it },
                            valueRange = 50f..1000f,
                            colors = SliderDefaults.colors(
                                thumbColor = themeRed,
                                activeTrackColor = themeRed,
                                inactiveTrackColor = themeLightRed.copy(alpha = 0.3f)
                            )
                        )
                        // 快捷调节按钮
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            TextButton(
                                onClick = { viewModel.surroundRadius = 250f },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = if (radius <= 250f) themeRed else themeWhite.copy(alpha = 0.7f)
                                )
                            ) {
                                Text("近距", fontSize = 12.sp)
                            }
                            TextButton(
                                onClick = { viewModel.surroundRadius = 500f },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = if (radius > 250f && radius <= 500f) themeRed else themeWhite.copy(alpha = 0.7f)
                                )
                            ) {
                                Text("中距", fontSize = 12.sp)
                            }
                            TextButton(
                                onClick = { viewModel.surroundRadius = 850f },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = if (radius > 500f) themeRed else themeWhite.copy(alpha = 0.7f)
                                )
                            ) {
                                Text("远距", fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // 运动速度调节
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "运动速度",
                                color = themeWhite.copy(alpha = 0.9f),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "${String.format("%.1f", speed)}x",
                                color = themeRed,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Slider(
                            value = speed,
                            onValueChange = { viewModel.surroundSpeed = it },
                            valueRange = 0.5f..5.0f,
                            colors = SliderDefaults.colors(
                                thumbColor = themeRed,
                                activeTrackColor = themeRed,
                                inactiveTrackColor = themeLightRed.copy(alpha = 0.3f)
                            )
                        )
                        // 快捷调节按钮
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            TextButton(
                                onClick = { viewModel.surroundSpeed = 1.0f },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = if (speed <= 1.5f) themeRed else themeWhite.copy(alpha = 0.7f)
                                )
                            ) {
                                Text("慢速", fontSize = 12.sp)
                            }
                            TextButton(
                                onClick = { viewModel.surroundSpeed = 2.5f },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = if (speed > 1.5f && speed <= 3.0f) themeRed else themeWhite.copy(alpha = 0.7f)
                                )
                            ) {
                                Text("中速", fontSize = 12.sp)
                            }
                            TextButton(
                                onClick = { viewModel.surroundSpeed = 4.0f },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = if (speed > 3.0f) themeRed else themeWhite.copy(alpha = 0.7f)
                                )
                            ) {
                                Text("快速", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}