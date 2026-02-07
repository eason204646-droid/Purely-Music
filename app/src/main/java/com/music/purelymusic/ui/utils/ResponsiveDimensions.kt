package com.music.purelymusic.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 响应式尺寸配置
 * 定义了应用中所有UI元素在不同屏幕尺寸下的尺寸值
 * 使用响应式设计原则，确保在不同屏幕上保持一致的视觉效果
 */

object AppDimensions {
    // ==================== 响应式尺寸配置 ====================
    // 根据屏幕宽度（COMPACT/MEDIUM/EXPANDED）自动调整尺寸
    // COMPACT: 小屏手机 (< 600dp)
    // MEDIUM: 折叠屏/平板竖屏 (600dp - 840dp)
    // EXPANDED: 平板/桌面横屏 (>= 840dp)
    
    // ==================== 基础间距 ====================
    @Composable
    fun spacingXS() = responsiveSize(4.dp, 6.dp, 8.dp)
    @Composable
    fun spacingS() = responsiveSize(8.dp, 12.dp, 16.dp)
    @Composable
    fun spacingM() = responsiveSize(12.dp, 16.dp, 20.dp)
    @Composable
    fun spacingL() = responsiveSize(16.dp, 24.dp, 32.dp)
    @Composable
    fun spacingXL() = responsiveSize(24.dp, 32.dp, 40.dp)

    // ==================== 内边距 ====================
    @Composable
    fun paddingScreen() = responsiveSize(16.dp, 24.dp, 32.dp)
    @Composable
    fun paddingCard() = responsiveSize(12.dp, 16.dp, 20.dp)
    @Composable
    fun paddingSmall() = responsiveSize(8.dp, 12.dp, 16.dp)

    // ==================== 圆角 ====================
    @Composable
    fun cornerRadiusS() = responsiveSize(8.dp, 10.dp, 12.dp)
    @Composable
    fun cornerRadiusM() = responsiveSize(12.dp, 16.dp, 20.dp)
    @Composable
    fun cornerRadiusL() = responsiveSize(16.dp, 20.dp, 24.dp)
    @Composable
    fun cornerRadiusXL() = responsiveSize(20.dp, 24.dp, 28.dp)

    // ==================== 图标尺寸 ====================
    // COMPACT: 小屏手机，保持适中尺寸
    // MEDIUM: 折叠屏/平板竖屏，稍大
    // EXPANDED: 平板/桌面，最大
    @Composable
    fun iconXS() = responsiveSize(16.dp, 18.dp, 20.dp)
    @Composable
    fun iconS() = responsiveSize(20.dp, 24.dp, 28.dp)
    @Composable
    fun iconM() = responsiveSize(24.dp, 28.dp, 32.dp)
    @Composable
    fun iconL() = responsiveSize(28.dp, 32.dp, 36.dp)
    @Composable
    fun iconXL() = responsiveSize(32.dp, 36.dp, 40.dp)

    // ==================== 按钮尺寸 ====================
    @Composable
    fun buttonHeightS() = responsiveSize(36.dp, 40.dp, 44.dp)
    @Composable
    fun buttonHeightM() = responsiveSize(40.dp, 44.dp, 48.dp)
    @Composable
    fun buttonHeightL() = responsiveSize(48.dp, 52.dp, 56.dp)

    @Composable
    fun iconButtonSizeS() = responsiveSize(36.dp, 40.dp, 44.dp)
    @Composable
    fun iconButtonSizeM() = responsiveSize(40.dp, 44.dp, 48.dp)
    @Composable
    fun iconButtonSizeL() = responsiveSize(48.dp, 52.dp, 56.dp)
    @Composable
    fun iconButtonSizeXL() = responsiveSize(56.dp, 64.dp, 72.dp)

    // ==================== 卡片尺寸 ====================
    @Composable
    fun cardHeightS() = responsiveSize(56.dp, 64.dp, 72.dp)
    @Composable
    fun cardHeightM() = responsiveSize(64.dp, 72.dp, 80.dp)
    @Composable
    fun cardHeightL() = responsiveSize(72.dp, 80.dp, 88.dp)

    @Composable
    fun cardWidthS() = responsiveSize(120.dp, 140.dp, 160.dp)
    @Composable
    fun cardWidthM() = responsiveSize(140.dp, 160.dp, 180.dp)

