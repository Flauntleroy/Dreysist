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
    
    // ============================================================
    // REMEMBER Feature - Statistical Queries
    // ============================================================
    
    // Total pengeluaran keseluruhan
    @Query("SELECT SUM(total) FROM transaksi")
    suspend fun getTotalSpending(): Int?
    
    // Total pengeluaran sejak waktu tertentu
    @Query("SELECT SUM(total) FROM transaksi WHERE tanggal >= :startTime")
    suspend fun getSpendingSince(startTime: Long): Int?
    
    // Total pengeluaran hari ini
    @Query("SELECT SUM(total) FROM transaksi WHERE tanggal >= :todayStart AND tanggal < :todayEnd")
    suspend fun getTodaySpending(todayStart: Long, todayEnd: Long): Int?
    
    // Total pengeluaran bulan ini
    @Query("SELECT SUM(total) FROM transaksi WHERE tanggal >= :monthStart")
    suspend fun getMonthSpending(monthStart: Long): Int?
    
    // Transaksi terbaru
    @Query("SELECT * FROM transaksi ORDER BY tanggal DESC LIMIT 1")
    suspend fun getLatest(): TransaksiEntity?
    
    // Transaksi dengan keperluan tertentu (pencarian)
    @Query("SELECT * FROM transaksi WHERE keperluan LIKE '%' || :query || '%' ORDER BY tanggal DESC")
    fun searchByKeperluan(query: String): Flow<List<TransaksiEntity>>
    
    // Jumlah transaksi
    @Query("SELECT COUNT(*) FROM transaksi")
    suspend fun getCount(): Int
    
    // Transaksi dengan total tertinggi
    @Query("SELECT * FROM transaksi ORDER BY total DESC LIMIT 1")
    suspend fun getHighestTransaction(): TransaksiEntity?
    
    // Rata-rata pengeluaran per transaksi
    @Query("SELECT AVG(total) FROM transaksi")
    suspend fun getAverageSpending(): Double?
    
    // Transaksi dalam rentang waktu
    @Query("SELECT * FROM transaksi WHERE tanggal >= :startTime AND tanggal <= :endTime ORDER BY tanggal DESC")
    fun getTransactionsBetween(startTime: Long, endTime: Long): Flow<List<TransaksiEntity>>
}

