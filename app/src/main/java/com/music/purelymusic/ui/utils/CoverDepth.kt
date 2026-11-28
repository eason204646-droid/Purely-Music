// Copyright (c) 2026 eason204646. Licensed under Mulan PSL v2.
package com.music.purelymusic.ui.utils

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale

internal val CoverDepthSurface = Color(0xFF0C1216)
internal val CoverDepthAccent = Color(0xFFF5F8F8)

internal data class CoverDepthArt(
    val sharp: ImageBitmap,
    val soft: ImageBitmap,
    val surface: Color,
    val accent: Color
)

internal fun buildCoverDepthArt(bitmap: Bitmap): CoverDepthArt {
    val scale = 24f / maxOf(bitmap.width, bitmap.height)
    val soft = Bitmap.createScaledBitmap(
        bitmap,
        (bitmap.width * scale).toInt().coerceAtLeast(1),
        (bitmap.height * scale).toInt().coerceAtLeast(1),
        true
    )
    var hue = Color(soft.getPixel(soft.width / 2, soft.height / 2)).copy(alpha = 1f)
    for (step in 1..4) {
        val color = Color(soft.getPixel((soft.width - 1) * step / 5, (soft.height - 1) * 3 / 4))
        hue = lerp(hue, color.copy(alpha = 1f), 0.20f)
    }
    return CoverDepthArt(
        sharp = bitmap.asImageBitmap(),
        soft = soft.asImageBitmap(),
        surface = lerp(CoverDepthSurface, hue, 0.16f),
        accent = lerp(CoverDepthAccent, hue, 0.15f)
    )
}

@Composable
internal fun CoverDepthLayers(
    art: CoverDepthArt,
    modifier: Modifier = Modifier,
    focusCenter: Offset = Offset(0.50f, 0.37f),
    scrollOffset: () -> Float = { 0f }
) {
    Box(modifier.clipToBounds()) {
        Image(
            bitmap = art.soft,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopCenter,
            modifier = Modifier.fillMaxSize().graphicsLayer {
                scaleX = 1.12f
                scaleY = 1.12f
                translationY = scrollOffset() * 0.55f
            }
        )
        Image(
            bitmap = art.sharp,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopCenter,
            modifier = Modifier.fillMaxSize()
                .graphicsLayer {
                    compositingStrategy = CompositingStrategy.Offscreen
                    translationY = scrollOffset()
                }
                .drawWithCache {
                    val focus = Brush.radialGradient(
                        0f to Color.Black,
                        0.59f to Color.Black,
                        1f to Color.Transparent,
                        center = Offset(size.width * focusCenter.x, size.height * focusCenter.y),
                        radius = size.width * 0.85f
                    )
                    onDrawWithContent {
                        drawContent()
                        drawRect(brush = focus, blendMode = BlendMode.DstIn)
                    }
                }
        )
    }
}
