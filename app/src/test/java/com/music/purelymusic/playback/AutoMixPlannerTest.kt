package com.music.purelymusic.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoMixPlannerTest {
    @Test
    fun trimsShortSilenceAndBeginsAtIncomingAudio() {
        val outgoing = TrackAnalysis(180_000, 0, 177_000, null, null, 0.14f)
        val incoming = TrackAnalysis(200_000, 1_400, 200_000, null, null, 0.12f)

        val plan = AutoMixPlanner.plan(180_000, outgoing, incoming)

        assertNotNull(plan)
        assertEquals(1_400L, plan!!.incomingStartMs)
        assertEquals(176_850L, plan.startMs + plan.fadeMs)
        assertTrue(plan.fadeMs in 2_000L..4_000L)
    }

    @Test
    fun matchesCompatibleBeatsWithSmallSpeedCorrection() {
        val outgoing = TrackAnalysis(180_000, 0, 180_000, 120f, 500L, 0.16f)
        val incoming = TrackAnalysis(190_000, 600, 190_000, 118f, 1_000L, 0.17f)

        val plan = AutoMixPlanner.plan(180_000, outgoing, incoming)

        assertNotNull(plan)
        assertEquals(1_000L, plan!!.incomingStartMs)
        assertTrue(plan.incomingSpeed > 1f && plan.incomingSpeed < 1.06f)
        assertTrue(plan.fadeMs >= 3_800L)
    }

    @Test
    fun incompatibleTempoUsesOrdinaryFade() {
        val outgoing = TrackAnalysis(180_000, 0, 180_000, 80f, 400L, 0.2f)
        val incoming = TrackAnalysis(180_000, 0, 180_000, 140f, 400L, 0.2f)

        val plan = AutoMixPlanner.plan(180_000, outgoing, incoming)

        assertNotNull(plan)
        assertEquals(1f, plan!!.incomingSpeed, 0.0001f)
        assertEquals(2_700L, plan.fadeMs)
    }

    @Test
    fun conflictingPitchClassesAvoidBeatMix() {
        val firstChroma = List(12) { if (it == 0) 1f else 0f }
        val secondChroma = List(12) { if (it == 6) 1f else 0f }
        val outgoing = TrackAnalysis(
            180_000, 0, 180_000, 120f, 500L, 0.16f,
            closingChroma = firstChroma
        )
        val incoming = TrackAnalysis(
            190_000, 600, 190_000, 118f, 1_000L, 0.17f,
            openingChroma = secondChroma
        )

        val plan = AutoMixPlanner.plan(180_000, outgoing, incoming)

        assertNotNull(plan)
        assertEquals(1f, plan!!.incomingSpeed, 0.0001f)
        assertEquals(2_700L, plan.fadeMs)
    }

    @Test
    fun shortTracksDoNotOverlap() {
        assertNull(AutoMixPlanner.plan(7_000, null, null))
    }
}
