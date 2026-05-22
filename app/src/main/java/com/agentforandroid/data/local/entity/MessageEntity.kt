package com.agentforandroid.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.agentforandroid.model.Message

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val role: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toModel(): Message = Message(
        id = id,
        sessionId = sessionId,
        role = role,
        content = content,
        timestamp = timestamp
    )

    companion object {
        fun fromModel(message: Message): MessageEntity = MessageEntity(
            id = message.id,
            sessionId = message.sessionId,
            role = message.role,
            content = message.content,
            timestamp = message.timestamp
        )
    }
}
