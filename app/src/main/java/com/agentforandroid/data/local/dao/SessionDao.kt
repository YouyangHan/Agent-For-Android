package com.agentforandroid.data.local.dao

import androidx.room.*
import com.agentforandroid.data.local.entity.SessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Query("SELECT * FROM chat_sessions ORDER BY createdAt DESC")
    fun getAll(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM chat_sessions WHERE id = :id")
    suspend fun getById(id: String): SessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: SessionEntity)

    @Update
    suspend fun update(session: SessionEntity)

    @Delete
    suspend fun delete(session: SessionEntity)
}
