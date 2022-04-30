package izumi.music_cloud.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import izumi.music_cloud.R

class PlayingActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()

        setContentView(R.layout.activity_playing)
    }

    override fun finish() {
        super.finish()

        overridePendingTransition(
            R.anim.anim_no_anim,
            R.anim.anim_slide_exit_bottom
        )
    }
}