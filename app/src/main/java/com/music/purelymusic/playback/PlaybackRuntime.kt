// Copyright (c) 2026 eason204646. Licensed under Mulan PSL v2.
package com.music.purelymusic.playback

import android.content.Context
import android.os.SystemClock
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ShuffleOrder.DefaultShuffleOrder
import com.music.purelymusic.utils.PreferencesManager
import java.util.concurrent.CopyOnWriteArraySet
import java.util.IdentityHashMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
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
    private val mixRenderer = AutoMixRenderer(appContext)
    private val renderedPlayers = IdentityHashMap<ExoPlayer, RenderedMix>()
    private var autoMixPlanKey: String? = null
    private var preparedMix: RenderedMix? = null
    private var analysisJob: Job? = null
    private var monitorJob: Job? = null
    private var crossfadeJob: Job? = null
    private var transitionPlayer: ExoPlayer? = null
    private var restoringOriginalItems = false
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
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            crossfadeScheduledForItem = false
            if (transitionPlayer != null) cancelCrossfade()
            restoreOriginalItem()
            resetAutoMixPlan()
        }

        override fun onTimelineChanged(timeline: Timeline, reason: Int) {
            if (reason == Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED && !restoringOriginalItems) {
                cancelCrossfade()
                // Appending/reordering the queue must keep the current audio source playing.
                if (currentPlayer.currentMediaItem?.localConfiguration?.tag !is RenderedMix) restoreOriginalItem()
                resetAutoMixPlan()
            }
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            cancelCrossfade()
            resetAutoMixPlan()
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            cancelCrossfade()
            resetAutoMixPlan()
        }

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int
        ) {
            if (reason == Player.DISCONTINUITY_REASON_SEEK) {
                if (transitionPlayer != null) cancelCrossfade()
                restoreOriginalItem()
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
        mixRenderer.clearStaleFiles()
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

    private fun releasePlayer(player: ExoPlayer) {
        player.release()
        renderedPlayers.remove(player)?.file?.delete()
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
        if (!enabled) restoreOriginalItem()
        resetAutoMixPlan()
    }

    private fun resetAutoMixPlan() {
        analysisJob?.cancel()
        analysisJob = null
        autoMixPlanKey = null
        preparedMix?.file?.delete()
        preparedMix = null
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
        equalizer.releaseTransition()
        if (runningJob == null) transitionPlayer?.let(::releasePlayer)
        transitionPlayer = null
        crossfadeScheduledForItem = false
        if (!released) {
            currentPlayer.volume = 1f
        }
    }

    private fun originalItem(item: MediaItem): MediaItem =
        (item.localConfiguration?.tag as? RenderedMix)?.original ?: item

    private fun restoreOriginalItem() {
        if (restoringOriginalItems) return
        restoringOriginalItems = true
        val currentIndex = currentPlayer.currentMediaItemIndex
        val position = currentPlayer.currentPosition
        val restoreCurrent = currentPlayer.currentMediaItem?.localConfiguration?.tag is RenderedMix
        // Restore queued references too: repeat/shuffle must never revisit a deleted mix file.
        try {
            for (index in 0 until currentPlayer.mediaItemCount) {
                val item = currentPlayer.getMediaItemAt(index)
                val mix = item.localConfiguration?.tag as? RenderedMix
                if (mix != null) currentPlayer.replaceMediaItem(index, mix.original)
            }
            if (restoreCurrent) currentPlayer.seekTo(currentIndex, position)
            renderedPlayers.remove(currentPlayer)?.file?.delete()
        } finally { restoringOriginalItems = false }
    }

    private fun startMonitor() {
        if (monitorJob?.isActive == true) return
        monitorJob = scope.launch {
            while (isActive && !released && currentPlayer.isPlaying) {
                if (autoMixEnabled) prepareAutoMixPlan()
                startCrossfadeIfNeeded()
                delay(if (currentPlayer.duration - currentPlayer.currentPosition < 30_000L) 50L else 300L)
            }
        }
    }

    private fun nextIndex(player: ExoPlayer): Int = if (player.repeatMode == Player.REPEAT_MODE_ONE) {
        player.currentMediaItemIndex
    } else {
        player.nextMediaItemIndex
    }

    private fun copyShuffleOrder(outgoing: ExoPlayer, incoming: ExoPlayer) {
        if (!outgoing.shuffleModeEnabled) return
        val timeline = outgoing.currentTimeline
        val order = mutableListOf<Int>()
        var index = timeline.getFirstWindowIndex(true)
        while (index != C.INDEX_UNSET && order.size < outgoing.mediaItemCount) {
            order += index
            index = timeline.getNextWindowIndex(index, Player.REPEAT_MODE_OFF, true)
        }
        if (order.size == outgoing.mediaItemCount) {
            incoming.setShuffleOrder(DefaultShuffleOrder(order.toIntArray(), SystemClock.elapsedRealtime()))
        }
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
        if (outgoing.mediaItemCount < 2 || outgoing.duration < 20_000L ||
            outgoing.repeatMode == Player.REPEAT_MODE_ONE || outgoing.playbackParameters.speed != 1f ||
            outgoing.playbackParameters.pitch != 1f) return
        val index = nextIndex(outgoing)
        if (index !in 0 until outgoing.mediaItemCount || isContinuousAlbum(outgoing, index)) return
        val current = originalItem(outgoing.currentMediaItem ?: return)
        val next = originalItem(outgoing.getMediaItemAt(index))
        val key = "${current.mediaId}:${current.localConfiguration?.uri}:$index:${next.mediaId}:${next.localConfiguration?.uri}"
        if (autoMixPlanKey == key) return
        resetAutoMixPlan()
        autoMixPlanKey = key
        analysisJob = scope.launch {
            var pending: RenderedMix? = null
            try {
                val outgoingAnalysis = analyzeCached(current)
                val incomingAnalysis = analyzeCached(next) ?: return@launch
                val plan = AutoMixPlanner.plan(outgoing.duration, outgoingAnalysis, incomingAnalysis) ?: return@launch
                withContext(Dispatchers.IO) {
                    val jobContext = coroutineContext
                    pending = mixRenderer.render(current, next, incomingAnalysis, plan) { jobContext.ensureActive() }
                }
                if (autoMixEnabled && currentPlayer === outgoing && autoMixPlanKey == key) {
                    preparedMix = pending
                    pending = null
                }
            } finally { pending?.file?.delete() }
        }
    }

    private suspend fun analyzeCached(item: MediaItem): TrackAnalysis? {
        val uri = item.localConfiguration?.uri ?: return null
        val key = uri.toString()
        if (analysisCache.containsKey(key)) return analysisCache[key]
        val analysis = withContext(Dispatchers.IO) {
            val analysisContext = coroutineContext
            trackAnalyzer.analyze(uri) { analysisContext.ensureActive() }
        }
        analysisCache[key] = analysis
        if (analysisCache.size > 24) analysisCache.remove(analysisCache.keys.first())
        return analysis
    }

    private fun startCrossfadeIfNeeded() {
        val outgoing = currentPlayer
        if ((!autoMixEnabled && !crossfadeEnabled) || crossfadeScheduledForItem || crossfadeJob != null) return
        if (!outgoing.isPlaying || outgoing.mediaItemCount == 0) return
        val index = nextIndex(outgoing)
        if (index !in 0 until outgoing.mediaItemCount) return
        if (autoMixEnabled && (outgoing.repeatMode == Player.REPEAT_MODE_ONE || isContinuousAlbum(outgoing, index))) return
        if (autoMixEnabled && (outgoing.playbackParameters.speed != 1f || outgoing.playbackParameters.pitch != 1f)) return
        val mix = if (autoMixEnabled) preparedMix ?: return else null
        val plan = mix?.plan ?: TransitionPlan(
            outgoing.duration - crossfadeDurationMs, crossfadeDurationMs, 0L, prerollMs = 0L
        )
        val warmupMs = if (mix != null) minOf(500L, plan.incomingStartMs - plan.prerollMs) else 0L
        val lastStart = if (mix != null) plan.startMs - plan.prerollMs - warmupMs - 150L else plan.startMs + plan.fadeMs - 400L
        if (plan.startMs < 1_000L || outgoing.currentPosition !in
            (plan.startMs - 4_000L).coerceAtLeast(0L)..lastStart) return
        crossfadeScheduledForItem = true
        // Ownership moves to the transition player; resetting analysis must not delete its audio.
        if (mix != null) preparedMix = null
        val items = (0 until outgoing.mediaItemCount).map { originalItem(outgoing.getMediaItemAt(it)) }.toMutableList()
        if (mix != null) items[index] = mix.mediaItem()
        val outgoingItem = outgoing.currentMediaItem
        val job = scope.launch(start = CoroutineStart.LAZY) {
            var incoming: ExoPlayer? = null
            var committed = false
            try {
                incoming = createPlayer(manageAudioFocus = false)
                if (mix != null) renderedPlayers[incoming] = mix
                transitionPlayer = incoming
                incoming.setMediaItems(items, index, plan.incomingStartMs - plan.prerollMs - warmupMs)
                incoming.repeatMode = outgoing.repeatMode
                copyShuffleOrder(outgoing, incoming)
                incoming.shuffleModeEnabled = outgoing.shuffleModeEnabled
                if (mix == null) incoming.playbackParameters = outgoing.playbackParameters
                incoming.volume = 0f
                incoming.prepare()
                val ready = withTimeoutOrNull(5_000L) {
                    while (incoming.playbackState != Player.STATE_READY && incoming.playerError == null) delay(10L)
                    incoming.playbackState == Player.STATE_READY
                } == true
                if (!ready || currentPlayer !== outgoing || !outgoing.isPlaying || outgoing.currentMediaItem != outgoingItem) return@launch
                equalizer.bindTransition(incoming)
                if (mix != null) {
                    playRenderedMix(outgoing, incoming, mix)
                } else {
                    while (outgoing.currentPosition < plan.startMs && outgoing.isPlaying) delay(10L)
                    if (!outgoing.isPlaying) return@launch
                    incoming.play()
                    val started = outgoing.currentPosition
                    val duration = (outgoing.duration - started - 100L).coerceAtLeast(1L)
                    while (isActive) {
                        ensureTransitionPlaying(outgoing, incoming)
                        val fraction = ((outgoing.currentPosition - started) / duration.toFloat()).coerceIn(0f, 1f)
                        outgoing.volume = 1 - fraction
                        incoming.volume = fraction
                        if (fraction >= 1f) break
                        delay(10L)
                    }
                }
                transitionPlayer = null
                incoming.volume = 1f
                equalizer.promoteTransition(incoming)
                replacePlayer(incoming)
                committed = true
                outgoing.stop()
                releasePlayer(outgoing)
                incoming.setAudioAttributes(audioAttributes, true)
                incoming.setHandleAudioBecomingNoisy(true)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Log.e("PlaybackRuntime", "Track transition failed", error)
            } finally {
                setAutoMixTransitioning(false)
                if (!committed) {
                    equalizer.releaseTransition()
                    if (transitionPlayer === incoming) transitionPlayer = null
                    incoming?.let(::releasePlayer)
                    if (incoming == null) mix?.file?.delete()
                    if (currentPlayer === outgoing && !released) outgoing.volume = 1f
                }
                if (crossfadeJob === coroutineContext[Job]) crossfadeJob = null
            }
        }
        crossfadeJob = job
        job.start()
    }

    private fun ensureTransitionPlaying(outgoing: ExoPlayer, incoming: ExoPlayer) {
        check(currentPlayer === outgoing && outgoing.isPlaying && incoming.playerError == null &&
            incoming.playWhenReady && incoming.playbackState == Player.STATE_READY) { "Transition playback interrupted" }
    }

    private suspend fun playRenderedMix(outgoing: ExoPlayer, incoming: ExoPlayer, mix: RenderedMix) {
        val plan = mix.plan
        val guardStart = plan.startMs - plan.prerollMs
        val warmupMs = minOf(500L, plan.incomingStartMs - plan.prerollMs)
        val warmupStart = guardStart - warmupMs
        while (outgoing.currentPosition < warmupStart && outgoing.isPlaying) delay(5L)
        check(outgoing.isPlaying && currentPlayer === outgoing)
        check(outgoing.currentPosition - warmupStart < 100L) { "Late AutoMix preroll" }
        incoming.play()
        val ready = withTimeoutOrNull(700L) {
            while (!incoming.isPlaying || incoming.currentPosition <= plan.incomingStartMs - plan.prerollMs - warmupMs + 20L) delay(5L)
            true
        } == true
        check(ready) { "Rendered AutoMix did not start" }
        // AudioTrack's initial media-clock estimate settles after actual audio starts flowing.
        delay(250L)
        // Both players reproduce the SAME outgoing PCM during preroll. Synchronize while muted.
        var corrections = 0
        var seekLeadMs = 35L
        val handoverAt = plan.startMs - mix.handoverBeforeMixMs
        while (outgoing.currentPosition < handoverAt) {
            ensureTransitionPlaying(outgoing, incoming)
            val expected = plan.incomingStartMs + outgoing.currentPosition - plan.startMs
            val error = incoming.currentPosition - expected
            if (kotlin.math.abs(error) > 40L && corrections < 3 && outgoing.currentPosition < handoverAt - 350L) {
                // A seek stalls the muted decoder briefly. Compensate using measured recovery lag.
                incoming.seekTo(expected + seekLeadMs)
                corrections++
                delay(300L)
                val residual = incoming.currentPosition - (plan.incomingStartMs + outgoing.currentPosition - plan.startMs)
                seekLeadMs = (seekLeadMs - residual).coerceIn(0L, 250L)
            } else delay(5L)
        }
        val error = incoming.currentPosition - (plan.incomingStartMs + outgoing.currentPosition - plan.startMs)
        // This quiet bridge is separate from the beat-locked mix, which has one PCM clock.
        val maximumSlipMs = minOf(180L, (plan.beatMs * 0.3).toLong())
        check(kotlin.math.abs(error) <= maximumSlipMs) { "Rendered AutoMix preroll failed to synchronize: $error ms" }
        // A brief handover in a quiet part of the same passage; musical mixing is in one file.
        val handover = incoming.currentPosition
        while (incoming.currentPosition < handover + 60L) {
            ensureTransitionPlaying(outgoing, incoming)
            val t = ((incoming.currentPosition - handover) / 60f).coerceIn(0f, 1f)
            outgoing.volume = 1 - t
            incoming.volume = t
            delay(5L)
        }
        outgoing.volume = 0f
        incoming.volume = 1f
        while (incoming.currentPosition < plan.incomingStartMs) {
            ensureTransitionPlaying(outgoing, incoming)
            delay(5L)
        }
        setAutoMixTransitioning(true)
        // One audible PCM stream and one sample clock throughout the actual mix.
        while (incoming.currentPosition < plan.incomingStartMs + plan.fadeMs) {
            ensureTransitionPlaying(outgoing, incoming)
            delay(10L)
        }
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
        releasePlayer(currentPlayer)
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
