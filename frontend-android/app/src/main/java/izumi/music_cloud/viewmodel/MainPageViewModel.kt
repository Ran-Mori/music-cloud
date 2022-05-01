package izumi.music_cloud.viewmodel

import android.util.Log
import androidx.core.content.FileProvider
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import izumi.music_cloud.App
import izumi.music_cloud.global.GlobalConst
import izumi.music_cloud.global.GlobalUtil.getFilePathBySongId
import izumi.music_cloud.recycler.SongData
import izumi.music_cloud.retrofit.SongService
import izumi.music_cloud.global.GlobalUtil.md5
import java.io.File
import kotlin.concurrent.thread

class MainPageViewModel : ViewModel() {
    private val _songList = MutableLiveData<List<SongData>>()
    private val _currentPlayIndex = MutableLiveData<Int>()

    private val disposable = CompositeDisposable()
    private var showToast: ((String) -> Unit)? = null
    private var playSong: ((String) -> Unit)? = null

    val songList: LiveData<List<SongData>>
        get() = _songList

    val currentPlayIndex : LiveData<Int>
        get() = _currentPlayIndex

    init {
        getSongList()
        _currentPlayIndex.value = -1
    }

    fun setPlaySong(playSong: (String) -> Unit) {
        this.playSong = playSong
    }


    private fun getSongList() {
        SongService.getSongList()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                {
                    Log.d(GlobalConst.LOG_TAG, "resetSongList on next")
                    _songList.value = it
                },
                {
                    Log.d(GlobalConst.LOG_TAG, "resetSongList on complete")
                    showToast?.invoke(GlobalConst.ErrorMsg.GET_SONG_LIST_ERROR)
                }
            ).let { disposable.add(it) }
    }

    fun downloadSong(songId: String) {

        val file = File(songId.getFilePathBySongId())
        // file have existed
        if (file.exists()) {
            Log.d(GlobalConst.LOG_TAG, "file has existed")
            if (file.md5() == songId) {
                Log.d(GlobalConst.LOG_TAG, "file has exited and mds same")
                playSong?.invoke(songId)
                return
            } else {
                Log.d(GlobalConst.LOG_TAG, "file has exited and md5 not same")
                file.delete()
                file.createNewFile()
            }
        } else {
            file.createNewFile()
        }

        SongService.downloadSong(songId)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                {
                    Log.d(GlobalConst.LOG_TAG, "downloadSong do on next")

                    val uri = FileProvider.getUriForFile(
                        App.context,
                        GlobalConst.FILE_PROVIDER_AUTHORITIES,
                        file
                    )

                    thread {
                        val input = it.body()?.byteStream() ?: return@thread

                        Log.d(GlobalConst.LOG_TAG, "downloadSong start")

                        App.context.contentResolver?.openOutputStream(uri).use { output ->
                            output ?: return@thread
                            val buffer = ByteArray(4 * 1024)
                            var read: Int
                            while (input.read(buffer).also { read = it } != -1) {
                                Log.d(GlobalConst.LOG_TAG, "downloadSong downloading")
                                output.write(buffer, 0, read)
                            }
                            output.flush()
                        }
                        Log.d(GlobalConst.LOG_TAG, "downloadSong finish")
                    }

                },
                {
                    Log.d(GlobalConst.LOG_TAG, "downloadSong do on error")
                }
            ).let { disposable.add(it) }
    }
}