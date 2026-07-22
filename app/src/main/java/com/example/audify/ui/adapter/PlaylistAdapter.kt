package com.example.audify.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.audify.data.MockData
import com.example.audify.databinding.ItemPlaylistBinding
import com.example.audify.model.Playlist

class PlaylistAdapter(
    private val items: List<Playlist>,
    private val onItemClick: (Playlist) -> Unit
) : RecyclerView.Adapter<PlaylistAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPlaylistBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    inner class ViewHolder(private val binding: ItemPlaylistBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(playlist: Playlist) {
            val count = playlist.podcastIds.size
            binding.tvListName.text = playlist.name
            binding.tvListCount.text = "$count podcast${if (count != 1) "s" else ""}"
            binding.root.setOnClickListener { onItemClick(playlist) }
        }
    }
}
