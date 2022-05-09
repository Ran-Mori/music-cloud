package izumi.music_cloud.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import izumi.music_cloud.callback.DownloadCallBack
import izumi.music_cloud.global.GlobalConst
import izumi.music_cloud.toast.ToastMsg
import izumi.music_cloud.recycler.SongData
import okhttp3.MultipartBody
import kotlin.random.Random

class SongViewModel : ViewModel() {

    companion object {
        const val STATUS_NOT_INIT = 0
        const val STATUS_PLAYING = 1
        const val STATUS_PAUSED = 2
    }

    private val _songList = MutableLiveData<List<SongData>>()
    private val _currentIndex = MutableLiveData<Int>()
    private val _toast = MutableLiveData<ToastMsg?>()
    private val _shuffle = MutableLiveData<Boolean>()
    private val _playingStatus = MutableLiveData<Int>()
    private val _isDownloading = MutableLiveData<Boolean>()
    private val _isUploading = MutableLiveData<Boolean>()
    private val _loadingProgress = MutableLiveData<Int>()
    private var _currentMilliSec = MutableLiveData<Int>()
    private var _endMilliSec = MutableLiveData<Int>()

    private val mainModel by lazy { SongModel() }

    val songList: LiveData<List<SongData>>
        get() = _songList

    val currentIndex: LiveData<Int>
        get() = _currentIndex

    val toastMsg: LiveData<ToastMsg?>
        get() = _toast

    val shuffle: LiveData<Boolean>
        get() = _shuffle

    val playingStatus: LiveData<Int>
        get() = _playingStatus

    val isDownloading: LiveData<Boolean>
        get() = _isDownloading

    val isUploading: LiveData<Boolean>
        get() = _isUploading

    val loadingProgress: LiveData<Int>
        get() = _loadingProgress

    val currentMilliSec: LiveData<Int>
        get() = _currentMilliSec

    val endMilliSec: LiveData<Int>
        get() = _endMilliSec

    init {
        mainModel.getSongList(_songList, _toast)
        _currentIndex.value = -1
        _shuffle.value = false
        _playingStatus.value = STATUS_NOT_INIT
        _isDownloading.value = false
        _currentMilliSec.value = 0
        _endMilliSec.value = 0

        //should not init these value
        //_toast.value = null
        //_downloadProgress.value = 0
    }

    // download music
    fun download(index: Int, callBack: DownloadCallBack? = null) {
        mainModel.downloadSong(_songList, _toast, _loadingProgress, index, callBack)
    }

    fun upload(filePart: MultipartBody.Part) {
        mainModel.uploadSong(_isUploading, filePart)
    }

    fun restSongList() {
        mainModel.getSongList(_songList, _toast)
    }

    fun setCurrentIndex(currentIndex: Int) {
        _currentIndex.value = currentIndex
    }

    fun setPlayingStatus(playingStatus: Int) {
        _playingStatus.value = playingStatus
    }

    fun setToastMsg(toastMsg: ToastMsg) {
        _toast.value = toastMsg
    }

    fun setIsDownloading(isDownloading: Boolean) {
        _isDownloading.value = isDownloading
    }

    fun setIsUploading(isUploading: Boolean) {
        _isUploading.value = isUploading
    }

    fun setShuffle(shuffle: Boolean) {
        _shuffle.value = shuffle
    }

    fun setLoadingProgress(progress: Int) {
        _loadingProgress.value = progress
    }

    fun setCurrentMilliSec(milliSec: Int) {
        _currentMilliSec.value = milliSec
    }

    fun setEndMilliSec(milliSec: Int) {
        _endMilliSec.value = milliSec
    }

    fun getSongByIndex(index: Int): SongData? {
        val size = _songList.value?.size ?: 0
        return if (size <= 0 || index < 0 || index >= size) {
            null
        } else {
            songList.value?.get(index)
        }
    }

    fun getNextIndex(): Int {
        val size = _songList.value?.size ?: return -1
        return when {
            size <= 0 -> -1
            _shuffle.value == true -> {
                val seed = System.currentTimeMillis()
                Log.d(GlobalConst.LOG_TAG, "getNextIndex() random seed = $seed")
                val index = Random(seed).nextInt(size)
                if (index == _currentIndex.value) {
                    // shuffle next equals current play
                    getNextIndex()
                } else {
                    index
                }
            }
            _shuffle.value == false -> ((_currentIndex.value ?: 0) + 1) % size
            else -> -1
        }
    }
}