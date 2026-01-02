package com.example.dreyassist.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    @Insert
    suspend fun insert(reminder: ReminderEntity): Long

    @Update
    suspend fun update(reminder: ReminderEntity)

    @Delete
    suspend fun delete(reminder: ReminderEntity)

    @Query("SELECT * FROM reminder ORDER BY reminderTime ASC")
    fun getAll(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminder WHERE isCompleted = 0 ORDER BY reminderTime ASC")
    fun getActive(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminder WHERE id = :id")
    suspend fun getById(id: Int): ReminderEntity?

    @Query("UPDATE reminder SET isCompleted = 1 WHERE id = :id")
    suspend fun markComplete(id: Int)
    
    @Query("SELECT * FROM reminder WHERE isCompleted = 0 ORDER BY reminderTime ASC")
    suspend fun getActiveList(): List<ReminderEntity>
}
