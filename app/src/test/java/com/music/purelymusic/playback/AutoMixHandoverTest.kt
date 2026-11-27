// Copyright (c) 2026 eason204646. Licensed under Mulan PSL v2.
package com.music.purelymusic.playback

import kotlin.math.abs
import org.junit.Assert.*
import org.junit.Test

class AutoMixHandoverTest {
    @Test fun prerollLearnsDecoderRecoveryLagAndCorrectsSmallClockErrors() {
        for (initialError in listOf(-100L, -30L, 30L, 100L)) {
            for (decoderLag in listOf(10L, 70L, 130L, 220L)) {
                var error = initialError
                var remaining = 3_000L
                var lead = 35L
                var attempts = 0
                while (remaining > 0L) {
                    if (AutoMixHandover.needsCorrection(error, remaining, attempts)) {
                        // After seeking ahead, the outgoing deck advances during decoder recovery.
                        error = lead - decoderLag
                        lead = AutoMixHandover.seekLeadAfterRecovery(lead, error)
                        attempts++
                        remaining -= AutoMixHandover.SEEK_SETTLE_MS
                    } else remaining -= 5L
                }
                assertTrue("$initialError ms initial slip, $decoderLag ms recovery: $error", abs(error) <= 25L)
                assertTrue(attempts <= 3)
            }
        }
    }

    @Test fun correctionsLeaveTimeForTheDecoderToSettleBeforeHandover() {
        assertTrue(AutoMixHandover.needsCorrection(30L, 2_000L, 0))
        assertTrue(AutoMixHandover.needsCorrection(-30L, 2_000L, 0))
        assertFalse(AutoMixHandover.needsCorrection(25L, 2_000L, 0))
        assertFalse(AutoMixHandover.needsCorrection(100L, AutoMixHandover.SEEK_SETTLE_MS + 500L, 0))
        assertFalse(AutoMixHandover.needsCorrection(100L, 2_000L, 3))
        assertTrue(100L > AutoMixHandover.MAX_SLIP_MS)
    }

    @Test fun measuredRecoveryAdjustsLeadInBothDirectionsWithinSafeBounds() {
        assertEquals(70L, AutoMixHandover.seekLeadAfterRecovery(35L, -35L))
        assertEquals(10L, AutoMixHandover.seekLeadAfterRecovery(35L, 25L))
        assertEquals(0L, AutoMixHandover.seekLeadAfterRecovery(35L, 100L))
        assertEquals(250L, AutoMixHandover.seekLeadAfterRecovery(35L, -500L))
    }

    @Test fun quietBridgeHasComplementaryMonotonicGainsAndGentleEndpoints() {
        assertEquals(0f, AutoMixHandover.fraction(-1L), 0f)
        assertEquals(1f, AutoMixHandover.fraction(AutoMixHandover.DURATION_MS + 1L), 0f)
        var previous = 0f
        for (elapsed in 0L..AutoMixHandover.DURATION_MS) {
            val incoming = AutoMixHandover.fraction(elapsed)
            assertTrue(incoming >= previous)
            assertEquals(1f, (1f - incoming) + incoming, 0f)
            previous = incoming
        }
        assertEquals(0.5f, AutoMixHandover.fraction(AutoMixHandover.DURATION_MS / 2), 0f)
        assertTrue(AutoMixHandover.fraction(5L) < 5f / AutoMixHandover.DURATION_MS)
    }
}
