package izumi.music_cloud.viewmodel

import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import androidx.core.content.FileProvider
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import izumi.music_cloud.App
import izumi.music_cloud.callback.DownloadCallBack
import izumi.music_cloud.callback.UpdateSongListCallback
import izumi.music_cloud.toast.ToastMsg
import izumi.music_cloud.global.GlobalConst
import izumi.music_cloud.global.GlobalUtil.getFilePathBySongId
import izumi.music_cloud.global.GlobalUtil.musicExists
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

    fun getSongList(callback: UpdateSongListCallback) {
        SongService.getSongList()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                {
                    Log.d(GlobalConst.LOG_TAG, "resetSongList on next")
                    callback.onSuccess(it)
                },
                {
                    Log.d(GlobalConst.LOG_TAG, "resetSongList on error")
                    callback.onError(ToastMsg.GET_SONG_LIST_ERROR)
                }
            ).let { disposable.add(it) }
    }

    fun downloadSong(
        index: Int,
        songId: String,
        callBack: DownloadCallBack? = null
    ) {
        Log.d(GlobalConst.LOG_TAG, "SongModel.downloadSong index = $index")
        if (isDownloading) return

        SongService.downloadSong(songId)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe {
                isDownloading = true
            }
            .subscribe(
                {
                    callBack?.onStart()
                    //downloading is a time consuming task. it should do async
                    thread {
                        Log.d(GlobalConst.LOG_TAG, "downloadSong do on next")

                        val path = songId.getFilePathBySongId()
                        val file = File(path)
                        if (file.exists() && songId.musicExists()) {
                            handler.postDelayed({
                                // modify inDownloading in anther thread
                                isDownloading = false
                                callBack?.onComplete(index)
                            }, GlobalConst.HANDLER_POST_DELAY_TIME)
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
                                    callBack?.onProgress(((readed / contentLength) * 100).toInt())
//                                    _downloadProgress.value =
                                })
                            }
                            output.flush()
                        }
                        Log.d(GlobalConst.LOG_TAG, "downloadSong finish")

                        // modify inDownloading in anther thread
                        isDownloading = false

                        handler.sendMessage(Message.obtain(handler) {
                            Log.d(GlobalConst.LOG_TAG, "downloadSong handler do")
                            callBack?.onComplete(index)
                        })
                    }
                },
                {
                    Log.d(GlobalConst.LOG_TAG, "downloadSong do on error")
                    callBack?.onError(ToastMsg.DOWNLOAD_SONG_ERROR)
                    isDownloading = false
                }
            ).let { disposable.add(it) }
    }

    fun uploadSong(filePart: MultipartBody.Part) {

        val descriptionBody =
            "this is to set 'multipart/form-data' header".toRequestBody(MultipartBody.FORM)
        SongService.uploadSong(descriptionBody, filePart)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe {
                Log.d(GlobalConst.LOG_TAG, "uploadSong doOnSubscribe")
            }
            .subscribe(
                {
                    Log.d(GlobalConst.LOG_TAG, "uploadSong doOnNext")
                }, {
                    Log.d(GlobalConst.LOG_TAG, "uploadSong doOnError")
                }
            ).let { disposable.add(it) }
    }
}