package izumi.music_cloud

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import izumi.music_cloud.adapter.SongAdapter
import izumi.music_cloud.adapter.SongData

class MainActivity : AppCompatActivity() {

    private var songRecyclerView:RecyclerView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_main)

        initView()

    }


    private fun initView() {
        songRecyclerView = findViewById(R.id.rv_playlist)
        songRecyclerView?.layoutManager = LinearLayoutManager(this)
        val adapter = SongAdapter().apply {
            submitList(
                mutableListOf(
                    SongData("2","3","3","3"),
                    SongData("3","3","3","3"),
                    SongData("4","3","3","3"),
                    SongData("5","3","3","3"),
                    SongData("6","3","3","3"),
                    SongData("7","3","3","3"),
                    SongData("8","3","3","3"),
                    SongData("9","3","3","3"),
                    SongData("10","3","3","3"),
                    SongData("11","3","3","3"),
                    SongData("12","3","3","3"),
                    SongData("13","3","3","3"),
                    SongData("14","3","3","3"),
                    SongData("15","3","3","3"),
                    SongData("16","3","3","3"),
                    SongData("17","3","3","3"),
                    SongData("18","3","3","3"),
                    SongData("19","3","3","3"),
                    SongData("20","3","3","3"),
                )
            )
        }

        songRecyclerView?.adapter = adapter
    }


}