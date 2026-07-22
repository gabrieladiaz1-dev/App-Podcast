package com.example.audify.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.audify.data.MockData
import com.example.audify.databinding.ItemCategoryGridBinding

class CategoryAdapter(
    private val categories: List<String>
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
        fun bind(category: String) {
            val count = MockData.getPodcasts().count { it.category == category }
            binding.tvCategoryName.text = category
            binding.tvCategoryCount.text = "$count podcast${if (count != 1) "s" else ""}"
        }
    }
}
