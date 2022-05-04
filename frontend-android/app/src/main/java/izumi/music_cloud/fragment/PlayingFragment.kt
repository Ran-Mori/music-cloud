package izumi.music_cloud.fragment

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import com.facebook.drawee.view.SimpleDraweeView
import izumi.music_cloud.R
import izumi.music_cloud.callback.DownloadCallBack
import izumi.music_cloud.controller.MusicController
import izumi.music_cloud.global.GlobalConst
import izumi.music_cloud.global.GlobalUtil.getCoverUrlBySongId
import izumi.music_cloud.global.GlobalUtil.getFilePathBySongId
import izumi.music_cloud.global.GlobalUtil.milliSecToMinute
import izumi.music_cloud.global.GlobalUtil.milliSecToSecond
import izumi.music_cloud.viewmodel.SongViewModel


class PlayingFragment : BaseFragment() {

    companion object {
        const val TAG = "playing_fragment"
        private const val HANDLER_DELAY_MILLI_SEC: Long = 1000

        @JvmStatic
        fun newInstance() = PlayingFragment()
    }

    private var playingCover: SimpleDraweeView? = null
    private var titleTextView: TextView? = null
    private var artistTextView: TextView? = null
    private var playPrevious: ImageView? = null
    private var startAndPause: ImageView? = null
    private var playNext: ImageView? = null
    private var seekBar: SeekBar? = null
    private var currentTimeText: TextView? = null
    private var endTimeText: TextView? = null

    private val handler = Handler(Looper.getMainLooper())

    //handler task to update every second
    private val updateProgressRunnable: Runnable = object : Runnable {
        override fun run() {
            Log.d(GlobalConst.LOG_TAG, "updateProgressRunnable run, position is ${getPosition()}")

            //same means pause
            if (getPosition() != songViewModel.currentMilliSec.value) {
                songViewModel.setCurrentMilliSec(getPosition())

                //post the same runnable with 1 sec delayed
                handler.postDelayed(this, HANDLER_DELAY_MILLI_SEC)
            }
        }
    }

    override fun resetDownloadCallBack(): DownloadCallBack = object : DownloadCallBack {

        override fun onComplete(index: Int) {
            val songId = songViewModel.getSongByIndex(index)?.id ?: ""
            songViewModel.setCurrentIndex(index)
            songViewModel.setStatus(SongViewModel.STATUS_PLAYING)
            MusicController.startPlay(songId.getFilePathBySongId())
        }

        override fun onError() {
            songViewModel.setStatus(SongViewModel.STATUS_NOT_INIT)
        }
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
            playingCover = findViewById(R.id.playing_cover)
            titleTextView = findViewById(R.id.playing_title)
            artistTextView = findViewById(R.id.playing_artist)
            playPrevious = findViewById(R.id.playing_play_previous)
            startAndPause = findViewById(R.id.playing_play_and_pause)
            playNext = findViewById(R.id.playing_play_next)
            seekBar = findViewById(R.id.playing_seek_bar)
            currentTimeText = findViewById(R.id.playing_current_time)
            endTimeText = findViewById(R.id.playing_end_time)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        initObserver()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initView() {
        playPrevious?.setOnClickListener(this)
        startAndPause?.setOnClickListener(this)
        playNext?.setOnClickListener(this)

        //not allow to touch seekbar when music isn't playing
        seekBar?.setOnTouchListener { _, _ ->
            return@setOnTouchListener songViewModel.status.value != SongViewModel.STATUS_PLAYING
        }

        seekBar?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {}

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                val endMilliSec = songViewModel.endMilliSec.value ?: 0
                val targetMilliSec = ((seekBar?.progress ?: 0) / 100.0 * endMilliSec).toInt()
                seekTo(targetMilliSec)
            }
        })
    }


    private fun initObserver() {
        songViewModel.currentIndex.observe(viewLifecycleOwner) {
            if (it < 0) return@observe
            val song = songViewModel.getSongByIndex(it) ?: return@observe

            song.apply {
                id?.let { id -> playingCover?.setImageURI(Uri.parse(id.getCoverUrlBySongId())) }
                title?.let { title -> titleTextView?.text = title }
                artist?.let { artist -> artistTextView?.text = artist }
            }
            songViewModel.setEndMilliSec(getDuration())
        }

        songViewModel.status.observe(viewLifecycleOwner) {
            if (it == SongViewModel.STATUS_PLAYING) {
                startAndPause?.setImageResource(R.drawable.ic_play_song_black)

                //handler to post a runnable
                handler.postDelayed(updateProgressRunnable, HANDLER_DELAY_MILLI_SEC)
            } else {
                startAndPause?.setImageResource(R.drawable.ic_pause_song_black)

                //handler to remove a runnable
                handler.removeCallbacks(updateProgressRunnable)
            }
        }

        songViewModel.currentMilliSec.observe(viewLifecycleOwner) {
            currentTimeText?.text =
                getString(R.string.duration_time, it.milliSecToMinute(), it.milliSecToSecond())

            seekBar?.progress =
                ((it.toDouble() / (songViewModel.endMilliSec.value ?: Int.MAX_VALUE)) * 100).toInt()
        }

        songViewModel.endMilliSec.observe(viewLifecycleOwner) {
            endTimeText?.text =
                getString(R.string.duration_time, it.milliSecToMinute(), it.milliSecToSecond())
        }
    }

    override fun onClick(view: View?) {
        view ?: return
        when (view.id) {
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