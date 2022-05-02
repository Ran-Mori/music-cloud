package izumi.music_cloud.callback

interface DownloadCallBack {

    fun onDownloading(percent: Int)

    fun onComplete(index: Int)
}