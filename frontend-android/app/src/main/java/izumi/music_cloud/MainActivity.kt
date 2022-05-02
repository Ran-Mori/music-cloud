package izumi.music_cloud

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import izumi.music_cloud.fragment.HomeFragment

class MainActivity : AppCompatActivity() {

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
}