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

import android.content.Intent
import android.net.Uri
import android.text.method.LinkMovementMethod
import android.util.TypedValue
import android.view.ViewGroup
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.widget.NestedScrollView
import com.music.purelymusic.viewmodel.PlayerViewModel
import com.music.purelymusic.ui.utils.AppDimensions
import io.noties.markwon.Markwon

@Composable
fun SettingsScreen(
    viewModel: PlayerViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var showLyricStyleDialog by remember { mutableStateOf(false) }
    var showTranslateLogDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var helpDialogTitle by remember { mutableStateOf("") }
    var helpDialogContent by remember { mutableStateOf("") }
    var showSleepTimerDialog by remember { mutableStateOf(false) } // 🚩 v2.5
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 顶部标题栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppDimensions.paddingScreen(), vertical = AppDimensions.paddingScreen()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (viewModel.currentLanguage == "zh") "设置" else "Settings",
                    fontSize = AppDimensions.textXXXL().value.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = if (viewModel.currentLanguage == "zh") "播放、歌词与资料库偏好" else "Playback, lyrics, and library preferences",
                    fontSize = AppDimensions.textS().value.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        // 设置内容
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = AppDimensions.paddingScreen())
                .padding(bottom = 92.dp),
            verticalArrangement = Arrangement.spacedBy(AppDimensions.spacingM())
        ) {
            // 播放设置
            SettingsSection(
                title = if (viewModel.currentLanguage == "zh") "播放" else "Playback",
                icon = Icons.Default.GraphicEq
            ) {
                SettingsSwitch(
                    title = if (viewModel.currentLanguage == "zh") "智能混音（AutoMix）" else "Smart Mix (AutoMix)",
                    subtitle = if (viewModel.currentLanguage == "zh") {
                        "分析本地歌曲的静音、节拍与调性，自动选择切歌时机；连续专辑曲目保持原样"
                    } else {
                        "Analyzes silence, beats and pitch to time transitions; preserves album continuity"
                    },
                    checked = viewModel.autoMixEnabled,
                    onCheckedChange = { viewModel.autoMixEnabled = it }
                )
                SettingsSwitch(
                    title = if (viewModel.currentLanguage == "zh") "自动切歌交叉渐入渐出" else "Auto Track Crossfade",
                    subtitle = if (viewModel.currentLanguage == "zh") "固定时长渐变；开启后会关闭智能混音" else "Fixed-duration fade; enabling this turns off Smart Mix",
                    checked = viewModel.crossfadeEnabled,
                    onCheckedChange = { viewModel.crossfadeEnabled = it }
                )
                SettingsSlider(
                    title = if (viewModel.currentLanguage == "zh") "渐入渐出时长" else "Crossfade Duration",
                    subtitle = if (viewModel.currentLanguage == "zh") {
                        "默认 3 秒，可调 1-10 秒"
                    } else {
                        "Default 3 seconds, adjustable from 1 to 10 seconds"
                    },
                    value = viewModel.crossfadeDurationSeconds.toFloat(),
                    valueText = if (viewModel.currentLanguage == "zh") {
                        "${viewModel.crossfadeDurationSeconds} 秒"
                    } else {
                        "${viewModel.crossfadeDurationSeconds} s"
                    },
                    enabled = viewModel.crossfadeEnabled,
                    onValueChange = { viewModel.crossfadeDurationSeconds = it.toInt() },
                    valueRange = 1f..10f,
                    steps = 8
                )
                SettingsSwitch(
                    title = if (viewModel.currentLanguage == "zh") "均衡器" else "Equalizer",
                    subtitle = if (viewModel.currentLanguage == "zh") "开启后可在播放界面进入均衡器" else "Enable access to the equalizer from the player screen",
                    checked = viewModel.equalizerEnabled,
                    onCheckedChange = { viewModel.equalizerEnabled = it }
                )

                // 🚩 v2.5: 睡眠定时器
                val sleepTimerActive = viewModel.sleepTimerActive
                val sleepTimerDisplay = viewModel.sleepTimerDisplay
                SettingsOption(
                    title = if (viewModel.currentLanguage == "zh") "睡眠定时器" else "Sleep Timer",
                    subtitle = when {
                        sleepTimerActive -> if (viewModel.currentLanguage == "zh") "剩余 $sleepTimerDisplay" else "$sleepTimerDisplay remaining"
                        else -> if (viewModel.currentLanguage == "zh") "点击设置定时停止播放" else "Set timer to stop playback"
                    },
                    showChevron = true,
                    onClick = { showSleepTimerDialog = true }
                )
            }

            // 歌词设置
            SettingsSection(
                title = if (viewModel.currentLanguage == "zh") "歌词" else "Lyrics",
                icon = Icons.AutoMirrored.Filled.Notes
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

            // 导入设置
            SettingsSection(
                title = if (viewModel.currentLanguage == "zh") "导入" else "Import",
                icon = Icons.Default.LibraryMusic
            ) {
                // 自动从元数据获取封面和歌词开关
                SettingsSwitch(
                    title = if (viewModel.currentLanguage == "zh") "从元数据自动获取封面和歌词" else "Auto Fetch Cover & Lyrics",
                    subtitle = if (viewModel.currentLanguage == "zh") "尝试从元数据自动获取封面和歌词" else "Try to fetch cover and lyrics from metadata",
                    checked = viewModel.autoFetchMetadata,
                    onCheckedChange = { viewModel.autoFetchMetadata = it }
                )
            }

            // 自动获取源设置
            SettingsSection(
                title = if (viewModel.currentLanguage == "zh") "自动获取源" else "Auto Fetch Source",
                icon = Icons.Default.CloudDownload
            ) {
                SettingsOption(
                    title = if (viewModel.currentLanguage == "zh") "网易云" else "Netease",
                    subtitle = if (viewModel.currentLanguage == "zh") "最稳定，支持大部分歌曲" else "Most stable, supports most songs",
                    isSelected = viewModel.autoFetchSource == "netease",
                    onClick = { viewModel.autoFetchSource = "netease" }
                )
                SettingsOption(
                    title = if (viewModel.currentLanguage == "zh") "混合" else "Mixed",
                    subtitle = if (viewModel.currentLanguage == "zh") "如果遇到网易云曲库没有的歌，可以尝试这个选项" else "Try this if songs are missing from Netease library",
                    isSelected = viewModel.autoFetchSource == "mixed",
                    onClick = { viewModel.autoFetchSource = "mixed" }
                )
            }

            // 帮助
            SettingsSection(
                title = if (viewModel.currentLanguage == "zh") "帮助" else "Help",
                icon = Icons.AutoMirrored.Filled.Help
            ) {
                SettingsOption(
                    title = if (viewModel.currentLanguage == "zh") "使用说明" else "User Guide",
                    subtitle = if (viewModel.currentLanguage == "zh") "如何使用 Purely Music" else "How to use Purely Music",
                    showChevron = true,
                    onClick = {
                        helpDialogTitle = if (viewModel.currentLanguage == "zh") "使用说明" else "User Guide"
                        helpDialogContent = loadHelpDocument(context, "help/使用说明.md")
                        showHelpDialog = true
                    }
                )
                SettingsOption(
                    title = if (viewModel.currentLanguage == "zh") "功能特性" else "Features",
                    subtitle = if (viewModel.currentLanguage == "zh") "了解 Purely Music 的功能" else "Learn about Purely Music features",
                    showChevron = true,
                    onClick = {
                        helpDialogTitle = if (viewModel.currentLanguage == "zh") "功能特性" else "Features"
                        helpDialogContent = loadHelpDocument(context, "help/功能特性.md")
                        showHelpDialog = true
                    }
                )
                SettingsOption(
                    title = if (viewModel.currentLanguage == "zh") "疑难解答" else "Troubleshooting",
                    subtitle = if (viewModel.currentLanguage == "zh") "常见问题与解决方案" else "Common issues and solutions",
                    showChevron = true,
                    onClick = {
                        helpDialogTitle = if (viewModel.currentLanguage == "zh") "疑难解答" else "Troubleshooting"
                        helpDialogContent = loadHelpDocument(context, "help/疑难解答.md")
                        showHelpDialog = true
                    }
                )
                SettingsOption(
                    title = if (viewModel.currentLanguage == "zh") "歌词翻译说明" else "Lyric Translation Guide",
                    subtitle = if (viewModel.currentLanguage == "zh") "如何使用歌词翻译功能" else "How to use lyric translation",
                    showChevron = true,
                    onClick = {
                        helpDialogTitle = if (viewModel.currentLanguage == "zh") "歌词翻译说明" else "Lyric Translation Guide"
                        helpDialogContent = loadHelpDocument(context, "help/歌词翻译说明.md")
                        showHelpDialog = true
                    }
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

            // 贡献
            SettingsSection(
                title = if (viewModel.currentLanguage == "zh") "贡献" else "Support",
                icon = Icons.Default.Favorite
            ) {
                Text(
                    text = if (viewModel.currentLanguage == "zh") {
                        "如果觉得我们的软件不错的话，欢迎为我们发电，或是在GitHub上给一个免费的star，都是对我的很大鼓励。"
                    } else {
                        "If you like our app, please consider supporting us on Afdian or giving us a free star on GitHub. Your support is greatly appreciated."
                    },
                    fontSize = AppDimensions.textS().value.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = AppDimensions.paddingCard())
                )
                SettingsOption(
                    title = if (viewModel.currentLanguage == "zh") "爱发电" else "Afdian",
                    subtitle = "ifdian.net/a/purelymusic",
                    showChevron = true,
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://ifdian.net/a/purelymusic"))
                        context.startActivity(intent)
                    }
                )
                SettingsOption(
                    title = if (viewModel.currentLanguage == "zh") "GitHub" else "GitHub",
                    subtitle = "github.com/eason204646-droid/Purely-Music",
                    showChevron = true,
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/eason204646-droid/Purely-Music"))
                        context.startActivity(intent)
                    }
                )
            }

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

            Spacer(modifier = Modifier.height(AppDimensions.miniPlayerHeight() + AppDimensions.paddingScreen()))
        }
    }

    // 歌词样式选择弹窗
    if (showLyricStyleDialog) {
        GlassAlertDialog(
            onDismissRequest = { showLyricStyleDialog = false },
            title = {
                Text(
                    if (viewModel.currentLanguage == "zh") "选择歌词样式" else "Select Lyrics Style",
                    color = MaterialTheme.colorScheme.onSurface,
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
                            containerColor = if (viewModel.lyricStyle == "multi") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer
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
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    if (viewModel.currentLanguage == "zh") "显示多行歌词，当前歌词高亮" else "Show multiple lyrics with current one highlighted",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp
                                )
                            }
                            if (viewModel.lyricStyle == "multi") {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
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
                            containerColor = if (viewModel.lyricStyle == "single") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer
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
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    if (viewModel.currentLanguage == "zh") "只显示当前歌词，更大更醒目" else "Show only current lyrics, larger and more prominent",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp
                                )
                            }
                            if (viewModel.lyricStyle == "single") {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
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
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    }

    // 🚩 v2.5: 睡眠定时器弹窗
    if (showSleepTimerDialog) {
        val sleepOptions = listOf(
            0 to if (viewModel.currentLanguage == "zh") "关闭" else "Off",
            15 to if (viewModel.currentLanguage == "zh") "15 分钟" else "15 min",
            30 to if (viewModel.currentLanguage == "zh") "30 分钟" else "30 min",
            45 to if (viewModel.currentLanguage == "zh") "45 分钟" else "45 min",
            60 to if (viewModel.currentLanguage == "zh") "60 分钟" else "60 min"
        )

        GlassDialog(
            onDismissRequest = { showSleepTimerDialog = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            // Haze 1.7 synchronizes dialog effects with the activity source.
            LiquidGlass(
                modifier = Modifier.fillMaxWidth(0.88f).widthIn(max = 420.dp),
                shape = RoundedCornerShape(28.dp),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 22.dp),
                opacity = 0.91f,
                highlightAlpha = 0.34f,
                edgeAlpha = 0.30f,
                shadowElevation = 0.dp
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        if (viewModel.currentLanguage == "zh") "睡眠定时器" else "Sleep Timer",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        if (viewModel.currentLanguage == "zh") "选择多久后停止播放" else "Choose when playback should stop",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    sleepOptions.forEach { (minutes, label) ->
                        val isSelected = !viewModel.sleepTimerActive && viewModel.sleepTimerMinutes == minutes
                        val isActiveOption = viewModel.sleepTimerActive && viewModel.sleepTimerMinutes == minutes
                        GlassPressable(
                            modifier = Modifier
                                .fillMaxWidth(),
                            onClick = {
                                if (minutes == 0) viewModel.cancelSleepTimer() else viewModel.startSleepTimer(minutes)
                                showSleepTimerDialog = false
                            }
                        ) {
                            LiquidGlass(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                                opacity = if (isSelected || isActiveOption) 0.88f else 0.72f,
                                highlightAlpha = if (isSelected || isActiveOption) 0.44f else 0.26f,
                                edgeAlpha = 0.34f,
                                shadowElevation = 0.dp
                            ) {
                                if (isSelected || isActiveOption) {
                                    Box(
                                        Modifier
                                            .matchParentSize()
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.13f), RoundedCornerShape(18.dp))
                                    )
                                }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        label,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = if (isSelected || isActiveOption) FontWeight.Bold else FontWeight.Normal
                                    )
                                    if (isActiveOption && minutes > 0) {
                                        Text(
                                            if (viewModel.currentLanguage == "zh") "剩余 ${viewModel.sleepTimerDisplay}" else "${viewModel.sleepTimerDisplay} remaining",
                                            color = MaterialTheme.colorScheme.primary,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                                if (isSelected || isActiveOption) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                    }
                    TextButton(onClick = { showSleepTimerDialog = false }, modifier = Modifier.align(Alignment.End)) {
                        Text(
                            if (viewModel.currentLanguage == "zh") "关闭" else "Close",
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }

    // 翻译日志弹窗
    if (showTranslateLogDialog) {
        val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
        val annotatedString = androidx.compose.ui.text.AnnotatedString(viewModel.translateLogs)
        val scrollState = androidx.compose.foundation.rememberScrollState()

        GlassAlertDialog(
            onDismissRequest = { showTranslateLogDialog = false },
            title = {
                Text(
                    if (viewModel.currentLanguage == "zh") "翻译日志" else "Translation Logs",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Box(
                    modifier = Modifier
                        .height(400.dp)
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainer)
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
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
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
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTranslateLogDialog = false }) {
                    Text(
                        if (viewModel.currentLanguage == "zh") "关闭" else "Close",
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    }

    // 帮助文档全屏弹窗（Markdown 渲染）
    if (showHelpDialog) {
        val markwon = remember { Markwon.create(context) }
        val helpTextColor = MaterialTheme.colorScheme.onSurface.toArgb()
        val helpLinkColor = MaterialTheme.colorScheme.primary.toArgb()
        GlassDialog(
            onDismissRequest = { showHelpDialog = false },
            properties = androidx.compose.ui.window.DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false
            )
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = AppDimensions.paddingScreen(), vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = helpDialogTitle,
                            fontSize = AppDimensions.textL().value.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { showHelpDialog = false }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = if (viewModel.currentLanguage == "zh") "关闭" else "Close",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            val scrollView = NestedScrollView(ctx)
                            val textView = TextView(ctx)
                            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                            textView.setLineSpacing(0f, 1.2f)
                            textView.movementMethod = LinkMovementMethod.getInstance()
                            val padding = (16 * ctx.resources.displayMetrics.density).toInt()
                            textView.setPadding(padding, padding, padding, padding)
                            scrollView.addView(
                                textView,
                                ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.WRAP_CONTENT
                                )
                            )
                            scrollView.tag = textView
                            scrollView
                        },
                        update = { scrollView ->
                            val textView = scrollView.tag as TextView
                            textView.setTextColor(helpTextColor)
                            textView.setLinkTextColor(helpLinkColor)
                            markwon.setMarkdown(textView, helpDialogContent)
                        }
                    )
                }
            }
        }
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
        // Restrained section chrome keeps settings readable; unlike navigation it is nearly opaque.
        Row(
            modifier = Modifier.padding(vertical = AppDimensions.spacingS()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(AppDimensions.iconS())
            )
            Spacer(modifier = Modifier.width(AppDimensions.spacingS()))
            Text(
                text = title,
                fontSize = AppDimensions.textL().value.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Settings are a clean continuous preference sheet, not a floating card inside a card.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppDimensions.paddingSmall()),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            content()
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
            .background(if (isSelected) MaterialTheme.colorScheme.surfaceContainerHigh else Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = AppDimensions.paddingCard(), horizontal = AppDimensions.paddingCard()),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = AppDimensions.textM().value.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    fontSize = AppDimensions.textS().value.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (isSelected) {
            Icon(
                Icons.Default.Check,
                contentDescription = "已选中",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(AppDimensions.iconM())
            )
        } else if (showChevron) {
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
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
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    fontSize = AppDimensions.textS().value.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )    }
}

@Composable
fun SettingsSlider(
    title: String,
    subtitle: String? = null,
    value: Float,
    valueText: String,
    enabled: Boolean = true,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int = 0
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = AppDimensions.paddingCard(), horizontal = AppDimensions.paddingCard())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = AppDimensions.textM().value.sp,
                    fontWeight = FontWeight.Normal,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        fontSize = AppDimensions.textS().value.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.width(AppDimensions.spacingS()))
            Text(
                text = valueText,
                fontSize = AppDimensions.textS().value.sp,
                fontWeight = FontWeight.Bold,
                color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End
            )
        }
        Spacer(modifier = Modifier.height(AppDimensions.spacingS()))
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.primaryContainer,
                disabledThumbColor = MaterialTheme.colorScheme.outline,
                disabledActiveTrackColor = MaterialTheme.colorScheme.outline,
                disabledInactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}

fun loadHelpDocument(context: android.content.Context, fileName: String): String {
    return try {
        val inputStream = context.assets.open(fileName)
        val size = inputStream.available()
        val buffer = ByteArray(size)
        inputStream.read(buffer)
        inputStream.close()
        String(buffer, Charsets.UTF_8)
    } catch (e: Exception) {
        "无法加载文档：${e.message}"
    }
}
