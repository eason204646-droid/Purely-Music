package com.music.purelymusic.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoMixSpectrumTest {
    @Test
    fun bassMovesFromOutgoingToIncoming() {
        assertEquals(0, AutoMixSpectrum.attenuationMillibels(100, 0f, outgoing = true))
        assertEquals(-1200, AutoMixSpectrum.attenuationMillibels(100, 0f, outgoing = false))
        assertEquals(-1200, AutoMixSpectrum.attenuationMillibels(100, 1f, outgoing = true))
        assertEquals(0, AutoMixSpectrum.attenuationMillibels(100, 1f, outgoing = false))
    }

    @Test
    fun incomingTrebleOpensBeforeBass() {
        val treble = AutoMixSpectrum.attenuationMillibels(5_000, 0.4f, outgoing = false)
        val bass = AutoMixSpectrum.attenuationMillibels(100, 0.4f, outgoing = false)
        assertEquals(0, treble)
        assertTrue(bass < 0)
    }
}
