package izumi.music_cloud.callback

interface UploadingCallBack {
    fun onStart()
    fun onProgress(percent: Int)
    fun onComplete()
    fun onError()
}