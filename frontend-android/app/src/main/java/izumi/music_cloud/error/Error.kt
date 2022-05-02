package izumi.music_cloud.error

enum class Error(val msg: String) {
    GET_SONG_LIST_ERROR("Fail to load your playlist, please checkout internet or contact via izumisakai@aliyun.com"),
    DOWNLOAD_SONG_ERROR("Fail to download music, please checkout internet or contact via izumisakai@aliyun.com"),
}