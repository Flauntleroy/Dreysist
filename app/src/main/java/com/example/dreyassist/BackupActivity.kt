package com.example.dreyassist

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.example.dreyassist.databinding.ActivityBackupBinding
import com.example.dreyassist.util.BackupHelper
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BackupActivity : BaseActivity() {

    private lateinit var binding: ActivityBackupBinding
    private val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())

    private val restoreFilePicker = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                AlertDialog.Builder(this)
                    .setTitle(getString(R.string.restore_db_title))
                    .setMessage(getString(R.string.restore_db_message))
                    .setPositiveButton(getString(R.string.restore)) { _, _ ->
                        if (BackupHelper.restoreBackup(this, uri)) {
                            // Restart app
                            val intent = packageManager.getLaunchIntentForPackage(packageName)
                            intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            startActivity(intent)
                            finishAffinity()
                        }
                    }
                    .setNegativeButton(getString(R.string.cancel), null)
                    .show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT

        binding = ActivityBackupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        binding.btnBackup.setOnClickListener {
            val backupFile = BackupHelper.createBackup(this)
            if (backupFile != null) {
                AlertDialog.Builder(this)
                    .setTitle(getString(R.string.backup_success_title))
                    .setMessage(getString(R.string.backup_success_message))
                    .setPositiveButton(getString(R.string.share)) { _, _ ->
                        BackupHelper.shareBackup(this, backupFile)
                    }
                    .setNegativeButton(getString(R.string.later), null)
                    .show()
                loadBackupList()
            }
        }

        binding.btnRestore.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
            }
            restoreFilePicker.launch(intent)
        }

        loadBackupList()
    }

    private fun loadBackupList() {
        binding.backupListContainer.removeAllViews()
        val backups = BackupHelper.getBackupFiles(this)

        if (backups.isEmpty()) {
            binding.textNoBackups.visibility = View.VISIBLE
        } else {
            binding.textNoBackups.visibility = View.GONE
            
            backups.forEach { file ->
                val itemView = createBackupItemView(file)
                binding.backupListContainer.addView(itemView)
            }
        }
    }

    private fun createBackupItemView(file: File): View {
        val itemView = LayoutInflater.from(this)
            .inflate(R.layout.item_backup, binding.backupListContainer, false)

        itemView.findViewById<TextView>(R.id.text_filename).text = file.name
        itemView.findViewById<TextView>(R.id.text_date).text = dateFormat.format(Date(file.lastModified()))
        itemView.findViewById<TextView>(R.id.text_size).text = formatFileSize(file.length())

        itemView.findViewById<Button>(R.id.btn_share).setOnClickListener {
            BackupHelper.shareBackup(this, file)
        }

        itemView.findViewById<Button>(R.id.btn_restore_local).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.restore_db_title))
                .setMessage(getString(R.string.restore_db_message))
                .setPositiveButton(getString(R.string.restore)) { _, _ ->
                    if (BackupHelper.restoreBackup(this, android.net.Uri.fromFile(file))) {
                        val intent = packageManager.getLaunchIntentForPackage(packageName)
                        intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        startActivity(intent)
                        finishAffinity()
                    }
                }
                .setNegativeButton(getString(R.string.cancel), null)
                .show()
        }

        return itemView
    }

    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            else -> "${bytes / (1024 * 1024)} MB"
        }
    }
}
