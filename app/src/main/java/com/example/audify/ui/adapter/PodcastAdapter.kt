package com.example.audify.ui.adapter

import android.graphics.Paint
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import com.example.audify.R
import com.example.audify.SessionManager
import com.example.audify.databinding.ItemPodcastBinding
import com.example.audify.model.Podcast

class PodcastAdapter(
    private val items: List<Podcast>,
    private val onItemClick: ((Podcast) -> Unit)? = null,
    private val onFavoriteClick: ((Podcast) -> Unit)? = null,
    private val onAuthorClick: ((Podcast) -> Unit)? = null,
    private val contentTopPaddingDp: Int? = null,
    private val cardTopMarginDp: Int? = null,
    favoriteIds: Set<String> = emptySet()
) : RecyclerView.Adapter<PodcastAdapter.ViewHolder>() {

    private val favoriteIds = favoriteIds.toMutableSet()

    fun updateFavoriteIds(ids: Set<String>) {
        favoriteIds.clear()
        favoriteIds.addAll(ids)
        notifyDataSetChanged()
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
        private val basePaddingStart = binding.contentContainer.paddingStart
        private val basePaddingTop = binding.contentContainer.paddingTop
        private val basePaddingEnd = binding.contentContainer.paddingEnd
        private val basePaddingBottom = binding.contentContainer.paddingBottom
        private val baseCardTopMargin = (binding.root.layoutParams as? ViewGroup.MarginLayoutParams)?.topMargin ?: 0

        fun bind(podcast: Podcast) {
            val resolvedTopPadding = contentTopPaddingDp?.let { dp ->
                TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    dp.toFloat(),
                    binding.root.resources.displayMetrics
                ).toInt()
            } ?: basePaddingTop
            binding.contentContainer.setPaddingRelative(
                basePaddingStart,
                resolvedTopPadding,
                basePaddingEnd,
                basePaddingBottom
            )

            val params = binding.root.layoutParams as? ViewGroup.MarginLayoutParams
            if (params != null) {
                val resolvedTopMargin = cardTopMarginDp?.let { dp ->
                    TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        dp.toFloat(),
                        binding.root.resources.displayMetrics
                    ).toInt()
                } ?: baseCardTopMargin
                if (params.topMargin != resolvedTopMargin) {
                    params.topMargin = resolvedTopMargin
                    binding.root.layoutParams = params
                }
            }

            binding.tvTitle.text = podcast.title
            binding.tvAuthor.text = podcast.author
            binding.tvDescription.text = podcast.description

            val canOpenAuthor = onAuthorClick != null && podcast.userId.isNotBlank()
            binding.tvAuthor.isClickable = canOpenAuthor
            binding.tvAuthor.isFocusable = canOpenAuthor
            binding.tvAuthor.paintFlags = if (canOpenAuthor) {
                binding.tvAuthor.paintFlags or Paint.UNDERLINE_TEXT_FLAG
            } else {
                binding.tvAuthor.paintFlags and Paint.UNDERLINE_TEXT_FLAG.inv()
            }
            binding.tvAuthor.setOnClickListener {
                if (canOpenAuthor) {
                    onAuthorClick?.invoke(podcast)
                }
            }

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

            binding.btnFavorite.setImageResource(
                if (favoriteIds.contains(podcast.supabaseId)) R.drawable.ic_favorite
                else R.drawable.ic_favorite_border
            )
            binding.btnFavorite.setOnClickListener {
                if (SessionManager.isLoggedIn()) {
                    onFavoriteClick?.invoke(podcast)
                } else {
                    binding.btnFavorite.setImageResource(R.drawable.ic_favorite_border)
                    binding.btnFavorite.alpha = 0.4f
                }
            }

            itemView.setOnClickListener { onItemClick?.invoke(podcast) }
        }
    }
}
