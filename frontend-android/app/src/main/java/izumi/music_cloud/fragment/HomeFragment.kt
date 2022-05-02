package izumi.music_cloud.fragment

import android.os.Bundle
import android.text.TextUtils
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

    private var songRecyclerView: RecyclerView? = null
    private var bottomMiniPlayer: View? = null
    private var bmpCover: SimpleDraweeView? = null
    private var bmpTitle: TextView? = null
    private var bmpStartAndPause: ImageView? = null
    private var bmpPlayNext: ImageView? = null

    private var songAdapter: SongAdapter? = null
    private val songModel: SongViewModel by lazy {
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
        MusicController.setOnPlayCompleteCallBack {
            // the index ready to play
            val nextIndex = songModel.getNextIndex()
            // set current index as index ready to play
            songModel.setCurrentIndex(nextIndex)
            // return the index ready to play
            return@setOnPlayCompleteCallBack (songModel.getSongByIndex(nextIndex)?.id ?: "").getFilePathBySongId()
        }
    }


    override fun onResume() {
        super.onResume()
        requireActivity().window.statusBarColor =
            resources.getColor(R.color.second_background, null)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false).apply {
            songRecyclerView = findViewById(R.id.main_playlist)
            bottomMiniPlayer = findViewById(R.id.main_bottom_mini_player)
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
    }

    private fun initObserver() {
        songModel.songList.observe(viewLifecycleOwner) {
            songAdapter?.submitList(it)
        }

        songModel.currentIndex.observe(viewLifecycleOwner) {
            songModel.getSongByIndex(it)?.title?.let { title ->
                bmpTitle?.text = title
            }
        }

        songModel.nextIndex.observe(viewLifecycleOwner) {
            val songId = songModel.getSongByIndex(it)?.id ?: ""
            //set next to play path
            MusicController.setReadyToPlayPath(songId.getFilePathBySongId())
        }

        songModel.error.observe(viewLifecycleOwner) {
            it?.let {
                Toast.makeText(requireContext(), it.msg, Toast.LENGTH_LONG).show()
            }
        }

        songModel.shuffle.observe(viewLifecycleOwner) {

        }

        songModel.pause.observe(viewLifecycleOwner) {

        }
    }

    //when click a song view holder
    private fun onItemClicked(index: Int) {
        //the clicked item equals to current play item
        if (index == songModel.currentIndex.value) {
            if (songModel.pause.value == true) {
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

    private fun startPlay(index: Int) {

        songModel.setCurrentIndex(index)
        val songId = songModel.getSongByIndex(index)?.id ?: ""

        if (songModel.getSongByIndex(index)?.downloaded == true) {
            //song has been downloaded
            MusicController.startPlay(songId.getFilePathBySongId())
        } else {
            //song haven't been downloaded
            songModel.download(index, object : DownloadCallBack {

                override fun onDownloading(percent: Int) {

                }

                override fun onComplete(index: Int) {
                    MusicController.startPlay(songId.getFilePathBySongId())
                }
            })
        }
    }

    private fun playNext() {

    }

    private fun playPrevious() {

    }

    private fun pausePlay() {

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

    override fun onClick(view: View?) {
        view ?: return
        when (view.id) {
            R.id.main_bottom_mini_player -> {
                switchPlayingFragment()
            }
        }
    }
}