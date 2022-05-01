package izumi.music_cloud.recycler

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.recyclerview.widget.ListAdapter
import izumi.music_cloud.R

class SongAdapter : ListAdapter<SongData, SongViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        return SongViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.recycler_song_item, parent, false)
        )
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        setAnimation(holder.itemView)

        val item = currentList[position]
        holder.songTitle.text = item.title
        holder.songArtist.text = item.artist
    }

    private fun setAnimation(viewToAnimate: View) {
        // If the bound view wasn't previously displayed on screen, it's animated
        val animation: Animation =
            AnimationUtils.loadAnimation(viewToAnimate.context, R.anim.anim_recycle_item)
        viewToAnimate.startAnimation(animation)
    }
}