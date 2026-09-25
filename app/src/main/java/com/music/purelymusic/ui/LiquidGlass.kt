package com.music.purelymusic.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.material3.DropdownMenu
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect

/** Optional blur source scoped to the glass control's surrounding UI. */
val LocalLiquidGlassHazeState = compositionLocalOf<HazeState?> { null }

/**
 * Keeps the visual glass treatment while preventing an effect from reading a Haze source owned
 * by another Compose window. The fallback below is only used when a menu has no in-window host.
 */
@Composable
fun WithoutLiquidGlassHaze(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalLiquidGlassHazeState provides null, content = content)
}

/**
 * 3.0 的基础材质。Android 12 及以上使用硬件加速的实时背景模糊；旧系统自然回退为
 * 半透明漫反射层。文字与图标始终绘制在模糊层上方，不参与模糊。
 */
@Composable
fun LiquidGlass(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(28.dp),
    dark: Boolean = isSystemInDarkTheme(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    opacity: Float = if (dark) 0.62f else 0.84f,
    highlightAlpha: Float = if (dark) 0.14f else 0.34f,
    edgeAlpha: Float = if (dark) 0.14f else 0.30f,
    // Transparent surfaces with platform elevation render as a dirty gray halo on Android.
    // Components opt into a shadow only when it is visually justified.
    shadowElevation: Dp = 0.dp,
    content: @Composable BoxScope.() -> Unit = {}
) {
    val base = if (dark) Color(0xFF17171C).copy(alpha = opacity) else Color(0xFFF9F9FC).copy(alpha = opacity)
    // Haze expands the sampled layer beyond the glass bounds to keep blur edges smooth. That
    // expansion needs an opaque fallback color; leaving it unspecified crashes on Android's
    // graphics-layer path before the first frame is presented.
    val hazeBackgroundColor = if (dark) Color(0xFF17171C) else Color(0xFFF9F9FC)
    val highlight = Color.White.copy(alpha = highlightAlpha)
    val edge = if (dark) {
        Color.White.copy(alpha = edgeAlpha)
    } else {
        Color(0xFF87919D).copy(alpha = edgeAlpha)
    }
    val hazeState = LocalLiquidGlassHazeState.current
    Surface(
        modifier = modifier,
        shape = shape,
        color = Color.Transparent,
        shadowElevation = shadowElevation,
        tonalElevation = 0.dp
    ) {
        Box(
            modifier = Modifier
                .clip(shape)
        ) {
            // Haze only blurs content drawn behind this layer; controls remain crisp.
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(shape)
                    .then(
                        if (hazeState != null) {
                            Modifier.hazeEffect(hazeState) {
                                // A restrained radius keeps navigation and scrolling smooth.
                                blurRadius = 10.dp
                                noiseFactor = 0.02f
                                backgroundColor = hazeBackgroundColor
                            }
                        } else {
                            Modifier
                        }
                    )
                    .background(
                        Brush.linearGradient(
                            listOf(highlight, base.copy(alpha = base.alpha * 0.96f), base.copy(alpha = base.alpha * 0.84f))
                        )
                    )
                    .background(
                        Brush.radialGradient(
                            listOf(
                                Color.White.copy(alpha = if (dark) 0.06f else 0.11f),
                                Color.Transparent
                            )
                        )
                    )
            )
            Box(modifier = Modifier.padding(contentPadding), content = content)
            Box(modifier = Modifier.matchParentSize().border(1.dp, edge, shape))
        }
    }
}

/** In-window menus sample the live page source beneath them. */
@Composable
fun GlassInlineMenu(
    expanded: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    if (expanded) {
        LiquidGlass(
            modifier = modifier.width(260.dp),
            shape = RoundedCornerShape(20.dp),
            contentPadding = PaddingValues(vertical = 6.dp),
            opacity = 0.72f,
            highlightAlpha = 0.20f,
            edgeAlpha = 0.36f
        ) {
            Column(content = content)
        }
    }
}

/** Anchor a contextual menu in the activity overlay so it can blur the live page. */
@Composable
fun GlassDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    offset: androidx.compose.ui.unit.DpOffset = androidx.compose.ui.unit.DpOffset(0.dp, 8.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val host = LocalGlassMenuHost.current
    if (host != null) {
        val key = remember { Any() }
        var anchor by remember { mutableStateOf(Offset.Zero) }
        val latestContent by rememberUpdatedState(content)
        val latestDismiss by rememberUpdatedState(onDismissRequest)
        val density = LocalDensity.current
        val pixelOffset = with(density) {
            androidx.compose.ui.unit.IntOffset(offset.x.roundToPx(), offset.y.roundToPx())
        }
        Spacer(modifier.size(0.dp).onGloballyPositioned {
            anchor = it.positionInWindow()
            host.move(key, anchor)
        })
        DisposableEffect(expanded, host, pixelOffset) {
            if (expanded) {
                host.show(
                    GlassMenuHostState.Menu(
                        key = key,
                        anchor = anchor,
                        offset = pixelOffset,
                        onDismissRequest = { latestDismiss() },
                        content = { latestContent() }
                    )
                )
            }
            onDispose { host.hide(key) }
        }
        return
    }
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier.width(260.dp),
        offset = offset,
        shape = RoundedCornerShape(20.dp),
        containerColor = Color.Transparent,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = null
    ) {
        // DropdownMenu is backed by a Popup window. Keep its glass appearance, but do not let
        // it attempt to sample the source layer from the activity window.
        WithoutLiquidGlassHaze {
            LiquidGlass(
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                shape = RoundedCornerShape(20.dp),
                contentPadding = PaddingValues(vertical = 6.dp),
                opacity = 0.94f,
                highlightAlpha = 0.36f,
                edgeAlpha = 0.32f,
                shadowElevation = 0.dp
            ) {
                Column(content = content)
            }
        }
    }
}

@Composable
fun GlassMenuItem(
    label: String,
    icon: ImageVector,
    destructive: Boolean = false,
    onClick: () -> Unit
) {
    val color = if (destructive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    GlassPressable(modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), onClick = onClick) {
        Icon(icon, null, tint = color, modifier = Modifier.align(Alignment.CenterStart).padding(start = 16.dp))
        Text(
            label,
            color = color,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.align(Alignment.CenterStart).padding(start = 52.dp)
        )
    }
}

/** Subtle spring-like press feedback used by glass controls throughout 3.0. */
@Composable
fun GlassPressable(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = tween(170, easing = FastOutSlowInEasing),
        label = "glassPressScale"
    )
    Box(
        modifier = modifier
            .scale(scale)
            .clickable(enabled = enabled, interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
        content = content
    )
}

@Composable
fun GlassControl(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(22.dp),
    dark: Boolean = true,
    opacity: Float = if (dark) 0.62f else 0.88f,
    onClick: () -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    GlassPressable(modifier = modifier, onClick = onClick) {
        LiquidGlass(
            modifier = Modifier.fillMaxSize(),
            shape = shape,
            dark = dark,
            opacity = opacity,
            highlightAlpha = if (dark) 0.14f else 0.28f,
            edgeAlpha = if (dark) 0.14f else 0.28f
        )
        content()
    }
}
