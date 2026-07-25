package com.example.audify.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.audify.R
import com.example.audify.databinding.ItemCategoryGridBinding
import com.google.android.material.card.MaterialCardView

data class CategoryUiItem(
    val key: String,
    val title: String,
    val count: Int
)

class CategoryAdapter(
    private val categories: List<CategoryUiItem>,
    private val selectedKey: String?,
    private val onCategoryClick: (CategoryUiItem) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCategoryGridBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(categories[position])
    }

    override fun getItemCount() = categories.size

    inner class ViewHolder(private val binding: ItemCategoryGridBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(category: CategoryUiItem) {
            val context = binding.root.context
            val isSelected = category.key == selectedKey
            val card = binding.root as MaterialCardView

            card.setCardBackgroundColor(
                ContextCompat.getColor(context, if (isSelected) R.color.violet_light else android.R.color.white)
            )
            card.strokeColor = ContextCompat.getColor(context, if (isSelected) R.color.violet_primary else R.color.violet_light)

            binding.tvCategoryName.text = category.title
            binding.tvCategoryCount.text = "${category.count} podcast${if (category.count != 1) "s" else ""}"

            binding.root.setOnClickListener {
                onCategoryClick(category)
            }
        }
    }
}
