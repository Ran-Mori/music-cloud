package izumi.music_cloud.viewmodel

import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import androidx.core.content.FileProvider
import androidx.lifecycle.MutableLiveData
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import izumi.music_cloud.App
import izumi.music_cloud.callback.DownloadCallBack
import izumi.music_cloud.toast.ToastMsg
import izumi.music_cloud.global.GlobalConst
import izumi.music_cloud.global.GlobalUtil.getFilePathBySongId
import izumi.music_cloud.global.GlobalUtil.musicExists
import izumi.music_cloud.recycler.SongData
import izumi.music_cloud.retrofit.SongService
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import kotlin.concurrent.thread


class SongModel {

    private val disposable = CompositeDisposable()

    private val handler = Handler(Looper.getMainLooper())

    // make it to be volatile, as it will be modify in other threads
    @Volatile
    private var isDownloading: Boolean = false

    private fun updateDownloadStatus(songList: List<SongData>) {
        for (song in songList) {
            val id = song.id ?: continue
            if (id.musicExists()) song.downloaded = true
        }
    }


    fun getSongList(
        _songList: MutableLiveData<List<SongData>>,
        _toastMsg: MutableLiveData<ToastMsg?>
    ) {
        SongService.getSongList()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                {
                    Log.d(GlobalConst.LOG_TAG, "resetSongList on next")
                    updateDownloadStatus(it)
                    _songList.value = it
                },
                {
                    Log.d(GlobalConst.LOG_TAG, "resetSongList on complete")
                    _toastMsg.value = ToastMsg.GET_SONG_LIST_ERROR
                    _songList.value = listOf()
                }
            ).let { disposable.add(it) }
    }

    fun downloadSong(
        _songList: MutableLiveData<List<SongData>>,
        _toastMsg: MutableLiveData<ToastMsg?>,
        _downloadProgress: MutableLiveData<Int>,
        index: Int,
        callBack: DownloadCallBack? = null
    ) {
        Log.d(GlobalConst.LOG_TAG, "SongModel.downloadSong index = $index")
        val songId = _songList.value?.get(index)?.id ?: return
        if (isDownloading) return

        SongService.downloadSong(songId)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe {
                isDownloading = true
            }
            .subscribe(
                {
                    thread {
                        Log.d(GlobalConst.LOG_TAG, "downloadSong do on next")

                        val path = songId.getFilePathBySongId()
                        val file = File(path)
                        if (file.exists() && songId.musicExists()) {
                            handler.postDelayed({
                                // modify inDownloading in anther thread
                                isDownloading = false
                                callBack?.onComplete(index)
                            }, 1000L)
                            return@thread
                        } else {
                            file.delete()
                            file.createNewFile()
                        }

                        val uri = FileProvider.getUriForFile(
                            App.context,
                            GlobalConst.FILE_PROVIDER_AUTHORITIES,
                            file
                        )

                        val input = it.body()?.byteStream() ?: return@thread
                        val contentLength: Long = it.raw().body?.contentLength() ?: Long.MAX_VALUE

                        Log.d(GlobalConst.LOG_TAG, "downloadSong start")

                        App.context.contentResolver?.openOutputStream(uri).use { output ->
                            output ?: return@thread
                            val buffer = ByteArray(4 * 1024)
                            var read: Int
                            var readed = 0.0
                            while (input.read(buffer).also { read = it } != -1) {
                                Log.d(GlobalConst.LOG_TAG, "downloadSong downloading")
                                readed += read
                                output.write(buffer, 0, read)
                                //show download percent
                                handler.sendMessage(Message.obtain(handler) {
                                    _downloadProgress.value =
                                        ((readed / contentLength) * 100).toInt()
                                })
                            }
                            output.flush()
                        }
                        Log.d(GlobalConst.LOG_TAG, "downloadSong finish")

                        // modify inDownloading in anther thread
                        isDownloading = false

                        handler.sendMessage(Message.obtain(handler) {
                            Log.d(GlobalConst.LOG_TAG, "downloadSong handler do")
                            _songList.value?.get(index)?.downloaded = true
                            callBack?.onComplete(index)
                        })
                    }
                },
                {
                    Log.d(GlobalConst.LOG_TAG, "downloadSong do on error")
                    _toastMsg.value = ToastMsg.DOWNLOAD_SONG_ERROR
                    callBack?.onError()
                    isDownloading = false
                }
            ).let { disposable.add(it) }
    }

    fun uploadSong(_isUploading: MutableLiveData<Boolean>, filePart: MultipartBody.Part) {

        val descriptionBody =
            "this is to set 'multipart/form-data' header".toRequestBody(MultipartBody.FORM)
        SongService.uploadSong(descriptionBody, filePart)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe {
                _isUploading.value = true
                Log.d(GlobalConst.LOG_TAG, "uploadSong doOnSubscribe")
            }
            .doOnError {
                Log.d(GlobalConst.LOG_TAG, "uploadSong doOnError")
            }
            .doOnTerminate {
                _isUploading.value = false
                Log.d(GlobalConst.LOG_TAG, "uploadSong doOnTerminate")
            }
            .subscribe {
                Log.d(GlobalConst.LOG_TAG, "uploadSong doOnNext")
            }.let { disposable.add(it) }
    }
}