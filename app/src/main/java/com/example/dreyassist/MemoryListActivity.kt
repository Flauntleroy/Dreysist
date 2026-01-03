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
import com.example.dreyassist.data.MemoryEntity
import com.example.dreyassist.databinding.ActivityMemoryListBinding
import com.example.dreyassist.ui.MainViewModel
import com.example.dreyassist.ui.MainViewModelFactory
import com.example.dreyassist.ui.MemoryListAdapter

class MemoryListActivity : BaseActivity() {

    private lateinit var binding: ActivityMemoryListBinding
    private val database by lazy { AppDatabase.getDatabase(this) }
    private lateinit var adapter: MemoryListAdapter

    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory(database.transaksiDao(), database.journalDao(), database.reminderDao(), database.memoryDao())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT

        binding = ActivityMemoryListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }
        binding.btnAdd.setOnClickListener { showEditDialog(null) }

        adapter = MemoryListAdapter(
            onClick = { showDetailDialog(it) },
            onEdit = { showEditDialog(it) },
            onDelete = { showDeleteConfirmation(it) }
        )
        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(this)

        viewModel.allMemories.observe(this) { list ->
            adapter.submitList(list)
        }
    }

    private fun showEditDialog(memory: MemoryEntity?) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_edit_memory)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        val title = dialog.findViewById<TextView>(R.id.dialog_title)
        val editContent = dialog.findViewById<EditText>(R.id.edit_content)
        val editCategory = dialog.findViewById<EditText>(R.id.edit_category)

        if (memory != null) {
            title.text = getString(R.string.edit_memory)
            editContent.setText(memory.content)
            editCategory.setText(memory.category)
        } else {
            title.text = getString(R.string.add_memory)
        }

        dialog.findViewById<Button>(R.id.btn_cancel).setOnClickListener {
            dialog.dismiss()
        }

        dialog.findViewById<Button>(R.id.btn_save).setOnClickListener {
            val content = editContent.text.toString().trim()
            val category = editCategory.text.toString().trim()

            if (content.isEmpty()) {
                Toast.makeText(this, getString(R.string.error_memory_empty), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (memory != null) {
                viewModel.updateMemory(memory.copy(content = content, category = category))
                Toast.makeText(this, getString(R.string.memory_updated), Toast.LENGTH_SHORT).show()
            } else {
                viewModel.insertMemory(MemoryEntity(
                    content = content,
                    category = category
                ))
                Toast.makeText(this, getString(R.string.memory_added), Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showDeleteConfirmation(memory: MemoryEntity) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.delete_memory_title))
            .setMessage(getString(R.string.delete_memory_message))
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                viewModel.deleteMemory(memory)
                Toast.makeText(this, getString(R.string.memory_deleted), Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun showDetailDialog(memory: MemoryEntity) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_item_detail)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        val fullDateFormat = java.text.SimpleDateFormat("EEEE, dd MMM yyyy • HH:mm", java.util.Locale.getDefault())

        dialog.findViewById<TextView>(R.id.text_type_label).text = getString(R.string.menu_notes).uppercase()
        dialog.findViewById<TextView>(R.id.text_title).text = memory.content
        dialog.findViewById<TextView>(R.id.text_subtitle).text = if (memory.category.isNotBlank()) memory.category else getString(R.string.menu_notes)
        dialog.findViewById<TextView>(R.id.text_date).text = fullDateFormat.format(java.util.Date(memory.createdAt))

        dialog.findViewById<Button>(R.id.btn_close).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}
