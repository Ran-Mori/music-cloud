package izumi.music_cloud.fragment

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.facebook.drawee.view.SimpleDraweeView
import izumi.music_cloud.R
import izumi.music_cloud.callback.DownloadCallBack
import izumi.music_cloud.callback.UpdateSongListCallback
import izumi.music_cloud.callback.UploadingCallBack
import izumi.music_cloud.callback.ViewHolderCallback
import izumi.music_cloud.controller.MusicController
import izumi.music_cloud.global.FileUtils
import izumi.music_cloud.global.GlobalConst
import izumi.music_cloud.global.GlobalUtil
import izumi.music_cloud.global.GlobalUtil.getCoverUrlBySongId
import izumi.music_cloud.global.GlobalUtil.getFilePathBySongId
import izumi.music_cloud.recycler.SongAdapter
import izumi.music_cloud.recycler.SongData
import izumi.music_cloud.retrofit.UploadRequestBody
import izumi.music_cloud.toast.ToastMsg
import izumi.music_cloud.viewmodel.SongViewModel
import okhttp3.MultipartBody

class HomeFragment : BaseFragment() {

    companion object {
        const val TAG = "home_fragment"

        @JvmStatic
        fun newInstance() = HomeFragment()
    }

    private var coverImage: SimpleDraweeView? = null
    private var playAllIcon: ImageView? = null
    private var playAllTextView: TextView? = null
    private var downloadAllIcon: ImageView? = null
    private var uploadSongIcon: ImageView? = null
    private var songRecyclerView: RecyclerView? = null
    private var bottomMiniPlayer: View? = null
    private var progressText: TextView? = null
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
            songViewModel.setToastMsg(ToastMsg.NOT_IMPLEMENTED)
        }
    }

    private val updateSongListCallback = object : UpdateSongListCallback {
        override fun onSuccess(list: List<SongData>) {
            songViewModel.setSongList(list)
        }

        override fun onError(msg: ToastMsg) {
            songViewModel.setToastMsg(msg)
        }

    }

    private val startSelectMusicIntent =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val uri = it.data?.data ?: return@registerForActivityResult
            val file = FileUtils.getFile(requireContext(), uri)

            //only support up to 10M music file
            if (file.length() > GlobalConst.MAX_MUSIC_SIZE) {
                songViewModel.setToastMsg(ToastMsg.MUSIC_SIZE_OVER_TEN_M)
                return@registerForActivityResult
            }

            if (file.name.contains("mp3", true)) {
                val callBack = object : UploadingCallBack {
                    override fun onStart() {
                        songViewModel.setIsUploading(true)
                    }

                    override fun onProgress(percent: Int) {
                        songViewModel.setLoadingProgress(percent)
                    }

                    override fun onComplete() {
                        songViewModel.setToastMsg(ToastMsg.UploadSuccess)
                        handler.postDelayed(
                            { songViewModel.requestUpdateSongList(updateSongListCallback) },
                            GlobalConst.HANDLER_POST_DELAY_TIME
                        )
                        songViewModel.setIsUploading(false)
                    }

                    override fun onError() {
                        songViewModel.setIsUploading(false)
                    }
                }
                val fileBody = UploadRequestBody(file, callBack)
                //the Part body key is 'file', the value is the file itself
                val filePart = MultipartBody.Part.createFormData("file", file.name, fileBody)
                songViewModel.upload(filePart)
            } else {
                //only mp3 file supported
                songViewModel.setToastMsg(ToastMsg.NOT_SELECT_A_MP3_FILE)
            }
        }

    private fun getSelectMusicIntent() = Intent().apply {
        action = Intent.ACTION_GET_CONTENT
        type = "audio/mp3"
    }

    private val requestStoragePermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            if (it) {
                startSelectMusicIntent.launch(getSelectMusicIntent())
            } else {
                songViewModel.setToastMsg(ToastMsg.NOT_AUTHORIZE_STORAGE_PERMISSION)
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
            songAdapter?.notifyItemChanged(index)
            songViewModel.setCurrentIndex(index)

            songViewModel.setPlayingStatus(SongViewModel.STATUS_PLAYING)
            val songId = songViewModel.getSongByIndex(index)?.id ?: ""
            MusicController.startPlay(songId.getFilePathBySongId())
        }

        override fun onError(msg: ToastMsg) {
            songViewModel.setToastMsg(msg)
            songViewModel.setIsDownloading(false)
            songViewModel.setPlayingStatus(SongViewModel.STATUS_NOT_INIT)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false).apply {
            coverImage = findViewById(R.id.main_cover)
            downloadAllIcon = findViewById(R.id.main_download_all)
            uploadSongIcon = findViewById(R.id.main_upload_song)
            playAllIcon = findViewById(R.id.main_ic_play_all)
            playAllTextView = findViewById(R.id.main_text_play_all)
            songRecyclerView = findViewById(R.id.main_playlist)
            bottomMiniPlayer = findViewById(R.id.main_bottom_mini_player)
            progressText = findViewById(R.id.main_progress_text)
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

        songViewModel.requestUpdateSongList(updateSongListCallback)

        playAllIcon?.setOnClickListener(this)
        playAllTextView?.setOnClickListener(this)
        downloadAllIcon?.setOnClickListener(this)
        uploadSongIcon?.setOnClickListener(this)
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
                //deprecated but still use it anyway
                coverImage?.setImageURI(Uri.parse(id.getCoverUrlBySongId()))
                bmpCover?.setImageURI(Uri.parse(id.getCoverUrlBySongId()))
            }
        }

        songViewModel.toastMsg.observe(viewLifecycleOwner) {
            it?.let {
                Toast.makeText(requireContext(), it.msg, Toast.LENGTH_SHORT).show()
                songViewModel.setToastMsg(null)
            }
        }

        songViewModel.playingStatus.observe(viewLifecycleOwner) {
            if (it == SongViewModel.STATUS_PLAYING) {
                bmpStartAndPause?.setImageResource(R.drawable.ic_play_song_black)
            } else {
                //STATUS_NOT_INIT or STATUS_PAUSED
                bmpStartAndPause?.setImageResource(R.drawable.ic_pause_song_black)
            }
        }

        songViewModel.isDownloading.observe(viewLifecycleOwner) {
            progressText?.visibility = if (it) View.VISIBLE else View.GONE
        }

        songViewModel.isUploading.observe(viewLifecycleOwner) {
            progressText?.visibility = if (it) View.VISIBLE else View.GONE
        }

        songViewModel.loadingProgress.observe(viewLifecycleOwner) { percent ->
            if (songViewModel.isDownloading.value == true) {
                progressText?.text = getString(R.string.download_progress, percent)
            } else if (songViewModel.isUploading.value == true) {
                progressText?.text = getString(R.string.uploading_progress, percent)
            }
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
            songViewModel.setToastMsg(ToastMsg.DOWNLOADING_NOT_ALLOW_CLICK)
        }
    }

    private fun downloadAll() {
        val songList = songViewModel.songList.value?.toList() ?: return
        if (songList.isEmpty()) return

        val size = songList.size
        var nextDownloadIndex = 0
        val runnable = object : Runnable {
            override fun run() {
                // that = this runnable
                val that = this

                val songId = songViewModel.getSongByIndex(nextDownloadIndex)?.id ?: ""
                songViewModel.download(nextDownloadIndex, songId, object : DownloadCallBack {
                    override fun onStart() {
                        songViewModel.setIsDownloading(true)
                    }

                    override fun onProgress(percent: Int) {
                        songViewModel.setLoadingProgress(percent)
                    }

                    override fun onComplete(index: Int) {
                        songViewModel.setIsDownloading(false)

                        songViewModel.getSongByIndex(index)?.downloaded = true
                        songAdapter?.notifyItemChanged(index)

                        nextDownloadIndex = index + 1
                        while (nextDownloadIndex != size && songList[nextDownloadIndex].downloaded) {
                            ++nextDownloadIndex
                            continue
                        }

                        Log.d(GlobalConst.LOG_TAG, "nextDownloadIndex == $nextDownloadIndex")
                        if (nextDownloadIndex != size) {
                            handler.post(that)
                        }
                    }

                    override fun onError(msg: ToastMsg) {
                        songViewModel.setToastMsg(msg)
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
                if (songViewModel.isDownloading.value == true) {
                    songViewModel.setToastMsg(ToastMsg.DOWNLOADING_NOT_ALLOW_CLICK)
                    return
                }
                downloadAll()
            }
            R.id.main_upload_song -> {
                if (songViewModel.isDownloading.value == true) {
                    songViewModel.setToastMsg(ToastMsg.DOWNLOADING_NOT_ALLOW_CLICK)
                    return
                }
                if (GlobalUtil.checkPermission(
                        requireContext(),
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
                ) {
                    startSelectMusicIntent.launch(getSelectMusicIntent())
                } else {
                    requestStoragePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
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