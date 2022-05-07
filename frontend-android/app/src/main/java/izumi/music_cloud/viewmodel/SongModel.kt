package izumi.music_cloud.viewmodel

import android.content.Context
import android.net.Uri
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
import izumi.music_cloud.error.Error
import izumi.music_cloud.global.GlobalConst
import izumi.music_cloud.global.GlobalUtil.getFileName
import izumi.music_cloud.global.GlobalUtil.getFilePathBySongId
import izumi.music_cloud.global.GlobalUtil.musicExists
import izumi.music_cloud.recycler.SongData
import izumi.music_cloud.retrofit.SongService
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import kotlin.concurrent.thread


class SongModel {

    private val disposable = CompositeDisposable()

    private val handler = Handler(Looper.getMainLooper())

    // make it to be volatile, as it will be modify in other threads
    @Volatile private var  isDownloading: Boolean = false

    private fun updateDownloadStatus(songList: List<SongData>) {
        for (song in songList) {
            val id = song.id ?: continue
            if (id.musicExists()) song.downloaded = true
        }
    }


    fun getSongList(_songList: MutableLiveData<List<SongData>>, _error: MutableLiveData<Error?>) {
        SongService.getSongList()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnError {
                Log.d(GlobalConst.LOG_TAG, "resetSongList on complete")
                _error.value = Error.GET_SONG_LIST_ERROR
            }
            .subscribe {
                Log.d(GlobalConst.LOG_TAG, "resetSongList on next")
                updateDownloadStatus(it)
                _songList.value = it
            }.let { disposable.add(it) }
    }

    fun downloadSong(
        _songList: MutableLiveData<List<SongData>>,
        _error: MutableLiveData<Error?>,
        _downloadProgress: MutableLiveData<Int>,
        index: Int,
        callBack: DownloadCallBack? = null
    ) {
        val songId = _songList.value?.get(index)?.id ?: return
        if (isDownloading) return

        SongService.downloadSong(songId)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe {
                isDownloading = true
            }
            .doOnError {
                Log.d(GlobalConst.LOG_TAG, "downloadSong do on error")
                _error.value = Error.DOWNLOAD_SONG_ERROR
                callBack?.onError()
                isDownloading = false
            }
            .subscribe {
                // do on next will immediately return as 'thread{}'
                thread {
                    Log.d(GlobalConst.LOG_TAG, "downloadSong do on next")

                    val path = songId.getFilePathBySongId()
                    val file = File(path)
                    if (file.exists() && songId.musicExists()) {
                        handler.post {
                            callBack?.onComplete(index)
                        }
                        // modify inDownloading in anther thread
                        isDownloading = false
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
                        var totalRead = 0.0
                        while (input.read(buffer).also { read = it } != -1) {
                            Log.d(GlobalConst.LOG_TAG, "downloadSong downloading")
                            totalRead += read
                            output.write(buffer, 0, read)
                            //show download percent
                            handler.sendMessage(Message.obtain(handler) {
                                _downloadProgress.value = ((totalRead / contentLength) * 100).toInt()
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
            }.let { disposable.add(it) }
    }

    fun uploadSong(context: Context, uri: Uri) {
        val path = uri.path ?: return
        val file = File(path)
        val fileName = uri.getFileName(context)
        val mediaType = context.contentResolver.getType(uri) ?: return
        val mediaTypeObject = mediaType.toMediaTypeOrNull() ?: return
        val requestFile = file.asRequestBody(mediaTypeObject)
        val partFile = MultipartBody.Part.createFormData("file", fileName, requestFile)

        val requestBody = "this is to set 'multipart/form-data' header".toRequestBody(MultipartBody.FORM)
        SongService.uploadSong(requestBody, partFile)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe {
                Log.d(GlobalConst.LOG_TAG, "uploadSong doOnSubscribe")
            }
            .doOnError {
                val throwable = it
                Log.d(GlobalConst.LOG_TAG, "uploadSong doOnError")
            }
            .subscribe {
                Log.d(GlobalConst.LOG_TAG, "uploadSong doOnNext")
            }.let { disposable.add(it) }
    }
}