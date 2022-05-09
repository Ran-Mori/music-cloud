package izumi.music_cloud.callback

import izumi.music_cloud.recycler.SongData
import izumi.music_cloud.toast.ToastMsg

interface UpdateSongListCallback {
    fun onSuccess(list: List<SongData>)
    fun onError(msg: ToastMsg)
}