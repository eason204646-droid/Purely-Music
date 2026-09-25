// Copyright (c) 2026 eason204646. Licensed under Mulan PSL v2.
package com.music.purelymusic.playback

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

internal data class TransitionGains(val outgoing: Float, val incoming: Float)

/** Different loudness shapes for a beat mix, quiet handoff, and gentle blend. */
internal object TransitionEnvelope {
    fun gains(style: TransitionStyle, progress: Float): TransitionGains {
        val t = progress.coerceIn(0f, 1f)
        if (style == TransitionStyle.SHORT_CUT) {
            return TransitionGains(
                outgoing = 1f - smoothStep(t / 0.9f),
                incoming = smoothStep((t - 0.12f) / 0.88f)
            )
        }
        if (style == TransitionStyle.DROP_MIX) {
            val headroom = 1f - 0.05f * sin(t * PI).toFloat()
            return TransitionGains(
                outgoing = (cos(smoothStep(t / 0.72f) * PI / 2.0) * headroom).toFloat(),
                incoming = (sin(smoothStep((t - 0.15f) / 0.75f) * PI / 2.0) * headroom).toFloat()
            )
        }
        val shaped = if (style == TransitionStyle.BEAT_MIX) smoothStep(t) else t
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
