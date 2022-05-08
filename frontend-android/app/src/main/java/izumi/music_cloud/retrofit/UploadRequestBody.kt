package izumi.music_cloud.retrofit

import android.os.Handler
import android.os.Looper
import izumi.music_cloud.callback.UploadingCallBack
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okio.BufferedSink
import java.io.File
import java.io.FileInputStream
import java.lang.Exception

class UploadRequestBody(private val file: File, private val callBack: UploadingCallBack) :
    RequestBody() {

    private val handler = Handler(Looper.getMainLooper())

    override fun contentType(): MediaType? = "audio/mpeg".toMediaTypeOrNull()

    override fun contentLength(): Long = file.length()

    override fun writeTo(sink: BufferedSink) {
        val contentLength = contentLength()
        val buffer = ByteArray(4 * 1024)
        var upload = 0
        var uploaded = 0.0
        try {
            FileInputStream(file).use { input ->
                while (input.read(buffer).also { upload = it } != -1) {
                    uploaded += upload
                    sink.write(buffer, 0, upload)
                    handler.post {
                        callBack.onProgress(((upload / contentLength) * 100).toInt())
                    }
                }
                sink.flush()
                handler.post {
                    callBack.onComplete()
                }
            }
        } catch (e: Exception) {
            handler.post {
                callBack.onError()
            }
        }

    }
}