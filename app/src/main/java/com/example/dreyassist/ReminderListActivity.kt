package com.example.dreyassist

import android.app.DatePickerDialog
import android.app.Dialog
import android.app.TimePickerDialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Window
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dreyassist.data.AppDatabase
import com.example.dreyassist.data.ReminderEntity
import com.example.dreyassist.databinding.ActivityReminderListBinding
import com.example.dreyassist.notification.ReminderScheduler
import com.example.dreyassist.ui.MainViewModel
import com.example.dreyassist.ui.MainViewModelFactory
import com.example.dreyassist.ui.ReminderListAdapter
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT

        binding = ActivityReminderListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }
        binding.btnAdd.setOnClickListener { showEditDialog(null) }

        adapter = ReminderListAdapter(
            onClick = { showDetailDialog(it) },
            onEdit = { showEditDialog(it) },
            onDelete = { showDeleteConfirmation(it) }
        )
        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(this)

        viewModel.allReminders.observe(this) { list ->
            adapter.submitList(list)
        }
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

        selectedDateTime = Calendar.getInstance()

        if (reminder != null) {
            title.text = getString(R.string.edit_reminder)
            editContent.setText(reminder.content)
            selectedDateTime.timeInMillis = reminder.reminderTime
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

            if (content.isEmpty()) {
                Toast.makeText(this, getString(R.string.error_reminder_empty), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (reminder != null) {
                val updated = reminder.copy(
                    content = content,
                    reminderTime = selectedDateTime.timeInMillis
                )
                viewModel.updateReminder(updated)
                ReminderScheduler.cancelReminder(this, reminder.id)
                ReminderScheduler.scheduleReminder(this, reminder.id, content, selectedDateTime.timeInMillis)
                Toast.makeText(this, getString(R.string.reminder_updated), Toast.LENGTH_SHORT).show()
            } else {
                val newReminder = ReminderEntity(
                    content = content,
                    reminderTime = selectedDateTime.timeInMillis
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

        dialog.findViewById<TextView>(R.id.text_type_label).text = getString(R.string.menu_reminder).uppercase()
        dialog.findViewById<TextView>(R.id.text_title).text = reminder.content
        dialog.findViewById<TextView>(R.id.text_subtitle).text = if (reminder.isCompleted) getString(R.string.status_completed) else getString(R.string.status_active)
        dialog.findViewById<TextView>(R.id.text_date).text = fullDateFormat.format(Date(reminder.reminderTime))

        dialog.findViewById<Button>(R.id.btn_close).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}
