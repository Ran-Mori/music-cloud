package izumi.music_cloud.fragment

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.facebook.drawee.view.SimpleDraweeView
import izumi.music_cloud.R
import izumi.music_cloud.callback.DownloadCallBack
import izumi.music_cloud.callback.UploadingCallBack
import izumi.music_cloud.callback.ViewHolderCallback
import izumi.music_cloud.controller.MusicController
import izumi.music_cloud.global.FileUtils
import izumi.music_cloud.global.GlobalConst
import izumi.music_cloud.global.GlobalUtil
import izumi.music_cloud.global.GlobalUtil.getCoverUrlBySongId
import izumi.music_cloud.global.GlobalUtil.getFilePathBySongId
import izumi.music_cloud.recycler.SongAdapter
import izumi.music_cloud.retrofit.UploadRequestBody
import izumi.music_cloud.toast.ToastMsg
import izumi.music_cloud.viewmodel.SongViewModel
import okhttp3.MultipartBody

class HomeFragment : BaseFragment() {

    companion object {
        const val TAG = "home_fragment"
        private const val SELECT_FILE_REQUEST_CODE = 1024
        private const val REQUEST_STORAGE_PERMISSION = 2048

        @JvmStatic
        fun newInstance() = HomeFragment()
    }

    private var mainCover: SimpleDraweeView? = null
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
                mainCover?.setImageURI(Uri.parse(id.getCoverUrlBySongId()))
                bmpCover?.setImageURI(Uri.parse(id.getCoverUrlBySongId()))
            }
        }

        songViewModel.toastMsg.observe(viewLifecycleOwner) {
            it?.let { Toast.makeText(requireContext(), it.msg, Toast.LENGTH_LONG).show() }
        }

        songViewModel.playingStatus.observe(viewLifecycleOwner) {
            if (it == SongViewModel.STATUS_PLAYING) {
                bmpStartAndPause?.setImageResource(R.drawable.ic_play_song_black)
            } else {
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

    private fun onDownloadAllClick() {
        if (songViewModel.isDownloading.value == true) return

        val songList = songViewModel.songList.value?.toList() ?: return
        if (songList.isEmpty()) return

        val size = songList.size
        var nextDownloadIndex = 0
        val runnable = object : Runnable {
            override fun run() {
                // that = this runnable
                val that = this

                songViewModel.download(nextDownloadIndex, object : DownloadCallBack {
                    override fun onComplete(index: Int) {
                        songAdapter?.notifyItemChanged(index)
                        songViewModel.setIsDownloading(false)

                        nextDownloadIndex = index + 1

                        while (nextDownloadIndex != size && songList[nextDownloadIndex].downloaded) {
                            ++nextDownloadIndex
                            continue
                        }

                        Log.d(GlobalConst.LOG_TAG, "nextDownloadIndex == $nextDownloadIndex")
                        if (nextDownloadIndex != size) {
                            //set isDownloading = true when post a downloading runnable
                            songViewModel.setIsDownloading(true)
                            handler.post(that)
                        }
                    }

                    override fun onError() {
                        songViewModel.setPlayingStatus(SongViewModel.STATUS_NOT_INIT)
                        songViewModel.setIsDownloading(false)
                    }
                })
            }
        }
        //set isDownloading = true when post a downloading runnable
        songViewModel.setIsDownloading(true)
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

    private fun requestStoragePermission() {
        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
            REQUEST_STORAGE_PERMISSION
        )
    }

    private fun startChooseFileActivity() {
        val intent = Intent().apply {
            action = Intent.ACTION_GET_CONTENT
            type = "audio/mp3"
        }
        startActivityForResult(
            Intent.createChooser(
                intent,
                getString(R.string.select_audio_file_title)
            ), SELECT_FILE_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED && requestCode == REQUEST_STORAGE_PERMISSION) {
            startChooseFileActivity()
        } else {
            songViewModel.setToastMsg(ToastMsg.NOT_AUTHORIZE_STORAGE_PERMISSION)
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SELECT_FILE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            //the uri of selected mp3 file
            val uri = data?.data ?: return
            val file = FileUtils.getFile(requireContext(), uri)
            val fileName = file.name
            if (fileName.contains("mp3", true)) {
                val callBack = object : UploadingCallBack {

                    override fun onProgress(percent: Int) {
                        songViewModel.setLoadingProgress(percent)
                    }

                    override fun onComplete() {
                        songViewModel.setToastMsg(ToastMsg.UploadSuccess)
                        handler.postDelayed({ songViewModel.restSongList() }, 3000L)
                        songViewModel.setIsUploading(false)
                    }

                    override fun onError() {
                        songViewModel.setIsUploading(false)
                    }
                }
                val fileBody = UploadRequestBody(file, callBack)
                val filePart = MultipartBody.Part.createFormData("file", file.name, fileBody)
                songViewModel.upload(filePart)
            } else {
                songViewModel.setToastMsg(ToastMsg.NOT_SELECT_A_MP3_FILE)
            }
        }
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
                    startChooseFileActivity()
                } else {
                    requestStoragePermission()
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