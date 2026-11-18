package com.dssrkh.app.data.tts

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import java.io.File

/** 用 Media3 ExoPlayer 播放本地 WAV，倍速在播放端实现。 */
class VoicePlayer(context: Context) {

    private val player = ExoPlayer.Builder(context).build()

    fun play(file: File, speed: Float = 1.0f) {
        player.setMediaItem(MediaItem.fromUri(Uri.fromFile(file)))
        player.setPlaybackSpeed(speed)
        player.prepare()
        player.play()
    }

    fun stop() {
        player.stop()
        player.clearMediaItems()
    }
}
