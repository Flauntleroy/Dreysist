package com.example.dreyassist.ui

import com.example.dreyassist.data.JournalEntity
import com.example.dreyassist.data.MemoryEntity
import com.example.dreyassist.data.ReminderEntity
import com.example.dreyassist.data.TransaksiEntity

enum class ItemType {
    TRANSAKSI,
    JURNAL,
    PENGINGAT,
    MEMORY
}

data class HistoryItem(
    val id: Int,
    val type: ItemType,
    val title: String,
    val subtitle: String,
    val timestamp: Long,
    val category: String? = null  // Transaction category (optional)
) {
    companion object {
        fun fromTransaksi(transaksi: TransaksiEntity): HistoryItem {
            return HistoryItem(
                id = transaksi.id,
                type = ItemType.TRANSAKSI,
                title = transaksi.keperluan,
                subtitle = "Rp ${String.format("%,d", transaksi.total).replace(',', '.')}",
                timestamp = transaksi.tanggal,
                category = transaksi.category
            )
        }

        fun fromJournal(journal: JournalEntity): HistoryItem {
            return HistoryItem(
                id = journal.id,
                type = ItemType.JURNAL,
                title = journal.kegiatan,
                subtitle = "Jurnal",
                timestamp = journal.tanggal
            )
        }

        fun fromReminder(reminder: ReminderEntity): HistoryItem {
            val status = if (reminder.isCompleted) "Selesai" else "Aktif"
            return HistoryItem(
                id = reminder.id,
                type = ItemType.PENGINGAT,
                title = reminder.content,
                subtitle = "Pengingat - $status",
                timestamp = reminder.reminderTime
            )
        }

        fun fromMemory(memory: MemoryEntity): HistoryItem {
            val categoryLabel = if (memory.category.isNotBlank()) memory.category else "Catatan"
            return HistoryItem(
                id = memory.id,
                type = ItemType.MEMORY,
                title = memory.content,
                subtitle = categoryLabel,
                timestamp = memory.createdAt
            )
        }
    }
}
