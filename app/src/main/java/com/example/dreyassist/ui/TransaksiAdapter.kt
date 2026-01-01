package com.example.dreyassist.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.dreyassist.R
import com.example.dreyassist.data.TransaksiEntity
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TransaksiAdapter : ListAdapter<TransaksiEntity, TransaksiAdapter.TransaksiViewHolder>(TRANSAKSI_COMPARATOR) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransaksiViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaksi, parent, false)
        return TransaksiViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransaksiViewHolder, position: Int) {
        val current = getItem(position)
        holder.bind(current)
    }

    class TransaksiViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val keperluanItemView: TextView = itemView.findViewById(R.id.text_view_keperluan)
        private val totalItemView: TextView = itemView.findViewById(R.id.text_view_total)
        private val tanggalItemView: TextView = itemView.findViewById(R.id.text_view_tanggal)

        private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
            maximumFractionDigits = 0
        }

        fun bind(transaksi: TransaksiEntity) {
            keperluanItemView.text = transaksi.keperluan
            totalItemView.text = currencyFormat.format(transaksi.total)
            tanggalItemView.text = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID")).format(Date(transaksi.tanggal))
        }
    }

    companion object {
        private val TRANSAKSI_COMPARATOR = object : DiffUtil.ItemCallback<TransaksiEntity>() {
            override fun areItemsTheSame(oldItem: TransaksiEntity, newItem: TransaksiEntity): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: TransaksiEntity, newItem: TransaksiEntity): Boolean {
                return oldItem == newItem
            }
        }
    }
}
