package com.agentforandroid.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.agentforandroid.model.ChatSession

@Entity(tableName = "chat_sessions")
data class SessionEntity(
    @PrimaryKey val id: String,
    val title: String,
    val modelConfigId: String,
    val enabledSkills: String = "",  // comma-separated skill names
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toModel(): ChatSession = ChatSession(
        id = id,
        title = title,
        modelConfigId = modelConfigId,
        enabledSkills = if (enabledSkills.isBlank()) emptyList() else enabledSkills.split(","),
        createdAt = createdAt
    )

    companion object {
        fun fromModel(session: ChatSession): SessionEntity = SessionEntity(
            id = session.id,
            title = session.title,
            modelConfigId = session.modelConfigId,
            enabledSkills = session.enabledSkills.joinToString(","),
            createdAt = session.createdAt
        )
    }
}
