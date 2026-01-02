package com.example.dreyassist.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryDao {
    @Insert
    suspend fun insert(memory: MemoryEntity)

    @Update
    suspend fun update(memory: MemoryEntity)

    @Delete
    suspend fun delete(memory: MemoryEntity)

    @Query("SELECT * FROM memory ORDER BY createdAt DESC")
    fun getAll(): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memory WHERE id = :id")
    suspend fun getById(id: Int): MemoryEntity?

    @Query("SELECT * FROM memory WHERE content LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    fun search(query: String): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memory WHERE category = :category ORDER BY createdAt DESC")
    fun getByCategory(category: String): Flow<List<MemoryEntity>>
}
