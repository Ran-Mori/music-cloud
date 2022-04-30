package izumi.music_cloud.activity

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import izumi.music_cloud.R
import izumi.music_cloud.adapter.SongAdapter
import izumi.music_cloud.adapter.SongData

class MainActivity : AppCompatActivity(), View.OnClickListener {

    private var songRecyclerView: RecyclerView? = null
    private var bottomMiniPlayer: View? = null

    private var songAdapter: SongAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_main)
        window?.statusBarColor = resources.getColor(R.color.second_background, null)


        findAllViewById()
        initView()

    }

    private fun findAllViewById() {
        songRecyclerView = findViewById(R.id.main_playlist)
        bottomMiniPlayer = findViewById(R.id.main_bottom_mini_player)
    }

    private fun initView() {
        songRecyclerView?.layoutManager = LinearLayoutManager(this)
        songAdapter = SongAdapter().apply {
            submitList(
                mutableListOf(
                    SongData("2", "3", "3", "3"),
                    SongData("3", "3", "3", "3"),
                    SongData("4", "3", "3", "3"),
                    SongData("5", "3", "3", "3"),
                    SongData("6", "3", "3", "3"),
                    SongData("7", "3", "3", "3"),
                    SongData("8", "3", "3", "3"),
                    SongData("9", "3", "3", "3"),
                    SongData("10", "3", "3", "3"),
                    SongData("11", "3", "3", "3"),
                    SongData("12", "3", "3", "3"),
                    SongData("13", "3", "3", "3"),
                    SongData("14", "3", "3", "3"),
                    SongData("15", "3", "3", "3"),
                    SongData("16", "3", "3", "3"),
                    SongData("17", "3", "3", "3"),
                    SongData("18", "3", "3", "3"),
                    SongData("19", "3", "3", "3"),
                    SongData("20", "3", "3", "3"),
                )
            )
            songRecyclerView?.adapter = this
        }

        bottomMiniPlayer?.setOnClickListener(this)
    }

    override fun onClick(view: View?) {
        view ?: return
        when (view.id) {
            R.id.main_bottom_mini_player -> {
                startActivity(Intent(this, PlayingActivity::class.java))
                overridePendingTransition(
                    R.anim.anim_slide_enter_bottom,
                    R.anim.anim_no_anim
                )
            }
        }
    }

}