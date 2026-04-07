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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
                    color = Color.Gray,
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

            // 帮助
            SettingsSection(
                title = if (viewModel.currentLanguage == "zh") "帮助" else "Help",
                icon = Icons.Default.Help
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

    // 帮助文档全屏弹窗（Markdown 渲染）
    if (showHelpDialog) {
        val markwon = remember { Markwon.create(context) }
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showHelpDialog = false },
            properties = androidx.compose.ui.window.DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false
            )
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.White
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
                            color = Color.Black,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { showHelpDialog = false }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = if (viewModel.currentLanguage == "zh") "关闭" else "Close",
                                tint = Color.Black
                            )
                        }
                    }

                    Divider(color = Color(0xFFE0E0E0))

                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            val scrollView = NestedScrollView(ctx)
                            val textView = TextView(ctx)
                            textView.setTextColor(android.graphics.Color.BLACK)
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
        // 标题
        Row(
            modifier = Modifier.padding(vertical = AppDimensions.spacingS()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = Color(0xFFE53935),
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
