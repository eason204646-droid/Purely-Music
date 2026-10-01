package com.music.purelymusic.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.random.Random

class TransitionAnalysisTest {
    @Test
    fun findsBeatGridInClosingWindow() {
        val frames = List(500) { index ->
            EnergyFrame(160_000L + index * 40L, if (index % 15 == 3) 0.3f else 0.01f)
        }

        val beat = BeatAnalyzer.estimate(frames)

        assertNotNull(beat)
        assertEquals(100f, beat!!.bpm, 0.1f)
        assertEquals(160_120L, beat.anchorMs)
    }

    @Test
    fun doesNotInventBeatsInSteadyAudio() {
        val frames = List(500) { index -> EnergyFrame(index * 40L, 0.1f) }
        assertNull(BeatAnalyzer.estimate(frames))
    }

    @Test
    fun refinesFractionalTempoWithoutIntegerFrameQuantization() {
        val period = 60_000.0 / 123.4
        val frames = List(2_800) { index ->
            val time = index * 10L
            val beat = kotlin.math.round((time - 127.0) / period)
            val attack = abs(time - 127.0 - beat * period) < 5.1
            EnergyFrame(152_000L + time, if (attack) 0.3f else 0.015f)
        }
        val estimate = BeatAnalyzer.estimate(frames)!!
        assertEquals(123.4f, estimate.bpm, 0.12f)
        assertTrue(estimate.confidence >= 0.85f)
        val phase = (estimate.anchorMs - 152_127L) / period
        assertEquals(kotlin.math.round(phase), phase, 0.02)
    }

    @Test
    fun irregularAttacksDoNotInventAConfidentGrid() {
        val random = Random(7)
        val frames = List(2_800) { index ->
            EnergyFrame(index * 10L, if (random.nextFloat() < 0.07f) random.nextFloat() * 0.3f else 0.01f)
        }
        assertNull(BeatAnalyzer.estimate(frames))
    }

    @Test
    fun strongSingleCrashDoesNotMoveTheBeatGrid() {
        val frames = List(2_800) { index ->
            EnergyFrame(index * 10L, when {
                index == 1_157 -> 1f
                index % 50 == 13 -> 0.2f
                else -> 0.01f
            })
        }
        val estimate = BeatAnalyzer.estimate(frames)!!
        assertEquals(120f, estimate.bpm, 0.15f)
        assertTrue(abs(estimate.anchorMs - 130L) <= 10L)
    }

    @Test
    fun ignoresIsolatedClicksAtSongBoundaries() {
        val frames = List(100) { index ->
            val energy = when {
                index == 0 || index == 90 -> 0.2f
                index in 10..80 -> 0.1f
                else -> 0f
            }
            EnergyFrame(index * 40L, energy)
        }

        assertEquals(400L, SilenceDetector.firstAudible(frames, 0.01f))
        assertEquals(3_200L, SilenceDetector.lastAudible(frames, 0.01f))
    }
}
