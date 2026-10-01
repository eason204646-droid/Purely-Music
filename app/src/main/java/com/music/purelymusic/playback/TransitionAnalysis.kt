// Copyright (c) 2026 eason204646. Licensed under Mulan PSL v2.
package com.music.purelymusic.playback

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.math.sin
import kotlin.math.sqrt

internal data class EnergyFrame(val timeMs: Long, val energy: Float, val onset: Float? = null, val bass: Float = 0f)
internal data class BeatEstimate(
    val bpm: Float,
    val anchorMs: Long,
    val confidence: Float,
    val phraseAnchorMs: Long? = null
)

/** Estimates a beat grid from local onset strength, including the ending of a track. */
internal object BeatAnalyzer {
    fun estimate(frames: List<EnergyFrame>): BeatEstimate? {
        if (frames.size < 100 || frames.last().timeMs - frames.first().timeMs < 5_000L) return null
        val onset = FloatArray(frames.size) { index ->
            frames[index].onset ?: if (index == 0) 0f else
                (frames[index].energy - frames[index - 1].energy).coerceAtLeast(0f)
        }
        val maxOnset = onset.maxOrNull() ?: return null
        if (maxOnset < 0.008f) return null
        val threshold = maxOf(maxOnset * 0.12f, onset.average().toFloat() * 1.6f)
        val peaks = mutableListOf<Int>()
        for (index in 1 until onset.lastIndex) {
            if (onset[index] < threshold || onset[index] < onset[index - 1] ||
                onset[index] <= onset[index + 1]) continue
            if (peaks.isNotEmpty() && frames[index].timeMs - frames[peaks.last()].timeMs < 120L) {
                if (onset[index] > onset[peaks.last()]) peaks[peaks.lastIndex] = index
            } else peaks += index
        }
        if (peaks.size < 8) return null
        val origin = frames[peaks.first()].timeMs
        val times = peaks.map { (frames[it].timeMs - origin).toDouble() }
        if (times.last() < 4_000.0) return null
        // Compress strong accents so a single crash does not define the whole grid.
        val weights = peaks.map { sqrt((onset[it] / maxOnset).toDouble()) }
        val weightSum = weights.sum()
        var bestPeriod = 0.0
        var bestPhase = 0.0
        var bestScore = 0.0
        // Continuous tempo candidates avoid the old 40 ms / integer-lag quantization.
        for (step in 260..760) {
            val period = 60_000.0 / (step / 4.0)
            var real = 0.0
            var imaginary = 0.0
            for (index in times.indices) {
                val angle = 2.0 * PI * times[index] / period
                real += weights[index] * cos(angle)
                imaginary += weights[index] * sin(angle)
            }
            val coherence = sqrt(real * real + imaginary * imaginary) / weightSum
            val phase = ((atan2(imaginary, real) / (2.0 * PI) * period) + period) % period
            val aligned = times.count { time ->
                val distance = abs(time - phase - ((time - phase) / period).roundToInt() * period)
                distance <= minOf(65.0, period * 0.12)
            }
            val coverage = (aligned / (times.last() / period + 1.0)).coerceAtMost(1.0)
            val score = coherence * (0.7 + 0.3 * coverage)
            if (score > bestScore) {
                bestScore = score
                bestPeriod = period
                bestPhase = phase
            }
        }
        if (bestScore < 0.60) return null
        val aligned = times.indices.filter { index ->
            abs(times[index] - bestPhase -
                ((times[index] - bestPhase) / bestPeriod).roundToInt() * bestPeriod) < 70.0
        }
        if (aligned.size < 8 || aligned.size < peaks.size * 0.55) return null
        // Fit actual attacks to the grid, refining both tempo and phase below one frame.
        val beatNumbers = aligned.map { ((times[it] - bestPhase) / bestPeriod).roundToInt() }
        val meanBeat = beatNumbers.average()
        val meanTime = aligned.map { times[it] }.average()
        var numerator = 0.0
        var denominator = 0.0
        aligned.forEachIndexed { index, onsetIndex ->
            val beat = beatNumbers[index] - meanBeat
            numerator += beat * (times[onsetIndex] - meanTime)
            denominator += beat * beat
        }
        if (denominator == 0.0) return null
        val period = numerator / denominator
        if (period !in (60_000.0 / 190.0)..(60_000.0 / 65.0)) return null
        val phase = meanTime - meanBeat * period
        val residual = sqrt(aligned.mapIndexed { index, onsetIndex ->
            val error = times[onsetIndex] - (phase + beatNumbers[index] * period)
            error * error
        }.average())
        val confidence = (bestScore * (1.0 - residual / 100.0).coerceIn(0.0, 1.0)).toFloat()
        if (confidence < 0.55f) return null
        val anchor = (origin + phase + beatNumbers.first() * period).roundToLong()
        val accents = DoubleArray(4)
        aligned.forEachIndexed { index, onsetIndex ->
            accents[Math.floorMod(beatNumbers[index] - beatNumbers.first(), 4)] += weights[onsetIndex]
        }
        val accent = accents.indices.maxByOrNull { accents[it] } ?: 0
        val phraseAnchor = if (aligned.size >= 16 && accents[accent] > accents.average() * 1.35) {
            (anchor + accent * period).roundToLong()
        } else null
        return BeatEstimate((60_000.0 / period).toFloat(), anchor, confidence, phraseAnchor)
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
