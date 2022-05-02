package izumi.music_cloud.recycler

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.recyclerview.widget.ListAdapter
import izumi.music_cloud.R
import izumi.music_cloud.callback.ViewHolderCallback

class SongAdapter(private val holderCallback: ViewHolderCallback) : ListAdapter<SongData, SongViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        return SongViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.recycler_song_item, parent, false),
            holderCallback
        )
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        setAnimation(holder.itemView)
        val item = currentList[position]
        holder.songTitle.text = item.title
        holder.songArtist.text = item.artist
        holder.hasDownloaded.visibility = if (item.downloaded) {
            View.VISIBLE
        } else {
            View.GONE
        }

        holder.wholeView.setOnClickListener {
            holderCallback.onSingleClick(position)
        }
    }

    private fun setAnimation(viewToAnimate: View) {
        val animation: Animation =
            AnimationUtils.loadAnimation(viewToAnimate.context, R.anim.anim_recycle_item)
        viewToAnimate.startAnimation(animation)
    }
}