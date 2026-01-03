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
import com.example.dreyassist.util.CategoryDetector
import android.widget.ArrayAdapter
import android.widget.Spinner
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class TransaksiListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTransaksiListBinding
    private val database by lazy { AppDatabase.getDatabase(this) }
    private lateinit var adapter: TransaksiListAdapter
    
    // All transactions for filtering
    private var allTransactions: List<TransaksiEntity> = emptyList()
    
    // Date filter
    private var filterStartDate: Long? = null
    private var filterEndDate: Long? = null

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
        
        binding.fabDateFilter.setOnClickListener { showDateFilterDialog() }

        adapter = TransaksiListAdapter(
            onClick = { showDetailDialog(it) },
            onEdit = { showEditDialog(it) },
            onDelete = { showDeleteConfirmation(it) }
        )
        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(this)

        viewModel.allTransaksi.observe(this) { list ->
            allTransactions = list ?: emptyList()
            applyDateFilter()
        }
    }
    
    private fun applyDateFilter() {
        val filtered = if (filterStartDate != null && filterEndDate != null) {
            allTransactions.filter { it.tanggal in filterStartDate!!..filterEndDate!! }
        } else {
            allTransactions
        }
        adapter.submitList(filtered)
    }
    
    private fun showDateFilterDialog() {
        val calendar = Calendar.getInstance()
        
        DatePickerDialog(this, { _, year, month, day ->
            val startCal = Calendar.getInstance()
            startCal.set(year, month, day, 0, 0, 0)
            startCal.set(Calendar.MILLISECOND, 0)
            filterStartDate = startCal.timeInMillis
            
            DatePickerDialog(this, { _, eYear, eMonth, eDay ->
                val endCal = Calendar.getInstance()
                endCal.set(eYear, eMonth, eDay, 23, 59, 59)
                endCal.set(Calendar.MILLISECOND, 999)
                filterEndDate = endCal.timeInMillis
                
                binding.textDateFilter.text = "${dateFormat.format(java.util.Date(filterStartDate!!))} - ${dateFormat.format(java.util.Date(filterEndDate!!))}"
                binding.textDateFilter.visibility = android.view.View.VISIBLE
                
                applyDateFilter()
                Toast.makeText(this, "Filter applied", Toast.LENGTH_SHORT).show()
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).apply {
                setTitle("Select End Date")
                show()
            }
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).apply {
            setTitle("Select Start Date")
            show()
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
        val spinnerCategory = dialog.findViewById<Spinner>(R.id.spinner_category)

        // Setup category spinner
        val categories = CategoryDetector.Category.values()
        val categoryNames = categories.map { it.displayName }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categoryNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategory.adapter = adapter

        selectedDate = Calendar.getInstance()

        if (transaksi != null) {
            title.text = "Edit Transaksi"
            editKeperluan.setText(transaksi.keperluan)
            editTotal.setText(transaksi.total.toString())
            editKeterangan.setText(transaksi.keterangan)
            selectedDate.timeInMillis = transaksi.tanggal
            
            // Set spinner to existing category
            val categoryIndex = categories.indexOfFirst { it.name == transaksi.category }
            if (categoryIndex >= 0) {
                spinnerCategory.setSelection(categoryIndex)
            }
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
            val selectedCategory = categories[spinnerCategory.selectedItemPosition].name

            if (keperluan.isEmpty() || total <= 0) {
                Toast.makeText(this, "Keperluan dan Total harus diisi", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (transaksi != null) {
                viewModel.updateTransaksi(transaksi.copy(
                    tanggal = selectedDate.timeInMillis,
                    keperluan = keperluan,
                    total = total,
                    keterangan = keterangan,
                    category = selectedCategory
                ))
                Toast.makeText(this, "Transaksi diperbarui", Toast.LENGTH_SHORT).show()
            } else {
                viewModel.insertTransaksi(TransaksiEntity(
                    tanggal = selectedDate.timeInMillis,
                    keperluan = keperluan,
                    total = total,
                    keterangan = keterangan,
                    category = selectedCategory
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

    private fun showDetailDialog(transaksi: TransaksiEntity) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_item_detail)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        val currencyFormat = java.text.NumberFormat.getCurrencyInstance(java.util.Locale("id", "ID")).apply {
            maximumFractionDigits = 0
        }
        val fullDateFormat = java.text.SimpleDateFormat("EEEE, dd MMM yyyy • HH:mm", java.util.Locale("id", "ID"))

        dialog.findViewById<TextView>(R.id.text_type_label).text = "TRANSAKSI"
        dialog.findViewById<TextView>(R.id.text_title).text = transaksi.keperluan
        dialog.findViewById<TextView>(R.id.text_subtitle).text = currencyFormat.format(transaksi.total)
        dialog.findViewById<TextView>(R.id.text_date).text = fullDateFormat.format(java.util.Date(transaksi.tanggal))

        // Show category
        val containerCategory = dialog.findViewById<LinearLayout>(R.id.container_category)
        containerCategory.visibility = android.view.View.VISIBLE
        dialog.findViewById<android.widget.ImageView>(R.id.img_category).setImageResource(
            CategoryDetector.getCategoryIconResId(transaksi.category)
        )
        dialog.findViewById<TextView>(R.id.text_category).text = CategoryDetector.getCategoryName(transaksi.category)

        dialog.findViewById<Button>(R.id.btn_close).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}
