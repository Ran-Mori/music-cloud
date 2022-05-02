package izumi.music_cloud.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import izumi.music_cloud.callback.DownloadCallBack
import izumi.music_cloud.error.Error
import izumi.music_cloud.recycler.SongData
import kotlin.random.Random

class SongViewModel : ViewModel() {
    private val _songList = MutableLiveData<List<SongData>>()
    private val _currentIndex = MutableLiveData<Int>()
    private val _nextIndex = MutableLiveData<Int>()
    private val _error = MutableLiveData<Error?>()
    private val _shuffle = MutableLiveData<Boolean>()
    private val _pause = MutableLiveData<Boolean>()

    private val mainModel by lazy { SongModel() }

    val songList: LiveData<List<SongData>>
        get() = _songList

    val currentIndex: LiveData<Int>
        get() = _currentIndex

    val nextIndex: LiveData<Int>
        get() = _nextIndex

    val error: LiveData<Error?>
        get() = _error

    val shuffle: LiveData<Boolean>
        get() = _shuffle

    val pause: LiveData<Boolean>
        get() = _pause

    init {
        mainModel.getSongList(_songList, _error)
        _currentIndex.value = -1
        _error.value = null
        _shuffle.value = false
    }

    fun download(index: Int, callBack: DownloadCallBack? = null) {
        mainModel.downloadSong(_songList, _error, index, callBack)
    }

    fun setCurrentIndex(currentIndex: Int) {
        _currentIndex.value = currentIndex
    }

    fun getSongIdByIndex(index: Int): String {
        val size = _songList.value?.size ?: 0
        return if (size <= 0 || index < 0 || index >= size) {
            ""
        } else {
            songList.value?.get(index)?.id ?: ""
        }
    }

    fun getSongByIndex(index: Int): SongData? {
        val size = _songList.value?.size ?: 0
        return if (size <= 0 || index < 0 || index >= size) {
            null
        } else {
            songList.value?.get(index)
        }
    }

    fun setPause(pause: Boolean) {
        _pause.value = pause
    }

    fun getNextIndex(): Int {
        val size = _songList.value?.size ?: return -1
        return when {
            size <= 0 -> -1
            _shuffle.value == true -> Random(0).nextInt(size)
            _shuffle.value == false -> ((_currentIndex.value ?: 0) + 1) % size
            else -> -1
        }
    }

}