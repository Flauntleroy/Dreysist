package com.example.dreyassist.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.dreyassist.data.JournalDao
import com.example.dreyassist.data.JournalEntity
import com.example.dreyassist.data.ReminderDao
import com.example.dreyassist.data.ReminderEntity
import com.example.dreyassist.data.TransaksiDao
import com.example.dreyassist.data.TransaksiEntity
import kotlinx.coroutines.launch

class MainViewModel(
    private val transaksiDao: TransaksiDao,
    private val journalDao: JournalDao,
    private val reminderDao: ReminderDao
) : ViewModel() {

    val allTransaksi: LiveData<List<TransaksiEntity>> = transaksiDao.getAll().asLiveData()
    val allJournal: LiveData<List<JournalEntity>> = journalDao.getAll().asLiveData()
    val allReminders: LiveData<List<ReminderEntity>> = reminderDao.getAll().asLiveData()
    val activeReminders: LiveData<List<ReminderEntity>> = reminderDao.getActive().asLiveData()

    // Transaksi CRUD
    fun insertTransaksi(transaksi: TransaksiEntity) = viewModelScope.launch {
        transaksiDao.insert(transaksi)
    }

    fun updateTransaksi(transaksi: TransaksiEntity) = viewModelScope.launch {
        transaksiDao.update(transaksi)
    }

    fun deleteTransaksi(transaksi: TransaksiEntity) = viewModelScope.launch {
        transaksiDao.delete(transaksi)
    }

    // Journal CRUD
    fun insertJournal(journal: JournalEntity) = viewModelScope.launch {
        journalDao.insert(journal)
    }

    fun updateJournal(journal: JournalEntity) = viewModelScope.launch {
        journalDao.update(journal)
    }

    fun deleteJournal(journal: JournalEntity) = viewModelScope.launch {
        journalDao.delete(journal)
    }

    // Reminder CRUD
    fun insertReminder(reminder: ReminderEntity, onInserted: (Long) -> Unit) = viewModelScope.launch {
        val id = reminderDao.insert(reminder)
        onInserted(id)
    }

    fun updateReminder(reminder: ReminderEntity) = viewModelScope.launch {
        reminderDao.update(reminder)
    }

    fun deleteReminder(reminder: ReminderEntity) = viewModelScope.launch {
        reminderDao.delete(reminder)
    }
}

class MainViewModelFactory(
    private val transaksiDao: TransaksiDao,
    private val journalDao: JournalDao,
    private val reminderDao: ReminderDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(transaksiDao, journalDao, reminderDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}