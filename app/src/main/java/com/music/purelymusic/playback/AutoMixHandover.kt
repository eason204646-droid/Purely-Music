// Copyright (c) 2026 eason204646. Licensed under Mulan PSL v2.
package com.music.purelymusic.playback

import kotlin.math.abs

/** Synchronize only the muted preroll; the rendered musical transition always plays at native speed. */
internal object AutoMixHandover {
    // Allow for Android's sampled playhead (~20 ms) without accepting an audible rhythmic skip.
    const val MAX_SLIP_MS = 40L
    const val SEEK_SETTLE_MS = 500L
    const val DURATION_MS = 80L

    fun needsCorrection(errorMs: Long, remainingMs: Long, attempts: Int): Boolean =
        abs(errorMs) > 25L && attempts < 3 && remainingMs > SEEK_SETTLE_MS + 500L

    fun seekLeadAfterRecovery(previousLeadMs: Long, residualMs: Long): Long =
        (previousLeadMs - residualMs).coerceIn(0L, 250L)

    fun fraction(elapsedMs: Long): Float {
        val t = (elapsedMs.toFloat() / DURATION_MS).coerceIn(0f, 1f)
        return t * t * (3 - 2 * t)
    }
}
