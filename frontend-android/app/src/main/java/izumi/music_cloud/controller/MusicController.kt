package izumi.music_cloud.controller

import android.media.MediaPlayer
import android.util.Log
import izumi.music_cloud.global.GlobalConst
import java.io.IOException

object MusicController {

    private var onPlayCompleteCallBack: (() -> Unit)? = null

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
                onPlayCompleteCallBack?.invoke()
            }
        }
    }

    fun startPlay(filePath: String?) {
        filePath ?: return
        player.reset()
        try {
            player.setDataSource(filePath)
            player.prepareAsync()
        } catch (e: IOException) {
            Log.d(GlobalConst.LOG_TAG, "setDatasource catch an IOException")
        } catch (e: IllegalArgumentException) {
            Log.d(GlobalConst.LOG_TAG, "setDatasource catch an IllegalArgumentException")
        }

    }

    fun playPrevious() {

    }

    fun pausePlay() {
        if (player.isPlaying) player.pause()
    }

    fun setOnPlayCompleteCallBack(callback: () -> Unit) {
        onPlayCompleteCallBack = callback
    }

    fun resumePlay() {
        player.start()
    }
}