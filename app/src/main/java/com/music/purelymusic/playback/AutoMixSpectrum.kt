// Copyright (c) 2026 eason204646. Licensed under Mulan PSL v2.
package com.music.purelymusic.playback

import kotlin.math.roundToInt

/** Bass handoff and a restrained filter sweep during beat-aligned transitions. */
internal object AutoMixSpectrum {
    fun attenuationMillibels(centerHz: Int, progress: Float, outgoing: Boolean): Int {
        val t = progress.coerceIn(0f, 1f)
        val attenuation = if (outgoing) {
            when {
                centerHz < 250 -> -1200f * smoothStep(t / 0.6f)
                centerHz < 1800 -> -250f * smoothStep((t - 0.5f) / 0.5f)
                else -> -500f * smoothStep((t - 0.55f) / 0.45f)
            }
        } else {
            when {
                centerHz < 250 -> -1200f * (1f - smoothStep((t - 0.25f) / 0.55f))
                centerHz < 1800 -> -200f * (1f - smoothStep(t / 0.35f))
                else -> -400f * (1f - smoothStep(t / 0.25f))
            }
        }
        return attenuation.roundToInt()
    }

    private fun smoothStep(value: Float): Float {
        val x = value.coerceIn(0f, 1f)
        return x * x * (3f - 2f * x)
    }
}
