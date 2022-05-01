package izumi.music_cloud.recycler

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.facebook.drawee.view.SimpleDraweeView
import izumi.music_cloud.R

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
            return@setOnLongClickListener true
        }

        wholeView.setOnClickListener {

        }

        memu.setOnClickListener {

        }
    }
}