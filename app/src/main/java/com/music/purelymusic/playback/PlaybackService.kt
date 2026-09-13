// Copyright (c) 2026 eason204646. Licensed under Mulan PSL v2.
package com.music.purelymusic.playback

import android.content.Intent
import androidx.media3.common.Player
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * 后台播放与系统媒体控制入口。
 * Media3 会根据会话状态创建前台媒体通知，并处理耳机、锁屏和系统面板控制。
 */
class PlaybackService : MediaSessionService() {
    private lateinit var runtime: PlaybackRuntime
    private var mediaSession: MediaSession? = null

    private val playerChangeListener: (androidx.media3.exoplayer.ExoPlayer) -> Unit = { player ->
        mediaSession?.setPlayer(player)
    }

    override fun onCreate() {
        super.onCreate()
        runtime = PlaybackRuntime.get(this)
        mediaSession = MediaSession.Builder(this, runtime.player).build()
        runtime.addPlayerChangeListener(playerChangeListener)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        runtime.removePlayerChangeListener(playerChangeListener)
        mediaSession?.release()
        mediaSession = null
        runtime.release()
        super.onDestroy()
    }
}
