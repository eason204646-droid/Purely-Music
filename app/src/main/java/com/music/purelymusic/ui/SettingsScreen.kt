//Copyright (c) [2026] [eason204646]
//[purelymusic] is licensed under Mulan PSL v2.
//You can use this software according to the terms and conditions of the Mulan
//PSL v2.
//You may obtain a copy ，of Mulan PSL v2 at:
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
package com.music.purelymusic.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.purelymusic.viewmodel.PlayerViewModel
import com.music.purelymusic.ui.utils.AppDimensions

@Composable
fun SettingsScreen(
    viewModel: PlayerViewModel,
    onBack: () -> Unit
) {
    var showLyricStyleDialog by remember { mutableStateOf(false) }
    var showTranslateLogDialog by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // 顶部标题栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppDimensions.paddingScreen(), vertical = AppDimensions.paddingScreen()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (viewModel.currentLanguage == "zh") "设置" else "Settings",
                fontSize = AppDimensions.textXXL().value.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }

        // 设置内容
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = AppDimensions.paddingScreen()),
            verticalArrangement = Arrangement.spacedBy(AppDimensions.spacingM())
        ) {
            // 语言设置
            SettingsSection(
                title = if (viewModel.currentLanguage == "zh") "语言" else "Language",
                icon = Icons.Default.Language
            ) {
                SettingsOption(
                    title = if (viewModel.currentLanguage == "zh") "中文" else "中文",
                    subtitle = "Chinese",
                    isSelected = viewModel.currentLanguage == "zh",
                    onClick = { viewModel.currentLanguage = "zh" }
                )
                SettingsOption(
                    title = if (viewModel.currentLanguage == "zh") "英语" else "English",
                    subtitle = "English",
                    isSelected = viewModel.currentLanguage == "en",
                    onClick = { viewModel.currentLanguage = "en" }
                )
            }

            // 歌词设置
            SettingsSection(
                title = if (viewModel.currentLanguage == "zh") "歌词" else "Lyrics",
                icon = Icons.Default.Notes
            ) {
                // 歌词发光特效
                SettingsSwitch(
                    title = if (viewModel.currentLanguage == "zh") "歌词发光特效" else "Lyrics Glow Effect",
                    subtitle = if (viewModel.currentLanguage == "zh") "当前播放歌词的发光效果" else "Glow effect for current lyrics",
                    checked = viewModel.lyricGlowEnabled,
                    onCheckedChange = { viewModel.lyricGlowEnabled = it }
                )

                // 过滤脏字
                SettingsSwitch(
                    title = if (viewModel.currentLanguage == "zh") "过滤敏感词" else "Filter Sensitive Words",
                    subtitle = if (viewModel.currentLanguage == "zh") "自动过滤歌词中的敏感词" else "Automatically filter sensitive words in lyrics",
                    checked = viewModel.lyricFilterEnabled,
                    onCheckedChange = { viewModel.lyricFilterEnabled = it }
                )

                // 歌词样式切换
                SettingsOption(
                    title = if (viewModel.currentLanguage == "zh") "歌词样式" else "Lyrics Style",
                    subtitle = if (viewModel.lyricStyle == "multi") {
                        if (viewModel.currentLanguage == "zh") "多行歌词" else "Multi-line"
                    } else {
                        if (viewModel.currentLanguage == "zh") "单行歌词" else "Single-line"
                    },
                    showChevron = true,
                    onClick = { showLyricStyleDialog = true }
                )
            }

            // 日志设置
            SettingsSection(
                title = if (viewModel.currentLanguage == "zh") "日志" else "Logs",
                icon = Icons.Default.History
            ) {
                SettingsOption(
                    title = if (viewModel.currentLanguage == "zh") "查看翻译日志" else "View Translation Logs",
                    subtitle = if (viewModel.currentLanguage == "zh") "查看歌词翻译的详细日志" else "View detailed logs of lyric translation",
                    showChevron = true,
                    onClick = { showTranslateLogDialog = true }
                )
            }

            Spacer(modifier = Modifier.height(AppDimensions.miniPlayerHeight() + AppDimensions.paddingScreen()))
        }
    }

    // 歌词样式选择弹窗
    if (showLyricStyleDialog) {
        AlertDialog(
            onDismissRequest = { showLyricStyleDialog = false },
            title = {
                Text(
                    if (viewModel.currentLanguage == "zh") "选择歌词样式" else "Select Lyrics Style",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // 多行歌词选项
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.lyricStyle = "multi"
                                showLyricStyleDialog = false
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (viewModel.lyricStyle == "multi") Color(0xFFFFCDD2) else Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    if (viewModel.currentLanguage == "zh") "多行歌词" else "Multi-line Lyrics",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    if (viewModel.currentLanguage == "zh") "显示多行歌词，当前歌词高亮" else "Show multiple lyrics with current one highlighted",
                                    color = Color.Black.copy(alpha = 0.7f),
                                    fontSize = 12.sp
                                )
                            }
                            if (viewModel.lyricStyle == "multi") {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color(0xFFE53935)
                                )
                            }
                        }
                    }

                    // 单行歌词选项
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.lyricStyle = "single"
                                showLyricStyleDialog = false
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (viewModel.lyricStyle == "single") Color(0xFFFFCDD2) else Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    if (viewModel.currentLanguage == "zh") "单行歌词" else "Single-line Lyrics",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    if (viewModel.currentLanguage == "zh") "只显示当前歌词，更大更醒目" else "Show only current lyrics, larger and more prominent",
                                    color = Color.Black.copy(alpha = 0.7f),
                                    fontSize = 12.sp
                                )
                            }
                            if (viewModel.lyricStyle == "single") {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color(0xFFE53935)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLyricStyleDialog = false }) {
                    Text(
                        if (viewModel.currentLanguage == "zh") "取消" else "Cancel",
                        color = Color(0xFFE53935)
                    )
                }
            },
            containerColor = Color(0xFFF5F5F5)
        )
    }

    // 翻译日志弹窗
    if (showTranslateLogDialog) {
        val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
        val annotatedString = androidx.compose.ui.text.AnnotatedString(viewModel.translateLogs)
        val scrollState = androidx.compose.foundation.rememberScrollState()

        AlertDialog(
            onDismissRequest = { showTranslateLogDialog = false },
            title = {
                Text(
                    if (viewModel.currentLanguage == "zh") "翻译日志" else "Translation Logs",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Box(
                    modifier = Modifier
                        .height(400.dp)
                        .fillMaxWidth()
                        .background(Color(0xFFF5F5F5))
                ) {
                    Column(
                        modifier = Modifier
                            .verticalScroll(scrollState)
                            .fillMaxSize()
                            .padding(8.dp)
                    ) {
                        Text(
                            text = if (viewModel.translateLogs.isBlank()) {
                                if (viewModel.currentLanguage == "zh") "暂无日志" else "No logs"
                            } else viewModel.translateLogs,
                            color = Color.Black.copy(alpha = 0.9f),
                            fontSize = 12.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }
                }
            },
            dismissButton = {
                if (viewModel.translateLogs.isNotBlank()) {
                    TextButton(
                        onClick = {
                            clipboardManager.setText(annotatedString)
                            showTranslateLogDialog = false
                        }
                    ) {
                        Text(
                            if (viewModel.currentLanguage == "zh") "复制" else "Copy",
                            color = Color(0xFFE53935)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTranslateLogDialog = false }) {
                    Text(
                        if (viewModel.currentLanguage == "zh") "关闭" else "Close",
                        color = Color(0xFFE53935)
                    )
                }
            },
            containerColor = Color.White
        )
    }
}

@Composable
fun SettingsSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(AppDimensions.spacingS())
    ) {
        // 标题
        Row(
            modifier = Modifier.padding(vertical = AppDimensions.spacingS()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = Color(0xFF757575),
                modifier = Modifier.size(AppDimensions.iconM())
            )
            Spacer(modifier = Modifier.width(AppDimensions.spacingS()))
            Text(
                text = title,
                fontSize = AppDimensions.textL().value.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF424242)
            )
        }

        // 内容卡片
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(AppDimensions.cornerRadiusM()),
            color = Color(0xFFF5F5F5),
            tonalElevation = 0.dp
        ) {
            Column(
                modifier = Modifier.padding(AppDimensions.paddingCard()),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
fun SettingsOption(
    title: String,
    subtitle: String? = null,
    isSelected: Boolean = false,
    showChevron: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppDimensions.cornerRadiusS()))
            .background(if (isSelected) Color(0xFFE0E0E0).copy(alpha = 0.5f) else Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = AppDimensions.paddingCard(), horizontal = AppDimensions.paddingCard()),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = AppDimensions.textM().value.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = Color.Black
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    fontSize = AppDimensions.textS().value.sp,
                    color = Color.Gray
                )
            }
        }
        if (isSelected) {
            Icon(
                Icons.Default.Check,
                contentDescription = "已选中",
                tint = Color(0xFF757575),
                modifier = Modifier.size(AppDimensions.iconM())
            )
        } else if (showChevron) {
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(AppDimensions.iconM())
            )
        }
    }
}

@Composable
fun SettingsSwitch(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = AppDimensions.paddingCard(), horizontal = AppDimensions.paddingCard()),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = AppDimensions.textM().value.sp,
                fontWeight = FontWeight.Normal,
                color = Color.Black
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    fontSize = AppDimensions.textS().value.sp,
                    color = Color.Gray
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFFE53935),
                checkedTrackColor = Color(0xFFFFCDD2),
                uncheckedThumbColor = Color(0xFF9E9E9E),
                uncheckedTrackColor = Color(0xFFE0E0E0)
            )
        )    }
}