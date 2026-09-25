// Copyright (c) 2026 eason204646. Licensed under Mulan PSL v2.
package com.music.purelymusic.playback

import android.content.Context
import android.os.SystemClock
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
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
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/** Owns playback that must continue after the Activity and its ViewModel disappear. */
@androidx.annotation.OptIn(UnstableApi::class)
class PlaybackRuntime private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val playerChangeListeners = CopyOnWriteArraySet<(ExoPlayer) -> Unit>()
    private val transitionListeners = CopyOnWriteArraySet<(Boolean) -> Unit>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val equalizer = PlaybackEqualizer()
    private val audioAttributes = AudioAttributes.Builder()
        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
        .setUsage(C.USAGE_MEDIA)
        .build()

    private var crossfadeEnabled: Boolean
    private var crossfadeDurationMs: Long
    private var autoMixEnabled: Boolean
    private val trackAnalyzer = LocalTrackAnalyzer(appContext)
    private val analysisCache = LinkedHashMap<String, TrackAnalysis?>()
    private var autoMixPlanKey: String? = null
    private var autoMixPlan: TransitionPlan? = null
    private var analysisJob: Job? = null
    private var monitorJob: Job? = null
    private var crossfadeJob: Job? = null
    private var speedRecoveryJob: Job? = null
    private var transitionPlayer: ExoPlayer? = null
    @Volatile
    var isAutoMixTransitioning = false
        private set
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
                cancelSpeedRecovery()
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            crossfadeScheduledForItem = false
            if (transitionPlayer != null) cancelCrossfade()
            cancelSpeedRecovery()
            resetAutoMixPlan()
        }

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int
        ) {
            if (reason == Player.DISCONTINUITY_REASON_SEEK) {
                if (transitionPlayer != null) cancelCrossfade()
                cancelSpeedRecovery()
                crossfadeScheduledForItem = false
                resetAutoMixPlan()
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
        autoMixEnabled = PreferencesManager.getAutoMixEnabled()
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

    fun configureAutoMix(enabled: Boolean) {
        if (autoMixEnabled == enabled) return
        autoMixEnabled = enabled
        cancelCrossfade()
        resetAutoMixPlan()
    }

    private fun resetAutoMixPlan() {
        analysisJob?.cancel()
        analysisJob = null
        autoMixPlanKey = null
        autoMixPlan = null
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
        setAutoMixTransitioning(false)
        val runningJob = crossfadeJob
        runningJob?.cancel()
        cancelSpeedRecovery()
        equalizer.releaseTransition()
        if (runningJob == null) transitionPlayer?.release()
        transitionPlayer = null
        crossfadeScheduledForItem = false
        if (!released) currentPlayer.volume = 1f
    }

    private fun cancelSpeedRecovery() {
        speedRecoveryJob?.cancel()
        speedRecoveryJob = null
        if (!released && currentPlayer.playbackParameters.speed != 1f) {
            currentPlayer.playbackParameters = PlaybackParameters(1f)
        }
    }

    private fun recoverPlaybackSpeed(player: ExoPlayer, initialSpeed: Float) {
        if (initialSpeed == 1f) return
        speedRecoveryJob?.cancel()
        speedRecoveryJob = scope.launch {
            val started = SystemClock.elapsedRealtime()
            while (isActive && currentPlayer === player && player.isPlaying) {
                val fraction = ((SystemClock.elapsedRealtime() - started) / 3_000f)
                    .coerceIn(0f, 1f)
                val eased = fraction * fraction * (3f - 2f * fraction)
                player.playbackParameters = PlaybackParameters(
                    initialSpeed + (1f - initialSpeed) * eased,
                    1f
                )
                if (fraction >= 1f) break
                delay(100L)
            }
            if (currentPlayer === player && !released) {
                player.playbackParameters = PlaybackParameters(1f)
            }
            if (speedRecoveryJob === coroutineContext[Job]) speedRecoveryJob = null
        }
    }

    private fun startMonitor() {
        if (monitorJob?.isActive == true) return
        monitorJob = scope.launch {
            while (isActive && !released && currentPlayer.isPlaying) {
                if (autoMixEnabled) prepareAutoMixPlan()
                startCrossfadeIfNeeded()
                delay(200L)
            }
        }
    }

    private fun nextIndex(player: ExoPlayer): Int = if (player.repeatMode == Player.REPEAT_MODE_ONE) {
        player.currentMediaItemIndex
    } else {
        player.nextMediaItemIndex
    }

    private fun isContinuousAlbum(outgoing: ExoPlayer, nextIndex: Int): Boolean {
        if (outgoing.shuffleModeEnabled || nextIndex != outgoing.currentMediaItemIndex + 1) return false
        val current = outgoing.currentMediaItem?.mediaMetadata ?: return false
        val next = outgoing.getMediaItemAt(nextIndex).mediaMetadata
        val currentAlbumId = current.extras?.getString(EXTRA_ALBUM_ID)
        val nextAlbumId = next.extras?.getString(EXTRA_ALBUM_ID)
        if (!currentAlbumId.isNullOrEmpty() && !nextAlbumId.isNullOrEmpty()) {
            return currentAlbumId == nextAlbumId
        }
        val album = current.albumTitle?.toString()?.trim()
        return !album.isNullOrEmpty() && album == next.albumTitle?.toString()?.trim() &&
            current.artist?.toString() == next.artist?.toString()
    }

    private fun prepareAutoMixPlan() {
        val outgoing = currentPlayer
        if (outgoing.mediaItemCount < 2 || outgoing.duration < 8_000L ||
            outgoing.repeatMode == Player.REPEAT_MODE_ONE) return
        val index = nextIndex(outgoing)
        if (index !in 0 until outgoing.mediaItemCount || isContinuousAlbum(outgoing, index)) return
        val current = outgoing.currentMediaItem ?: return
        val next = outgoing.getMediaItemAt(index)
        val key = "${current.mediaId}:$index:${next.mediaId}"
        if (autoMixPlanKey == key) return
        analysisJob?.cancel()
        autoMixPlanKey = key
        autoMixPlan = null
        analysisJob = scope.launch {
            val outgoingAnalysis = analyzeCached(current)
            val incomingAnalysis = analyzeCached(next)
            if (autoMixEnabled && currentPlayer === outgoing && autoMixPlanKey == key) {
                autoMixPlan = AutoMixPlanner.plan(outgoing.duration, outgoingAnalysis, incomingAnalysis)
            }
        }
    }

    private suspend fun analyzeCached(item: MediaItem): TrackAnalysis? {
        val uri = item.localConfiguration?.uri ?: return null
        val key = uri.toString()
        if (analysisCache.containsKey(key)) return analysisCache[key]
        val analysis = withContext(Dispatchers.IO) { trackAnalyzer.analyze(uri) }
        analysisCache[key] = analysis
        if (analysisCache.size > 24) analysisCache.remove(analysisCache.keys.first())
        return analysis
    }

    private fun startCrossfadeIfNeeded() {
        val outgoing = currentPlayer
        if ((!autoMixEnabled && !crossfadeEnabled) || crossfadeScheduledForItem || crossfadeJob != null) return
        if (!outgoing.isPlaying || outgoing.mediaItemCount == 0) return
        val nextIndex = nextIndex(outgoing)
        if (nextIndex !in 0 until outgoing.mediaItemCount) return
        if (autoMixEnabled && (outgoing.repeatMode == Player.REPEAT_MODE_ONE ||
                    isContinuousAlbum(outgoing, nextIndex))) return
        val remaining = outgoing.duration - outgoing.currentPosition
        val plan = if (autoMixEnabled) {
            autoMixPlan ?: if (remaining <= 13_000L) {
                AutoMixPlanner.plan(outgoing.duration, null, null)
            } else null
        } else {
            if (outgoing.duration <= crossfadeDurationMs) null else TransitionPlan(
                outgoing.duration - crossfadeDurationMs,
                crossfadeDurationMs,
                0L,
                1f
            )
        }
        val prefetchMs = if (autoMixEnabled) 3_000L else 2_000L
        if (plan == null || outgoing.currentPosition !in
            (plan.startMs - prefetchMs).coerceAtLeast(0L)..(plan.startMs + plan.fadeMs - 400L)) return

        crossfadeScheduledForItem = true
        val items = (0 until outgoing.mediaItemCount).map(outgoing::getMediaItemAt)
        val outgoingItem = outgoing.currentMediaItem
        val useAutoMixEnvelope = autoMixEnabled
        val job = scope.launch(start = CoroutineStart.LAZY) {
            var incoming: ExoPlayer? = null
            var committed = false
            try {
                incoming = createPlayer(manageAudioFocus = false)
                transitionPlayer = incoming
                incoming.setMediaItems(items, nextIndex, plan.incomingStartMs)
                incoming.repeatMode = outgoing.repeatMode
                incoming.shuffleModeEnabled = outgoing.shuffleModeEnabled
                if (plan.incomingSpeed != 1f) {
                    incoming.playbackParameters = PlaybackParameters(plan.incomingSpeed, 1f)
                }
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

                while (outgoing.currentPosition < plan.startMs && outgoing.isPlaying &&
                    currentPlayer === outgoing) delay(25L)
                if (!outgoing.isPlaying || currentPlayer !== outgoing) return@launch
                val fadeMs = minOf(
                    plan.startMs + plan.fadeMs - outgoing.currentPosition,
                    outgoing.duration - outgoing.currentPosition - 150L
                )
                if (fadeMs < if (useAutoMixEnvelope) AutoMixPlanner.MIN_BLEND_MS else 400L) return@launch
                val spectralMix = autoMixEnabled
                equalizer.bindTransition(incoming, forAutoMix = spectralMix)
                if (spectralMix) equalizer.beginAutoMixTransition(outgoing, plan.style)
                incoming.play()
                if (useAutoMixEnvelope) setAutoMixTransitioning(true)
                val fadeStarted = SystemClock.elapsedRealtime()
                var lastSculptAt = fadeStarted - 100L
                while (isActive) {
                    val elapsed = SystemClock.elapsedRealtime() - fadeStarted
                    if (incoming.playerError != null ||
                        (elapsed > 750L && !incoming.isPlaying) ||
                        !outgoing.isPlaying || currentPlayer !== outgoing) {
                        throw IllegalStateException("Transition playback interrupted")
                    }
                    val fraction = (elapsed.toFloat() / fadeMs)
                        .coerceIn(0f, 1f)
                    if (useAutoMixEnvelope) {
                        val gains = TransitionEnvelope.gains(plan.style, fraction)
                        outgoing.volume = gains.outgoing
                        incoming.volume = gains.incoming
                    } else {
                        outgoing.volume = 1f - fraction
                        incoming.volume = fraction
                    }
                    if (spectralMix && (elapsed - lastSculptAt >= 100L || fraction >= 1f)) {
                        equalizer.shapeAutoMixTransition(fraction)
                        lastSculptAt = elapsed
                    }
                    if (fraction >= 1f) break
                    delay(25L)
                }

                // Give media controls the new player before stopping the old one.
                transitionPlayer = null
                equalizer.promoteTransition(incoming)
                replacePlayer(incoming)
                committed = true
                outgoing.stop()
                outgoing.release()
                incoming.setAudioAttributes(audioAttributes, true)
                incoming.setHandleAudioBecomingNoisy(true)
                recoverPlaybackSpeed(incoming, plan.incomingSpeed)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Log.e("PlaybackRuntime", "Track transition failed", error)
            } finally {
                setAutoMixTransitioning(false)
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
        resetAutoMixPlan()
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

    fun addTransitionListener(listener: (Boolean) -> Unit) {
        transitionListeners += listener
        listener(isAutoMixTransitioning)
    }

    fun removeTransitionListener(listener: (Boolean) -> Unit) {
        transitionListeners -= listener
    }

    private fun setAutoMixTransitioning(value: Boolean) {
        if (isAutoMixTransitioning == value) return
        isAutoMixTransitioning = value
        transitionListeners.forEach { listener ->
            runCatching { listener(value) }
                .onFailure { Log.e("PlaybackRuntime", "Transition observer failed", it) }
        }
    }

    @Synchronized
    fun release() {
        if (released) return
        cancelCrossfade()
        resetAutoMixPlan()
        analysisCache.clear()
        cancelSleepTimer()
        monitorJob?.cancel()
        monitorJob = null
        released = true
        equalizer.release()
        currentPlayer.removeListener(playerListener)
        currentPlayer.release()
    }

    companion object {
        const val EXTRA_ALBUM_ID = "com.music.purelymusic.album_id"

        @Volatile
        private var instance: PlaybackRuntime? = null

        fun get(context: Context): PlaybackRuntime = synchronized(this) {
            instance ?: PlaybackRuntime(context).also { instance = it }
        }
    }
}
