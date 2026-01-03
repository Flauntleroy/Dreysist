package com.example.dreyassist.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.dreyassist.R

class HistoryAdapter(
    private val onItemClick: ((HistoryItem) -> Unit)? = null
) : ListAdapter<HistoryItem, HistoryAdapter.HistoryViewHolder>(HistoryDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history, parent, false)
        return HistoryViewHolder(view, onItemClick)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class HistoryViewHolder(
        itemView: View,
        private val onItemClick: ((HistoryItem) -> Unit)?
    ) : RecyclerView.ViewHolder(itemView) {
        private val textType: TextView = itemView.findViewById(R.id.text_type)
        private val textTitle: TextView = itemView.findViewById(R.id.text_title)
        private val textSubtitle: TextView = itemView.findViewById(R.id.text_subtitle)
        private val textDate: TextView = itemView.findViewById(R.id.text_date)

        private val dateFormat = java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", java.util.Locale("id", "ID"))

        fun bind(item: HistoryItem) {
            textType.text = when (item.type) {
                ItemType.TRANSAKSI -> "T"
                ItemType.JURNAL -> "J"
                ItemType.PENGINGAT -> "P"
                ItemType.MEMORY -> "M"
            }
            textTitle.text = item.title
            textSubtitle.text = item.subtitle
            textDate.text = dateFormat.format(java.util.Date(item.timestamp))
            
            itemView.setOnClickListener {
                onItemClick?.invoke(item)
            }
        }
    }

    class HistoryDiffCallback : DiffUtil.ItemCallback<HistoryItem>() {
        override fun areItemsTheSame(oldItem: HistoryItem, newItem: HistoryItem): Boolean {
            return oldItem.id == newItem.id && oldItem.type == newItem.type
        }

        override fun areContentsTheSame(oldItem: HistoryItem, newItem: HistoryItem): Boolean {
            return oldItem == newItem
        }
    }
}

