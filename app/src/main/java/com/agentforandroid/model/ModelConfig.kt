package com.agentforandroid.model

data class ModelConfig(
    val id: String,
    val name: String,
    val modelId: String,
    val apiKey: String,
    val baseUrl: String,
    val apiType: String = "anthropic",
    val isDefault: Boolean = false
)
