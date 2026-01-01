package com.example.dreyassist.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TransaksiDao {
    @Insert
    suspend fun insert(transaksi: TransaksiEntity)

    @Update
    suspend fun update(transaksi: TransaksiEntity)

    @Delete
    suspend fun delete(transaksi: TransaksiEntity)

    @Query("SELECT * FROM transaksi ORDER BY tanggal DESC")
    fun getAll(): Flow<List<TransaksiEntity>>

    @Query("SELECT * FROM transaksi WHERE id = :id")
    suspend fun getById(id: Int): TransaksiEntity?
}
