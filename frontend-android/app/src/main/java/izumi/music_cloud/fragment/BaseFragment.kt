package izumi.music_cloud.fragment

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import izumi.music_cloud.callback.DownloadCallBack
import izumi.music_cloud.controller.MusicController
import izumi.music_cloud.global.GlobalUtil.getFilePathBySongId
import izumi.music_cloud.toast.ToastMsg
import izumi.music_cloud.viewmodel.SongViewModel
import izumi.music_cloud.viewmodel.SongViewModelFactory

abstract class BaseFragment : Fragment(), View.OnClickListener {
    protected val songViewModel: SongViewModel by lazy {
        ViewModelProvider(
            requireActivity(),
            SongViewModelFactory()
        ).get(SongViewModel::class.java)
    }

    protected val handler = Handler(Looper.getMainLooper())

    private var singleDownloadCallBack: DownloadCallBack? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MusicController.setOnPlayCompleteCallBack { playNext() }
        singleDownloadCallBack = resetSingleDownloadCallBack()
    }

    abstract fun resetSingleDownloadCallBack(): DownloadCallBack

    protected fun startPlay(index: Int) {
        songViewModel.setCurrentIndex(index)
        val songId = songViewModel.getSongByIndex(index)?.id ?: ""

        if (songViewModel.getSongByIndex(index)?.downloaded == true) {
            //song has been downloaded
            MusicController.startPlay(songId.getFilePathBySongId())
            songViewModel.setPlayingStatus(SongViewModel.STATUS_PLAYING)
        } else {
            //song haven't been downloaded
            songViewModel.setPlayingStatus(SongViewModel.STATUS_NOT_INIT)
            songViewModel.setIsDownloading(true)
            songViewModel.download(index, singleDownloadCallBack)
        }
    }

    private fun playNext() {
        // the index ready to play
        val nextIndex = songViewModel.getNextIndex()
        // start to play
        startPlay(nextIndex)
    }

    private fun pausePlay() {
        MusicController.pausePlay()
    }

    protected fun resumePlay() {
        MusicController.resumePlay()
    }

    protected fun onPlayPreviousClick() {
        songViewModel.setToastMsg(ToastMsg.NOT_IMPLEMENT_PLAY_PREVIOUS)
    }

    protected fun onPauseOrStartClick() {
        when (songViewModel.playingStatus.value) {
            SongViewModel.STATUS_PLAYING -> {
                pausePlay()
                songViewModel.setPlayingStatus(SongViewModel.STATUS_PAUSED)
            }
            SongViewModel.STATUS_PAUSED -> {
                resumePlay()
                songViewModel.setPlayingStatus(SongViewModel.STATUS_PLAYING)
            }
            SongViewModel.STATUS_NOT_INIT -> {
                if (songViewModel.isDownloading.value == true) {
                    // not allow to play potentially undownloaded music when downloading
                    songViewModel.setToastMsg(ToastMsg.DOWNLOADING_NOT_ALLOW_CLICK)
                } else {
                    // play any music including undownloaed music when not downloading
                    val currentIndex = songViewModel.currentIndex.value ?: -1
                    if (currentIndex != -1) {
                        startPlay(currentIndex)
                    } else {
                        playNext()
                    }
                }
            }
        }
    }

    protected fun onPlayNextClick() {
        if(songViewModel.isDownloading.value == true) {
            songViewModel.setToastMsg(ToastMsg.DOWNLOADING_NOT_ALLOW_CLICK)
        } else {
            playNext()
        }
    }

    protected fun getDuration() = MusicController.getDuration()

    protected fun getPosition() = MusicController.getPosition()

    protected fun seekTo(milliSec: Int) {
        MusicController.seekTo(milliSec)
    }
}