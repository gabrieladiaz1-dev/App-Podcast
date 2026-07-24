package com.example.audify.ui.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.audify.LoginActivity
import com.example.audify.R
import com.example.audify.SessionManager
import com.example.audify.data.MockData
import com.example.audify.databinding.ItemPodcastBinding
import com.example.audify.model.Podcast

class PodcastAdapter(
    private val items: List<Podcast>,
    private val onItemClick: ((Podcast) -> Unit)? = null,
    private val onFavoriteClick: ((Podcast) -> Unit)? = null
) : RecyclerView.Adapter<PodcastAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPodcastBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    inner class ViewHolder(private val binding: ItemPodcastBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(podcast: Podcast) {
            binding.tvTitle.text = podcast.title
            binding.tvAuthor.text = podcast.author
            binding.tvDescription.text = podcast.description
            binding.tvDuration.text = podcast.duration

            if (SessionManager.isLoggedIn()) {
                val isFav = MockData.isFavorite(podcast.id)
                binding.btnFavorite.setImageResource(
                    if (isFav) R.drawable.ic_favorite
                    else R.drawable.ic_favorite_border
                )
                binding.btnFavorite.setOnClickListener {
                    MockData.toggleFavorite(podcast.id)
                    val nowFav = MockData.isFavorite(podcast.id)
                    binding.btnFavorite.setImageResource(
                        if (nowFav) R.drawable.ic_favorite
                        else R.drawable.ic_favorite_border
                    )
                    onFavoriteClick?.invoke(podcast)
                }
            } else {
                binding.btnFavorite.setImageResource(R.drawable.ic_favorite_border)
                binding.btnFavorite.alpha = 0.4f
                binding.btnFavorite.setOnClickListener {
                    Toast.makeText(binding.root.context, "Ingresa para guardar favoritos", Toast.LENGTH_SHORT).show()
                    binding.root.context.startActivity(Intent(binding.root.context, LoginActivity::class.java))
                }
            }

            itemView.setOnClickListener { onItemClick?.invoke(podcast) }
        }
    }
}
