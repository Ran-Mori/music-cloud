package izumi.music_cloud.recycler

import androidx.recyclerview.widget.DiffUtil

object DiffCallback : DiffUtil.ItemCallback<SongData>() {
    override fun areItemsTheSame(oldItem: SongData, newItem: SongData): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: SongData, newItem: SongData): Boolean {
        return oldItem == newItem
    }
}