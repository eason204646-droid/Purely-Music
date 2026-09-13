// Copyright (c) 2026 eason204646. Licensed under Mulan PSL v2.
package com.music.purelymusic.playback

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import java.util.concurrent.CopyOnWriteArraySet

/**
 * 进程内唯一的主播放器。
 *
 * 播放器的生命周期由 [PlaybackService] 管理，界面层只注册监听和发出控制命令。
 * 交叉淡化完成时允许原子替换播放器，并同步更新 MediaSession。
 */
@androidx.annotation.OptIn(UnstableApi::class)
class PlaybackRuntime private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val playerChangeListeners = CopyOnWriteArraySet<(ExoPlayer) -> Unit>()

    @Volatile
    private var currentPlayer: ExoPlayer = createPlayer()

    val player: ExoPlayer
        @Synchronized get() {
            if (released) {
                currentPlayer = createPlayer()
                released = false
                playerChangeListeners.forEach { it(currentPlayer) }
            }
            return currentPlayer
        }

    @Volatile
    private var released = false

    fun createPlayer(): ExoPlayer {
        val renderersFactory = DefaultRenderersFactory(appContext)
            .setEnableDecoderFallback(true)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)

        return ExoPlayer.Builder(appContext, renderersFactory).build().apply {
            val attributes = AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .setUsage(C.USAGE_MEDIA)
                .build()
            setAudioAttributes(attributes, true)
            setHandleAudioBecomingNoisy(true)
            volume = 1f
        }
    }

    @Synchronized
    fun replacePlayer(newPlayer: ExoPlayer) {
        currentPlayer = newPlayer
        released = false
        playerChangeListeners.forEach { it(newPlayer) }
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
        released = true
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
