package com.example.dreyassist

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.example.dreyassist.data.AppDatabase
import com.example.dreyassist.databinding.ActivityInsightsBinding
import com.example.dreyassist.util.CategoryDetector
import com.example.dreyassist.util.InsightHelper
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
        
        loadInsights()
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
                val weekJournals = database.journalDao().getJournalsSince(weekStart).size
                
                // Month data
                val monthSpending = database.transaksiDao().getSpendingSince(monthStart) ?: 0
                
                // Categories
                val categoryStats = database.transaksiDao().getCategoryStats()
                
                withContext(Dispatchers.Main) {
                    // Update Today card
                    binding.textTodaySpending.text = currencyFormat.format(todaySpending)
                    binding.textSpendingInsight.text = InsightHelper.generateSpendingInsight(todaySpending, avgSpending) ?: "Start tracking your expenses"
                    binding.textTodayJournals.text = todayJournals.toString()
                    binding.textActiveReminders.text = activeReminders.toString()
                    binding.textTodayTransactions.text = todayTransactions.toString()
                    
                    // Update Week card
                    binding.textWeekSpending.text = currencyFormat.format(weekSpending)
                    binding.textWeekInsight.text = InsightHelper.generateWeeklySummary(
                        weekSpending, weekJournals, 0, activeReminders
                    )
                    
                    // Update Month card
                    binding.textMonthSpending.text = currencyFormat.format(monthSpending)
                    binding.textMonthInsight.text = "Total from ${getTransactionCountSince(monthStart)} transactions"
                    
                    // Update categories
                    binding.containerCategories.removeAllViews()
                    if (categoryStats.isEmpty()) {
                        addCategoryRow(R.drawable.ic_category_other, "No transactions yet", "", 0)
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

    private suspend fun getTransactionCountSince(startTime: Long): Int {
        return withContext(Dispatchers.IO) {
            database.transaksiDao().getTransactionCountSince(startTime)
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
