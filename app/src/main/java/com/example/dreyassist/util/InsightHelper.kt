package com.example.dreyassist.util

import java.text.NumberFormat
import java.util.Locale

/**
 * InsightHelper - Generates meaningful reflections for REFLECT feature
 * 
 * Memberikan insight singkat dan bermakna tentang aktivitas pengguna
 * tanpa membebani dengan grafik atau laporan yang rumit.
 */
object InsightHelper {

    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))

    /**
     * Generate daily spending insight
     */
    fun generateSpendingInsight(
        todaySpending: Int,
        avgDailySpending: Int
    ): String? {
        if (todaySpending == 0 && avgDailySpending == 0) return null
        
        return when {
            avgDailySpending == 0 -> {
                "Pengeluaran hari ini: ${formatCurrency(todaySpending)}"
            }
            todaySpending > avgDailySpending * 2 -> {
                "Hari ini kamu lebih boros dari biasanya (${formatCurrency(todaySpending)} vs rata-rata ${formatCurrency(avgDailySpending)})"
            }
            todaySpending > avgDailySpending * 1.5 -> {
                "Pengeluaran hari ini sedikit di atas rata-rata"
            }
            todaySpending > avgDailySpending -> {
                "Pengeluaran hari ini normal"
            }
            todaySpending < avgDailySpending * 0.5 && todaySpending > 0 -> {
                "Hari ini cukup hemat! Pengeluaran di bawah rata-rata"
            }
            todaySpending == 0 -> {
                "Belum ada pengeluaran hari ini"
            }
            else -> {
                "Pengeluaran hari ini: ${formatCurrency(todaySpending)}"
            }
        }
    }

    /**
     * Generate monthly comparison insight
     */
    fun generateMonthlyInsight(
        thisMonthSpending: Int,
        lastMonthSpending: Int
    ): String? {
        if (thisMonthSpending == 0 && lastMonthSpending == 0) return null
        
        if (lastMonthSpending == 0) {
            return "Total bulan ini: ${formatCurrency(thisMonthSpending)}"
        }
        
        val percentChange = ((thisMonthSpending - lastMonthSpending).toDouble() / lastMonthSpending * 100).toInt()
        
        return when {
            percentChange > 50 -> {
                "Pengeluaran bulan ini naik signifikan (+$percentChange%)"
            }
            percentChange > 20 -> {
                "Pengeluaran bulan ini meningkat (+$percentChange%)"
            }
            percentChange > 0 -> {
                "Pengeluaran bulan ini sedikit naik (+$percentChange%)"
            }
            percentChange < -30 -> {
                "Bulan ini lebih hemat ($percentChange%)"
            }
            percentChange < 0 -> {
                "Pengeluaran bulan ini turun ($percentChange%)"
            }
            else -> {
                "Pengeluaran bulan ini stabil"
            }
        }
    }

    /**
     * Generate activity insight based on journal entries
     */
    fun generateActivityInsight(
        todayJournalCount: Int,
        weekJournalCount: Int
    ): String? {
        return when {
            todayJournalCount >= 5 -> {
                "Hari yang produktif! Kamu sudah mencatat $todayJournalCount kegiatan"
            }
            todayJournalCount >= 3 -> {
                "Sudah mencatat $todayJournalCount kegiatan hari ini"
            }
            todayJournalCount == 1 -> {
                "Ada 1 catatan kegiatan hari ini"
            }
            todayJournalCount == 0 && weekJournalCount > 0 -> {
                "Belum ada catatan hari ini. Minggu ini ada $weekJournalCount catatan"
            }
            else -> null
        }
    }

    /**
     * Generate reminder insight
     */
    fun generateReminderInsight(
        pendingCount: Int,
        overdueCount: Int
    ): String? {
        return when {
            overdueCount > 0 -> {
                "Ada $overdueCount pengingat yang terlewat!"
            }
            pendingCount > 5 -> {
                "Ada $pendingCount pengingat yang menunggu. Cek prioritas kamu"
            }
            pendingCount > 0 -> {
                "Ada $pendingCount pengingat aktif"
            }
            else -> null
        }
    }

    /**
     * Generate time pattern insight
     */
    fun generateTimePatternInsight(
        mostActiveHour: Int?
    ): String? {
        if (mostActiveHour == null) return null
        
        val period = when (mostActiveHour) {
            in 5..10 -> "pagi"
            in 11..14 -> "siang"
            in 15..18 -> "sore"
            in 19..23 -> "malam"
            else -> "dini hari"
        }
        
        return "Kamu paling sering mencatat di jam $mostActiveHour:00 ($period)"
    }

    /**
     * Generate category spending insight
     */
    fun generateCategoryInsight(
        topCategory: String?,
        amount: Int
    ): String? {
        if (topCategory.isNullOrBlank()) return null
        
        return "Pengeluaran terbanyak: $topCategory (${formatCurrency(amount)})"
    }

    /**
     * Generate daily summary combining multiple insights
     */
    fun generateDailySummary(
        todaySpending: Int,
        avgDailySpending: Int,
        todayJournalCount: Int,
        pendingReminders: Int
    ): String {
        val insights = mutableListOf<String>()
        
        // Spending insight
        generateSpendingInsight(todaySpending, avgDailySpending)?.let {
            insights.add(it)
        }
        
        // Activity insight
        if (todayJournalCount > 0) {
            insights.add("$todayJournalCount catatan kegiatan")
        }
        
        // Reminder insight
        if (pendingReminders > 0) {
            insights.add("$pendingReminders pengingat aktif")
        }
        
        return if (insights.isEmpty()) {
            "Belum ada aktivitas hari ini"
        } else {
            insights.joinToString(" • ")
        }
    }

    /**
     * Generate weekly summary
     */
    fun generateWeeklySummary(
        weeklySpending: Int,
        weeklyJournalCount: Int,
        completedReminders: Int,
        totalReminders: Int
    ): String {
        val parts = mutableListOf<String>()
        
        if (weeklySpending > 0) {
            parts.add("Pengeluaran minggu ini: ${formatCurrency(weeklySpending)}")
        }
        
        if (weeklyJournalCount > 0) {
            parts.add("$weeklyJournalCount jurnal")
        }
        
        if (totalReminders > 0) {
            val completionRate = (completedReminders.toDouble() / totalReminders * 100).toInt()
            parts.add("$completionRate% pengingat selesai")
        }
        
        return if (parts.isEmpty()) {
            "Mulai minggu baru dengan semangat!"
        } else {
            parts.joinToString(" | ")
        }
    }

    private fun formatCurrency(amount: Int): String {
        return currencyFormatter.format(amount).replace(",00", "")
    }
}
