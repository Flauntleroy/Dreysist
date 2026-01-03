package com.example.dreyassist.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.dreyassist.R
import com.example.dreyassist.data.TransaksiEntity
import com.example.dreyassist.util.CategoryDetector
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TransaksiListAdapter(
    private val onClick: (TransaksiEntity) -> Unit,
    private val onEdit: (TransaksiEntity) -> Unit,
    private val onDelete: (TransaksiEntity) -> Unit
) : ListAdapter<TransaksiEntity, TransaksiListAdapter.ViewHolder>(DiffCallback()) {

    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault()).apply {
        maximumFractionDigits = 0
    }
    private val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_list_with_actions, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imgCategory: ImageView = itemView.findViewById(R.id.img_category)
        private val textTitle: TextView = itemView.findViewById(R.id.text_title)
        private val textSubtitle: TextView = itemView.findViewById(R.id.text_subtitle)
        private val textDate: TextView = itemView.findViewById(R.id.text_date)
        private val btnMore: ImageButton = itemView.findViewById(R.id.btn_more)

        fun bind(item: TransaksiEntity) {
            // Show category icon
            imgCategory.visibility = View.VISIBLE
            imgCategory.setImageResource(CategoryDetector.getCategoryIconResId(item.category))
            
            textTitle.text = item.keperluan
            textSubtitle.text = currencyFormat.format(item.total)
            textDate.text = dateFormat.format(Date(item.tanggal))

            // Card click for detail view
            itemView.setOnClickListener { onClick(item) }

            // More button with popup menu
            btnMore.setOnClickListener { view ->
                val popup = PopupMenu(view.context, view)
                popup.menuInflater.inflate(R.menu.menu_item_actions, popup.menu)
                popup.setOnMenuItemClickListener { menuItem ->
                    when (menuItem.itemId) {
                        R.id.action_edit -> {
                            onEdit(item)
                            true
                        }
                        R.id.action_delete -> {
                            onDelete(item)
                            true
                        }
                        else -> false
                    }
                }
                popup.show()
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<TransaksiEntity>() {
        override fun areItemsTheSame(oldItem: TransaksiEntity, newItem: TransaksiEntity) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: TransaksiEntity, newItem: TransaksiEntity) = oldItem == newItem
    }
}
