package com.agentforandroid.model

data class ChatSession(
    val id: String,
    val title: String,
    val modelConfigId: String,
    val enabledSkills: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)
