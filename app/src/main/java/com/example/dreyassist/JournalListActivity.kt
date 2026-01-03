package com.example.dreyassist

import android.app.Dialog
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
import com.example.dreyassist.data.JournalEntity
import com.example.dreyassist.databinding.ActivityJournalListBinding
import com.example.dreyassist.ui.JournalListAdapter
import com.example.dreyassist.ui.MainViewModel
import com.example.dreyassist.ui.MainViewModelFactory

class JournalListActivity : BaseActivity() {

    private lateinit var binding: ActivityJournalListBinding
    private val database by lazy { AppDatabase.getDatabase(this) }
    private lateinit var adapter: JournalListAdapter

    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory(database.transaksiDao(), database.journalDao(), database.reminderDao(), database.memoryDao())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT

        binding = ActivityJournalListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }
        binding.btnAdd.setOnClickListener { showEditDialog(null) }

        adapter = JournalListAdapter(
            onClick = { showDetailDialog(it) },
            onEdit = { showEditDialog(it) },
            onDelete = { showDeleteConfirmation(it) }
        )
        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(this)

        viewModel.allJournal.observe(this) { list ->
            adapter.submitList(list)
        }
    }

    private fun showEditDialog(journal: JournalEntity?) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_edit_journal)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        val title = dialog.findViewById<TextView>(R.id.dialog_title)
        val editKegiatan = dialog.findViewById<EditText>(R.id.edit_kegiatan)

        if (journal != null) {
            title.text = getString(R.string.edit_journal)
            editKegiatan.setText(journal.kegiatan)
        } else {
            title.text = getString(R.string.add_journal)
        }

        dialog.findViewById<Button>(R.id.btn_cancel).setOnClickListener {
            dialog.dismiss()
        }

        dialog.findViewById<Button>(R.id.btn_save).setOnClickListener {
            val kegiatan = editKegiatan.text.toString().trim()

            if (kegiatan.isEmpty()) {
                Toast.makeText(this, getString(R.string.error_journal_empty), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (journal != null) {
                viewModel.updateJournal(journal.copy(kegiatan = kegiatan))
                Toast.makeText(this, getString(R.string.journal_updated), Toast.LENGTH_SHORT).show()
            } else {
                viewModel.insertJournal(JournalEntity(
                    tanggal = System.currentTimeMillis(),
                    kegiatan = kegiatan
                ))
                Toast.makeText(this, getString(R.string.journal_added), Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showDeleteConfirmation(journal: JournalEntity) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.delete_journal_title))
            .setMessage(getString(R.string.delete_journal_message))
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                viewModel.deleteJournal(journal)
                Toast.makeText(this, getString(R.string.journal_deleted), Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun showDetailDialog(journal: JournalEntity) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_item_detail)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        val fullDateFormat = java.text.SimpleDateFormat("EEEE, dd MMM yyyy • HH:mm", java.util.Locale.getDefault())

        dialog.findViewById<TextView>(R.id.text_type_label).text = getString(R.string.menu_journal).uppercase()
        dialog.findViewById<TextView>(R.id.text_title).text = journal.kegiatan
        dialog.findViewById<TextView>(R.id.text_subtitle).text = getString(R.string.journal_subtitle)
        dialog.findViewById<TextView>(R.id.text_date).text = fullDateFormat.format(java.util.Date(journal.tanggal))

        dialog.findViewById<Button>(R.id.btn_close).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}
