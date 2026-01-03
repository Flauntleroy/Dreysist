package com.example.dreyassist

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.example.dreyassist.data.AppDatabase
import com.example.dreyassist.databinding.ActivityInsightsBinding
import com.example.dreyassist.util.CategoryDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

class InsightsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityInsightsBinding
    private val database by lazy { AppDatabase.getDatabase(this) }
    
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
        maximumFractionDigits = 0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        
        binding = ActivityInsightsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        binding.btnBack.setOnClickListener { finish() }
        
        // Setup scroll indicator
        setupScrollIndicator()
        
        loadInsights()
    }

    private fun setupScrollIndicator() {
        binding.scrollPeriods.setOnScrollChangeListener { _, scrollX, _, _, _ ->
            val cardWidth = 312 * resources.displayMetrics.density // 300dp + 12dp margin
            val position = (scrollX / cardWidth).toInt()
            
            // Update indicators
            updateIndicator(position)
        }
    }

    private fun updateIndicator(position: Int) {
        val activeColor = resources.getColor(R.color.accent_cyan, null)
        val inactiveColor = resources.getColor(R.color.text_hint, null)
        
        // Reset all indicators
        binding.indicatorToday.setBackgroundColor(inactiveColor)
        binding.indicatorWeek.setBackgroundColor(inactiveColor)
        binding.indicatorMonth.setBackgroundColor(inactiveColor)
        
        // Reset widths
        val activeWidth = (20 * resources.displayMetrics.density).toInt()
        val inactiveWidth = (8 * resources.displayMetrics.density).toInt()
        
        binding.indicatorToday.layoutParams.width = inactiveWidth
        binding.indicatorWeek.layoutParams.width = inactiveWidth
        binding.indicatorMonth.layoutParams.width = inactiveWidth
        
        // Set active indicator
        when (position) {
            0 -> {
                binding.indicatorToday.setBackgroundColor(activeColor)
                binding.indicatorToday.layoutParams.width = activeWidth
            }
            1 -> {
                binding.indicatorWeek.setBackgroundColor(activeColor)
                binding.indicatorWeek.layoutParams.width = activeWidth
            }
            2 -> {
                binding.indicatorMonth.setBackgroundColor(activeColor)
                binding.indicatorMonth.layoutParams.width = activeWidth
            }
        }
        
        binding.indicatorToday.requestLayout()
        binding.indicatorWeek.requestLayout()
        binding.indicatorMonth.requestLayout()
    }

    private fun loadInsights() {
        lifecycleScope.launch {
            val today = getStartOfDay()
            val weekStart = getStartOfWeek()
            val monthStart = getStartOfMonth()
            
            withContext(Dispatchers.IO) {
                // Today's data
                val todaySpending = database.transaksiDao().getSpendingSince(today) ?: 0
                val avgSpending = database.transaksiDao().getAverageSpending()?.toInt() ?: 0
                val todayJournals = database.journalDao().getJournalsSince(today).size
                val activeReminders = database.reminderDao().getActiveList().size
                val todayTransactions = database.transaksiDao().getTransactionCountSince(today)
                
                // Week data
                val weekSpending = database.transaksiDao().getSpendingSince(weekStart) ?: 0
                val weekTransactions = database.transaksiDao().getTransactionCountSince(weekStart)
                
                // Month data
                val monthSpending = database.transaksiDao().getSpendingSince(monthStart) ?: 0
                val monthTransactions = database.transaksiDao().getTransactionCountSince(monthStart)
                
                // Categories
                val categoryStats = database.transaksiDao().getCategoryStats()
                
                withContext(Dispatchers.Main) {
                    // Update Today card
                    binding.textTodaySpending.text = currencyFormat.format(todaySpending)
                    binding.textSpendingInsight.text = generateTodayNarrative(todaySpending, avgSpending)
                    binding.textTodayStats.text = "$todayTransactions transaksi • $todayJournals jurnal • $activeReminders pengingat"
                    
                    // Update Week card
                    binding.textWeekSpending.text = currencyFormat.format(weekSpending)
                    binding.textWeekInsight.text = generateWeekNarrative(weekSpending, weekTransactions)
                    binding.textWeekStats.text = "$weekTransactions transaksi"
                    
                    // Update Month card
                    binding.textMonthSpending.text = currencyFormat.format(monthSpending)
                    binding.textMonthInsight.text = generateMonthNarrative(monthSpending, monthTransactions, avgSpending)
                    binding.textMonthStats.text = "$monthTransactions transaksi"
                    
                    // Generate Category Narrative
                    if (categoryStats.isNotEmpty()) {
                        val topCategory = categoryStats.first()
                        val categoryName = CategoryDetector.getCategoryName(topCategory.category)
                        binding.textCategoryNarrative.text = "$categoryName masih jadi kategori terbesar bulan ini."
                    } else {
                        binding.textCategoryNarrative.text = "Belum ada data kategori."
                    }
                    
                    // Update categories list
                    binding.containerCategories.removeAllViews()
                    if (categoryStats.isEmpty()) {
                        addCategoryRow(R.drawable.ic_category_other, "Belum ada transaksi", "", 0)
                    } else {
                        categoryStats.take(5).forEach { stat ->
                            val iconResId = CategoryDetector.getCategoryIconResId(stat.category)
                            val name = CategoryDetector.getCategoryName(stat.category)
                            addCategoryRow(iconResId, name, currencyFormat.format(stat.total), stat.count)
                        }
                    }
                }
            }
        }
    }

    private fun generateTodayNarrative(spending: Int, avgSpending: Int): String {
        return when {
            spending == 0 -> "Belum ada pengeluaran hari ini."
            avgSpending == 0 -> "Mulai hari dengan ${currencyFormat.format(spending)}."
            spending < avgSpending * 0.5 -> "Pengeluaran ringan, jauh di bawah rata-rata."
            spending < avgSpending -> "Pengeluaran normal, di bawah rata-rata."
            spending < avgSpending * 1.5 -> "Sedikit di atas rata-rata harianmu."
            else -> "Hari yang sibuk untuk dompetmu!"
        }
    }

    private fun generateWeekNarrative(spending: Int, transactions: Int): String {
        return when {
            spending == 0 -> "Minggu ini belum ada pengeluaran."
            transactions <= 3 -> "Minggu tenang dengan $transactions transaksi."
            transactions <= 7 -> "Minggu normal dengan $transactions transaksi."
            else -> "Minggu yang cukup aktif."
        }
    }

    private fun generateMonthNarrative(spending: Int, transactions: Int, avgDaily: Int): String {
        val dayOfMonth = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
        val expectedSpending = avgDaily * dayOfMonth
        
        return when {
            spending == 0 -> "Bulan ini belum ada pengeluaran."
            expectedSpending == 0 -> "Total $transactions transaksi bulan ini."
            spending < expectedSpending * 0.8 -> "Bulan ini pengeluaranmu stabil."
            spending > expectedSpending * 1.2 -> "Bulan ini pengeluaran agak tinggi."
            else -> "Sesuai dengan kebiasaanmu."
        }
    }

    private fun addCategoryRow(iconResId: Int, name: String, amount: String, count: Int) {
        val row = LayoutInflater.from(this).inflate(R.layout.item_category_stat, binding.containerCategories, false)
        row.findViewById<android.widget.ImageView>(R.id.img_category_icon).setImageResource(iconResId)
        row.findViewById<TextView>(R.id.text_category_name).text = name
        row.findViewById<TextView>(R.id.text_category_amount).text = amount
        row.findViewById<TextView>(R.id.text_category_count).text = if (count > 0) "$count items" else ""
        binding.containerCategories.addView(row)
    }

    private fun getStartOfDay(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun getStartOfWeek(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
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
