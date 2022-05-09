package izumi.music_cloud.callback

import izumi.music_cloud.toast.ToastMsg

interface DownloadCallBack {
    fun onStart()
    fun onProgress(percent: Int)
    fun onComplete(index: Int)
    fun onError(msg: ToastMsg)
}