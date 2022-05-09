package izumi.music_cloud.fragment

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import com.facebook.drawee.view.SimpleDraweeView
import izumi.music_cloud.R
import izumi.music_cloud.callback.DownloadCallBack
import izumi.music_cloud.controller.MusicController
import izumi.music_cloud.global.GlobalConst
import izumi.music_cloud.global.GlobalUtil.getCoverUrlBySongId
import izumi.music_cloud.global.GlobalUtil.getFilePathBySongId
import izumi.music_cloud.global.GlobalUtil.milliSecToMinute
import izumi.music_cloud.global.GlobalUtil.milliSecToSecond
import izumi.music_cloud.toast.ToastMsg
import izumi.music_cloud.viewmodel.SongViewModel


class PlayingFragment : BaseFragment() {

    companion object {
        const val TAG = "playing_fragment"
        private const val KEY_MODE_IS_SHUFFLE = "key_mode_is_shuffle"

        @JvmStatic
        fun newInstance() = PlayingFragment()
    }

    private var coverImage: SimpleDraweeView? = null
    private var downloadProgress: TextView? = null
    private var titleTextView: TextView? = null
    private var artistTextView: TextView? = null
    private var playModeIcon: ImageView? = null
    private var playPrevious: ImageView? = null
    private var startAndPause: ImageView? = null
    private var playNext: ImageView? = null
    private var seekBar: SeekBar? = null
    private var currentTimeText: TextView? = null
    private var totalTimeText: TextView? = null

    private val updateProgressRunnable: Runnable = object : Runnable {
        override fun run() {
            //same position means it is paused now.
            if (getPosition() != songViewModel.currentMilliSec.value) {
                songViewModel.setCurrentMilliSec(getPosition())

                //post the same runnable with 1 sec delayed
                handler.postDelayed(this, GlobalConst.HANDLER_POST_DELAY_TIME)
            }
        }
    }

