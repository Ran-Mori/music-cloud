package izumi.music_cloud.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import izumi.music_cloud.global.GlobalUtil.getFilePathBySongId
import izumi.music_cloud.MainActivity
import izumi.music_cloud.R
import izumi.music_cloud.recycler.SongAdapter
import izumi.music_cloud.viewmodel.MainPageViewModel
import izumi.music_cloud.viewmodel.MainPageViewModelFactory

class HomeFragment : Fragment(), View.OnClickListener {

    companion object {
        const val TAG = "home_fragment"

        @JvmStatic
        fun newInstance() = HomeFragment()
    }

    private var songRecyclerView: RecyclerView? = null
    private var bottomMiniPlayer: View? = null

    private var songAdapter: SongAdapter? = null
    private var viewModel: MainPageViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initViewModel()
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
            songRecyclerView = findViewById(R.id.main_playlist)
            bottomMiniPlayer = findViewById(R.id.main_bottom_mini_player)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initView()
        initObserver()
    }

    private fun initViewModel() {
        viewModel = ViewModelProvider(
            this,
            MainPageViewModelFactory()
        ).get(MainPageViewModel::class.java)

        viewModel?.setPlaySong(::playSong)
    }

    private fun initView() {
        songRecyclerView?.layoutManager = LinearLayoutManager(requireContext())
        songAdapter = SongAdapter().apply {
            songRecyclerView?.adapter = this
        }

        bottomMiniPlayer?.setOnClickListener(this)
    }

    private fun initObserver() {
        viewModel?.songList?.observe(viewLifecycleOwner) {
            songAdapter?.submitList(it)
        }
    }

    override fun onClick(view: View?) {
        view ?: return
        when (view.id) {
            R.id.main_bottom_mini_player -> {
                requireActivity().supportFragmentManager.beginTransaction()
                    .setCustomAnimations(
                        R.anim.anim_slide_enter_bottom,
                        R.anim.anim_slide_exit_bottom,
                        R.anim.anim_slide_exit_bottom,
                        R.anim.anim_slide_enter_bottom,
                    )
                    .replace(
                        R.id.main_activity_container, PlayingFragment.newInstance(), PlayingFragment.TAG
                    )
                    .addToBackStack(PlayingFragment.TAG)
                    .commit()
            }
        }
    }

    private fun playSong(songId: String) {
        (activity as? MainActivity)?.getPlayer()?.apply {
            reset()
            setDataSource(songId.getFilePathBySongId())
            prepareAsync()
        }
    }

    fun downloadSong(songId:String) {
        viewModel?.downloadSong(songId)
    }

}