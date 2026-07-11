package com.chatwing.db.dao

import androidx.room.*
import com.chatwing.db.entity.MemoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryDao {

    @Query("SELECT * FROM memories WHERE contact_id = :contactId ORDER BY timestamp DESC")
    fun getMemoriesForContact(contactId: Long): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memories WHERE contact_id = :contactId AND type = :type ORDER BY timestamp DESC")
    suspend fun getMemoriesByType(contactId: Long, type: String): List<MemoryEntity>

    @Query("SELECT * FROM memories WHERE contact_id = :contactId AND type = 'summary' ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestSummary(contactId: Long): MemoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(memory: MemoryEntity): Long

    @Query("DELETE FROM memories WHERE contact_id = :contactId")
    suspend fun deleteAllForContact(contactId: Long)

    @Delete
    suspend fun delete(memory: MemoryEntity)
}
