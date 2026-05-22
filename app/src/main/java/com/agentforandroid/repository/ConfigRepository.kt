package com.agentforandroid.repository

import com.agentforandroid.data.local.AppDatabase
import com.agentforandroid.data.local.entity.ConfigEntity
import com.agentforandroid.model.ModelConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class ConfigRepository(private val database: AppDatabase) {

    private val dao = database.configDao()

    fun getAll(): Flow<List<ModelConfig>> =
        dao.getAll().map { entities -> entities.map { it.toModel() } }

    suspend fun getDefault(): ModelConfig? = dao.getDefault()?.toModel()

    suspend fun getById(id: String): ModelConfig? = dao.getById(id)?.toModel()

    suspend fun add(
        name: String,
        modelId: String,
        apiKey: String,
        baseUrl: String,
        apiType: String = "openai"
    ): ModelConfig {
        val config = ModelConfig(
            id = UUID.randomUUID().toString(),
            name = name,
            modelId = modelId,
            apiKey = apiKey,
            baseUrl = baseUrl,
            apiType = apiType
        )
        dao.insert(ConfigEntity.fromModel(config))
        return config
    }

    suspend fun update(config: ModelConfig) {
        dao.update(ConfigEntity.fromModel(config))
    }

    suspend fun delete(config: ModelConfig) {
        dao.delete(ConfigEntity.fromModel(config))
    }

    suspend fun setDefault(id: String) {
        dao.clearDefaults()
        dao.setDefault(id)
    }
}
