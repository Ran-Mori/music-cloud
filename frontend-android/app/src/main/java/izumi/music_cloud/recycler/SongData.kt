package izumi.music_cloud.recycler

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class SongData(
    @SerializedName("id")
    var id: String?,
    @SerializedName("title")
    var title: String?,
    @SerializedName("artist")
    var artist: String?,

    var coverUrl: String?
)