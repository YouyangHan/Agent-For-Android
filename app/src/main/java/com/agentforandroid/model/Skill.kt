package com.agentforandroid.model

data class Skill(
    val name: String,
    val description: String,
    val content: String,
    val sourcePath: String,
    val isBuiltin: Boolean,
    val enabled: Boolean = true
)
