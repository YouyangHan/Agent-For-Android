package com.agentforandroid.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.agentforandroid.model.ModelConfig

@Entity(tableName = "model_configs")
data class ConfigEntity(
    @PrimaryKey val id: String,
    val name: String,
    val modelId: String,
    val apiKey: String,
    val baseUrl: String,
    val isDefault: Boolean = false
) {
    fun toModel(): ModelConfig = ModelConfig(
        id = id,
        name = name,
        modelId = modelId,
        apiKey = apiKey,
        baseUrl = baseUrl,
        isDefault = isDefault
    )

    companion object {
        fun fromModel(config: ModelConfig): ConfigEntity = ConfigEntity(
            id = config.id,
            name = config.name,
            modelId = config.modelId,
            apiKey = config.apiKey,
            baseUrl = config.baseUrl,
            isDefault = config.isDefault
        )
    }
}
