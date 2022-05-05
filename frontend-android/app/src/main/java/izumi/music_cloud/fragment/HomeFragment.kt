package izumi.music_cloud.fragment

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.facebook.drawee.view.SimpleDraweeView
import izumi.music_cloud.R
import izumi.music_cloud.callback.DownloadCallBack
import izumi.music_cloud.callback.ViewHolderCallback
import izumi.music_cloud.controller.MusicController
import izumi.music_cloud.global.GlobalUtil.getCoverUrlBySongId
import izumi.music_cloud.global.GlobalUtil.getFilePathBySongId
import izumi.music_cloud.recycler.SongAdapter
import izumi.music_cloud.viewmodel.SongViewModel

class HomeFragment : BaseFragment() {

    companion object {
        const val TAG = "home_fragment"

        @JvmStatic
        fun newInstance() = HomeFragment()
    }

    private var mainCover: SimpleDraweeView? = null
    private var playAllIcon: ImageView? = null
    private var playAllTextView: TextView? = null
    private var downloadAllIcon: ImageView? = null
    private var songRecyclerView: RecyclerView? = null
    private var bottomMiniPlayer: View? = null
    private var downloadProgress: TextView? = null
    private var bmpCover: SimpleDraweeView? = null
    private var bmpTitle: TextView? = null
    private var bmpStartAndPause: ImageView? = null
    private var bmpPlayNext: ImageView? = null

    private var songAdapter: SongAdapter? = null

    private val viewHolderCallBack = object : ViewHolderCallback {
        override fun onSingleClick(index: Int) {
            onItemClicked(index)
        }

        override fun onLongClick() {
            switchPlayingFragment()
        }

        override fun onMenuCLick() {

        }
    }

    override fun onResume() {
        super.onResume()
        requireActivity().window.statusBarColor =
            resources.getColor(R.color.second_background, null)

        // music is downloaded in PlayingFragment and then come back
        val index = songViewModel.currentIndex.value ?: -1
        if (index != -1) {
            songAdapter?.notifyItemChanged(index)
        }
    }

