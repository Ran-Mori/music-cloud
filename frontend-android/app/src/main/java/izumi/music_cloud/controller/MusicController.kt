package izumi.music_cloud.controller

import android.media.MediaPlayer
import android.util.Log
import izumi.music_cloud.global.GlobalConst
import java.io.IOException

object MusicController {

    //in fact this call back is to 'play next song'
    private var playNextSongFunc: (() -> Unit)? = null

    private val player by lazy {
        MediaPlayer().apply {
            setOnPreparedListener {
                Log.d(GlobalConst.LOG_TAG, "prepared over")
                it.start()
                Log.d(GlobalConst.LOG_TAG, "start to play")
            }

            setOnErrorListener { mp, what, extra ->
                Log.d(
                    GlobalConst.LOG_TAG,
                    "MediaPlayer onError. mp is '$mp', what is '$what', extra is '$extra'"
                )
                return@setOnErrorListener true
            }

            setOnCompletionListener {
                playNextSongFunc?.invoke()
            }
        }
    }

    fun startPlay(filePath: String?) {
        filePath ?: return
        try {
            player.apply {
                reset()
                setDataSource(filePath)
                prepareAsync()
            }
        } catch (e: IOException) {
            Log.d(GlobalConst.LOG_TAG, "setDatasource catch an IOException")
        } catch (e: IllegalArgumentException) {
            Log.d(GlobalConst.LOG_TAG, "setDatasource catch an IllegalArgumentException")
        }

    }

    fun getDuration() = player.duration

    fun getPosition() = player.currentPosition

    fun seekTo(milliSec: Int) {
        player.seekTo(milliSec)
    }

    fun pausePlay() {
        if (player.isPlaying) player.pause()
    }

    fun setPlayNextFunc(func: () -> Unit) {
        playNextSongFunc = func
    }

    fun resumePlay() {
        player.start()
    }
}