package com.example.dreyassist.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.dreyassist.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getIntExtra("reminder_id", -1)
        if (reminderId == -1) return

        when (intent.action) {
            "ACTION_COMPLETE" -> {
                // Mark as complete and cancel notification
                CoroutineScope(Dispatchers.IO).launch {
                    val database = AppDatabase.getDatabase(context)
                    database.reminderDao().markComplete(reminderId)
                }
                NotificationHelper.cancelNotification(context, reminderId)
            }
            "ACTION_SNOOZE" -> {
                val content = intent.getStringExtra("content") ?: "Pengingat"
                
                // Cancel current notification
                NotificationHelper.cancelNotification(context, reminderId)
                
                // Schedule new reminder 30 minutes later
                val snoozeTime = System.currentTimeMillis() + (30 * 60 * 1000) // 30 minutes
                ReminderScheduler.scheduleReminder(context, reminderId, content, snoozeTime)
            }
        }
    }
}
