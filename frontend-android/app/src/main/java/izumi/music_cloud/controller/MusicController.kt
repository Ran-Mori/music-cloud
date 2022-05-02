package izumi.music_cloud.controller

import android.media.MediaPlayer
import android.util.Log
import izumi.music_cloud.global.GlobalConst

object MusicController {

    private var readyToPlayPath: String? = null
    private var onPlayCompleteCallBack: (() -> String)? = null

    private val player by lazy {
        MediaPlayer().apply {
            setOnPreparedListener {
                Log.d(GlobalConst.LOG_TAG, "prepared over")
                it.start()
                Log.d(GlobalConst.LOG_TAG, "start to play")
            }

            setOnErrorListener { mp, what, extra ->
                return@setOnErrorListener true
            }

            setOnCompletionListener {
                val path = onPlayCompleteCallBack?.invoke()
                startPlay(path)
            }
        }
    }

    fun setReadyToPlayPath(filePath: String) {
        readyToPlayPath = filePath
    }

    fun startPlay(filePath: String?) {
        filePath ?: return
        player.reset()
        player.setDataSource(filePath)
        player.prepareAsync()
    }

    fun playPrevious() {

    }

    fun pausePlay() {
        if (player.isPlaying) player.pause()
    }

    fun setOnPlayCompleteCallBack(callback: () -> String) {
        onPlayCompleteCallBack = callback
    }

    fun resumePlay() {
        player.start()
    }
}