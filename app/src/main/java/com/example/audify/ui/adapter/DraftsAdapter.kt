package com.example.audify.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.audify.data.DraftsManager
import com.example.audify.databinding.ItemDraftBinding

class DraftsAdapter(
    private val items: List<DraftsManager.Draft>,
    private val onItemClick: (DraftsManager.Draft) -> Unit,
    private val onDeleteClick: (DraftsManager.Draft) -> Unit
) : RecyclerView.Adapter<DraftsAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDraftBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    inner class ViewHolder(private val binding: ItemDraftBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(draft: DraftsManager.Draft) {
            binding.txtDraftTitle.text = draft.title.ifEmpty { "Sin título" }
            binding.txtDraftInfo.text = draft.audioFileName
            binding.btnDeleteDraft.setOnClickListener { onDeleteClick(draft) }
            itemView.setOnClickListener { onItemClick(draft) }
        }
    }
}