    // ==================== 封面尺寸 ====================
    @Composable
    fun coverXS() = responsiveSize(36.dp, 44.dp, 52.dp)
    @Composable
    fun coverS() = responsiveSize(48.dp, 56.dp, 64.dp)
    @Composable
    fun coverM() = responsiveSize(56.dp, 64.dp, 72.dp)
    @Composable
    fun coverL() = responsiveSize(120.dp, 140.dp, 160.dp)
    @Composable
    fun coverXL() = responsiveSize(140.dp, 160.dp, 180.dp)

    // ==================== 字体大小 ====================
    @Composable
    fun textXS() = responsiveSize(10.dp, 11.dp, 12.dp)
    @Composable
    fun textS() = responsiveSize(12.dp, 13.dp, 14.dp)
    @Composable
    fun textM() = responsiveSize(14.dp, 15.dp, 16.dp)
    @Composable
    fun textL() = responsiveSize(16.dp, 17.dp, 18.dp)
    @Composable
    fun textXL() = responsiveSize(20.dp, 22.dp, 24.dp)
    @Composable
    fun textXXL() = responsiveSize(24.dp, 26.dp, 28.dp)
    @Composable
    fun textXXXL() = responsiveSize(28.dp, 30.dp, 32.dp)

    // ==================== MiniPlayer 尺寸 ====================
    @Composable
    fun miniPlayerHeight() = responsiveSize(64.dp, 72.dp, 80.dp)
    @Composable
    fun miniPlayerPaddingH() = responsiveSize(12.dp, 16.dp, 20.dp)
    @Composable
    fun miniPlayerPaddingV() = responsiveSize(6.dp, 8.dp, 10.dp)

    // ==================== 导航栏尺寸 ====================
    @Composable
    fun navigationBarHeight() = responsiveSize(56.dp, 64.dp, 72.dp)
    @Composable
    fun navigationBarIconSize() = responsiveSize(24.dp, 28.dp, 32.dp)

    // ==================== 播放器屏幕尺寸 ====================
    @Composable
    fun playerCoverSize() = responsiveSize(240.dp, 280.dp, 320.dp)
    @Composable
    fun playerCoverPadding() = responsiveSize(24.dp, 32.dp, 40.dp)

    @Composable
    fun playerControlButtonSize() = responsiveSize(70.dp, 80.dp, 90.dp)
    @Composable
    fun playerControlIconSize() = responsiveSize(42.dp, 48.dp, 54.dp)
    @Composable
    fun playerPlayButtonSize() = responsiveSize(70.dp, 80.dp, 90.dp)
    @Composable
    fun playerPlayIconSize() = responsiveSize(48.dp, 54.dp, 60.dp)

    @Composable
    fun playerSliderHeight() = responsiveSize(8.dp, 10.dp, 12.dp)
    @Composable
    fun playerTextSize() = responsiveSize(12.dp, 14.dp, 16.dp)

    // ==================== 首页尺寸 ====================
    @Composable
    fun homeHeaderPadding() = responsiveSize(20.dp, 24.dp, 28.dp)
    @Composable
    fun homeSectionTitleSize() = responsiveSize(18.dp, 20.dp, 22.dp)
    @Composable
    fun homeRecentCardWidth() = responsiveSize(130.dp, 140.dp, 150.dp)
    @Composable
    fun homeRecentCardHeight() = responsiveSize(130.dp, 140.dp, 150.dp)

    // ==================== 资料库尺寸 ====================
    @Composable
    fun libraryGridSpacing() = responsiveSize(16.dp, 20.dp, 24.dp)
    @Composable
    fun libraryPlaylistWidth() = responsiveSize(130.dp, 150.dp, 170.dp)
    @Composable
    fun libraryPlaylistHeight() = responsiveSize(130.dp, 150.dp, 170.dp)

    // ==================== 弹窗尺寸 ====================
    @Composable
    fun dialogPadding() = responsiveSize(16.dp, 20.dp, 24.dp)
    @Composable
    fun dialogSpacing() = responsiveSize(8.dp, 12.dp, 16.dp)

    // ==================== 阴影 ====================
    @Composable
    fun elevationS() = responsiveSize(2.dp, 3.dp, 4.dp)
    @Composable
    fun elevationM() = responsiveSize(4.dp, 6.dp, 8.dp)
    @Composable
    fun elevationL() = responsiveSize(8.dp, 12.dp, 16.dp)
    @Composable
    fun elevationXL() = responsiveSize(12.dp, 16.dp, 20.dp)
}