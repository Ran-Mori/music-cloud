package izumi.music_cloud

import android.media.MediaPlayer
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import izumi.music_cloud.fragment.HomeFragment
import izumi.music_cloud.global.GlobalConst

class MainActivity : AppCompatActivity() {

    private val mediaPlayer: MediaPlayer by lazy {
        val mediaPlayer = MediaPlayer()
        mediaPlayer.setOnPreparedListener {
            Log.d(GlobalConst.LOG_TAG, "setOnPrepareListener")
            Log.d(GlobalConst.LOG_TAG, "准备完成")
            it.start()
            Log.d(GlobalConst.LOG_TAG, "开始完成")
        }
        mediaPlayer.setOnCompletionListener {

        }
        mediaPlayer
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()

        setContentView(R.layout.activity_main)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(
                    R.id.main_activity_container, HomeFragment.newInstance(), HomeFragment.TAG
                )
                .commit()
        }
    }

    fun getPlayer() = mediaPlayer
}