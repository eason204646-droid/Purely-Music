package com.music.purelymusic.playback

import kotlin.math.PI
import kotlin.math.sin
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PitchClassAnalyzerTest {
    @Test
    fun detectsDominantPitchClassInDecodedSamples() {
        val rate = 11_025f
        val samples = FloatArray(22_050) { index ->
            sin(2.0 * PI * 261.6256 * index / rate).toFloat() * 0.7f
        }

        val chroma = PitchClassAnalyzer.analyze(samples, samples.size, rate)

        assertNotNull(chroma)
        assertTrue(chroma!![0] > chroma[1] * 2f)
        assertTrue(chroma[0] > chroma[6] * 2f)
    }
}
