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
        assertEquals(177_080L, plan.startMs + plan.fadeMs)
        assertEquals(TransitionStyle.SHORT_CUT, plan.style)
        assertEquals(650L, plan.fadeMs)
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
        assertEquals(TransitionStyle.BEAT_MIX, plan.style)
    }

    @Test
    fun incompatibleTempoUsesOrdinaryFade() {
        val outgoing = TrackAnalysis(180_000, 0, 180_000, 80f, 400L, 0.2f)
        val incoming = TrackAnalysis(180_000, 0, 180_000, 140f, 400L, 0.2f)

        val plan = AutoMixPlanner.plan(180_000, outgoing, incoming)

        assertNotNull(plan)
        assertEquals(1f, plan!!.incomingSpeed, 0.0001f)
        assertEquals(2_700L, plan.fadeMs)
        assertEquals(TransitionStyle.SOFT_BLEND, plan.style)
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
    fun usesClosingBeatWhenTempoChangesDuringTrack() {
        val outgoing = TrackAnalysis(
            180_000, 0, 180_000, 90f, 400L, 0.15f,
            closingBpm = 100f, closingBeatMs = 160_120L
        )
        val incoming = TrackAnalysis(190_000, 0, 190_000, 100f, 400L, 0.15f)

        val plan = AutoMixPlanner.plan(180_000, outgoing, incoming)

        assertNotNull(plan)
        assertEquals(1f, plan!!.incomingSpeed, 0.0001f)
        assertTrue(plan.fadeMs >= 4_500L)
    }

    @Test
    fun cuesIncomingAtFirstBeatAfterItsAudibleStart() {
        val outgoing = TrackAnalysis(180_000, 0, 180_000, 100f, 120L, 0.15f)
        val incoming = TrackAnalysis(190_000, 2_000L, 190_000, 100f, 400L, 0.15f)

        val plan = AutoMixPlanner.plan(180_000, outgoing, incoming)

        assertNotNull(plan)
        assertEquals(2_200L, plan!!.incomingStartMs)
    }

    @Test
    fun shortTracksDoNotOverlap() {
        assertNull(AutoMixPlanner.plan(7_000, null, null))
    }

    @Test
    fun alignedDropUsesAnalyzedEarlyExitAndStrongEntry() {
        val outgoing = TrackAnalysis(
            180_000L, 0L, 180_000L, 120f, 0L, 0.16f,
            closingBpm = 120f, closingBeatMs = 160_000L, earlyExitMs = 165_000L
        )
        val incoming = TrackAnalysis(
            190_000L, 0L, 190_000L, 120f, 0L, 0.16f,
            strongEntryMs = 8_000L
        )

        val plan = AutoMixPlanner.plan(180_000L, outgoing, incoming)

        assertNotNull(plan)
        assertEquals(TransitionStyle.DROP_MIX, plan!!.style)
        assertEquals(8_000L, plan.incomingStartMs)
        assertTrue(plan.startMs in 165_000L..165_500L)
        assertTrue(plan.startMs + plan.fadeMs <= 172_000L)
    }

    @Test
    fun incompatibleTempoDoesNotUseEarlyExit() {
        val outgoing = TrackAnalysis(
            180_000L, 0L, 180_000L, 120f, 0L, 0.16f,
            earlyExitMs = 165_000L
        )
        val incoming = TrackAnalysis(190_000L, 0L, 190_000L, 90f, 0L, 0.16f)

        val plan = AutoMixPlanner.plan(180_000L, outgoing, incoming)

        assertNotNull(plan)
        assertEquals(TransitionStyle.SOFT_BLEND, plan!!.style)
        assertTrue(plan.startMs > 175_000L)
    }
}
