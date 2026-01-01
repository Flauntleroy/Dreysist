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
}
