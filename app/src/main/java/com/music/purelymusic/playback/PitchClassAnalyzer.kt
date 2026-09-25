// Copyright (c) 2026 eason204646. Licensed under Mulan PSL v2.
package com.music.purelymusic.playback

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sqrt

/** A small chroma estimate over C3–B5, independent of Android audio APIs. */
internal object PitchClassAnalyzer {
    fun analyze(samples: FloatArray, count: Int, sampleRate: Float): List<Float>? {
        val blockSize = 4_096
        if (count < blockSize * 4 || sampleRate <= 0f) return null
        val chroma = DoubleArray(12)
        val window = DoubleArray(blockSize) { index ->
            0.5 - 0.5 * cos(2.0 * PI * index / (blockSize - 1))
        }
        val coefficients = DoubleArray(36) { note ->
            val frequency = 440.0 * 2.0.pow((note + 48 - 69) / 12.0)
            2.0 * cos(2.0 * PI * frequency / sampleRate)
        }
        val block = DoubleArray(blockSize)
        var blocks = 0
        for (start in 0..(count - blockSize) step blockSize) {
            for (index in 0 until blockSize) {
                block[index] = samples[start + index] * window[index]
            }
            for (note in coefficients.indices) {
                val coefficient = coefficients[note]
                var previous = 0.0
                var beforePrevious = 0.0
                for (sample in block) {
                    val current = sample + coefficient * previous - beforePrevious
                    beforePrevious = previous
                    previous = current
                }
                val power = (previous * previous + beforePrevious * beforePrevious -
                    coefficient * previous * beforePrevious).coerceAtLeast(0.0)
                chroma[note % 12] += sqrt(power)
            }
            blocks++
        }
        if (blocks < 4) return null
        val total = chroma.sum()
        if (total < 0.01) return null
        return chroma.map { (it / total).toFloat() }
    }
}
