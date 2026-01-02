package com.example.dreyassist.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminder")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val content: String,
    val reminderTime: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val isCompleted: Boolean = false,
    val recurrenceType: String = "NONE",  // NONE, DAILY, WEEKLY, MONTHLY
    val recurrenceInterval: Int = 1        // every X days/weeks/months
)
