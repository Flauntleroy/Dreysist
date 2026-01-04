package com.example.dreyassist

import android.app.DatePickerDialog
import android.app.Dialog
import android.app.TimePickerDialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Window
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.view.WindowCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dreyassist.data.AppDatabase
import com.example.dreyassist.data.ReminderEntity
import com.example.dreyassist.databinding.ActivityReminderListBinding
import com.example.dreyassist.notification.ReminderScheduler
import com.example.dreyassist.ui.MainViewModel
import com.example.dreyassist.ui.MainViewModelFactory
import com.example.dreyassist.ui.ReminderListAdapter
import com.google.android.material.tabs.TabLayout
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ReminderListActivity : BaseActivity() {

    private lateinit var binding: ActivityReminderListBinding
    private val database by lazy { AppDatabase.getDatabase(this) }
    private lateinit var adapter: ReminderListAdapter

    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory(database.transaksiDao(), database.journalDao(), database.reminderDao(), database.memoryDao())
    }

    private val dateTimeFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    private var selectedDateTime: Calendar = Calendar.getInstance()
    
    private var allReminders: List<ReminderEntity> = emptyList()
    private var currentTabPosition = 0 // 0 = Pending, 1 = Completed
    
    // Recurrence types
    private val recurrenceTypes = listOf("NONE", "DAILY", "WEEKLY", "MONTHLY")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT

        binding = ActivityReminderListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }
        binding.btnAdd.setOnClickListener { showEditDialog(null) }

        // Setup TabLayout
        setupTabs()

        adapter = ReminderListAdapter(
            onClick = { showDetailDialog(it) },
            onEdit = { showEditDialog(it) },
            onDelete = { showDeleteConfirmation(it) },
            onToggleStatus = { toggleReminderStatus(it) }
        )
        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(this)

        viewModel.allReminders.observe(this) { list ->
            allReminders = list ?: emptyList()
            filterReminders()
        }
    }
    
    private fun setupTabs() {
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(R.string.tab_pending))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(R.string.tab_completed))
        
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentTabPosition = tab?.position ?: 0
                filterReminders()
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }
    
    private fun filterReminders() {
        val filtered = when (currentTabPosition) {
            0 -> allReminders.filter { !it.isCompleted }
            1 -> allReminders.filter { it.isCompleted }
            else -> allReminders
        }
        adapter.submitList(filtered)
    }
    
    private fun toggleReminderStatus(reminder: ReminderEntity) {
        val updated = reminder.copy(isCompleted = !reminder.isCompleted)
        viewModel.updateReminder(updated)
        
        val message = if (updated.isCompleted) {
            getString(R.string.mark_complete)
        } else {
            getString(R.string.mark_pending)
        }
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun showEditDialog(reminder: ReminderEntity?) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_edit_reminder)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        val title = dialog.findViewById<TextView>(R.id.dialog_title)
        val editContent = dialog.findViewById<EditText>(R.id.edit_content)
        val textDate = dialog.findViewById<TextView>(R.id.text_date)
        val textTime = dialog.findViewById<TextView>(R.id.text_time)
        val btnPickDate = dialog.findViewById<Button>(R.id.btn_pick_date)
        val btnPickTime = dialog.findViewById<Button>(R.id.btn_pick_time)
        val spinnerRecurrence = dialog.findViewById<Spinner>(R.id.spinner_recurrence)

        selectedDateTime = Calendar.getInstance()
        
        // Setup recurrence spinner
        val recurrenceLabels = listOf(
            getString(R.string.recurrence_none),
            getString(R.string.recurrence_daily),
            getString(R.string.recurrence_weekly),
            getString(R.string.recurrence_monthly)
        )
        val recurrenceAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, recurrenceLabels)
        recurrenceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerRecurrence.adapter = recurrenceAdapter

        if (reminder != null) {
            title.text = getString(R.string.edit_reminder)
            editContent.setText(reminder.content)
            selectedDateTime.timeInMillis = reminder.reminderTime
            
            // Set recurrence spinner selection
            val recurrenceIndex = recurrenceTypes.indexOf(reminder.recurrenceType)
            if (recurrenceIndex >= 0) {
                spinnerRecurrence.setSelection(recurrenceIndex)
            }
        } else {
            title.text = getString(R.string.add_reminder)
            selectedDateTime.add(Calendar.HOUR_OF_DAY, 1)
        }

        textDate.text = dateFormat.format(Date(selectedDateTime.timeInMillis))
        textTime.text = timeFormat.format(Date(selectedDateTime.timeInMillis))

        btnPickDate.setOnClickListener {
            DatePickerDialog(this, { _, year, month, day ->
                selectedDateTime.set(Calendar.YEAR, year)
                selectedDateTime.set(Calendar.MONTH, month)
                selectedDateTime.set(Calendar.DAY_OF_MONTH, day)
                textDate.text = dateFormat.format(Date(selectedDateTime.timeInMillis))
            }, selectedDateTime.get(Calendar.YEAR), selectedDateTime.get(Calendar.MONTH), selectedDateTime.get(Calendar.DAY_OF_MONTH)).show()
        }

        btnPickTime.setOnClickListener {
            TimePickerDialog(this, { _, hour, minute ->
                selectedDateTime.set(Calendar.HOUR_OF_DAY, hour)
                selectedDateTime.set(Calendar.MINUTE, minute)
                textTime.text = timeFormat.format(Date(selectedDateTime.timeInMillis))
            }, selectedDateTime.get(Calendar.HOUR_OF_DAY), selectedDateTime.get(Calendar.MINUTE), true).show()
        }

        dialog.findViewById<Button>(R.id.btn_cancel).setOnClickListener {
            dialog.dismiss()
        }

        dialog.findViewById<Button>(R.id.btn_save).setOnClickListener {
            val content = editContent.text.toString().trim()
            val selectedRecurrenceIndex = spinnerRecurrence.selectedItemPosition
            val recurrenceType = recurrenceTypes[selectedRecurrenceIndex]

            if (content.isEmpty()) {
                Toast.makeText(this, getString(R.string.error_reminder_empty), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (reminder != null) {
                val updated = reminder.copy(
                    content = content,
                    reminderTime = selectedDateTime.timeInMillis,
                    recurrenceType = recurrenceType
                )
                viewModel.updateReminder(updated)
                ReminderScheduler.cancelReminder(this, reminder.id)
                ReminderScheduler.scheduleReminder(this, reminder.id, content, selectedDateTime.timeInMillis)
                Toast.makeText(this, getString(R.string.reminder_updated), Toast.LENGTH_SHORT).show()
            } else {
                val newReminder = ReminderEntity(
                    content = content,
                    reminderTime = selectedDateTime.timeInMillis,
                    recurrenceType = recurrenceType
                )
                viewModel.insertReminder(newReminder) { id ->
                    ReminderScheduler.scheduleReminder(this, id.toInt(), content, selectedDateTime.timeInMillis)
                }
                Toast.makeText(this, getString(R.string.reminder_added), Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showDeleteConfirmation(reminder: ReminderEntity) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.delete_reminder_title))
            .setMessage(getString(R.string.delete_reminder_message))
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                ReminderScheduler.cancelReminder(this, reminder.id)
                viewModel.deleteReminder(reminder)
                Toast.makeText(this, getString(R.string.reminder_deleted), Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun showDetailDialog(reminder: ReminderEntity) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_item_detail)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        val fullDateFormat = java.text.SimpleDateFormat("EEEE, dd MMM yyyy • HH:mm", Locale.getDefault())
        
        // Get recurrence label
        val recurrenceLabel = when (reminder.recurrenceType) {
            "DAILY" -> getString(R.string.recurrence_daily)
            "WEEKLY" -> getString(R.string.recurrence_weekly)
            "MONTHLY" -> getString(R.string.recurrence_monthly)
            else -> getString(R.string.recurrence_none)
        }
        
        val statusText = if (reminder.isCompleted) getString(R.string.status_completed) else getString(R.string.status_active)
        val subtitleText = if (reminder.recurrenceType != "NONE") {
            "$statusText • $recurrenceLabel"
        } else {
            statusText
        }

        dialog.findViewById<TextView>(R.id.text_type_label).text = getString(R.string.menu_reminder).uppercase()
        dialog.findViewById<TextView>(R.id.text_title).text = reminder.content
        dialog.findViewById<TextView>(R.id.text_subtitle).text = subtitleText
        dialog.findViewById<TextView>(R.id.text_date).text = fullDateFormat.format(Date(reminder.reminderTime))

        dialog.findViewById<Button>(R.id.btn_close).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}
