package izumi.music_cloud.fragment

import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
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
import izumi.music_cloud.viewmodel.SongViewModelFactory

class HomeFragment : Fragment(), View.OnClickListener {

    companion object {
        const val TAG = "home_fragment"

        @JvmStatic
        fun newInstance() = HomeFragment()
    }

    private var mainCover: SimpleDraweeView? = null
    private var songRecyclerView: RecyclerView? = null
    private var bottomMiniPlayer: View? = null
    private var downloadProgress: TextView? = null
    private var bmpCover: SimpleDraweeView? = null
    private var bmpTitle: TextView? = null
    private var bmpStartAndPause: ImageView? = null
    private var bmpPlayNext: ImageView? = null

    private var songAdapter: SongAdapter? = null
    private val songViewModel: SongViewModel by lazy {
        ViewModelProvider(
            this,
            SongViewModelFactory()
        ).get(SongViewModel::class.java)
    }

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MusicController.setOnPlayCompleteCallBack { playNext() }
    }

    override fun onResume() {
        super.onResume()
        requireActivity().window.statusBarColor = resources.getColor(R.color.second_background, null)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false).apply {
            mainCover = findViewById(R.id.main_cover)
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

        bottomMiniPlayer?.setOnClickListener(this)
        bmpStartAndPause?.setOnClickListener(this)
        bmpPlayNext?.setOnClickListener(this)
    }

    private fun initObserver() {
        songViewModel.songList.observe(viewLifecycleOwner) {
            songAdapter?.submitList(it)
            if (it.isNotEmpty()) songViewModel.setCurrentIndex(0)
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

        songViewModel.status.observe(viewLifecycleOwner) {
            if (it != SongViewModel.STATUS_PLAYING) {
                bmpStartAndPause?.setImageResource(R.drawable.ic_pause_song_black)
            } else {
                bmpStartAndPause?.setImageResource(R.drawable.ic_play_song_black)
            }
        }
    }

    private fun startPlay(index: Int) {
        songViewModel.setCurrentIndex(index)
        val songId = songViewModel.getSongByIndex(index)?.id ?: ""

        if (songViewModel.getSongByIndex(index)?.downloaded == true) {
            //song has been downloaded
            MusicController.startPlay(songId.getFilePathBySongId())
            songViewModel.setStatus(SongViewModel.STATUS_PLAYING)
        } else {
            //song haven't been downloaded
            songViewModel.setStatus(SongViewModel.STATUS_DOWNLOADING)
            songViewModel.download(index, object : DownloadCallBack {
                override fun onDownloading(percent: Int) {
                    downloadProgress?.let {
                        it.visibility = View.VISIBLE
                        it.text = getString(R.string.download_progress, percent)
                    }
                }

                //do something when finish downloading
                override fun onComplete(index: Int) {
                    downloadProgress?.visibility = View.GONE
                    songAdapter?.notifyItemChanged(index)
                    songViewModel.setCurrentIndex(index)
                    songViewModel.setStatus(SongViewModel.STATUS_PLAYING)
                    MusicController.startPlay(songId.getFilePathBySongId())
                }

                override fun onError() {
                    songViewModel.setStatus(SongViewModel.STATUS_NOT_INIT)
                }
            })
        }
    }

    //when click a song view holder
    private fun onItemClicked(index: Int) {
        if (songViewModel.status.value == SongViewModel.STATUS_DOWNLOADING) {
            showToastWhenDownloading()
            return
        }
        if (index == songViewModel.currentIndex.value) {
            //the clicked item equals to current play item
            if (songViewModel.status.value == SongViewModel.STATUS_PAUSED) {
                //pause
                resumePlay()
            } else {
                //not pause
                switchPlayingFragment()
            }
        } else {
            //play a new song
            startPlay(index)
        }
    }

    private fun playNext() {
        // the index ready to play
        val nextIndex = songViewModel.getNextIndex()
        // start to play
        startPlay(nextIndex)
    }

    private fun playPrevious() {

    }

    private fun pausePlay() {
        MusicController.pausePlay()
    }

    private fun resumePlay() {
        MusicController.resumePlay()
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

    private fun showToastWhenDownloading() {
        Toast.makeText(
            requireContext(),
            getString(R.string.downloading_user_toast),
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onClick(view: View?) {
        view ?: return
        when (view.id) {
            R.id.bmp_play_next -> {
                when (songViewModel.status.value) {
                    SongViewModel.STATUS_DOWNLOADING -> {
                        showToastWhenDownloading()
                    }
                    else -> playNext()
                }
            }
            R.id.bmp_start_or_pause -> {
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
            R.id.main_bottom_mini_player -> {
                switchPlayingFragment()
            }
        }
    }
}