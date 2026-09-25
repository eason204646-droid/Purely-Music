// Copyright (c) 2026 eason204646. Licensed under Mulan PSL v2.
package com.music.purelymusic.playback

import android.media.audiofx.Equalizer
import android.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.music.purelymusic.utils.PreferencesManager
import java.util.concurrent.CopyOnWriteArraySet
import kotlin.math.roundToInt

/** Keeps the audio effect alive for the same lifetime as the playback service. */
@androidx.annotation.OptIn(UnstableApi::class)
class PlaybackEqualizer {
    data class State(
        val levels: List<Short> = emptyList(),
        val frequencies: List<Pair<Int, Int>> = emptyList(),
        val levelRange: Pair<Short, Short> = (-1500).toShort() to 1500.toShort(),
        val preset: String = "Flat"
    )

    private val listeners = CopyOnWriteArraySet<(State) -> Unit>()
    private data class BoundEffect(val effect: Equalizer, val sessionId: Int, val state: State)
    private var effect: Equalizer? = null
    private var sessionId: Int? = null
    private var transitionEffect: BoundEffect? = null
    private var enabled = false
    var state = State()
        private set

    fun addListener(listener: (State) -> Unit) {
        listeners += listener
        listener(state)
    }

    fun removeListener(listener: (State) -> Unit) {
        listeners -= listener
    }

    fun setEnabled(value: Boolean, player: ExoPlayer) {
        enabled = value
        PreferencesManager.saveEqualizerEnabled(value)
        if (value) bind(player) else release()
    }

    fun bind(player: ExoPlayer) {
        if (!enabled) return
        val newSessionId = player.audioSessionId
        if (newSessionId <= 0 || sessionId == newSessionId) return
        releaseEffect()
        try {
            val bound = createEffect(newSessionId)
            effect = bound.effect
            sessionId = bound.sessionId
            publish(bound.state)
        } catch (error: Exception) {
            Log.e("PlaybackEqualizer", "Unable to bind audio effect", error)
            publish(State())
        }
    }

    fun bindTransition(player: ExoPlayer) {
        releaseTransition()
        if (!enabled || player.audioSessionId <= 0) return
        runCatching { createEffect(player.audioSessionId) }
            .onSuccess { transitionEffect = it }
            .onFailure { Log.e("PlaybackEqualizer", "Unable to bind transition effect", it) }
    }

    fun promoteTransition(player: ExoPlayer) {
        val pending = transitionEffect
        transitionEffect = null
        if (pending?.sessionId == player.audioSessionId && enabled) {
            releaseEffect()
            effect = pending.effect
            sessionId = pending.sessionId
            val saved = PreferencesManager.getEqualizerBands()
            val levels = pending.state.levels.indices.map { index ->
                saved?.getOrNull(index)
                    ?.coerceIn(pending.state.levelRange.first, pending.state.levelRange.second)
                    ?: pending.state.levels[index]
            }
            runCatching {
                levels.forEachIndexed { index, level -> pending.effect.setBandLevel(index.toShort(), level) }
            }
            publish(pending.state.copy(levels = levels, preset = state.preset))
        } else {
            runCatching { pending?.effect?.release() }
            bind(player)
        }
    }

    fun releaseTransition() {
        runCatching { transitionEffect?.effect?.release() }
        transitionEffect = null
    }

    private fun createEffect(newSessionId: Int): BoundEffect {
        val newEffect = Equalizer(0, newSessionId)
        try {
            val range = newEffect.bandLevelRange
            val levelRange = range[0] to range[1]
            val savedLevels = PreferencesManager.getEqualizerBands()
            val frequencies = List(newEffect.numberOfBands.toInt()) { index ->
                val bandRange = newEffect.getBandFreqRange(index.toShort())
                (bandRange[0] / 1000) to (bandRange[1] / 1000)
            }
            val levels = List(newEffect.numberOfBands.toInt()) { index ->
                val level = savedLevels?.getOrNull(index)
                    ?.coerceIn(levelRange.first, levelRange.second)
                    ?: newEffect.getBandLevel(index.toShort())
                newEffect.setBandLevel(index.toShort(), level)
                level
            }
            newEffect.enabled = true
            return BoundEffect(newEffect, newSessionId, State(levels, frequencies, levelRange, state.preset))
        } catch (error: Exception) {
            runCatching { newEffect.release() }
            throw error
        }
    }

    fun updateBandLevel(index: Int, level: Short) {
        val currentEffect = effect ?: return
        if (index !in state.levels.indices) return
        val clamped = level.coerceIn(state.levelRange.first, state.levelRange.second)
        runCatching { currentEffect.setBandLevel(index.toShort(), clamped) }.onSuccess {
            runCatching { transitionEffect?.effect?.setBandLevel(index.toShort(), clamped) }
            val levels = state.levels.toMutableList().apply { this[index] = clamped }
            PreferencesManager.saveEqualizerBands(levels)
            publish(state.copy(levels = levels, preset = "Custom"))
        }
    }

    fun resetBands() {
        applyLevels(List(state.levels.size) { 0.toShort() }, "Flat")
    }

    fun applyPreset(preset: String) {
        if (state.levels.isEmpty()) return
        val curve = when (preset) {
            "Bass" -> listOf(0.72f, 0.56f, 0.25f, 0.06f, -0.10f)
            "Vocal" -> listOf(-0.16f, 0.02f, 0.34f, 0.48f, 0.12f)
            "Bright" -> listOf(-0.22f, -0.06f, 0.14f, 0.42f, 0.62f)
            "Night" -> listOf(0.30f, 0.20f, 0.10f, 0.04f, -0.08f)
            else -> List(5) { 0f }
        }
        val range = maxOf(kotlin.math.abs(state.levelRange.first.toInt()), kotlin.math.abs(state.levelRange.second.toInt()))
        val levels = state.levels.indices.map { index ->
            val curveIndex = if (state.levels.size == 1) 2 else {
                (index.toFloat() / state.levels.lastIndex * curve.lastIndex).roundToInt()
            }
            (curve[curveIndex] * range).roundToInt().toShort()
        }
        applyLevels(levels, preset)
    }

    private fun applyLevels(levels: List<Short>, preset: String) {
        val currentEffect = effect ?: return
        val clamped = levels.map { it.coerceIn(state.levelRange.first, state.levelRange.second) }
        runCatching {
            clamped.forEachIndexed { index, level -> currentEffect.setBandLevel(index.toShort(), level) }
        }.onSuccess {
            runCatching {
                clamped.forEachIndexed { index, level ->
                    transitionEffect?.effect?.setBandLevel(index.toShort(), level)
                }
            }
            PreferencesManager.saveEqualizerBands(clamped)
            publish(state.copy(levels = clamped, preset = preset))
        }
    }

    fun release() {
        releaseTransition()
        releaseEffect()
        publish(State())
    }

    private fun releaseEffect() {
        runCatching { effect?.release() }
        effect = null
        sessionId = null
    }

    private fun publish(value: State) {
        state = value
        listeners.forEach { it(value) }
    }
}
