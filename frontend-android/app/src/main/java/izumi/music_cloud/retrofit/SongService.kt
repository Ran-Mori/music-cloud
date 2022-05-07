package izumi.music_cloud.retrofit

import okhttp3.MultipartBody
import okhttp3.RequestBody

object SongService {
    private val service = RetrofitUtil.getRetrofitService().create(ISongService::class.java)

    fun getSongList() = service.getSongList()

    fun downloadSong(songId: String) = service.downloadSong(songId)

    fun uploadSong(description: RequestBody, file: MultipartBody.Part) = service.uploadSong(description, file)
}