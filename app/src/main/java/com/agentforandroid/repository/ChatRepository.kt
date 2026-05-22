package com.agentforandroid.repository

import com.agentforandroid.data.local.AppDatabase
import com.agentforandroid.data.local.entity.MessageEntity
import com.agentforandroid.data.local.entity.SessionEntity
import com.agentforandroid.data.remote.LLMClient
import com.agentforandroid.model.ChatSession
import com.agentforandroid.model.Message
import com.agentforandroid.model.ModelConfig
import com.agentforandroid.model.Skill
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class ChatRepository(private val database: AppDatabase) {

    private val sessionDao = database.sessionDao()
    private val messageDao = database.messageDao()
    private val llmClient = LLMClient()

    fun getSessions(): Flow<List<ChatSession>> =
        sessionDao.getAll().map { entities -> entities.map { it.toModel() } }

    fun getMessages(sessionId: String): Flow<List<Message>> =
        messageDao.getBySession(sessionId).map { entities -> entities.map { it.toModel() } }

    suspend fun createSession(modelConfigId: String, enabledSkills: List<String>): ChatSession {
        val session = ChatSession(
            id = UUID.randomUUID().toString(),
            title = "新对话",
            modelConfigId = modelConfigId,
            enabledSkills = enabledSkills
        )
        sessionDao.insert(SessionEntity.fromModel(session))
        return session
    }

    suspend fun updateSessionTitle(sessionId: String, title: String) {
        val entity = sessionDao.getById(sessionId) ?: return
        sessionDao.update(entity.copy(title = title))
    }

    suspend fun saveMessage(message: Message) {
        messageDao.insert(MessageEntity.fromModel(message))
    }

    fun buildSystemPrompt(basePrompt: String, skills: List<Skill>): String {
        if (skills.isEmpty()) return basePrompt
        val skillsSection = skills.joinToString("\n\n") { skill ->
            "[Skill: ${skill.name}]\n${skill.content}"
        }
        return "$basePrompt\n\n## Active Skills\n\n$skillsSection"
    }

    fun streamChat(
        config: ModelConfig,
        messages: List<Map<String, String>>
    ): Flow<LLMClient.LLMResult> {
        return llmClient.streamChat(
            LLMClient.LLMRequest(
                model = config.modelId,
                baseUrl = config.baseUrl,
                apiKey = config.apiKey,
                messages = messages
            )
        )
    }

    fun shutdown() {
        llmClient.shutdown()
    }
}
