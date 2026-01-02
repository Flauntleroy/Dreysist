package com.example.dreyassist.util

import com.example.dreyassist.data.JournalDao
import com.example.dreyassist.data.MemoryDao
import com.example.dreyassist.data.ReminderDao
import com.example.dreyassist.data.TransaksiDao
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * QueryHandler - Processes voice queries and returns answers
 * Part of the REMEMBER feature
 */
class QueryHandler(
    private val transaksiDao: TransaksiDao,
    private val journalDao: JournalDao,
    private val reminderDao: ReminderDao,
    private val memoryDao: MemoryDao
) {
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
        maximumFractionDigits = 0
    }
    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))

    enum class QueryType {
        TOTAL_SPENDING,
        TODAY_SPENDING,
        MONTH_SPENDING,
        LAST_TRANSACTION,
        HIGHEST_TRANSACTION,
        SEARCH_TRANSACTION,
        LAST_JOURNAL,
        TODAY_JOURNAL,
        JOURNAL_COUNT,
        SEARCH_JOURNAL,
        ACTIVE_REMINDERS,
        SEARCH_NOTES,
        UNKNOWN
    }

    data class QueryResult(
        val success: Boolean,
        val answer: String,
        val queryType: QueryType
    )

    suspend fun processQuery(queryText: String): QueryResult {
        val lowercased = queryText.lowercase(Locale.ROOT)
        
        return when {
            // Spending queries
            lowercased.contains("total pengeluaran") || lowercased.contains("total spending") -> 
                getTotalSpending()
            
            lowercased.contains("pengeluaran hari ini") || lowercased.contains("spending today") ||
            lowercased.contains("hari ini habis berapa") || lowercased.contains("hari ini keluar") ->
                getTodaySpending()
            
            lowercased.contains("pengeluaran bulan ini") || lowercased.contains("bulan ini habis") ->
                getMonthSpending()
            
            lowercased.contains("transaksi terakhir") || lowercased.contains("last transaction") ||
            lowercased.contains("terakhir beli") || lowercased.contains("beli terakhir") ->
                getLastTransaction()
            
            lowercased.contains("termahal") || lowercased.contains("paling mahal") ||
            lowercased.contains("terbesar") || lowercased.contains("highest") ->
                getHighestTransaction()
            
            // Journal queries
            lowercased.contains("jurnal terakhir") || lowercased.contains("kegiatan terakhir") ||
            lowercased.contains("aktivitas terakhir") ->
                getLastJournal()
            
            lowercased.contains("hari ini ngapain") || lowercased.contains("tadi ngapain") ||
            lowercased.contains("jurnal hari ini") ->
                getTodayJournals()
            
            lowercased.contains("berapa jurnal") || lowercased.contains("ada berapa jurnal") ->
                getJournalCount()
            
            // Reminder queries  
            lowercased.contains("pengingat aktif") || lowercased.contains("reminder aktif") ||
            lowercased.contains("ada pengingat") || lowercased.contains("berapa pengingat") ->
                getActiveReminders()
            
            // Search queries
            lowercased.contains("cari transaksi") || lowercased.contains("pernah beli") ->
                searchTransaction(extractSearchTerm(lowercased, listOf("cari transaksi", "pernah beli")))
            
            lowercased.contains("cari jurnal") || lowercased.contains("cari kegiatan") ->
                searchJournal(extractSearchTerm(lowercased, listOf("cari jurnal", "cari kegiatan")))
            
            lowercased.contains("cari catatan") || lowercased.contains("password") || 
            lowercased.contains("pin apa") || lowercased.contains("catatan tentang") ->
                searchNotes(extractSearchTerm(lowercased, listOf("cari catatan", "catatan tentang", "password", "pin")))
            
            else -> QueryResult(false, "Maaf, saya belum bisa menjawab pertanyaan itu.", QueryType.UNKNOWN)
        }
    }

    private suspend fun getTotalSpending(): QueryResult {
        val total = transaksiDao.getTotalSpending() ?: 0
        val count = transaksiDao.getCount()
        return QueryResult(
            success = true,
            answer = "Total pengeluaran kamu adalah ${currencyFormat.format(total)} dari $count transaksi.",
            queryType = QueryType.TOTAL_SPENDING
        )
    }

    private suspend fun getTodaySpending(): QueryResult {
        val today = getStartOfDay()
        val total = transaksiDao.getSpendingSince(today) ?: 0
        return if (total > 0) {
            QueryResult(
                success = true,
                answer = "Pengeluaran hari ini: ${currencyFormat.format(total)}",
                queryType = QueryType.TODAY_SPENDING
            )
        } else {
            QueryResult(
                success = true,
                answer = "Belum ada pengeluaran hari ini.",
                queryType = QueryType.TODAY_SPENDING
            )
        }
    }

    private suspend fun getMonthSpending(): QueryResult {
        val startOfMonth = getStartOfMonth()
        val total = transaksiDao.getSpendingSince(startOfMonth) ?: 0
        return QueryResult(
            success = true,
            answer = "Pengeluaran bulan ini: ${currencyFormat.format(total)}",
            queryType = QueryType.MONTH_SPENDING
        )
    }

    private suspend fun getLastTransaction(): QueryResult {
        val transaction = transaksiDao.getLatest()
        return if (transaction != null) {
            QueryResult(
                success = true,
                answer = "Transaksi terakhir: ${transaction.keperluan} sebesar ${currencyFormat.format(transaction.total)} pada ${dateFormat.format(Date(transaction.tanggal))}.",
                queryType = QueryType.LAST_TRANSACTION
            )
        } else {
            QueryResult(
                success = true,
                answer = "Belum ada transaksi yang tercatat.",
                queryType = QueryType.LAST_TRANSACTION
            )
        }
    }

    private suspend fun getHighestTransaction(): QueryResult {
        val transaction = transaksiDao.getHighestTransaction()
        return if (transaction != null) {
            QueryResult(
                success = true,
                answer = "Transaksi terbesar: ${transaction.keperluan} sebesar ${currencyFormat.format(transaction.total)}.",
                queryType = QueryType.HIGHEST_TRANSACTION
            )
        } else {
            QueryResult(
                success = true,
                answer = "Belum ada transaksi yang tercatat.",
                queryType = QueryType.HIGHEST_TRANSACTION
            )
        }
    }

    private suspend fun getLastJournal(): QueryResult {
        val journal = journalDao.getLatest()
        return if (journal != null) {
            QueryResult(
                success = true,
                answer = "Jurnal terakhir (${dateFormat.format(Date(journal.tanggal))}): ${journal.kegiatan}",
                queryType = QueryType.LAST_JOURNAL
            )
        } else {
            QueryResult(
                success = true,
                answer = "Belum ada jurnal yang tercatat.",
                queryType = QueryType.LAST_JOURNAL
            )
        }
    }

    private suspend fun getTodayJournals(): QueryResult {
        val today = getStartOfDay()
        val journals = journalDao.getJournalsSince(today)
        return if (journals.isNotEmpty()) {
            val activities = journals.joinToString(", ") { it.kegiatan }
            QueryResult(
                success = true,
                answer = "Hari ini kamu mencatat ${journals.size} kegiatan: $activities",
                queryType = QueryType.TODAY_JOURNAL
            )
        } else {
            QueryResult(
                success = true,
                answer = "Belum ada catatan kegiatan hari ini.",
                queryType = QueryType.TODAY_JOURNAL
            )
        }
    }

    private suspend fun getJournalCount(): QueryResult {
        val count = journalDao.getCount()
        return QueryResult(
            success = true,
            answer = "Kamu memiliki $count jurnal yang tercatat.",
            queryType = QueryType.JOURNAL_COUNT
        )
    }

    private suspend fun getActiveReminders(): QueryResult {
        val reminders = reminderDao.getActiveList()
        return if (reminders.isNotEmpty()) {
            val reminderList = reminders.take(3).joinToString("; ") { it.content }
            QueryResult(
                success = true,
                answer = "Ada ${reminders.size} pengingat aktif: $reminderList",
                queryType = QueryType.ACTIVE_REMINDERS
            )
        } else {
            QueryResult(
                success = true,
                answer = "Tidak ada pengingat yang aktif.",
                queryType = QueryType.ACTIVE_REMINDERS
            )
        }
    }

    private suspend fun searchTransaction(searchTerm: String): QueryResult {
        if (searchTerm.isBlank()) {
            return QueryResult(false, "Cari transaksi tentang apa?", QueryType.SEARCH_TRANSACTION)
        }
        val results = transaksiDao.searchByKeperluanList("%$searchTerm%")
        return if (results.isNotEmpty()) {
            val total = results.sumOf { it.total }
            QueryResult(
                success = true,
                answer = "Ditemukan ${results.size} transaksi \"$searchTerm\" dengan total ${currencyFormat.format(total)}.",
                queryType = QueryType.SEARCH_TRANSACTION
            )
        } else {
            QueryResult(
                success = true,
                answer = "Tidak ditemukan transaksi dengan kata \"$searchTerm\".",
                queryType = QueryType.SEARCH_TRANSACTION
            )
        }
    }

    private suspend fun searchJournal(searchTerm: String): QueryResult {
        if (searchTerm.isBlank()) {
            return QueryResult(false, "Cari jurnal tentang apa?", QueryType.SEARCH_JOURNAL)
        }
        val results = journalDao.searchByKegiatanList("%$searchTerm%")
        return if (results.isNotEmpty()) {
            QueryResult(
                success = true,
                answer = "Ditemukan ${results.size} jurnal tentang \"$searchTerm\".",
                queryType = QueryType.SEARCH_JOURNAL
            )
        } else {
            QueryResult(
                success = true,
                answer = "Tidak ditemukan jurnal dengan kata \"$searchTerm\".",
                queryType = QueryType.SEARCH_JOURNAL
            )
        }
    }

    private suspend fun searchNotes(searchTerm: String): QueryResult {
        if (searchTerm.isBlank()) {
            return QueryResult(false, "Cari catatan tentang apa?", QueryType.SEARCH_NOTES)
        }
        val results = memoryDao.searchList("%$searchTerm%")
        return if (results.isNotEmpty()) {
            val firstResult = results.first()
            QueryResult(
                success = true,
                answer = "Ditemukan: ${firstResult.content}",
                queryType = QueryType.SEARCH_NOTES
            )
        } else {
            QueryResult(
                success = true,
                answer = "Tidak ditemukan catatan tentang \"$searchTerm\".",
                queryType = QueryType.SEARCH_NOTES
            )
        }
    }

    private fun extractSearchTerm(text: String, prefixes: List<String>): String {
        var result = text
        for (prefix in prefixes) {
            result = result.replace(prefix, "").trim()
        }
        return result.trim()
    }

    private fun getStartOfDay(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun getStartOfMonth(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
