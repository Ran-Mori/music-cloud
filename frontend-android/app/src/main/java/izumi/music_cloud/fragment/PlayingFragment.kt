package izumi.music_cloud.fragment

import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import com.facebook.drawee.view.SimpleDraweeView
import izumi.music_cloud.R
import izumi.music_cloud.global.GlobalUtil.getCoverUrlBySongId
import izumi.music_cloud.viewmodel.SongViewModel
import izumi.music_cloud.viewmodel.SongViewModelFactory

class PlayingFragment : Fragment(), View.OnClickListener {

    companion object {
        const val TAG = "playing_fragment"

        @JvmStatic
        fun newInstance() = PlayingFragment()
    }

    private var playingCover: SimpleDraweeView? = null
    private var titleTextView: TextView? = null
    private var artistTextView: TextView? = null
    private var playPrevious: ImageView? = null
    private var startAndPause: ImageView? = null
    private var playNext: ImageView? = null

    private val songViewModel: SongViewModel by lazy {
        ViewModelProvider(
            this,
            SongViewModelFactory()
        ).get(SongViewModel::class.java)
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
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        initObserver()
    }

    private fun initView() {
        playPrevious?.setOnClickListener(this)
        startAndPause?.setOnClickListener(this)
        playNext?.setOnClickListener(this)
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
        }

        songViewModel.status.observe(viewLifecycleOwner) {
            if (it != SongViewModel.STATUS_PLAYING) {
                startAndPause?.setImageResource(R.drawable.ic_pause_song_black)
            } else {
                startAndPause?.setImageResource(R.drawable.ic_play_song_black)
            }
        }
    }

    override fun onClick(view: View?) {
        view ?: return
        when(view.id) {

        }
    }


}