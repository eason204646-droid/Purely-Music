package com.music.purelymusic.playback

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import org.junit.Assert.*
import org.junit.Test

class DjPcmMixerTest {
    private val rate = 16_000
    private val frames = rate * 4
    private val plan = TransitionPlan(20_000, 4_000, 2_000, style = TransitionStyle.BEAT_MIX, beatMs = 500.0)
    private fun signal(vararg tones: Pair<Double, Double>) = FloatArray(frames * 2) { i ->
        tones.sumOf { (hz, amplitude) -> amplitude * sin(2 * PI * hz * (i / 2) / rate) }.toFloat()
    }
    private fun amplitude(samples: FloatArray, hz: Double, from: Double, until: Double): Double {
        val first = (from * rate).toInt()
        val last = (until * rate).toInt()
        var a = 0.0; var b = 0.0
        for (frame in first until last) {
            val sample = samples[frame * 2]
            a += sample * sin(2 * PI * hz * frame / rate)
            b += sample * cos(2 * PI * hz * frame / rate)
        }
        return 2 * kotlin.math.sqrt(a * a + b * b) / (last - first)
    }

    @Test fun decoderHandoverAvoidsTheNeighborhoodOfStrongDrumAttacks() {
        val pre = FloatArray(rate * 2 * 2) { i ->
            val time = (i / 2).toDouble() / rate
            if (time in 1.6..1.75) 0.8f else 0.02f
        }
        val beforeEnd = DjPcmMixer.quietHandoverBeforeEndMs(pre, rate)
        val position = 2.0 - beforeEnd / 1_000.0
        assertTrue(position < 1.45)
        assertTrue(beforeEnd in 200L..1_000L)
    }

    @Test fun bassStaysWithOneDeckThenSwapsAtTheBar() {
        val mixed = DjPcmMixer.render(signal(60.0 to 0.3), signal(100.0 to 0.3), rate, plan)
        assertTrue(amplitude(mixed, 60.0, 0.8, 1.3) > 0.28)
        assertTrue(amplitude(mixed, 100.0, 0.8, 1.3) < 0.04)
        assertTrue(amplitude(mixed, 60.0, 2.7, 3.2) < 0.04)
        assertTrue(amplitude(mixed, 100.0, 2.7, 3.2) > 0.26)
    }

    @Test fun nextRhythmArrivesBeforeItsBassOrMelody() {
        val mixed = DjPcmMixer.render(signal(330.0 to 0.2), signal(100.0 to 0.2, 900.0 to 0.2, 5_000.0 to 0.2), rate, plan)
        assertTrue(amplitude(mixed, 5_000.0, 0.8, 1.2) > 0.16)
        assertTrue(amplitude(mixed, 100.0, 0.8, 1.2) < 0.04)
        assertTrue(amplitude(mixed, 900.0, 0.8, 1.2) < 0.03)
        assertTrue(amplitude(mixed, 330.0, 0.8, 1.2) > 0.17)
    }

    @Test fun sharedLimiterKeepsStereoImageAndBoundsLoudOverlaps() {
        val samples = FloatArray(rate * 2) { if (it % 2 == 0) 1.8f else 0.9f }
        DjPcmMixer.limit(samples, rate)
        assertTrue(samples.all { abs(it) <= 0.98001f })
        for (i in samples.indices step 2) assertEquals(samples[i] / 2, samples[i + 1], 0.00001f)
    }

    @Test fun passageEdgesReconstructTheOriginalWaveforms() {
        val outgoing = signal(80.0 to 0.2, 700.0 to 0.1, 5_000.0 to 0.1)
        val incoming = signal(90.0 to 0.2, 900.0 to 0.1, 6_000.0 to 0.1)
        val mixed = DjPcmMixer.render(outgoing, incoming, rate, plan)
        assertEquals(outgoing.first(), mixed.first(), 0.00001f)
        assertEquals(incoming.last(), mixed.last(), 0.00001f)
        assertTrue(mixed.all { it.isFinite() })
    }

    @Test fun cancellingRenderingStopsBeforeProducingAnAudibleResult() {
        var checks = 0
        try {
            DjPcmMixer.render(signal(60.0 to 0.2), signal(100.0 to 0.2), rate, plan) {
                if (++checks == 3) throw IllegalStateException("cancelled")
            }
            fail("Rendering ignored cancellation")
        } catch (error: IllegalStateException) { assertEquals("cancelled", error.message) }
    }
}
