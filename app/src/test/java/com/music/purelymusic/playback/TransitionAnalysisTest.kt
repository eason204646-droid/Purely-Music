package com.music.purelymusic.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

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
