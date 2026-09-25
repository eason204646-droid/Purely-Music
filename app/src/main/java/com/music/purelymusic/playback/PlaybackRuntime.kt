// Copyright (c) 2026 eason204646. Licensed under Mulan PSL v2.
package com.music.purelymusic.playback

import android.content.Context
import android.os.SystemClock
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import com.music.purelymusic.utils.PreferencesManager
import java.util.concurrent.CopyOnWriteArraySet
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/** Owns playback that must continue after the Activity and its ViewModel disappear. */
@androidx.annotation.OptIn(UnstableApi::class)
class PlaybackRuntime private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val playerChangeListeners = CopyOnWriteArraySet<(ExoPlayer) -> Unit>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val equalizer = PlaybackEqualizer()
    private val audioAttributes = AudioAttributes.Builder()
        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
        .setUsage(C.USAGE_MEDIA)
        .build()

    private var crossfadeEnabled: Boolean
    private var crossfadeDurationMs: Long
    private var monitorJob: Job? = null
    private var crossfadeJob: Job? = null
    private var transitionPlayer: ExoPlayer? = null
    private var crossfadeScheduledForItem = false
    private var sleepTimerJob: Job? = null
    private var sleepTimerDeadlineMs: Long = 0L
    var sleepTimerDurationMinutes: Int = 0
        private set

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_READY) equalizer.bind(currentPlayer)
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) {
                startMonitor()
            } else {
                monitorJob?.cancel()
                monitorJob = null
                if (transitionPlayer != null) cancelCrossfade()
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            crossfadeScheduledForItem = false
            if (transitionPlayer != null) cancelCrossfade()
        }

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int
        ) {
            if (reason == Player.DISCONTINUITY_REASON_SEEK) {
                if (transitionPlayer != null) cancelCrossfade()
                crossfadeScheduledForItem = false
            }
        }
    }

    @Volatile
    private var currentPlayer: ExoPlayer

    @Volatile
    private var released = false

    init {
        PreferencesManager.init(appContext)
        crossfadeEnabled = PreferencesManager.getCrossfadeEnabled()
        crossfadeDurationMs = PreferencesManager.getCrossfadeDurationSeconds().coerceIn(1, 10) * 1000L
        currentPlayer = createPlayer().also { it.addListener(playerListener) }
        equalizer.setEnabled(PreferencesManager.getEqualizerEnabled(), currentPlayer)
    }

    val player: ExoPlayer
        @Synchronized get() {
            if (released) {
                currentPlayer = createPlayer().also { it.addListener(playerListener) }
                released = false
                notifyPlayerChanged(currentPlayer)
            }
            return currentPlayer
        }

    private fun createPlayer(manageAudioFocus: Boolean = true): ExoPlayer {
        val renderersFactory = DefaultRenderersFactory(appContext)
            .setEnableDecoderFallback(true)
            // Use the platform decoder when possible; keep FFmpeg as fallback.
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)

        return ExoPlayer.Builder(appContext, renderersFactory).build().apply {
            setAudioAttributes(audioAttributes, manageAudioFocus)
            setHandleAudioBecomingNoisy(manageAudioFocus)
            volume = 1f
        }
    }

    fun configureCrossfade(enabled: Boolean, durationSeconds: Int) {
        crossfadeEnabled = enabled
        crossfadeDurationMs = durationSeconds.coerceIn(1, 10) * 1000L
        if (!enabled) cancelCrossfade()
    }

    fun startSleepTimer(minutes: Int) {
        cancelSleepTimer()
        if (minutes <= 0) return
        sleepTimerDurationMinutes = minutes
        sleepTimerDeadlineMs = SystemClock.elapsedRealtime() + minutes * 60_000L
        sleepTimerJob = scope.launch {
            while (isActive && sleepTimerRemainingSeconds > 0) delay(1_000L)
            if (isActive) {
                sleepTimerDeadlineMs = 0L
                sleepTimerDurationMinutes = 0
                cancelCrossfade()
                if (!released) currentPlayer.pause()
            }
        }
    }

    val sleepTimerRemainingSeconds: Int
        get() = if (sleepTimerDeadlineMs == 0L) 0 else {
            ((sleepTimerDeadlineMs - SystemClock.elapsedRealtime() + 999L) / 1_000L)
                .coerceAtLeast(0L).toInt()
        }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        sleepTimerDeadlineMs = 0L
        sleepTimerDurationMinutes = 0
    }

    fun cancelCrossfade() {
        val runningJob = crossfadeJob
        runningJob?.cancel()
        equalizer.releaseTransition()
        if (runningJob == null) transitionPlayer?.release()
        transitionPlayer = null
        crossfadeScheduledForItem = false
        if (!released) currentPlayer.volume = 1f
    }

    private fun startMonitor() {
        if (monitorJob?.isActive == true) return
        monitorJob = scope.launch {
            while (isActive && !released && currentPlayer.isPlaying) {
                startCrossfadeIfNeeded()
                delay(200L)
            }
        }
    }

    private fun startCrossfadeIfNeeded() {
        val outgoing = currentPlayer
        if (!crossfadeEnabled || crossfadeScheduledForItem || crossfadeJob != null) return
        if (!outgoing.isPlaying || outgoing.mediaItemCount == 0) return
        val remaining = outgoing.duration - outgoing.currentPosition
        if (outgoing.duration <= crossfadeDurationMs || remaining !in 400L..(crossfadeDurationMs + 500L)) return

        val nextIndex = if (outgoing.repeatMode == Player.REPEAT_MODE_ONE) {
            outgoing.currentMediaItemIndex
        } else {
            outgoing.nextMediaItemIndex
        }
        if (nextIndex !in 0 until outgoing.mediaItemCount) return

        crossfadeScheduledForItem = true
        val items = (0 until outgoing.mediaItemCount).map(outgoing::getMediaItemAt)
        val outgoingItem = outgoing.currentMediaItem
        val job = scope.launch(start = CoroutineStart.LAZY) {
            var incoming: ExoPlayer? = null
            var committed = false
            try {
                incoming = createPlayer(manageAudioFocus = false)
                transitionPlayer = incoming
                incoming.setMediaItems(items, nextIndex, 0L)
                incoming.repeatMode = outgoing.repeatMode
                incoming.shuffleModeEnabled = outgoing.shuffleModeEnabled
                incoming.volume = 0f
                incoming.prepare()

                val ready = withTimeoutOrNull(5_000L) {
                    while (incoming.playbackState != Player.STATE_READY && incoming.playerError == null) {
                        delay(25L)
                    }
                    incoming.playbackState == Player.STATE_READY
                } == true
                if (!ready || currentPlayer !== outgoing || !outgoing.isPlaying ||
                    outgoing.currentMediaItem != outgoingItem) return@launch

                val fadeMs = minOf(crossfadeDurationMs, outgoing.duration - outgoing.currentPosition - 250L)
                if (fadeMs < 300L) return@launch
                equalizer.bindTransition(incoming)
                incoming.play()
                val steps = 30
                repeat(steps) { step ->
                    val fraction = (step + 1) / steps.toFloat()
                    outgoing.volume = 1f - fraction
                    incoming.volume = fraction
                    delay(fadeMs / steps)
                }

                // Give media controls the new player before stopping the old one.
                transitionPlayer = null
                incoming.setAudioAttributes(audioAttributes, true)
                incoming.setHandleAudioBecomingNoisy(true)
                equalizer.promoteTransition(incoming)
                replacePlayer(incoming)
                committed = true
                outgoing.stop()
                outgoing.release()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Log.e("PlaybackRuntime", "Crossfade failed", error)
            } finally {
                if (!committed) {
                    equalizer.releaseTransition()
                    if (transitionPlayer === incoming) transitionPlayer = null
                    incoming?.release()
                    if (currentPlayer === outgoing && !released) outgoing.volume = 1f
                }
                if (crossfadeJob === coroutineContext[Job]) crossfadeJob = null
            }
        }
        crossfadeJob = job
        job.start()
    }

    @Synchronized
    private fun replacePlayer(newPlayer: ExoPlayer) {
        currentPlayer.removeListener(playerListener)
        currentPlayer = newPlayer
        newPlayer.addListener(playerListener)
        equalizer.bind(newPlayer)
        crossfadeScheduledForItem = false
        released = false
        notifyPlayerChanged(newPlayer)
        if (newPlayer.isPlaying) startMonitor()
    }

    private fun notifyPlayerChanged(player: ExoPlayer) {
        playerChangeListeners.forEach { listener ->
            runCatching { listener(player) }
                .onFailure { Log.e("PlaybackRuntime", "Player observer failed", it) }
        }
    }

    fun addPlayerChangeListener(listener: (ExoPlayer) -> Unit) {
        playerChangeListeners += listener
    }

    fun removePlayerChangeListener(listener: (ExoPlayer) -> Unit) {
        playerChangeListeners -= listener
    }

    @Synchronized
    fun release() {
        if (released) return
        cancelCrossfade()
        cancelSleepTimer()
        monitorJob?.cancel()
        monitorJob = null
        released = true
        equalizer.release()
        currentPlayer.removeListener(playerListener)
        currentPlayer.release()
    }

    companion object {
        @Volatile
        private var instance: PlaybackRuntime? = null

        fun get(context: Context): PlaybackRuntime = synchronized(this) {
            instance ?: PlaybackRuntime(context).also { instance = it }
        }
    }
}
