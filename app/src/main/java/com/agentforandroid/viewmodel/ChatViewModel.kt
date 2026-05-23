package com.agentforandroid.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.agentforandroid.AgentApp
import com.agentforandroid.data.remote.LLMClient
import com.agentforandroid.model.ChatSession
import com.agentforandroid.model.Message
import com.agentforandroid.model.Skill
import com.agentforandroid.repository.ChatRepository
import com.agentforandroid.repository.ConfigRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as AgentApp
    private val chatRepo = ChatRepository(app.database)
    private val configRepo = ConfigRepository(app.database)
    private val skillRepo = com.agentforandroid.repository.SkillRepository.getInstance(application)
    private val toolExecutor = com.agentforandroid.tool.ToolExecutor(application).apply {
        onSkillCreated = {
            skillRepo.reloadSkills()
        }
    }

    private val prefs = application.getSharedPreferences("chat_prefs", android.content.Context.MODE_PRIVATE)
    private var sendJob: kotlinx.coroutines.Job? = null

    private val _streamingText = MutableStateFlow("")
    val streamingText: StateFlow<String> = _streamingText.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var currentSession: ChatSession? = null

    private var selectedConfigId: String? = null

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _personalitySkills = MutableStateFlow<List<Skill>>(emptyList())
    val personalitySkills: StateFlow<List<Skill>> = _personalitySkills.asStateFlow()

    // Personality: store content directly, independent of skill system
    private val _selectedPersonality = MutableStateFlow<Skill?>(null)
    val selectedPersonality: StateFlow<Skill?> = _selectedPersonality.asStateFlow()
    private var activePersonaContent: String = ""
    var didInit = false

    fun setPersonality(skill: Skill?) {
        val oldName = _selectedPersonality.value?.name
        val newName = skill?.name
        _selectedPersonality.value = skill
        activePersonaContent = if (skill != null) {
            if (skill.content.length > 3000) skill.content.take(3000) + "\n...(truncated)"
            else skill.content
        } else ""
        prefs.edit().putString("last_personality", skill?.name ?: "").apply()

        // Clear context when personality changes: AI won't see old persona's responses
        if (oldName != newName) {
            clearContext()
        }
    }
    fun getPersonality(): Skill? = _selectedPersonality.value

    private fun clearContext() {
        _messages.value = emptyList()
        _streamingText.value = ""
        _error.value = null
    }

    fun setSelectedConfigId(id: String) { selectedConfigId = id }
    fun getSelectedConfigId(): String? {
        if (selectedConfigId == null) {
            selectedConfigId = prefs.getString("last_config_id", null)
        }
        return selectedConfigId
    }

    fun restorePersonality() {
        val savedName = prefs.getString("last_personality", "") ?: ""
        if (savedName.isNotBlank()) {
            val skill = skillRepo.skills.value.find { it.name == savedName && it.isPersonality }
            setPersonality(skill)  // uses setPersonality which sets both state + content
        }
    }

    fun refreshPersonalities() {
        _personalitySkills.value = skillRepo.getPersonalitySkills()
        if (_selectedPersonality.value != null) {
            val stillExists = _personalitySkills.value.any { it.name == _selectedPersonality.value!!.name }
            if (!stillExists) _selectedPersonality.value = null
        }
    }

    companion object {
        const val BASE_SYSTEM_PROMPT =
            "You are Agent For Android, an AI assistant running on a mobile phone. " +
            "You provide helpful, accurate responses."
    }

    suspend fun initOrCreateSession(modelConfigId: String, enabledSkills: List<String>) {
        currentSession = chatRepo.createSession(modelConfigId, enabledSkills)
    }

    fun loadMessages(sessionId: String) {
        viewModelScope.launch {
            chatRepo.getMessages(sessionId).collect { msgs ->
                _messages.value = msgs
            }
        }
    }

    fun sendMessage(text: String) {
        // Cancel previous send if still running
        sendJob?.cancel()
        sendJob = viewModelScope.launch {
            try {
                // Get or create session inline (robust against LaunchedEffect timing)
                val session = currentSession ?: run {
                    val config = configRepo.getDefault()
                    if (config == null) {
                        _error.value = "请先在设置中配置一个模型"
                        return@launch
                    }
                    val enabled = skillRepo.getEnabledSkills().map { it.name }
                    val s = chatRepo.createSession(config.id, enabled)
                    currentSession = s
                    s
                }

                _error.value = null
                _isLoading.value = true
                _streamingText.value = ""

                // Save user message
                val userMsg = Message(
                    id = UUID.randomUUID().toString(),
                    sessionId = session.id,
                    role = "user",
                    content = text
                )
                chatRepo.saveMessage(userMsg)
                _messages.value = _messages.value + userMsg

                // Auto-title: use first user message (count user messages)
                val userMsgCount = _messages.value.count { it.role == "user" }
                if (userMsgCount == 1) {
                    val title = if (text.length > 30) text.take(30) + "..." else text
                    chatRepo.updateSessionTitle(session.id, title)
                }

                // Get model config (use selected or default)
                val config = if (selectedConfigId != null)
                    configRepo.getById(selectedConfigId!!)
                else
                    configRepo.getDefault()
                if (config == null) {
                    _error.value = "请先在设置中配置一个模型"
                    _isLoading.value = false
                    return@launch
                }

                // System prompt: activePersonaContent set atomically in setPersonality()
                val personaName = _selectedPersonality.value?.personalityName ?: "无"
                var systemPrompt: String
                if (activePersonaContent.isNotEmpty()) {
                    systemPrompt = "$activePersonaContent\n\n[系统: 当前性格=$personaName, 模型=${config.name}]"
                } else {
                    systemPrompt = "$BASE_SYSTEM_PROMPT\n[系统: 无性格, 模型=${config.name}]"
                }

                // Only NON-personality skills + tools
                val regularSkills = skillRepo.getEnabledSkills().filter { !it.isPersonality }
                systemPrompt = chatRepo.buildSystemPrompt(systemPrompt, regularSkills)
                systemPrompt += "\n\n" + com.agentforandroid.tool.ToolExecutor.TOOL_DESCRIPTIONS

                // Build messages for LLM
                val llmMessages = mutableListOf<Map<String, String>>()
                llmMessages.add(mapOf("role" to "system", "content" to systemPrompt))

                val historyMessages = _messages.value.map {
                    mapOf("role" to it.role, "content" to it.content)
                }
                llmMessages.addAll(historyMessages)

                // Stream response
                val fullResponse = StringBuilder()
                chatRepo.streamChat(config, llmMessages).collect { result ->
                    when (result) {
                        is LLMClient.LLMResult.Chunk -> {
                            fullResponse.append(result.text)
                            _streamingText.value = fullResponse.toString()
                        }
                        is LLMClient.LLMResult.Error -> {
                            _error.value = result.message
                        }
                        is LLMClient.LLMResult.Done -> {
                            val responseText = fullResponse.toString()

                            // Check for tool calls in the response
                            if (toolExecutor.hasToolCall(responseText)) {
                                val (cleanText, toolResults) = toolExecutor.executeToolCalls(responseText)
                                val displayText = if (cleanText.isNotBlank()) cleanText else "🔧 执行工具..."

                                // Save AI response with tool results appended
                                val combined = "$displayText\n\n$toolResults"
                                val assistantMsg = Message(
                                    id = UUID.randomUUID().toString(),
                                    sessionId = session.id,
                                    role = "assistant",
                                    content = combined
                                )
                                chatRepo.saveMessage(assistantMsg)
                                _messages.value = _messages.value + assistantMsg
                            } else {
                                val assistantMsg = Message(
                                    id = UUID.randomUUID().toString(),
                                    sessionId = session.id,
                                    role = "assistant",
                                    content = responseText
                                )
                                chatRepo.saveMessage(assistantMsg)
                                _messages.value = _messages.value + assistantMsg
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                val msg = e.localizedMessage ?: e.message ?: "无详情"
                _error.value = "[${e.javaClass.simpleName}] $msg"
            } finally {
                _streamingText.value = ""
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    override fun onCleared() {
        super.onCleared()
        prefs.edit().putString("last_config_id", selectedConfigId ?: "").apply()
        chatRepo.shutdown()
    }
}
