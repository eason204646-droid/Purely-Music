// Copyright (c) 2026 eason204646. Licensed under Mulan PSL v2.
package com.music.purelymusic.playback

import kotlin.math.sqrt

internal data class EnergyFrame(val timeMs: Long, val energy: Float)
internal data class BeatEstimate(val bpm: Float, val anchorMs: Long)

/** Estimates a beat grid from local onset strength, including the ending of a track. */
internal object BeatAnalyzer {
    fun estimate(frames: List<EnergyFrame>): BeatEstimate? {
        if (frames.size < 150) return null
        val onset = FloatArray(frames.size) { index ->
            if (index == 0) 0f else (frames[index].energy - frames[index - 1].energy)
                .coerceAtLeast(0f)
        }
        val maxOnset = onset.maxOrNull() ?: return null
        if (maxOnset < 0.01f || onset.count { it >= maxOnset * 0.3f } < 8) return null

        var bestLag = 0
        var bestScore = 0.0
        for (lag in 8..22) { // Approximately 68–188 BPM at 40 ms per frame.
            var dot = 0.0
            var leftPower = 0.0
            var rightPower = 0.0
            for (index in lag until onset.size) {
                val left = onset[index].toDouble()
                val right = onset[index - lag].toDouble()
                dot += left * right
                leftPower += left * left
                rightPower += right * right
            }
            val score = if (leftPower > 0.0 && rightPower > 0.0) {
                dot / sqrt(leftPower * rightPower)
            } else 0.0
            if (score > bestScore) {
                bestScore = score
                bestLag = lag
            }
        }
        if (bestLag == 0 || bestScore < 0.24) return null

        val phaseStrength = DoubleArray(bestLag)
        for (index in onset.indices) phaseStrength[index % bestLag] += onset[index]
        val phase = phaseStrength.indices.maxByOrNull { phaseStrength[it] } ?: return null
        if (phaseStrength[phase] < onset.sum() / bestLag * 1.5) return null
        return BeatEstimate(60_000f / (bestLag * 40f), frames[phase].timeMs)
    }
}

/** Requires neighboring audio, so one stray click does not define a song boundary. */
internal object SilenceDetector {
    fun firstAudible(frames: List<EnergyFrame>, threshold: Float): Long? {
        for (index in frames.indices) {
            if (frames[index].energy >= threshold && hasNeighbor(frames, index, threshold)) {
                return frames[index].timeMs
            }
        }
        return null
    }

    fun lastAudible(frames: List<EnergyFrame>, threshold: Float): Long? {
        for (index in frames.indices.reversed()) {
            if (frames[index].energy >= threshold && hasNeighbor(frames, index, threshold)) {
                return frames[index].timeMs
            }
        }
        return null
    }

    private fun hasNeighbor(frames: List<EnergyFrame>, index: Int, threshold: Float): Boolean {
        val floor = threshold * 0.4f
        return (index > 0 && frames[index - 1].energy >= floor) ||
            (index + 1 < frames.size && frames[index + 1].energy >= floor)
    }
}
