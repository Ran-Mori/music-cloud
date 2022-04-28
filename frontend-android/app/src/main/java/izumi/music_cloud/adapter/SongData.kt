package izumi.music_cloud.adapter

import androidx.annotation.Keep

@Keep
data class SongData(
    var id: String?,
    var title: String?,
    var coverUrl: String?,
    var artist: String?,
)