    override fun resetSingleDownloadCallBack(): DownloadCallBack = object : DownloadCallBack {

        //do something when finish downloading
        override fun onComplete(index: Int) {
            val songId = songViewModel.getSongByIndex(index)?.id ?: ""
            songAdapter?.notifyItemChanged(index)
            songViewModel.setCurrentIndex(index)
            songViewModel.setPlayingStatus(SongViewModel.STATUS_PLAYING)
            songViewModel.setIsDownloading(false)
            MusicController.startPlay(songId.getFilePathBySongId())
        }

        override fun onError() {
            songViewModel.setPlayingStatus(SongViewModel.STATUS_NOT_INIT)
            songViewModel.setIsDownloading(false)
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false).apply {
            mainCover = findViewById(R.id.main_cover)
            downloadAllIcon = findViewById(R.id.main_download_all)
            playAllIcon = findViewById(R.id.main_ic_play_all)
            playAllTextView = findViewById(R.id.main_text_play_all)
            songRecyclerView = findViewById(R.id.main_playlist)
            bottomMiniPlayer = findViewById(R.id.main_bottom_mini_player)
            downloadProgress = findViewById(R.id.main_download_progress)
            bmpCover = findViewById(R.id.bmp_over)
            bmpTitle = findViewById(R.id.bmp_title)
            bmpStartAndPause = findViewById(R.id.bmp_start_or_pause)
            bmpPlayNext = findViewById(R.id.bmp_play_next)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        initObserver()
    }

    private fun initView() {
        songRecyclerView?.layoutManager = LinearLayoutManager(requireContext())

        songAdapter = SongAdapter(viewHolderCallBack).apply {
            songRecyclerView?.adapter = this
        }

        playAllIcon?.setOnClickListener(this)
        playAllTextView?.setOnClickListener(this)
        downloadAllIcon?.setOnClickListener(this)
        bottomMiniPlayer?.setOnClickListener(this)
        bmpStartAndPause?.setOnClickListener(this)
        bmpPlayNext?.setOnClickListener(this)
    }

    private fun initObserver() {
        songViewModel.songList.observe(viewLifecycleOwner) {
            songAdapter?.submitList(it)
        }

        songViewModel.currentIndex.observe(viewLifecycleOwner) {
            if (it < 0) return@observe
            val song = songViewModel.getSongByIndex(it) ?: return@observe

            song.title?.let { title -> bmpTitle?.text = title }
            song.id?.let { id ->
                mainCover?.setImageURI(Uri.parse(id.getCoverUrlBySongId()))
                bmpCover?.setImageURI(Uri.parse(id.getCoverUrlBySongId()))
            }
        }

        songViewModel.error.observe(viewLifecycleOwner) {
            it?.let { Toast.makeText(requireContext(), it.msg, Toast.LENGTH_LONG).show() }
        }

        songViewModel.shuffle.observe(viewLifecycleOwner) {

        }

        songViewModel.playingStatus.observe(viewLifecycleOwner) {
            if (it == SongViewModel.STATUS_PLAYING) {
                bmpStartAndPause?.setImageResource(R.drawable.ic_play_song_black)
            } else {
                bmpStartAndPause?.setImageResource(R.drawable.ic_pause_song_black)
            }
        }

        songViewModel.isDownloading.observe(viewLifecycleOwner) {
            downloadProgress?.visibility = if (it) View.VISIBLE else View.GONE
        }

        songViewModel.downloadProgress.observe(viewLifecycleOwner) { percent ->
            downloadProgress?.text = getString(R.string.download_progress, percent)
        }
    }

    //when click a song view holder
    private fun onItemClicked(index: Int) {

        if (index == songViewModel.currentIndex.value) {
            //the clicked item equals to current play item
            when (songViewModel.playingStatus.value) {
                //pause
                SongViewModel.STATUS_PAUSED -> {
                    resumePlay()
                }
                //not pause
                SongViewModel.STATUS_PLAYING -> {
                    switchPlayingFragment()
                }
                // status will not be 'STATUS_NOT_INIT' when click a real ViewHolder
            }
        } else if (songViewModel.isDownloading.value == false || songViewModel.getSongByIndex(index)?.downloaded == true) {
            //play a new downloaded song
            startPlay(index)
        } else {
            showToastWhenDownloading()
        }
    }

    private fun onDownloadAllClick() {
        if (songViewModel.isDownloading.value == true) return

        val songList = songViewModel.songList.value?.toList() ?: return
        if (songList.isEmpty()) return

        val size = songList.size
        var downloadingIndex = 0
        val runnable = object : Runnable {
            override fun run() {
                // that = this runnable
                val that = this

                songViewModel.download(downloadingIndex, object : DownloadCallBack {
                    override fun onComplete(index: Int) {
                        songAdapter?.notifyItemChanged(index)
                        songViewModel.setIsDownloading(false)

                        while (++downloadingIndex != size && !songList[downloadingIndex].downloaded) {
                            handler.post(that)
                            break
                        }
                    }

                    override fun onError() {
                        songViewModel.setPlayingStatus(SongViewModel.STATUS_NOT_INIT)
                        songViewModel.setIsDownloading(false)
                    }
                })
            }
        }
        handler.post(runnable)
    }

    private fun switchPlayingFragment() {
        requireActivity().supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.anim_slide_bottom_to_full,
                R.anim.anim_slide_alpha_hide,
                R.anim.anim_slide_alpha_show,
                R.anim.anim_slide_full_to_bottom
            )
            .replace(
                R.id.main_activity_container, PlayingFragment.newInstance(), PlayingFragment.TAG
            )
            .addToBackStack(PlayingFragment.TAG)
            .commit()
    }

    override fun onClick(view: View?) {
        view ?: return
        when (view.id) {
            R.id.bmp_play_next, R.id.main_ic_play_all, R.id.main_text_play_all -> {
                onPlayNextClick()
            }
            R.id.main_download_all -> {
                onDownloadAllClick()
            }
            R.id.bmp_start_or_pause -> {
                onPauseOrStartClick()
            }
            R.id.main_bottom_mini_player -> {
                switchPlayingFragment()
            }
        }
    }
}