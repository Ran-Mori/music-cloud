package izumi.music_cloud.global

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.ContextCompat
import izumi.music_cloud.App
import izumi.music_cloud.R
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

object GlobalUtil {
    private fun File.md5(): String {
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

    fun checkPermission(context: Context, permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    fun String.getFilePathBySongId(): String =
        "${App.context.filesDir.absolutePath}${File.separator}${this}.mp3"

    fun String.musicExists(): Boolean {
        if (this.length != 32) return false
        val file = File(this.getFilePathBySongId())
        return file.exists() && file.md5() == this
    }

    fun String.getCoverUrlBySongId(): String = "${GlobalConst.BASE_URL}/song/cover/${this}"

    fun Int.milliSecToMinute(): String {
        val minute = TimeUnit.MILLISECONDS.toMinutes(this.toLong())
        return if (minute < 10) {
            "0${minute}"
        } else {
            minute.toString()
        }
    }

    fun Int.milliSecToSecond(): String {
        val second = (this / 1000) % 60
        return if (second < 10) {
            "0${second}"
        } else {
            second.toString()
        }
    }

    @SuppressLint("Recycle")
    fun Uri.getFileName(context: Context): String {
        var result = ""
        if (this.scheme == "content") {
            val cursor =
                context.contentResolver.query(this, null, null, null, null) ?: return result
            if (!cursor.moveToFirst()) return result
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            result = cursor.getString(index)
        }
        return result.ifEmpty { context.getString(R.string.default_music_file_name) }
    }
}