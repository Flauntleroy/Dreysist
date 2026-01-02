package com.example.dreyassist.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface JournalDao {
    @Insert
    suspend fun insert(journal: JournalEntity)

    @Update
    suspend fun update(journal: JournalEntity)

    @Delete
    suspend fun delete(journal: JournalEntity)

    @Query("SELECT * FROM journal ORDER BY tanggal DESC")
    fun getAll(): Flow<List<JournalEntity>>

    @Query("SELECT * FROM journal WHERE id = :id")
    suspend fun getById(id: Int): JournalEntity?
    
    // ============================================================
    // REMEMBER Feature - Statistical Queries
    // ============================================================
    
    // Jurnal terbaru
    @Query("SELECT * FROM journal ORDER BY tanggal DESC LIMIT 1")
    suspend fun getLatest(): JournalEntity?
    
    // Pencarian jurnal
    @Query("SELECT * FROM journal WHERE kegiatan LIKE '%' || :query || '%' ORDER BY tanggal DESC")
    fun searchByKegiatan(query: String): Flow<List<JournalEntity>>
    
    // Jumlah jurnal
    @Query("SELECT COUNT(*) FROM journal")
    suspend fun getCount(): Int
    
    // Jurnal hari ini
    @Query("SELECT * FROM journal WHERE tanggal >= :todayStart AND tanggal < :todayEnd ORDER BY tanggal DESC")
    fun getTodayJournals(todayStart: Long, todayEnd: Long): Flow<List<JournalEntity>>
    
    // Jurnal dalam rentang waktu
    @Query("SELECT * FROM journal WHERE tanggal >= :startTime AND tanggal <= :endTime ORDER BY tanggal DESC")
    fun getJournalsBetween(startTime: Long, endTime: Long): Flow<List<JournalEntity>>
}

