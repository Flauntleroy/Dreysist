package com.example.dreyassist

import android.os.Bundle
import android.view.View
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
import com.example.dreyassist.ui.MainViewModel
import com.example.dreyassist.ui.MainViewModelFactory

class HistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoryBinding
    private val database by lazy { AppDatabase.getDatabase(this) }
    private lateinit var adapter: HistoryAdapter

    // Data holders
    private var currentTransaksi: List<TransaksiEntity> = emptyList()
    private var currentJournals: List<JournalEntity> = emptyList()
    private var currentReminders: List<ReminderEntity> = emptyList()
    private var currentMemories: List<MemoryEntity> = emptyList()

    private val mainViewModel: MainViewModel by viewModels {
        MainViewModelFactory(database.transaksiDao(), database.journalDao(), database.reminderDao(), database.memoryDao())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT

        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener {
            finish()
        }

        adapter = HistoryAdapter()
        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(this)

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
        
        val sortedItems = allItems.sortedByDescending { it.timestamp }
        
        if (sortedItems.isEmpty()) {
            binding.recyclerView.visibility = View.GONE
            binding.emptyState.visibility = View.VISIBLE
        } else {
            binding.recyclerView.visibility = View.VISIBLE
            binding.emptyState.visibility = View.GONE
            adapter.submitList(sortedItems)
        }
    }
}

