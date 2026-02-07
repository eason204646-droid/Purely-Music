package com.music.purelymusic.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 屏幕尺寸分类
 */
enum class WindowSize {
    COMPACT,   // 小屏手机 (宽度 < 600dp)
    MEDIUM,    // 折叠屏/平板竖屏 (600dp <= 宽度 < 840dp)
    EXPANDED   // 平板/桌面横屏 (宽度 >= 840dp)
}

/**
 * 屏幕尺寸信息
 */
@Stable
data class WindowSizeClass(
    val widthSize: WindowSize,
    val heightSize: WindowSize,
    val isVerySmallScreen: Boolean  // 极小屏幕标记（宽度 < 360dp）
) {
    val isCompact: Boolean get() = widthSize == WindowSize.COMPACT
    val isMedium: Boolean get() = widthSize == WindowSize.MEDIUM
    val isExpanded: Boolean get() = widthSize == WindowSize.EXPANDED
}

/**
 * 计算屏幕尺寸分类
 */
fun calculateWindowSizeClass(widthDp: Float): WindowSize {
    return when {
        widthDp < 600f -> WindowSize.COMPACT
        widthDp < 840f -> WindowSize.MEDIUM
        else -> WindowSize.EXPANDED
    }
}

/**
 * 获取当前屏幕尺寸信息
 */
@Composable
fun rememberWindowSizeClass(): WindowSizeClass {
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp.dp
    val screenHeightDp = configuration.screenHeightDp.dp

    return remember(screenWidthDp, screenHeightDp) {
        WindowSizeClass(
            widthSize = calculateWindowSizeClass(screenWidthDp.value),
            heightSize = calculateWindowSizeClass(screenHeightDp.value),
            isVerySmallScreen = screenWidthDp.value < 360f  // 检测极小屏幕
        )
    }
}

/**
 * 屏幕密度相关工具
 */
@Composable
fun rememberScreenDensity(): Float {
    val configuration = LocalConfiguration.current
    return configuration.densityDpi / 160f
}

/**
 * 根据屏幕密度和分辨率调整尺寸
 * 针对 1440x3120 等超高分辨率屏幕进行优化
 */
@Composable
fun adjustForDensity(dp: Dp): Dp {
    val density = rememberScreenDensity()
    val windowSize = rememberWindowSizeClass()
    
    // 针对超高密度屏幕，不再缩小，而是直接使用标准密度计算
    // 这样高密度屏幕上的元素会更大更清晰
    return (dp.value * (density / 2.0f)).dp
}

/**
 * 响应式尺寸计算
 * 根据屏幕宽度返回相应的尺寸值
 */
@Composable
fun responsiveSize(
    compact: Dp,
    medium: Dp = compact,
    expanded: Dp = medium
): Dp {
    val windowSize = rememberWindowSizeClass()
    
    // 极小屏幕使用更小的尺寸
    return when {
        windowSize.isVerySmallScreen -> compact * 0.85f  // 极小屏幕缩小 15%
        windowSize.isCompact -> compact
        windowSize.isMedium -> medium
        else -> expanded
    }
}

/**
 * 响应式内边距
 */
@Composable
fun responsivePadding(): Dp {
    return responsiveSize(
        compact = 16.dp,
        medium = 24.dp,
        expanded = 32.dp
    )
}

/**
 * 响应式圆角
 */
@Composable
fun responsiveCornerRadius(): Dp {
    return responsiveSize(
        compact = 12.dp,
        medium = 16.dp,
        expanded = 20.dp
    )
}