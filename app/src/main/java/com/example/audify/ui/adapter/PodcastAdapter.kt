package com.example.audify.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.audify.databinding.ItemPodcastBinding
import com.example.audify.model.Podcast

class PodcastAdapter(
    private val items: List<Podcast>
) : RecyclerView.Adapter<PodcastAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPodcastBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    class ViewHolder(private val binding: ItemPodcastBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(podcast: Podcast) {
            binding.tvTitle.text = podcast.title
            binding.tvAuthor.text = podcast.author
            binding.tvDescription.text = podcast.description
            binding.tvDuration.text = podcast.duration
        }
    }
}
