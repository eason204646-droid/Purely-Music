package com.music.purelymusic.playback

import kotlin.math.abs
import kotlin.math.round
import org.junit.Assert.*
import org.junit.Test

class AutoMixPlannerTest {
    private fun track(bpm: Float? = null, anchor: Long? = null) = TrackAnalysis(
        180_000L, 0L, 178_000L, bpm, anchor, 0.16f, beatConfidence = if (bpm == null) 0f else 0.95f
    )

    @Test fun alignsBothPassagesAndRendersOnIncomingNativeClock() {
        val plan = AutoMixPlanner.plan(180_000, track(123.4f, 163_127), track(120.7f, 371))!!
        assertEquals(TransitionStyle.BEAT_MIX, plan.style)
        assertEquals(1f, plan.incomingSpeed, 0f)
        assertEquals(120.7f / 123.4f, plan.outgoingSpeed, 0.00001f)
        val outPhase = (plan.outgoingSourceMs - 163_127) / (60_000.0 / 123.4 / 0.25)
        val inPhase = (plan.incomingStartMs - 371) / (60_000.0 / 120.7 / 0.25)
        assertEquals(round(outPhase), outPhase, 0.001)
        assertEquals(round(inPhase), inPhase, 0.001)
        assertEquals(16.0, plan.fadeMs / plan.beatMs, 0.002)
        assertTrue(plan.startMs + plan.fadeMs < 178_000)
    }

    @Test fun moderateTempoDifferenceIsActuallyMatched() {
        val plan = AutoMixPlanner.plan(180_000, track(120f, 0), track(128f, 0))!!
        assertEquals(TransitionStyle.BEAT_MIX, plan.style)
        assertEquals(128f / 120f, plan.outgoingSpeed, 0.0001f)
        assertTrue(plan.outgoingSourceMs + plan.fadeMs * plan.outgoingSpeed <= 178_000)
    }

    @Test fun halfAndDoubleTempoKeepPlaybackNearNativeSpeed() {
        for ((outBpm, inBpm) in listOf(70f to 140f, 140f to 70f)) {
            val plan = AutoMixPlanner.plan(180_000, track(outBpm, 0), track(inBpm, 0))!!
            assertEquals(TransitionStyle.BEAT_MIX, plan.style)
            assertEquals(1f, plan.outgoingSpeed, 0f)
        }
    }

    @Test fun choosesSparsePassageInsteadOfAlwaysTheFirstAudibleFrame() {
        val incoming = track(120f, 0).copy(openingSections = listOf(
            MixSection(6_000, 0.16f, 0.05f, 0.95f), MixSection(8_000, 0.16f, 0.8f, 0.25f),
            MixSection(10_000, 0.16f, 0.05f, 0.95f), MixSection(12_000, 0.16f, 0.05f, 0.95f)
        ))
        val plan = AutoMixPlanner.plan(180_000, track(120f, 0), incoming)!!
        assertEquals(4_000, plan.incomingStartMs)
    }

    @Test fun sparseRhythmHasALongerMusicalMixThanDenseMelodies() {
        val sparse = track(120f, 0).copy(openingDensity = 0.4f, closingDensity = 0.4f)
        val dense = track(120f, 0).copy(openingDensity = 0.9f, closingDensity = 0.9f)
        assertEquals(16_000, AutoMixPlanner.plan(180_000, sparse, sparse)!!.fadeMs)
        assertEquals(4_000, AutoMixPlanner.plan(180_000, dense, dense)!!.fadeMs)
    }

    @Test fun slowTempoStillUsesWholeBarsWithinTheRenderingBudget() {
        val sparse = track(70f, 0).copy(openingDensity = 0.4f, closingDensity = 0.4f)
        val plan = AutoMixPlanner.plan(180_000, sparse, sparse)!!
        val bars = plan.fadeMs / plan.beatMs / 4
        assertEquals(round(bars), bars, 0.001)
        assertTrue(plan.fadeMs <= 20_000)
        val handoffBar = bars * plan.handoffFraction
        assertEquals(round(handoffBar), handoffBar, 0.001)
    }

    @Test fun foregroundHandoffFallsOnABarBoundary() {
        val plan = AutoMixPlanner.plan(180_000, track(120f, 0).copy(earlyExitMs = 165_000),
            track(120f, 0).copy(strongEntryMs = 8_000))!!
        assertEquals(TransitionStyle.DROP_MIX, plan.style)
        val bars = plan.fadeMs / plan.beatMs * plan.handoffFraction / 4
        assertEquals(round(bars), bars, 0.001)
    }

    @Test fun weakOrIncompatibleRhythmDoesNotStretchAudio() {
        val weak = AutoMixPlanner.plan(180_000, track(120f, 0).copy(beatConfidence = 0.3f), track(118f, 0))!!
        assertEquals(TransitionStyle.SOFT_BLEND, weak.style)
        assertEquals(1f, weak.outgoingSpeed, 0f)
        val incompatible = AutoMixPlanner.plan(180_000, track(80f, 0), track(140f, 0))!!
        assertEquals(TransitionStyle.QUICK_HANDOFF, incompatible.style)
        assertEquals(1f, incompatible.outgoingSpeed, 0f)
    }

    @Test fun conflictingHarmonyAvoidsExtendedMelodyOverlap() {
        val outgoing = track(120f, 0).copy(closingChroma = List(12) { if (it == 0) 1f else 0f })
        val incoming = track(120f, 0).copy(openingChroma = List(12) { if (it == 6) 1f else 0f })
        assertEquals(TransitionStyle.QUICK_HANDOFF, AutoMixPlanner.plan(180_000, outgoing, incoming)!!.style)
    }

    @Test fun missingAnalysisAndShortTracksKeepNaturalPlayback() {
        assertNull(AutoMixPlanner.plan(180_000, null, null))
        assertNull(AutoMixPlanner.plan(15_000, track(), track()))
        assertNull(AutoMixPlanner.plan(180_000, track(), track().copy(durationMs = 8_000)))
    }

    @Test fun invalidFeaturesDoNotPoisonTheSignal() {
        val plan = AutoMixPlanner.plan(180_000, track(Float.NaN, 0).copy(averageEnergy = Float.NaN), track(0f, 0))!!
        assertTrue(plan.outgoingTrim.isFinite())
        assertTrue(plan.incomingTrim.isFinite())
        assertTrue(abs(plan.outgoingSpeed - 1f) < 0.0001f)
    }
}
