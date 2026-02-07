package com.music.purelymusic.ui.utils

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 调试工具：显示当前屏幕的详细信息
 * 用于查看屏幕密度、尺寸分类等
 */
@Composable
fun ScreenInfoDebug() {
    val configuration = LocalConfiguration.current
    val density = configuration.densityDpi / 160f
    val screenWidthDp = configuration.screenWidthDp
    val screenHeightDp = configuration.screenHeightDp
    val windowSize = rememberWindowSizeClass()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .background(Color.Black.copy(alpha = 0.8f))
            .padding(16.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("屏幕信息调试", color = Color.White, fontSize = 18.sp)
            Text("屏幕密度: ${String.format("%.2f", density)}", color = Color.White)
            Text("屏幕宽度: ${screenWidthDp}dp", color = Color.White)
            Text("屏幕高度: ${screenHeightDp}dp", color = Color.White)
            Text("窗口分类: ${windowSize.widthSize}", color = Color.White)
            Text("是否超高密度: ${density > 2.8f && windowSize.isCompact}", color = Color.White)
            Text("是否触发大尺寸: ${
                when {
                    density > 3.5f && windowSize.isCompact -> "是 (缩放0.85)"
                    density > 3.0f && windowSize.isCompact -> "是 (缩放0.90)"
                    density > 2.8f && windowSize.isCompact -> "是 (使用超高密度配置)"
                    else -> "否"
                }
            }", color = Color.White)
        }
    }
}