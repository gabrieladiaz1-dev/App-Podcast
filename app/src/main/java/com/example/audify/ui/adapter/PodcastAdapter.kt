package com.example.audify.ui.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import com.example.audify.LoginActivity
import com.example.audify.R
import com.example.audify.SessionManager
import com.example.audify.SupabaseService
import com.example.audify.databinding.ItemPodcastBinding
import com.example.audify.model.Podcast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PodcastAdapter(
    private val items: List<Podcast>,
    private val onItemClick: ((Podcast) -> Unit)? = null,
    private val onFavoriteClick: ((Podcast) -> Unit)? = null
) : RecyclerView.Adapter<PodcastAdapter.ViewHolder>() {

    private val favoriteIds = mutableSetOf<String>()

    init {
        loadFavorites()
    }

    private fun loadFavorites() {
        val userId = SessionManager.getUserId() ?: return
        CoroutineScope(Dispatchers.IO).launch {
            for (podcast in items) {
                try {
                    val isFav = SupabaseService.isFavorited(userId, podcast.supabaseId)
                    if (isFav) favoriteIds.add(podcast.supabaseId)
                } catch (_: Exception) {}
            }
            CoroutineScope(Dispatchers.Main).launch {
                notifyDataSetChanged()
            }
        }
    }

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

            if (!podcast.coverUrl.isNullOrEmpty()) {
                binding.ivThumbnail.load(podcast.coverUrl) {
                    crossfade(true)
                    placeholder(R.drawable.bg_circle_violet)
                    error(R.drawable.ic_audify_logo)
                    transformations(CircleCropTransformation())
                }
            } else {
                binding.ivThumbnail.setImageResource(R.drawable.ic_audify_logo)
            }

            if (podcast.duration.isNotEmpty()) {
                binding.tvDuration.text = podcast.duration
                binding.tvDuration.visibility = android.view.View.VISIBLE
            } else {
                binding.tvDuration.visibility = android.view.View.GONE
            }

            if (SessionManager.isLoggedIn()) {
                val isFav = favoriteIds.contains(podcast.supabaseId)
                binding.btnFavorite.setImageResource(
                    if (isFav) R.drawable.ic_favorite
                    else R.drawable.ic_favorite_border
                )
                binding.btnFavorite.setOnClickListener {
                    val userId = SessionManager.getUserId() ?: return@setOnClickListener
                    val podcastIdStr = podcast.supabaseId
                    if (isFav) {
                        favoriteIds.remove(podcastIdStr)
                        binding.btnFavorite.setImageResource(R.drawable.ic_favorite_border)
                        CoroutineScope(Dispatchers.IO).launch {
                            SupabaseService.removeFavorite(userId, podcastIdStr)
                        }
                    } else {
                        favoriteIds.add(podcastIdStr)
                        binding.btnFavorite.setImageResource(R.drawable.ic_favorite)
                        CoroutineScope(Dispatchers.IO).launch {
                            SupabaseService.addFavorite(
                                SupabaseService.Favorite(
                                    user_id = userId,
                                    podcast_id = podcastIdStr
                                )
                            )
                        }
                    }
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
