package com.example.dreyassist.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "memory")
data class MemoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val content: String,
    val category: String = "", // optional sub-category like "password", "contact", "preference"
    val createdAt: Long = System.currentTimeMillis()
)
