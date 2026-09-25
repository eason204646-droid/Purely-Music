// Copyright (c) 2026 eason204646. Licensed under Mulan PSL v2.
package com.music.purelymusic.playback

import kotlin.math.abs
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

enum class TransitionStyle { BEAT_MIX, DROP_MIX, SHORT_CUT, SOFT_BLEND }

/** Keeps musical choices independent of the players and Android codecs. */
object AutoMixPlanner {
    fun plan(
        outgoingDurationMs: Long,
        outgoing: TrackAnalysis?,
        incoming: TrackAnalysis?
    ): TransitionPlan? {
        if (outgoingDurationMs < 8_000L) return null

        // Never remove more than eight seconds: a long quiet passage may be intentional.
        val audibleEnd = outgoing?.lastAudibleMs
            ?.coerceIn(outgoingDurationMs - 8_000L, outgoingDurationMs)
            ?: outgoingDurationMs
        val trailingSilenceMs = outgoingDurationMs - audibleEnd
        var endMs = (audibleEnd - 150L).coerceAtMost(outgoingDurationMs - 250L)
        val outBpm = outgoing?.closingBpm ?: outgoing?.bpm
        val outBeatAnchor = outgoing?.closingBeatMs ?: outgoing?.firstBeatMs
        val inBpm = incoming?.bpm
        val speed = if (outBpm != null && inBpm != null) outBpm / inBpm else 1f
        val incomingAudible = incoming?.strongEntryMs?.takeIf { it in 0L..12_000L }
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
        val style = when {
            earlyExit != null -> TransitionStyle.DROP_MIX
            beatMatched -> TransitionStyle.BEAT_MIX
            trailingSilenceMs >= 1_000L -> TransitionStyle.SHORT_CUT
            else -> TransitionStyle.SOFT_BLEND
        }
        if (style == TransitionStyle.SHORT_CUT) {
            endMs = (audibleEnd + 80L).coerceAtMost(outgoingDurationMs - 250L)
        }
        val incomingStart = if (beatMatched) incomingBeat!! else incomingAudible

        val desiredFade = if (style == TransitionStyle.BEAT_MIX || style == TransitionStyle.DROP_MIX) {
            val beatMs = 60_000f / outBpm!!
            (beatMs * 8f).roundToLong().coerceIn(2_000L, 6_500L)
        } else if (style == TransitionStyle.SHORT_CUT) {
            650L
        } else if (outgoing != null && incoming != null &&
            outgoing.averageEnergy < 0.08f && incoming.averageEnergy < 0.08f) {
            4_000L
        } else {
            2_700L
        }

        var startMs = endMs - desiredFade
        if (earlyExit != null) {
            val beatMs = 60_000.0 / outBpm!!
            val beatAnchor = outBeatAnchor!!.toDouble()
            startMs = (beatAnchor + ceil((earlyExit - beatAnchor) / beatMs) * beatMs).roundToLong()
            endMs = (startMs + desiredFade).coerceAtMost(outgoingDurationMs - 250L)
        } else if (beatMatched) {
            val beatMs = 60_000.0 / outBpm!!
            val beatAnchor = outBeatAnchor!!.toDouble()
            val alignedEnd = beatAnchor + floor((endMs - beatAnchor) / beatMs) * beatMs
            if (abs(alignedEnd - endMs) < 300.0) {
                endMs = alignedEnd.roundToLong()
                startMs = endMs - desiredFade
            } else {
                val alignedStart = beatAnchor + floor((startMs - beatAnchor) / beatMs) * beatMs
                if (abs(alignedStart - startMs) < 300.0) startMs = alignedStart.roundToLong()
            }
        }
        if (startMs < 1_000L || endMs - startMs < 400L) return null

        return TransitionPlan(
            startMs = startMs,
            fadeMs = endMs - startMs,
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
