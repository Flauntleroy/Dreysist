package com.example.dreyassist.notification

import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.dreyassist.R
import com.example.dreyassist.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderPopupActivity : AppCompatActivity() {

    private var reminderId: Int = -1
    private var content: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Show over lock screen
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )
        
        setContentView(R.layout.activity_reminder_popup)

        reminderId = intent.getIntExtra("reminder_id", -1)
        content = intent.getStringExtra("content") ?: "Pengingat"

        findViewById<TextView>(R.id.text_content).text = content

        // Complete button
        findViewById<Button>(R.id.btn_complete).setOnClickListener {
            if (reminderId != -1) {
                CoroutineScope(Dispatchers.IO).launch {
                    val database = AppDatabase.getDatabase(this@ReminderPopupActivity)
                    database.reminderDao().markComplete(reminderId)
                }
                NotificationHelper.cancelNotification(this, reminderId)
            }
            finish()
        }

        // Snooze button
        findViewById<Button>(R.id.btn_snooze).setOnClickListener {
            if (reminderId != -1) {
                NotificationHelper.cancelNotification(this, reminderId)
                val snoozeTime = System.currentTimeMillis() + (30 * 60 * 1000) // 30 minutes
                ReminderScheduler.scheduleReminder(this, reminderId, content, snoozeTime)
            }
            finish()
        }
    }
}
