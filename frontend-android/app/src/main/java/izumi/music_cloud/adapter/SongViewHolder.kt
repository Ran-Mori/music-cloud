package izumi.music_cloud.adapter

import android.app.Activity
import android.content.Intent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.facebook.drawee.view.SimpleDraweeView
import izumi.music_cloud.R
import izumi.music_cloud.activity.PlayingActivity

class SongViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    private val context = view.context

    val wholeView = view.findViewById<ViewGroup>(R.id.item_song_whole_view)
    val songCover = view.findViewById<SimpleDraweeView>(R.id.item_song_cover)
    val songTitle = view.findViewById<TextView>(R.id.item_song_title)
    val songArtist = view.findViewById<TextView>(R.id.item_song_artist)
    val hasDownloaded = view.findViewById<ImageView>(R.id.item_have_downloaded)
    val memu = view.findViewById<ImageView>(R.id.item_menu)


    init {
        wholeView.setOnLongClickListener {
            context.startActivity(Intent(context, PlayingActivity::class.java))
            (context as? Activity)?.overridePendingTransition(
                R.anim.anim_slide_enter_bottom,
                R.anim.anim_no_anim
            )

            return@setOnLongClickListener true
        }

        wholeView.setOnClickListener {

        }

        memu.setOnClickListener {

        }
    }
}