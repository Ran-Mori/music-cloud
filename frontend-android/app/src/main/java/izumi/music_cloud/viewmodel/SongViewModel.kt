package izumi.music_cloud.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import izumi.music_cloud.callback.DownloadCallBack
import izumi.music_cloud.error.Error
import izumi.music_cloud.recycler.SongData
import kotlin.random.Random

class SongViewModel : ViewModel() {

    companion object {
        const val STATUS_NOT_INIT = 0
        const val STATUS_PLAYING = 1
        const val STATUS_PAUSED = 2
        const val STATUS_DOWNLOADING = 3
    }

    private val _songList = MutableLiveData<List<SongData>>()
    private val _currentIndex = MutableLiveData<Int>()
    private val _error = MutableLiveData<Error?>()
    private val _shuffle = MutableLiveData<Boolean>()
    private val _status = MutableLiveData<Int>()

    private val mainModel by lazy { SongModel() }

    val songList: LiveData<List<SongData>>
        get() = _songList

    val currentIndex: LiveData<Int>
        get() = _currentIndex

    val error: LiveData<Error?>
        get() = _error

    val shuffle: LiveData<Boolean>
        get() = _shuffle

    val status: LiveData<Int>
        get() = _status

    init {
        _currentIndex.value = -1
        _status.value = STATUS_NOT_INIT
        _error.value = null
        _shuffle.value = false
        mainModel.getSongList(_songList, _error)
    }

    // download music
    fun download(index: Int, callBack: DownloadCallBack? = null) {
        mainModel.downloadSong(_songList, _error, index, callBack)
    }

    fun setCurrentIndex(currentIndex: Int) {
        _currentIndex.value = currentIndex
    }

    fun getSongByIndex(index: Int): SongData? {
        val size = _songList.value?.size ?: 0
        return if (size <= 0 || index < 0 || index >= size) {
            null
        } else {
            songList.value?.get(index)
        }
    }

    fun setStatus(status: Int) {
        _status.value = status
    }

    fun getNextIndex(): Int {
        val size = _songList.value?.size ?: return -1
        return when {
            size <= 0 -> -1
            _shuffle.value == true -> {
                val index = Random(0).nextInt(size)
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