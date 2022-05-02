package izumi.music_cloud.global

import izumi.music_cloud.App
import java.io.File
import java.security.MessageDigest

object GlobalUtil {
    fun File.md5(): String {
        val md = MessageDigest.getInstance("MD5")
        return this.inputStream().use { fis ->
            val buffer = ByteArray(8192)
            generateSequence {
                when (val bytesRead = fis.read(buffer)) {
                    -1 -> null
                    else -> bytesRead
                }
            }.forEach { bytesRead -> md.update(buffer, 0, bytesRead) }
            md.digest().joinToString("") { "%02x".format(it) }
        }
    }

    fun String.getFilePathBySongId(): String = "${App.context.filesDir.absolutePath}${File.separator}${this}.mp3"

    fun String.musicExists(): Boolean {
        if (this.length != 32) return false
        val file = File(this.getFilePathBySongId())
        return file.exists() && file.md5() == this
    }
}