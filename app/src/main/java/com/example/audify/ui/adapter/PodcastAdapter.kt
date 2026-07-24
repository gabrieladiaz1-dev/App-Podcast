package com.example.audify.ui.adapter

import android.content.Intent
import android.util.Log
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PodcastAdapter(
    private val items: List<Podcast>,
    private val onItemClick: ((Podcast) -> Unit)? = null,
    private val onFavoriteClick: ((Podcast) -> Unit)? = null
) : RecyclerView.Adapter<PodcastAdapter.ViewHolder>() {

    private val favoriteIds = mutableSetOf<String>()
    private val adapterScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    init {
        loadFavorites()
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        adapterScope.coroutineContext[Job]?.cancelChildren()
    }

    private fun loadFavorites() {
        val userId = SessionManager.getUserId() ?: return
        adapterScope.launch {
            val ids = withContext(Dispatchers.IO) {
                items.mapNotNull { podcast ->
                    try {
                        if (SupabaseService.isFavorited(userId, podcast.supabaseId)) podcast.supabaseId else null
                    } catch (_: Exception) { null }
                }.toSet()
            }
            favoriteIds.addAll(ids)
            notifyDataSetChanged()
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

            if (!podcast.approved) {
                binding.tvDuration.text = "En revisión"
                binding.tvDuration.visibility = android.view.View.VISIBLE
                binding.tvDuration.setTextColor(0xFFD32F2F.toInt())
                binding.tvDuration.setBackgroundResource(R.drawable.bg_pill_pending)
            } else if (podcast.duration.isNotEmpty()) {
                binding.tvDuration.text = podcast.duration
                binding.tvDuration.visibility = android.view.View.VISIBLE
                binding.tvDuration.setTextColor(binding.root.context.getColor(R.color.purple))
                binding.tvDuration.setBackgroundResource(R.drawable.bg_pill)
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
                    binding.btnFavorite.isEnabled = false
                    adapterScope.launch {
                        val currentlyFav = withContext(Dispatchers.IO) {
                            SupabaseService.isFavorited(userId, podcastIdStr)
                        }
                        val result = withContext(Dispatchers.IO) {
                            if (currentlyFav) {
                                SupabaseService.removeFavorite(userId, podcastIdStr)
                            } else {
                                SupabaseService.addFavorite(userId, podcastIdStr)
                            }
                        }
                        binding.btnFavorite.isEnabled = true
                        if (result.isSuccess) {
                            if (currentlyFav) {
                                favoriteIds.remove(podcastIdStr)
                                binding.btnFavorite.setImageResource(R.drawable.ic_favorite_border)
                            } else {
                                favoriteIds.add(podcastIdStr)
                                binding.btnFavorite.setImageResource(R.drawable.ic_favorite)
                            }
                        } else {
                            val err = result.exceptionOrNull()
                            if (err is SupabaseService.SessionExpiredException) {
                                Toast.makeText(
                                    binding.root.context,
                                    "Tu sesión expiró. Inicia sesión de nuevo.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                Log.e("PodcastAdapter", "Error favorito: ${err?.message}", err)
                                Toast.makeText(
                                    binding.root.context,
                                    "No pudimos actualizar el favorito. Intenta de nuevo",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
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
