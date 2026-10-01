// Copyright (c) 2026 eason204646. Licensed under Mulan PSL v2.
package com.music.purelymusic.playback

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.sqrt

/** Joint PCM mix: complementary frequency bands, bar-level bass exchange and shared limiting. */
internal object DjPcmMixer {
    /** Put the decoder handover between attacks; inspect its whole neighborhood for clock jitter. */
    fun quietHandoverBeforeEndMs(preroll: FloatArray, rate: Int): Long {
        val binFrames = rate / 100
        val bins = preroll.size / 2 / binFrames
        require(bins >= 40)
        val energy = FloatArray(bins) { bin ->
            var power = 0.0
            for (frame in bin * binFrames until (bin + 1) * binFrames) {
                power += preroll[frame * 2] * preroll[frame * 2] + preroll[frame * 2 + 1] * preroll[frame * 2 + 1]
            }
            (power / (binFrames * 2)).toFloat()
        }
        var chosen = bins - 25
        var best = Double.POSITIVE_INFINITY
        for (bin in maxOf(15, bins - 100)..bins - 20) {
            val neighborhood = (bin - 15..bin + 15).maxOf { energy[it] }
            val score = neighborhood + (bins - bin) * 0.000001
            if (score < best) { best = score; chosen = bin }
        }
        return (bins - chosen) * 10L
    }

    fun render(outgoing: FloatArray, incoming: FloatArray, rate: Int, plan: TransitionPlan, check: () -> Unit = {}): FloatArray {
        require(outgoing.size == incoming.size && outgoing.size % 2 == 0)
        val outLow = lowPass(outgoing, rate, 180.0, check)
        val outUpper = lowPass(outgoing, rate, 2_800.0, check)
        val inLow = lowPass(incoming, rate, 180.0, check)
        val inUpper = lowPass(incoming, rate, 2_800.0, check)
        val result = FloatArray(incoming.size)
        val frames = incoming.size / 2
        val rhythmic = plan.style == TransitionStyle.BEAT_MIX || plan.style == TransitionStyle.DROP_MIX
        val handoff = plan.handoffFraction * frames
        // A beat around the chosen bar boundary, instead of bass fading for the whole mix.
        val bassWidth = (plan.beatMs * rate / 1_000.0).toFloat().coerceAtMost(frames * 0.35f)
        val collisionRelease = exp(-1.0 / (rate * 0.12)).toFloat()
        var outActivity = 0f
        var inActivity = 0f
        for (frame in 0 until frames) {
            if (frame % 4_096 == 0) check()
            val t = frame.toFloat() / (frames - 1).coerceAtLeast(1)
            val bass = ramp(frame.toFloat(), handoff - bassWidth / 2, handoff + bassWidth / 2)
            val melody = ramp(t, (plan.handoffFraction - 0.15f).coerceAtLeast(0.1f), (plan.handoffFraction + 0.2f).coerceAtMost(0.9f))
            val introduce = ramp(t, 0f, if (rhythmic) 0.25f else 0.4f)
            val exit = 1 - ramp(t, if (rhythmic) 0.65f else 0.5f, 1f)
            val outTrim = 1 + (plan.outgoingTrim - 1) * ramp(t, 0f, 0.15f)
            val inTrim = plan.incomingTrim + (1 - plan.incomingTrim) * ramp(t, 0.8f, 1f)
            var outMidPower = 0f
            var inMidPower = 0f
            for (channel in 0..1) {
                val index = frame * 2 + channel
                outMidPower += abs(outUpper[index] - outLow[index])
                inMidPower += abs(inUpper[index] - inLow[index])
            }
            outActivity = maxOf(outMidPower, outActivity * collisionRelease)
            inActivity = maxOf(inMidPower, inActivity * collisionRelease)
            val bothActive = minOf(outActivity, inActivity) / maxOf(outActivity, inActivity, 0.015f)
            // Suppress the background midrange when both passages have a sustained foreground.
            val outMidGain = (1 - melody) * (1 - bothActive * melody * 0.65f)
            val inMidGain = melody * (1 - bothActive * (1 - melody) * 0.65f)
            val outBass = if (rhythmic) 1 - bass else 1 - melody
            val inBass = if (rhythmic) bass else melody
            for (channel in 0..1) {
                val i = frame * 2 + channel
                val out = outLow[i] * outBass + (outUpper[i] - outLow[i]) * outMidGain + (outgoing[i] - outUpper[i]) * exit
                val next = inLow[i] * inBass + (inUpper[i] - inLow[i]) * inMidGain + (incoming[i] - inUpper[i]) * introduce
                result[i] = out * outTrim + next * inTrim
            }
        }
        limit(result, rate, check)
        return result
    }

    private fun ramp(value: Float, from: Float, until: Float): Float {
        val t = ((value - from) / (until - from).coerceAtLeast(0.0001f)).coerceIn(0f, 1f)
        return t * t * (3 - 2 * t)
    }

    /** Forward/backward Butterworth filters give zero phase and exact complementary reconstruction. */
    private fun lowPass(source: FloatArray, rate: Int, cutoff: Double, check: () -> Unit): FloatArray {
        val result = source.copyOf()
        val omega = 2 * PI * minOf(cutoff, rate * 0.4) / rate
        val alpha = sin(omega) / sqrt(2.0)
        val a0 = 1 + alpha
        val b0 = (1 - cos(omega)) / 2 / a0
        val b1 = 2 * b0
        val a1 = -2 * cos(omega) / a0
        val a2 = (1 - alpha) / a0
        for (direction in 0..1) for (channel in 0..1) {
            val first = if (direction == 0) channel else result.size - 2 + channel
            var x1 = result[first].toDouble()
            var x2 = x1
            var y1 = x1
            var y2 = x1
            val indices = if (direction == 0) first until result.size step 2 else first downTo channel step 2
            for (i in indices) {
                if (i % 8_192 == channel) check()
                val x = result[i].toDouble()
                val y = b0 * x + b1 * x1 + b0 * x2 - a1 * y1 - a2 * y2
                result[i] = y.toFloat()
                x2 = x1; x1 = x; y2 = y1; y1 = y
            }
        }
        return result
    }

    /** Shared stereo lookahead gain; never independently clip the two decks. */
    internal fun limit(samples: FloatArray, rate: Int, check: () -> Unit = {}) {
        val frames = samples.size / 2
        val lookahead = maxOf(1, rate / 200)
        val maxima = IntArray(frames)
        val peaks = FloatArray(frames) { maxOf(abs(samples[it * 2]), abs(samples[it * 2 + 1])) }
        var head = 0
        var tail = 0
        var gain = 1f
        val release = (1 - exp(-1.0 / (rate * 0.08))).toFloat()
        var inserted = 0
        for (frame in 0 until frames) {
            if (frame % 4_096 == 0) check()
            while (inserted < minOf(frames, frame + lookahead + 1)) {
                while (tail > head && peaks[maxima[tail - 1]] <= peaks[inserted]) tail--
                maxima[tail++] = inserted++
            }
            while (tail > head && maxima[head] < frame) head++
            val target = minOf(1f, 0.98f / peaks[maxima[head]].coerceAtLeast(0.00001f))
            gain = if (target < gain) target else gain + release * (target - gain)
            samples[frame * 2] *= gain
            samples[frame * 2 + 1] *= gain
        }
    }
}
