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

    private fun rms(samples: FloatArray, from: Double, until: Double): Double {
        val first = (from * rate).toInt() * 2
        val last = (until * rate).toInt() * 2
        var power = 0.0
        for (i in first until last) power += samples[i].toDouble() * samples[i]
        return kotlin.math.sqrt(power / (last - first))
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

    @Test fun unrelatedMidrangeKeepsItsLevelThroughTheHandoff() {
        val outgoing = signal(700.0 to 0.2)
        val incoming = signal(1_100.0 to 0.2)
        val reference = rms(outgoing, 1.7, 2.5)
        for (style in TransitionStyle.entries) {
            val mixed = DjPcmMixer.render(outgoing, incoming, rate, plan.copy(style = style))
            for (step in 0..6) {
                val from = 1.5 + step * 0.15
                val relative = rms(mixed, from, from + 0.1) / reference
                assertTrue("$style midrange level at $from s: $relative", relative in 0.85..1.12)
            }
        }
    }

    @Test fun correlatedMidrangeDoesNotSwellDuringTheHandoff() {
        val source = signal(700.0 to 0.2)
        val mixed = DjPcmMixer.render(source, source, rate, plan)
        for (step in 0..6) {
            val from = 1.5 + step * 0.15
            val relative = rms(mixed, from, from + 0.1) / rms(source, from, from + 0.1)
            assertTrue("Correlated midrange level at $from s: $relative", relative in 0.95..1.05)
        }
    }

    @Test fun midrangeStillFavorsTheForegroundPassage() {
        val mixed = DjPcmMixer.render(signal(700.0 to 0.2), signal(1_100.0 to 0.2), rate, plan)
        assertTrue(amplitude(mixed, 1_100.0, 1.55, 1.65) < amplitude(mixed, 700.0, 1.55, 1.65) * 0.15)
        assertTrue(amplitude(mixed, 700.0, 2.55, 2.65) < amplitude(mixed, 1_100.0, 2.55, 2.65) * 0.15)
    }

    @Test fun limiterAnticipatesATransientWithoutASuddenGainStep() {
        val samples = FloatArray(rate * 2) { if (it % 2 == 0) 0.25f else 0.125f }
        val attack = rate / 2
        samples[attack * 2] = 1.8f
        samples[attack * 2 + 1] = 0.9f
        DjPcmMixer.limit(samples, rate)
        val largestStep = (attack - rate / 200 until attack).maxOf { frame ->
            abs(samples[frame * 2] - samples[(frame - 1) * 2])
        }
        assertTrue("Limiter gain changed abruptly: $largestStep", largestStep < 0.005f)
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
