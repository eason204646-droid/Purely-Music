// Copyright (c) 2026 eason204646. Licensed under Mulan PSL v2.
package com.music.purelymusic.playback

/** Finds clear changes in the decoded intro and outro, leaving ambiguous passages intact. */
internal object TransitionOpportunities {
    fun earlyExit(frames: List<EnergyFrame>, durationMs: Long): Long? {
        if (durationMs < 60_000L || frames.size < 150) return null
        for (index in frames.indices step 5) {
            val time = frames[index].timeMs
            if (time !in (durationMs - 16_000L)..(durationMs - 9_000L)) continue
            val before = mean(frames, time - 2_400L, time - 400L) ?: continue
            val after = mean(frames, time + 400L, time + 3_400L) ?: continue
            val tail = frames.filter { it.timeMs >= time + 1_000L }
            if (tail.size < 120 || before < 0.07f || after > 0.09f ||
                after > before * 0.46f || tail.count { it.energy > before * 0.68f } > tail.size / 20 ||
                tail.map { it.energy }.average() > before * 0.46f) continue
            return time
        }
        return null
    }

    fun strongEntry(frames: List<EnergyFrame>): Long? {
        if (frames.size < 100) return null
        for (index in frames.indices step 5) {
            val time = frames[index].timeMs
            if (time !in 2_000L..11_500L) continue
            val before = mean(frames, time - 1_600L, time - 200L) ?: continue
            val after = mean(frames, time + 200L, time + 1_800L) ?: continue
            if (before > 0.06f || after < 0.09f || after < before * 2.5f) continue
            val lead = frames.filter { it.timeMs < time - 200L }
            if (lead.isEmpty() || lead.count { it.energy > after * 0.55f } > lead.size / 10) continue
            val onset = frames.firstOrNull {
                it.timeMs in time..(time + 1_800L) && it.energy >= after * 0.65f
            } ?: continue
            if (onset.timeMs <= 12_000L) return onset.timeMs
        }
        return null
    }

    private fun mean(frames: List<EnergyFrame>, fromMs: Long, untilMs: Long): Float? {
        val selected = frames.filter { it.timeMs >= fromMs && it.timeMs < untilMs }
        if (selected.size < (untilMs - fromMs) / 80L) return null
        return selected.map { it.energy }.average().toFloat()
    }
}
