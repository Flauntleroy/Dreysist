package com.example.dreyassist.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.dreyassist.data.AppDatabase
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BackupHelper {

    private const val DB_NAME = "dreysist_database"
    private val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())

    fun createBackup(context: Context): File? {
        return try {
            // Close database connections
            AppDatabase.closeDatabase()

            val dbFile = context.getDatabasePath(DB_NAME)
            if (!dbFile.exists()) {
                Toast.makeText(context, "Database tidak ditemukan", Toast.LENGTH_SHORT).show()
                return null
            }

            // Create backup directory
            val backupDir = File(context.getExternalFilesDir(null), "backups")
            if (!backupDir.exists()) {
                backupDir.mkdirs()
            }

            // Create backup file with timestamp
            val timestamp = dateFormat.format(Date())
            val backupFile = File(backupDir, "dreysist_backup_$timestamp.db")

            // Copy database
            FileInputStream(dbFile).use { input ->
                FileOutputStream(backupFile).use { output ->
                    input.copyTo(output)
                }
            }

            Toast.makeText(context, "Backup berhasil dibuat", Toast.LENGTH_SHORT).show()
            backupFile
        } catch (e: Exception) {
            Toast.makeText(context, "Backup gagal: ${e.message}", Toast.LENGTH_SHORT).show()
            null
        }
    }

    fun shareBackup(context: Context, backupFile: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                backupFile
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/octet-stream"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Dreysist Backup")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(intent, "Share backup ke..."))
        } catch (e: Exception) {
            Toast.makeText(context, "Share gagal: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun restoreBackup(context: Context, sourceUri: Uri): Boolean {
        return try {
            // Close database connections
            AppDatabase.closeDatabase()

            val dbFile = context.getDatabasePath(DB_NAME)

            // Copy from source URI
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(dbFile).use { output ->
                    input.copyTo(output)
                }
            }

            Toast.makeText(context, "Restore berhasil! Restart aplikasi", Toast.LENGTH_LONG).show()
            true
        } catch (e: Exception) {
            Toast.makeText(context, "Restore gagal: ${e.message}", Toast.LENGTH_SHORT).show()
            false
        }
    }

    fun getBackupFiles(context: Context): List<File> {
        val backupDir = File(context.getExternalFilesDir(null), "backups")
        return if (backupDir.exists()) {
            backupDir.listFiles()?.filter { it.extension == "db" }?.sortedByDescending { it.lastModified() } ?: emptyList()
        } else {
            emptyList()
        }
    }
}
