// Copyright (c) 2026 eason204646. Licensed under Mulan PSL v2.
package com.music.purelymusic.playback

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToLong
import kotlin.math.sqrt

/** Lightweight analysis of the beginning and end of a locally stored track. */
data class TrackAnalysis(
    val durationMs: Long,
    val firstAudibleMs: Long,
    val lastAudibleMs: Long,
    val bpm: Float?,
    val firstBeatMs: Long?,
    val averageEnergy: Float,
    val openingChroma: List<Float>? = null,
    val closingChroma: List<Float>? = null,
    val closingBpm: Float? = null,
    val closingBeatMs: Long? = null,
    val earlyExitMs: Long? = null,
    val strongEntryMs: Long? = null
)

data class TransitionPlan(
    val startMs: Long,
    val fadeMs: Long,
    val incomingStartMs: Long,
    val incomingSpeed: Float,
    val style: TransitionStyle = TransitionStyle.SOFT_BLEND
)

enum class TransitionStyle { BEAT_MIX, DROP_MIX, SOFT_BLEND }

/** Keeps musical choices independent of the players and Android codecs. */
object AutoMixPlanner {
    const val MIN_BLEND_MS = 5_000L

    fun plan(
        outgoingDurationMs: Long,
        outgoing: TrackAnalysis?,
        incoming: TrackAnalysis?
    ): TransitionPlan? {
        if (outgoingDurationMs < 20_000L) return null

        // Blend into the musical ending instead of spending the overlap in trailing silence.
        val audibleEnd = outgoing?.lastAudibleMs
            ?.coerceIn(outgoingDurationMs - 15_000L, outgoingDurationMs)
            ?: outgoingDurationMs
        val trailingSilenceMs = outgoingDurationMs - audibleEnd
        val regularEndMs = if (trailingSilenceMs >= 500L) {
            (audibleEnd + 100L).coerceAtMost(outgoingDurationMs - 250L)
        } else outgoingDurationMs - 250L
        val outBpm = outgoing?.closingBpm ?: outgoing?.bpm
        val outBeatAnchor = outgoing?.closingBeatMs ?: outgoing?.firstBeatMs
        val inBpm = incoming?.bpm
        val speed = if (outBpm != null && inBpm != null) outBpm / inBpm else 1f
        val strongEntry = incoming?.strongEntryMs?.takeIf { it in 2_000L..12_000L }
        val incomingAudible = strongEntry
            ?: incoming?.firstAudibleMs?.coerceIn(0L, 8_000L) ?: 0L
        val incomingBeat = if (incoming?.firstBeatMs != null && inBpm != null) {
            val period = 60_000.0 / inBpm
            val anchor = incoming.firstBeatMs.toDouble()
            (anchor + ceil((incomingAudible - anchor).coerceAtLeast(0.0) / period) * period)
                .roundToLong()
        } else null
        val harmony = harmonicSimilarity(outgoing?.closingChroma, incoming?.openingChroma)
        val beatMatched = speed in 0.94f..1.06f && (harmony == null || harmony >= 0.55f) &&
            outBeatAnchor != null &&
            incomingBeat != null && incomingBeat in incomingAudible..(incomingAudible + 1_200L)
        val earlyExit = outgoing?.earlyExitMs?.takeIf {
            beatMatched && it in (outgoingDurationMs - 16_000L)..(outgoingDurationMs - 9_000L)
        }
        val desiredFade = if (beatMatched) {
            val beatMs = 60_000.0 / outBpm!!
            (beatMs * ceil(10_000.0 / beatMs)).roundToLong().coerceIn(8_500L, 11_500L)
        } else if (outgoing != null && incoming != null &&
            outgoing.averageEnergy < 0.08f && incoming.averageEnergy < 0.08f) {
            12_000L
        } else {
            10_500L
        }

        val alignedEarlyStart = if (earlyExit != null) {
            val beatMs = 60_000.0 / outBpm!!
            val beatAnchor = outBeatAnchor!!.toDouble()
            (beatAnchor + ceil((earlyExit - beatAnchor) / beatMs) * beatMs).roundToLong()
        } else null
        val useEarlyExit = alignedEarlyStart != null &&
            regularEndMs - alignedEarlyStart >= MIN_BLEND_MS
        val style = when {
            useEarlyExit -> TransitionStyle.DROP_MIX
            beatMatched -> TransitionStyle.BEAT_MIX
            else -> TransitionStyle.SOFT_BLEND
        }
        val endMs = if (useEarlyExit) {
            minOf(alignedEarlyStart!! + desiredFade, regularEndMs)
        } else regularEndMs
        var startMs = if (useEarlyExit) alignedEarlyStart!! else endMs - desiredFade
        if (beatMatched && !useEarlyExit) {
            val beatMs = 60_000.0 / outBpm!!
            val beatAnchor = outBeatAnchor!!.toDouble()
            startMs = (beatAnchor + floor((startMs - beatAnchor) / beatMs + 0.5) * beatMs)
                .roundToLong()
        }
        val fadeMs = endMs - startMs
        if (startMs < 1_000L || fadeMs < MIN_BLEND_MS) return null

        // Keep a few seconds of the next intro before its detected rise reaches the blend's middle.
        val incomingLead = if (strongEntry != null) {
            (strongEntry - fadeMs * 0.55f * speed).roundToLong().coerceAtLeast(0L)
        } else {
            (incoming?.firstAudibleMs?.coerceIn(0L, 8_000L) ?: 0L) - 500L
        }.coerceAtLeast(0L)
        val incomingStart = if (beatMatched) {
            val beatMs = 60_000.0 / inBpm!!
            val beatAnchor = incoming!!.firstBeatMs!!.toDouble()
            (beatAnchor + floor((incomingLead - beatAnchor) / beatMs) * beatMs)
                .roundToLong().coerceAtLeast(0L)
        } else incomingLead

        return TransitionPlan(
            startMs = startMs,
            fadeMs = fadeMs,
            incomingStartMs = incomingStart,
            incomingSpeed = if (beatMatched) speed else 1f,
            style = style
        )
    }

    /** Cosine similarity after removing the broadband floor from pitch-class energy. */
    private fun harmonicSimilarity(first: List<Float>?, second: List<Float>?): Float? {
        if (first?.size != 12 || second?.size != 12) return null
        val firstFloor = first.minOrNull() ?: return null
        val secondFloor = second.minOrNull() ?: return null
        var dot = 0.0
        var firstPower = 0.0
        var secondPower = 0.0
        for (index in 0 until 12) {
            val a = (first[index] - firstFloor).coerceAtLeast(0f).toDouble()
            val b = (second[index] - secondFloor).coerceAtLeast(0f).toDouble()
            dot += a * b
            firstPower += a * a
            secondPower += b * b
        }
        if (firstPower < 0.0001 || secondPower < 0.0001) return null
        return (dot / sqrt(firstPower * secondPower)).toFloat()
    }
}
