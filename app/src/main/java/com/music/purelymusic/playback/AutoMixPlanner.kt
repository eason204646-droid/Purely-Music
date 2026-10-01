// Copyright (c) 2026 eason204646. Licensed under Mulan PSL v2.
package com.music.purelymusic.playback

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.roundToLong
import kotlin.math.sqrt

/** Spectral activity is a cue-selection hint, not a vocal/stem detector. */
data class MixSection(val startMs: Long, val energy: Float, val bassRatio: Float, val density: Float)

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
    val strongEntryMs: Long? = null,
    val beatConfidence: Float = 0f,
    val closingBeatConfidence: Float? = null,
    val openingPhraseMs: Long? = null,
    val closingPhraseMs: Long? = null,
    val closingEnergy: Float? = null,
    val openingDensity: Float = 0.5f,
    val closingDensity: Float = 0.5f,
    val openingSections: List<MixSection> = emptyList(),
    val closingSections: List<MixSection> = emptyList()
)

data class TransitionPlan(
    val startMs: Long,
    val fadeMs: Long,
    val incomingStartMs: Long,
    // Incoming stays at native speed so its timeline and unmixed tail remain intact.
    val incomingSpeed: Float = 1f,
    val style: TransitionStyle = TransitionStyle.SOFT_BLEND,
    val handoffFraction: Float = 0.5f,
    val outgoingTrim: Float = 1f,
    val incomingTrim: Float = 1f,
    val outgoingSpeed: Float = 1f,
    val beatMs: Double = 500.0,
    val outgoingSourceMs: Long = startMs,
    val prerollMs: Long = 2_000L
)

enum class TransitionStyle { BEAT_MIX, DROP_MIX, SOFT_BLEND, QUICK_HANDOFF }

/** Choose two musical passages; render both on the incoming sample clock. */
object AutoMixPlanner {
    const val MIN_BLEND_MS = 1_200L
    private const val MAX_TEMPO_CHANGE = 0.10f

