package com.agentforandroid.data.local.dao

import androidx.room.*
import com.agentforandroid.data.local.entity.ConfigEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConfigDao {
    @Query("SELECT * FROM model_configs ORDER BY isDefault DESC")
    fun getAll(): Flow<List<ConfigEntity>>

    @Query("SELECT * FROM model_configs WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefault(): ConfigEntity?

    @Query("SELECT * FROM model_configs WHERE id = :id")
    suspend fun getById(id: String): ConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(config: ConfigEntity)

    @Update
    suspend fun update(config: ConfigEntity)

    @Delete
    suspend fun delete(config: ConfigEntity)

    @Query("UPDATE model_configs SET isDefault = 0")
    suspend fun clearDefaults()

    @Query("UPDATE model_configs SET isDefault = 1 WHERE id = :id")
    suspend fun setDefault(id: String)
}
