package com.example.dreyassist.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.dreyassist.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getIntExtra("reminder_id", -1)
        val content = intent.getStringExtra("content") ?: "Reminder"

        if (reminderId != -1) {
            // Show notification
            NotificationHelper.showNotification(context, reminderId, content)

            // Mark reminder as completed
            CoroutineScope(Dispatchers.IO).launch {
                val database = AppDatabase.getDatabase(context)
                database.reminderDao().markComplete(reminderId)
            }
        }
    }
}