    override fun setSingleDownloadCallBack(): DownloadCallBack = object : DownloadCallBack {
        override fun onStart() {
            songViewModel.setIsDownloading(true)
        }

        override fun onProgress(percent: Int) {
            songViewModel.setLoadingProgress(percent)
        }

        override fun onComplete(index: Int) {
            songViewModel.setIsDownloading(false)

            songViewModel.getSongByIndex(index)?.downloaded = true
            songViewModel.setCurrentIndex(index)

            val songId = songViewModel.getSongByIndex(index)?.id ?: ""
            MusicController.startPlay(songId.getFilePathBySongId())
            songViewModel.setPlayingStatus(SongViewModel.STATUS_PLAYING)
        }

        override fun onError(msg: ToastMsg) {
            songViewModel.setToastMsg(msg)
            songViewModel.setPlayingStatus(SongViewModel.STATUS_NOT_INIT)
            songViewModel.setIsDownloading(false)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sharedPref = activity?.getPreferences(Context.MODE_PRIVATE) ?: return
        val isShuffle = sharedPref.getBoolean(KEY_MODE_IS_SHUFFLE, false)
        songViewModel.setShuffle(isShuffle)
    }

    override fun onResume() {
        super.onResume()
        requireActivity().window.statusBarColor = resources.getColor(R.color.white, null)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_playing, container, false)?.apply {
            coverImage = findViewById(R.id.playing_cover)
            downloadProgress = findViewById(R.id.playing_download_progress)
            titleTextView = findViewById(R.id.playing_title)
            artistTextView = findViewById(R.id.playing_artist)
            playModeIcon = findViewById(R.id.playing_play_mode)
            playPrevious = findViewById(R.id.playing_play_previous)
            startAndPause = findViewById(R.id.playing_play_and_pause)
            playNext = findViewById(R.id.playing_play_next)
            seekBar = findViewById(R.id.playing_seek_bar)
            currentTimeText = findViewById(R.id.playing_current_time)
            totalTimeText = findViewById(R.id.playing_total_time)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        initObserver()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initView() {
        playModeIcon?.setOnClickListener(this)
        playPrevious?.setOnClickListener(this)
        startAndPause?.setOnClickListener(this)
        playNext?.setOnClickListener(this)

        //not allow to touch seekbar when music isn't playing
        seekBar?.setOnTouchListener { _, _ ->
            return@setOnTouchListener songViewModel.playingStatus.value != SongViewModel.STATUS_PLAYING
        }

        seekBar?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {}

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                val totalMilliSec = songViewModel.totalMilliSec.value ?: 0
                val targetMilliSec = ((seekBar?.progress ?: 0) / 100.0 * totalMilliSec).toInt()
                seekTo(targetMilliSec)
            }
        })
    }


    private fun initObserver() {
        songViewModel.currentIndex.observe(viewLifecycleOwner) {
            if (it < 0) return@observe
            val song = songViewModel.getSongByIndex(it) ?: return@observe

            song.apply {
                //deprecated but still use it anyway
                id?.let { id -> coverImage?.setImageURI(Uri.parse(id.getCoverUrlBySongId())) }
                title?.let { title -> titleTextView?.text = title }
                artist?.let { artist -> artistTextView?.text = artist }
            }
            songViewModel.setEndMilliSec(getDuration())
        }

        songViewModel.playingStatus.observe(viewLifecycleOwner) {
            if (it == SongViewModel.STATUS_PLAYING) {
                startAndPause?.setImageResource(R.drawable.ic_play_song_black)

                //handler task to update every second
                //handler to post a runnable
                handler.postDelayed(updateProgressRunnable, GlobalConst.HANDLER_POST_DELAY_TIME)
            } else {
                startAndPause?.setImageResource(R.drawable.ic_pause_song_black)

                //handler to remove a runnable
                handler.removeCallbacks(updateProgressRunnable)
            }
        }

        songViewModel.toastMsg.observe(viewLifecycleOwner) {
            it?.let {
                Toast.makeText(requireContext(), it.msg, Toast.LENGTH_SHORT).show()
                songViewModel.setToastMsg(null)
            }
        }

        songViewModel.shuffle.observe(viewLifecycleOwner) {
            val resId = if (it) R.drawable.ic_shuffle_play else R.drawable.ic_order_play
            playModeIcon?.setImageResource(resId)
        }

        songViewModel.isDownloading.observe(viewLifecycleOwner) {
            // don't set it's visibility to be GONE, as it will change the UI
            downloadProgress?.visibility = if (it) View.VISIBLE else View.INVISIBLE
        }

        songViewModel.loadingProgress.observe(viewLifecycleOwner) { percent ->
            downloadProgress?.text = getString(R.string.download_progress, percent)
        }

        songViewModel.currentMilliSec.observe(viewLifecycleOwner) {
            currentTimeText?.text =
                getString(R.string.duration_time, it.milliSecToMinute(), it.milliSecToSecond())

            seekBar?.progress =
                ((it.toDouble() / (songViewModel.totalMilliSec.value ?: Int.MAX_VALUE)) * 100).toInt()
        }

        songViewModel.totalMilliSec.observe(viewLifecycleOwner) {
            totalTimeText?.text =
                getString(R.string.duration_time, it.milliSecToMinute(), it.milliSecToSecond())
        }
    }

    override fun onClick(view: View?) {
        view ?: return
        when (view.id) {
            R.id.playing_play_mode -> {
                val current = songViewModel.shuffle.value ?: false
                songViewModel.setShuffle(!current)
                val sp = activity?.getPreferences(Context.MODE_PRIVATE) ?: return
                with(sp.edit()) {
                    putBoolean(KEY_MODE_IS_SHUFFLE, !current)
                    apply()
                }
            }
            R.id.playing_play_previous -> {
                onPlayPreviousClick()
            }
            R.id.playing_play_and_pause -> {
                onPauseOrStartClick()
            }
            R.id.playing_play_next -> {
                onPlayNextClick()
            }
        }
    }
}