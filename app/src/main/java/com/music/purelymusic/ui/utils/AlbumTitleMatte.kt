// Copyright (c) 2026 eason204646. Licensed under Mulan PSL v2.
package com.music.purelymusic.ui.utils

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlin.math.max
import kotlin.math.min

internal fun smoothUnit(value: Float): Float {
    val clamped = value.coerceIn(0f, 1f)
    return clamped * clamped * (3f - 2f * clamped)
}

/**
 * Propagates foreground and background confidence along similar source colors. Color
 * discontinuities slow propagation, so shaded strokes can reconnect while the holes in
 * a letter and the edges between the print and the photograph retain their boundaries.
 */
internal suspend fun refineTitleMatte(pixels: IntArray, matte: FloatArray, width: Int, height: Int) {
    if (matte.none { it >= 0.80f }) return
    val context = currentCoroutineContext()
    val foreground = FloatArray(matte.size)
    val background = FloatArray(matte.size)
    val horizontal = FloatArray(matte.size)
    val vertical = FloatArray(matte.size)
    val step = (350f / max(width, height)).coerceIn(0.42f, 2.8f)
    for (y in 0 until height) {
        context.ensureActive()
        for (x in 0 until width) {
            val index = y * width + x
            val evidence = matte[index]
            foreground[index] = (0.87f - evidence).coerceAtLeast(0f) * 40f
            background[index] = smoothUnit((evidence - 0.06f) / 0.70f) * 48f
            if (x > 0) {
                horizontal[index] = step + colorDistanceSquared(pixels[index], pixels[index - 1]) / 115f
            }
            if (y > 0) {
                vertical[index] = step + colorDistanceSquared(pixels[index], pixels[index - width]) / 115f
            }
        }
    }

    repeat(2) {
        for (y in 0 until height) {
            context.ensureActive()
            for (x in 0 until width) {
                val index = y * width + x
                if (x > 0) {
                    val cost = horizontal[index]
                    foreground[index] = min(foreground[index], foreground[index - 1] + cost)
                    background[index] = min(background[index], background[index - 1] + cost)
                }
                if (y > 0) {
                    val cost = vertical[index]
                    foreground[index] = min(foreground[index], foreground[index - width] + cost)
                    background[index] = min(background[index], background[index - width] + cost)
                }
            }
        }
        for (y in height - 1 downTo 0) {
            context.ensureActive()
            for (x in width - 1 downTo 0) {
                val index = y * width + x
                if (x < width - 1) {
                    val cost = horizontal[index + 1]
                    foreground[index] = min(foreground[index], foreground[index + 1] + cost)
                    background[index] = min(background[index], background[index + 1] + cost)
                }
                if (y < height - 1) {
                    val cost = vertical[index + width]
                    foreground[index] = min(foreground[index], foreground[index + width] + cost)
                    background[index] = min(background[index], background[index + width] + cost)
                }
            }
        }
    }

    for (index in matte.indices) {
        val margin = background[index] - foreground[index]
        val widthOfTransition = max(4f, (background[index] + foreground[index]) * 0.62f)
        val spatial = smoothUnit(0.5f + margin / (2f * widthOfTransition))
        val amount = if (spatial > matte[index]) 0.82f else 0.60f
        matte[index] += (spatial - matte[index]) * amount
    }
}
