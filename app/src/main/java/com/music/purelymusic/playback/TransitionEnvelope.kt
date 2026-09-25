// Copyright (c) 2026 eason204646. Licensed under Mulan PSL v2.
package com.music.purelymusic.playback

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

internal data class TransitionGains(val outgoing: Float, val incoming: Float)

/** Staged loudness shapes that leave room for the incoming intro before the main handoff. */
internal object TransitionEnvelope {
    fun gains(style: TransitionStyle, progress: Float): TransitionGains {
        val t = progress.coerceIn(0f, 1f)
        if (style == TransitionStyle.SOFT_BLEND) {
            val outgoing = when {
                t < 0.35f -> 1f - 0.06f * smoothStep(t / 0.35f)
                t < 0.72f -> 0.94f - 0.35f * smoothStep((t - 0.35f) / 0.37f)
                else -> 0.59f * (1f - smoothStep((t - 0.72f) / 0.28f))
            }
            val incoming = when {
                t < 0.35f -> 0.22f * smoothStep(t / 0.35f)
                t < 0.72f -> 0.22f + 0.51f * smoothStep((t - 0.35f) / 0.37f)
                else -> 0.73f + 0.27f * smoothStep((t - 0.72f) / 0.28f)
            }
            return TransitionGains(
                outgoing = outgoing,
                incoming = incoming
            )
        }
        if (style == TransitionStyle.DROP_MIX) {
            val headroom = 1f - 0.05f * sin(t * PI).toFloat()
            return TransitionGains(
                outgoing = (cos(smoothStep(t / 0.72f) * PI / 2.0) * headroom).toFloat(),
                incoming = (sin(smoothStep((t - 0.15f) / 0.75f) * PI / 2.0) * headroom).toFloat()
            )
        }
        val shaped = smoothStep(t)
        val headroom = 1f - 0.08f * sin(t * PI).toFloat()
        return TransitionGains(
            outgoing = (cos(shaped * PI / 2.0) * headroom).toFloat(),
            incoming = (sin(shaped * PI / 2.0) * headroom).toFloat()
        )
    }

    private fun smoothStep(value: Float): Float {
        val x = value.coerceIn(0f, 1f)
        return x * x * (3f - 2f * x)
    }
}
