package izumi.music_cloud

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import com.facebook.drawee.backends.pipeline.Fresco

class App : Application() {

    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var context: Context
    }

    override fun onCreate() {
        super.onCreate()
        Fresco.initialize(this)

        context = applicationContext
    }
}