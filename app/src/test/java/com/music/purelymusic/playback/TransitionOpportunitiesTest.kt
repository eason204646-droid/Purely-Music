package com.music.purelymusic.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TransitionOpportunitiesTest {
    @Test
    fun sustainedQuietOutroCanExitEarly() {
        val frames = (160_000L until 180_000L step 40L).map { time ->
            EnergyFrame(time, if (time < 166_000L) 0.20f else 0.025f)
        }

        val exit = TransitionOpportunities.earlyExit(frames, 180_000L)

        assertTrue(exit != null && exit in 164_000L..167_000L)
    }

    @Test
    fun briefQuietBreakDoesNotCutTheEnding() {
        val frames = (160_000L until 180_000L step 40L).map { time ->
            EnergyFrame(time, if (time in 166_000L..170_000L) 0.02f else 0.20f)
        }

        assertNull(TransitionOpportunities.earlyExit(frames, 180_000L))
    }

    @Test
    fun clearRiseCanCuePastQuietIntro() {
        val frames = (0L until 20_000L step 40L).map { time ->
            EnergyFrame(time, if (time < 8_000L) 0.02f else 0.18f)
        }

        assertEquals(8_000L, TransitionOpportunities.strongEntry(frames))
    }

    @Test
    fun steadyIntroIsNotSkipped() {
        val frames = (0L until 20_000L step 40L).map { EnergyFrame(it, 0.14f) }
        assertNull(TransitionOpportunities.strongEntry(frames))
    }

    @Test
    fun meaningfulIntroBeforeBreakIsPreserved() {
        val frames = (0L until 20_000L step 40L).map { time ->
            EnergyFrame(time, when {
                time < 5_000L -> 0.16f
                time < 8_000L -> 0.02f
                else -> 0.18f
            })
        }
        assertNull(TransitionOpportunities.strongEntry(frames))
    }
}
