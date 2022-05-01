package izumi.music_cloud.retrofit

object SongService {
    private val service = RetrofitUtil.getRetrofitService().create(ISongService::class.java)

    fun getSongList() = service.getSongList()

    fun downloadSong(songId: String) = service.downloadSong(songId)
}