    fun plan(outgoingDurationMs: Long, outgoing: TrackAnalysis?, incoming: TrackAnalysis?): TransitionPlan? {
        // Missing analysis must not advertise a volume crossfade as an intelligent mix.
        if (outgoing == null || incoming == null || outgoingDurationMs < 20_000L ||
            incoming.durationMs < 20_000L) return null
        val audibleEnd = outgoing.lastAudibleMs.coerceIn(
            (outgoingDurationMs - 48_000L).coerceAtLeast(0L), outgoingDurationMs
        )
        val end = minOf(audibleEnd, outgoingDurationMs - 250L)
        val outBpm = validBpm(outgoing.closingBpm ?: outgoing.bpm)
        val inBpm = validBpm(incoming.bpm)
        val outAnchor = outgoing.closingBeatMs ?: outgoing.firstBeatMs
        val inAnchor = incoming.firstBeatMs
        val reliable = outBpm != null && inBpm != null && outAnchor != null && inAnchor != null &&
            (outgoing.closingBeatConfidence ?: outgoing.beatConfidence) >= 0.65f && incoming.beatConfidence >= 0.65f
        val effectiveInBpm = if (reliable) listOf(0.5f, 1f, 2f).map { inBpm!! * it }
            .minBy { abs(it / outBpm!! - 1f) } else null
        val speed = effectiveInBpm?.div(outBpm!!) ?: 1f
        val clash = harmonicSimilarity(outgoing.closingChroma, incoming.openingChroma)?.let { it < 0.40f } == true
        val rhythmic = reliable && abs(speed - 1f) <= MAX_TEMPO_CHANGE && !clash
        val dense = outgoing.closingDensity > 0.78f && incoming.openingDensity > 0.78f
        val outEnergy = safeEnergy(outgoing.closingEnergy ?: outgoing.averageEnergy)
        val inEnergy = safeEnergy(incoming.averageEnergy)
        val quiet = outEnergy < 0.075f && inEnergy < 0.075f
        val style = when {
            rhythmic && incoming.strongEntryMs != null && outgoing.earlyExitMs != null -> TransitionStyle.DROP_MIX
            rhythmic -> TransitionStyle.BEAT_MIX
            clash || reliable || (dense && !quiet) -> TransitionStyle.QUICK_HANDOFF
            else -> TransitionStyle.SOFT_BLEND
        }
        val beatMs = effectiveInBpm?.let { 60_000.0 / it } ?: 500.0
        var beats = if (dense) 8 else if (outgoing.closingDensity < 0.45f && incoming.openingDensity < 0.45f) 32 else 16
        while (beats * beatMs > 20_000 && beats > 8) beats -= 4
        val fade = when (style) {
            TransitionStyle.BEAT_MIX, TransitionStyle.DROP_MIX -> (beatMs * beats).roundToLong()
            TransitionStyle.QUICK_HANDOFF -> 2_400L
            TransitionStyle.SOFT_BLEND -> if (quiet) 8_000L else 4_500L
        }.coerceIn(MIN_BLEND_MS, 20_000L)
        val minimumCue = maxOf(2_200L, incoming.firstAudibleMs)
        var cue = minimumCue
        var source = end - fade - 250L
        if (rhythmic) {
            val inGrid = (incoming.openingPhraseMs ?: inAnchor!!).toDouble()
            val outGrid = (outgoing.closingPhraseMs ?: outAnchor!!).toDouble()
            val bar = beatMs * 4
            val outBar = 60_000.0 / outBpm!! * 4
            val firstCue = inGrid + ceil((minimumCue - inGrid) / bar) * bar
            val lastSource = outGrid + floor((end - maxOf(fade.toDouble(), fade * speed.toDouble()) - 250 - outGrid) / outBar) * outBar
            var best = Double.POSITIVE_INFINITY
            // Prefer sparse rhythmic passages; penalize discarded intro/outro content.
            for (introBar in 0..3) for (outroBar in 0..2) {
                val candidateCue = (firstCue + introBar * bar).roundToLong()
                val candidateSource = (lastSource - outroBar * outBar).roundToLong()
                if (candidateCue > maxOf(minimumCue + 4_000L, 8_000L) || candidateSource < outgoingDurationMs - 42_000L ||
                    candidateSource < 2_000L || candidateCue + fade > incoming.lastAudibleMs - 2_000L) continue
                val a = sectionAt(outgoing.closingSections, candidateSource + fade / 2)
                val b = sectionAt(incoming.openingSections, candidateCue + fade / 2)
                val score = (a?.let { it.density * (1 - it.bassRatio) } ?: outgoing.closingDensity) *
                    (b?.let { it.density * (1 - it.bassRatio) } ?: incoming.openingDensity) +
                    abs(ln(((a?.energy ?: outEnergy) + 0.01) / ((b?.energy ?: inEnergy) + 0.01))) * 0.12 +
                    introBar * 0.09 + outroBar * 0.12
                if (score < best) { best = score; cue = candidateCue; source = candidateSource }
            }
            if (!best.isFinite()) return null
        }
        if (source < 2_000L || source + fade * (if (rhythmic) speed else 1f) > end + 1 ||
            cue + fade > incoming.lastAudibleMs - 1_000L) return null
        val entryFraction = incoming.strongEntryMs?.let { (it - cue) / fade.toFloat() }
        val handoff = if (rhythmic) {
            val requestedBeat = ((entryFraction ?: 0.5f).coerceIn(0.25f, 0.75f) * beats / 4).toInt() * 4
            (requestedBeat.toFloat() / beats).coerceIn(0.25f, 0.75f)
        } else 0.5f
        val target = sqrt(outEnergy * inEnergy)
        return TransitionPlan(
            startMs = source, fadeMs = fade, incomingStartMs = cue, style = style,
            handoffFraction = handoff,
            outgoingTrim = (target / outEnergy).coerceIn(0.6f, 1f),
            incomingTrim = (target / inEnergy).coerceIn(0.6f, 1f),
            outgoingSpeed = if (rhythmic) speed else 1f, beatMs = beatMs, outgoingSourceMs = source
        )
    }

    private fun sectionAt(sections: List<MixSection>, time: Long) = sections.firstOrNull { time in it.startMs until it.startMs + 2_000L }
    private fun validBpm(bpm: Float?) = bpm?.takeIf { it.isFinite() && it in 40f..240f }
    private fun safeEnergy(value: Float) = if (value.isFinite()) value.coerceAtLeast(0.015f) else 0.15f

    private fun harmonicSimilarity(first: List<Float>?, second: List<Float>?): Float? {
        if (first?.size != 12 || second?.size != 12 || first.any { !it.isFinite() } || second.any { !it.isFinite() }) return null
        val firstFloor = first.min()
        val secondFloor = second.min()
        val a = first.map { (it - firstFloor).toDouble() }
        val b = second.map { (it - secondFloor).toDouble() }
        val power = sqrt(a.sumOf { it * it } * b.sumOf { it * it })
        return if (power < 0.0001) null else (a.indices.sumOf { a[it] * b[it] } / power).toFloat()
    }
}
