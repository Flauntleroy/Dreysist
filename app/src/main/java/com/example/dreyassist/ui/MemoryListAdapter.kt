package com.example.dreyassist.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.dreyassist.R
import com.example.dreyassist.data.MemoryEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MemoryListAdapter(
    private val onEdit: (MemoryEntity) -> Unit,
    private val onDelete: (MemoryEntity) -> Unit
) : ListAdapter<MemoryEntity, MemoryListAdapter.ViewHolder>(DiffCallback()) {

    private val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID"))

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_list_with_actions, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textTitle: TextView = itemView.findViewById(R.id.text_title)
        private val textSubtitle: TextView = itemView.findViewById(R.id.text_subtitle)
        private val textDate: TextView = itemView.findViewById(R.id.text_date)
        private val btnEdit: ImageButton = itemView.findViewById(R.id.btn_edit)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btn_delete)

        fun bind(item: MemoryEntity) {
            textTitle.text = item.content
            textSubtitle.text = if (item.category.isNotBlank()) item.category else "Catatan"
            textDate.text = dateFormat.format(Date(item.createdAt))

            btnEdit.setOnClickListener { onEdit(item) }
            btnDelete.setOnClickListener { onDelete(item) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<MemoryEntity>() {
        override fun areItemsTheSame(oldItem: MemoryEntity, newItem: MemoryEntity) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: MemoryEntity, newItem: MemoryEntity) = oldItem == newItem
    }
}
