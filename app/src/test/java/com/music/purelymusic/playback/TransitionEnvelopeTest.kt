package com.music.purelymusic.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TransitionEnvelopeTest {
    @Test
    fun allStylesStartAndEndAtFullTrackVolume() {
        TransitionStyle.entries.forEach { style ->
            val start = TransitionEnvelope.gains(style, 0f)
            val end = TransitionEnvelope.gains(style, 1f)
            assertEquals(1f, start.outgoing, 0.0001f)
            assertEquals(0f, start.incoming, 0.0001f)
            assertEquals(0f, end.outgoing, 0.0001f)
            assertEquals(1f, end.incoming, 0.0001f)
        }
    }

    @Test
    fun shortCutHasLessOverlapThanBeatMix() {
        val cut = TransitionEnvelope.gains(TransitionStyle.SHORT_CUT, 0.5f)
        val beat = TransitionEnvelope.gains(TransitionStyle.BEAT_MIX, 0.5f)
        assertTrue(cut.outgoing + cut.incoming < beat.outgoing + beat.incoming)
        assertTrue(cut.outgoing > 0f && cut.incoming > 0f)
    }

    @Test
    fun gainsStayInRangeThroughoutTransition() {
        TransitionStyle.entries.forEach { style ->
            (0..100).forEach { step ->
                val gains = TransitionEnvelope.gains(style, step / 100f)
                assertTrue(gains.outgoing in 0f..1f)
                assertTrue(gains.incoming in 0f..1f)
            }
        }
    }
}
