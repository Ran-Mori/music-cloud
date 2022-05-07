package izumi.music_cloud.retrofit

import io.reactivex.Observable
import izumi.music_cloud.recycler.SongData
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*


interface ISongService {
    @GET("/songs")
    fun getSongList(): Observable<List<SongData>>

    @Streaming
    @GET("/song/download/{song_id}")
    fun downloadSong(@Path(value = "song_id") songId: String): Observable<Response<ResponseBody>>

    @DELETE("/song/{song_id}")
    fun deleteSong(@Path(value = "song_id") songId: String)

    @GET("/song/query/{song_id}")
    fun getSong(@Path(value = "song_id") songId: String): Observable<SongData>

    @Multipart
    @POST("/song/upload")
    fun uploadSong(
        @Part("description") description: RequestBody,
        @Part file: MultipartBody.Part
    ): Observable<ResponseBody>
}