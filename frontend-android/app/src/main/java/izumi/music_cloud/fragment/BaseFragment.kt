package izumi.music_cloud.fragment

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import izumi.music_cloud.R
import izumi.music_cloud.callback.DownloadCallBack
import izumi.music_cloud.controller.MusicController
import izumi.music_cloud.global.GlobalUtil.getFilePathBySongId
import izumi.music_cloud.viewmodel.SongViewModel
import izumi.music_cloud.viewmodel.SongViewModelFactory

abstract class BaseFragment : Fragment(), View.OnClickListener {
    protected val songViewModel: SongViewModel by lazy {
        ViewModelProvider(
            requireActivity(),
            SongViewModelFactory()
        ).get(SongViewModel::class.java)
    }

    private var downloadCallBack: DownloadCallBack? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MusicController.setOnPlayCompleteCallBack { playNext() }
        downloadCallBack = resetDownloadCallBack()
    }

    abstract fun resetDownloadCallBack(): DownloadCallBack

    protected fun startPlay(index: Int) {
        songViewModel.setCurrentIndex(index)
        val songId = songViewModel.getSongByIndex(index)?.id ?: ""

        if (songViewModel.getSongByIndex(index)?.downloaded == true) {
            //song has been downloaded
            MusicController.startPlay(songId.getFilePathBySongId())
            songViewModel.setStatus(SongViewModel.STATUS_PLAYING)
        } else {
            //song haven't been downloaded
            songViewModel.setStatus(SongViewModel.STATUS_DOWNLOADING)
            songViewModel.download(index, downloadCallBack)
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
        Toast.makeText(
            requireContext(),
            getString(R.string.not_support_play_next_toast),
            Toast.LENGTH_LONG
        ).show()
    }

    protected fun onPauseOrStartClick() {
        when (songViewModel.status.value) {
            SongViewModel.STATUS_DOWNLOADING -> {
                showToastWhenDownloading()
            }
            SongViewModel.STATUS_NOT_INIT -> {
                val currentIndex = songViewModel.currentIndex.value ?: -1
                if (currentIndex != -1) {
                    startPlay(currentIndex)
                } else {
                    playNext()
                }
            }
            SongViewModel.STATUS_PLAYING -> {
                pausePlay()
                songViewModel.setStatus(SongViewModel.STATUS_PAUSED)
            }
            SongViewModel.STATUS_PAUSED -> {
                resumePlay()
                songViewModel.setStatus(SongViewModel.STATUS_PLAYING)
            }
        }
    }

    protected fun onPlayNextClick() {
        when (songViewModel.status.value) {
            SongViewModel.STATUS_DOWNLOADING -> {
                showToastWhenDownloading()
            }
            else -> playNext()
        }
    }

    protected fun showToastWhenDownloading() {
        Toast.makeText(
            requireContext(),
            getString(R.string.downloading_user_toast),
            Toast.LENGTH_SHORT
        ).show()
    }

    protected fun getDuration() = MusicController.getDuration()

    protected fun getPosition() = MusicController.getPosition()

    protected fun seekTo(milliSec: Int) {
        MusicController.seekTo(milliSec)
    }
}