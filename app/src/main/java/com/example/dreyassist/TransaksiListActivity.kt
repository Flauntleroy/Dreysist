package com.example.dreyassist

import android.app.DatePickerDialog
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
import com.example.dreyassist.data.TransaksiEntity
import com.example.dreyassist.databinding.ActivityTransaksiListBinding
import com.example.dreyassist.ui.MainViewModel
import com.example.dreyassist.ui.MainViewModelFactory
import com.example.dreyassist.ui.TransaksiListAdapter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class TransaksiListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTransaksiListBinding
    private val database by lazy { AppDatabase.getDatabase(this) }
    private lateinit var adapter: TransaksiListAdapter

    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory(database.transaksiDao(), database.journalDao(), database.reminderDao(), database.memoryDao())
    }

    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))
    private var selectedDate = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT

        binding = ActivityTransaksiListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }
        binding.btnAdd.setOnClickListener { showEditDialog(null) }

        adapter = TransaksiListAdapter(
            onEdit = { showEditDialog(it) },
            onDelete = { showDeleteConfirmation(it) }
        )
        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(this)

        viewModel.allTransaksi.observe(this) { list ->
            adapter.submitList(list)
        }
    }

    private fun showEditDialog(transaksi: TransaksiEntity?) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_edit_transaksi)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        val title = dialog.findViewById<TextView>(R.id.dialog_title)
        val editKeperluan = dialog.findViewById<EditText>(R.id.edit_keperluan)
        val editTotal = dialog.findViewById<EditText>(R.id.edit_total)
        val editKeterangan = dialog.findViewById<EditText>(R.id.edit_keterangan)
        val textDate = dialog.findViewById<TextView>(R.id.text_date)
        val btnPickDate = dialog.findViewById<Button>(R.id.btn_pick_date)

        selectedDate = Calendar.getInstance()

        if (transaksi != null) {
            title.text = "Edit Transaksi"
            editKeperluan.setText(transaksi.keperluan)
            editTotal.setText(transaksi.total.toString())
            editKeterangan.setText(transaksi.keterangan)
            selectedDate.timeInMillis = transaksi.tanggal
        } else {
            title.text = "Tambah Transaksi"
        }

        textDate.text = dateFormat.format(Date(selectedDate.timeInMillis))

        btnPickDate.setOnClickListener {
            DatePickerDialog(this, { _, year, month, day ->
                selectedDate.set(Calendar.YEAR, year)
                selectedDate.set(Calendar.MONTH, month)
                selectedDate.set(Calendar.DAY_OF_MONTH, day)
                textDate.text = dateFormat.format(Date(selectedDate.timeInMillis))
            }, selectedDate.get(Calendar.YEAR), selectedDate.get(Calendar.MONTH), selectedDate.get(Calendar.DAY_OF_MONTH)).show()
        }

        dialog.findViewById<Button>(R.id.btn_cancel).setOnClickListener {
            dialog.dismiss()
        }

        dialog.findViewById<Button>(R.id.btn_save).setOnClickListener {
            val keperluan = editKeperluan.text.toString().trim()
            val total = editTotal.text.toString().toIntOrNull() ?: 0
            val keterangan = editKeterangan.text.toString().trim()

            if (keperluan.isEmpty() || total <= 0) {
                Toast.makeText(this, "Keperluan dan Total harus diisi", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (transaksi != null) {
                viewModel.updateTransaksi(transaksi.copy(
                    tanggal = selectedDate.timeInMillis,
                    keperluan = keperluan,
                    total = total,
                    keterangan = keterangan
                ))
                Toast.makeText(this, "Transaksi diperbarui", Toast.LENGTH_SHORT).show()
            } else {
                viewModel.insertTransaksi(TransaksiEntity(
                    tanggal = selectedDate.timeInMillis,
                    keperluan = keperluan,
                    total = total,
                    keterangan = keterangan
                ))
                Toast.makeText(this, "Transaksi ditambahkan", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showDeleteConfirmation(transaksi: TransaksiEntity) {
        AlertDialog.Builder(this)
            .setTitle("Hapus Transaksi")
            .setMessage("Yakin ingin menghapus transaksi ini?")
            .setPositiveButton("Hapus") { _, _ ->
                viewModel.deleteTransaksi(transaksi)
                Toast.makeText(this, "Transaksi dihapus", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Batal", null)
            .show()
    }
}
