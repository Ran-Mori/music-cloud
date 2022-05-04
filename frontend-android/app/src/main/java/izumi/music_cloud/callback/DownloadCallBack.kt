package izumi.music_cloud.callback

interface DownloadCallBack {

    fun onComplete(index: Int)

    fun onError()
}