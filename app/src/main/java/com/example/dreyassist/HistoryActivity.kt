package com.example.dreyassist

import android.app.DatePickerDialog
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dreyassist.data.AppDatabase
import com.example.dreyassist.data.JournalEntity
import com.example.dreyassist.data.MemoryEntity
import com.example.dreyassist.data.ReminderEntity
import com.example.dreyassist.data.TransaksiEntity
import com.example.dreyassist.databinding.ActivityHistoryBinding
import com.example.dreyassist.ui.HistoryAdapter
import com.example.dreyassist.ui.HistoryItem
import com.example.dreyassist.ui.ItemType
import com.example.dreyassist.ui.MainViewModel
import com.example.dreyassist.ui.MainViewModelFactory
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class HistoryActivity : BaseActivity() {

    private lateinit var binding: ActivityHistoryBinding
    private val database by lazy { AppDatabase.getDatabase(this) }
    private lateinit var adapter: HistoryAdapter

    // Data holders
    private var currentTransaksi: List<TransaksiEntity> = emptyList()
    private var currentJournals: List<JournalEntity> = emptyList()
    private var currentReminders: List<ReminderEntity> = emptyList()
    private var currentMemories: List<MemoryEntity> = emptyList()
    
    // Date filter
    private var filterStartDate: Long? = null
    private var filterEndDate: Long? = null
    
    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    private val fullDateFormat = SimpleDateFormat("EEEE, dd MMM yyyy • HH:mm", Locale.getDefault())
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault()).apply {
        maximumFractionDigits = 0
    }

    private val mainViewModel: MainViewModel by viewModels {
        MainViewModelFactory(database.transaksiDao(), database.journalDao(), database.reminderDao(), database.memoryDao())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT

        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener {
            finish()
        }

        adapter = HistoryAdapter { item ->
            showItemDetailDialog(item)
        }
        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(this)

        binding.fabDateFilter.setOnClickListener {
            showDateFilterDialog()
        }

        observeAllItems()
    }

    private fun observeAllItems() {
        mainViewModel.allTransaksi.observe(this) { transactions ->
            currentTransaksi = transactions ?: emptyList()
            updateHistoryList()
        }
        
        mainViewModel.allJournal.observe(this) { journals ->
            currentJournals = journals ?: emptyList()
            updateHistoryList()
        }
        
        mainViewModel.allReminders.observe(this) { reminders ->
            currentReminders = reminders ?: emptyList()
            updateHistoryList()
        }
        
        mainViewModel.allMemories.observe(this) { memories ->
            currentMemories = memories ?: emptyList()
            updateHistoryList()
        }
    }

    private fun updateHistoryList() {
        val allItems = mutableListOf<HistoryItem>()
        currentTransaksi.forEach { allItems.add(HistoryItem.fromTransaksi(it)) }
        currentJournals.forEach { allItems.add(HistoryItem.fromJournal(it)) }
        currentReminders.forEach { allItems.add(HistoryItem.fromReminder(it)) }
        currentMemories.forEach { allItems.add(HistoryItem.fromMemory(it)) }
        
        // Apply date filter
        val filteredItems = if (filterStartDate != null && filterEndDate != null) {
            allItems.filter { it.timestamp in filterStartDate!!..filterEndDate!! }
        } else {
            allItems
        }
        
        val sortedItems = filteredItems.sortedByDescending { it.timestamp }
        
        if (sortedItems.isEmpty()) {
            binding.recyclerView.visibility = View.GONE
            binding.emptyState.visibility = View.VISIBLE
        } else {
            binding.recyclerView.visibility = View.VISIBLE
            binding.emptyState.visibility = View.GONE
            adapter.submitList(sortedItems)
        }
    }

    private fun showItemDetailDialog(item: HistoryItem) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_item_detail)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        val typeLabel = when (item.type) {
            ItemType.TRANSAKSI -> getString(R.string.menu_transaction).uppercase()
            ItemType.JURNAL -> getString(R.string.menu_journal).uppercase()
            ItemType.PENGINGAT -> getString(R.string.menu_reminder).uppercase()
            ItemType.MEMORY -> getString(R.string.menu_notes).uppercase()
        }
        
        dialog.findViewById<TextView>(R.id.text_type_label).text = typeLabel
        dialog.findViewById<TextView>(R.id.text_title).text = item.title
        dialog.findViewById<TextView>(R.id.text_subtitle).text = item.subtitle
        dialog.findViewById<TextView>(R.id.text_date).text = fullDateFormat.format(Date(item.timestamp))

        // Show category for transaction items
        val containerCategory = dialog.findViewById<LinearLayout>(R.id.container_category)
        if (item.type == ItemType.TRANSAKSI && !item.category.isNullOrBlank()) {
            containerCategory.visibility = View.VISIBLE
            dialog.findViewById<android.widget.ImageView>(R.id.img_category).setImageResource(
                com.example.dreyassist.util.CategoryDetector.getCategoryIconResId(item.category)
            )
            dialog.findViewById<TextView>(R.id.text_category).text = 
                com.example.dreyassist.util.CategoryDetector.getCategoryName(item.category, this)
        } else {
            containerCategory.visibility = View.GONE
        }

        dialog.findViewById<Button>(R.id.btn_close).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showDateFilterDialog() {
        val calendar = Calendar.getInstance()
        
        DatePickerDialog(this, { _, year, month, day ->
            val startCal = Calendar.getInstance()
            startCal.set(year, month, day, 0, 0, 0)
            startCal.set(Calendar.MILLISECOND, 0)
            filterStartDate = startCal.timeInMillis
            
            // Pick end date
            DatePickerDialog(this, { _, eYear, eMonth, eDay ->
                val endCal = Calendar.getInstance()
                endCal.set(eYear, eMonth, eDay, 23, 59, 59)
                endCal.set(Calendar.MILLISECOND, 999)
                filterEndDate = endCal.timeInMillis
                
                // Update filter label
                binding.textDateFilter.text = "${dateFormat.format(Date(filterStartDate!!))} - ${dateFormat.format(Date(filterEndDate!!))}"
                binding.textDateFilter.visibility = View.VISIBLE
                
                updateHistoryList()
                Toast.makeText(this, getString(R.string.filter_applied), Toast.LENGTH_SHORT).show()
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).apply {
                setTitle(getString(R.string.select_end_date))
                show()
            }
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).apply {
            setTitle(getString(R.string.select_start_date))
            show()
        }
    }
}

