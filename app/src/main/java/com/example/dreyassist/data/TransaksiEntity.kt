package com.example.dreyassist.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transaksi")
data class TransaksiEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val tanggal: Long,
    val keperluan: String,
    val total: Int,
    val keterangan: String
)
