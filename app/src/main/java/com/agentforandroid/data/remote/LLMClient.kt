package com.agentforandroid.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class LLMClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    data class LLMRequest(
        val model: String,
        val baseUrl: String,
        val apiKey: String,
        val messages: List<Map<String, String>>
    )

    sealed class LLMResult {
        data class Chunk(val text: String) : LLMResult()
        data class Error(val message: String) : LLMResult()
        data object Done : LLMResult()
    }

    fun streamChat(request: LLMRequest): Flow<LLMResult> = callbackFlow {
        val bodyJson = JSONObject().apply {
            if (request.model.isNotBlank()) put("model", request.model)
            put("messages", JSONArray(request.messages.map { msg ->
                JSONObject().apply {
                    put("role", msg["role"])
                    put("content", msg["content"])
                }
            }))
            put("stream", true)
        }

        val url = request.baseUrl.trimEnd('/') + "/chat/completions"
        val requestBody = bodyJson.toString()
            .toRequestBody("application/json".toMediaType())

        val httpRequest = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer ${request.apiKey}")
            .header("Content-Type", "application/json")
            .post(requestBody)
            .build()

        try {
            val response = client.newCall(httpRequest).execute()
            if (!response.isSuccessful) {
                val errorBody = try { response.body?.string()?.take(300) } catch (_: Exception) { "" } ?: ""
                trySend(LLMResult.Error(
                    when (response.code) {
                        401 -> "API Key 无效，请检查设置"
                        404 -> "模型 ID 不匹配，请检查配置"
                        429 -> "请求过于频繁，请稍后重试"
                        in 500..599 -> "模型服务异常，请稍后重试"
                        else -> "请求失败 (${response.code}): ${errorBody.take(150)}"
                    }
                ))
                close()
                return@callbackFlow
            }

            val body = response.body
            if (body == null) {
                trySend(LLMResult.Error("服务器返回空响应"))
                close()
                return@callbackFlow
            }

            val source = body.source()
            var hasContent = false
            while (!source.exhausted()) {
                val line = source.readUtf8Line() ?: break
                if (line.startsWith("data: ")) {
                    val json = line.removePrefix("data: ").trim()
                    if (json == "[DONE]") continue
                    try {
                        val delta = JSONObject(json)
                            .optJSONArray("choices")
                            ?.optJSONObject(0)
                            ?.optJSONObject("delta")
                            ?.optString("content", "") ?: ""
                        if (delta.isNotEmpty()) {
                            hasContent = true
                            trySend(LLMResult.Chunk(delta))
                        }
                    } catch (_: Exception) { /* skip bad chunks */ }
                }
            }
            if (!hasContent) {
                trySend(LLMResult.Error("未收到任何内容。请检查模型ID和API类型是否正确"))
            } else {
                trySend(LLMResult.Done)
            }
        } catch (e: IOException) {
            trySend(LLMResult.Error("网络连接失败: ${e.localizedMessage}"))
        } catch (e: Exception) {
            trySend(LLMResult.Error("请求出错: ${e.localizedMessage ?: "未知错误"}"))
        }
        close()
    }.flowOn(Dispatchers.IO)

    fun streamChatAnthropic(request: LLMRequest): Flow<LLMResult> = callbackFlow {
        // Separate system prompt from messages (Anthropic handles system differently)
        val systemPrompt = request.messages.firstOrNull { it["role"] == "system" }?.get("content") ?: ""
        val conversationMessages = request.messages.filter {
            it["role"] == "user" || it["role"] == "assistant"
        }

        val bodyJson = JSONObject().apply {
            if (request.model.isNotBlank()) put("model", request.model)
            put("max_tokens", 4096)
            put("stream", true)
            if (systemPrompt.isNotEmpty()) {
                put("system", systemPrompt)
            }
            put("messages", JSONArray(conversationMessages.map { msg ->
                JSONObject().apply {
                    put("role", msg["role"])
                    put("content", msg["content"])
                }
            }))
        }

        // Support both /v1/messages (standard) and /messages (custom endpoints)
        val base = request.baseUrl.trimEnd('/')
        val url = if (base.endsWith("/v1") || base.endsWith("/anthropic"))
            "$base/messages"
        else if (base.endsWith("/anthropic/v1"))
            "$base/messages"
        else if (!base.contains("/v1") && !base.endsWith("/messages"))
            "$base/v1/messages"
        else
            "$base/messages"

        val requestBody = bodyJson.toString()
            .toRequestBody("application/json".toMediaType())

        val httpRequest = Request.Builder()
            .url(url)
            .header("x-api-key", request.apiKey)
            .header("anthropic-version", "2023-06-01")
            .header("Content-Type", "application/json")
            .post(requestBody)
            .build()

        try {
            val response = client.newCall(httpRequest).execute()
            if (!response.isSuccessful) {
                val errorBody = try { response.body?.string()?.take(300) } catch (_: Exception) { "" } ?: ""
                trySend(LLMResult.Error(
                    when (response.code) {
                        401 -> "API Key 无效，请检查设置"
                        404 -> "模型 ID 不匹配，请检查配置。URL: $url"
                        429 -> "请求过于频繁，请稍后重试"
                        in 500..599 -> "模型服务异常，请稍后重试"
                        else -> "请求失败 (${response.code}): ${errorBody.take(150)}"
                    }
                ))
                close()
                return@callbackFlow
            }

            val body = response.body
            if (body == null) {
                trySend(LLMResult.Error("服务器返回空响应"))
                close()
                return@callbackFlow
            }

            val source = body.source()
            var hasContent = false
            while (!source.exhausted()) {
                val line = source.readUtf8Line() ?: break
                if (line.startsWith("data: ")) {
                    val json = line.removePrefix("data: ").trim()
                    try {
                        val event = JSONObject(json)

                        // Anthropic format: content_block_delta.delta.text
                        val eventType = event.optString("type", "")
                        if (eventType == "content_block_delta") {
                            val delta = event.optJSONObject("delta")
                            val text = delta?.optString("text", "") ?: ""
                            if (text.isNotEmpty()) {
                                hasContent = true
                                trySend(LLMResult.Chunk(text))
                            }
                        } else if (eventType == "message_stop") {
                            trySend(LLMResult.Done)
                            close()
                            return@callbackFlow
                        }
                        // Fallback: OpenAI format (some providers use this on anthropic endpoints)
                        else if (eventType.isEmpty() || eventType == "message_start" || eventType == "content_block_start") {
                            // Try OpenAI-style delta.content
                            val choices = event.optJSONArray("choices")
                            if (choices != null && choices.length() > 0) {
                                val delta = choices.optJSONObject(0)?.optJSONObject("delta")
                                val text = delta?.optString("content", "") ?: ""
                                if (text.isNotEmpty()) {
                                    hasContent = true
                                    trySend(LLMResult.Chunk(text))
                                }
                            }
                        }
                        // Also try top-level text field
                        if (!hasContent) {
                            val directText = event.optString("text", "")
                            if (directText.isNotEmpty()) {
                                hasContent = true
                                trySend(LLMResult.Chunk(directText))
                            }
                        }
                    } catch (_: Exception) { /* skip bad events */ }
                }
            }
            if (!hasContent) {
                trySend(LLMResult.Error("未收到任何内容。请检查模型ID和API类型是否正确"))
            } else {
                trySend(LLMResult.Done)
            }
        } catch (e: IOException) {
            trySend(LLMResult.Error("网络连接失败: ${e.localizedMessage}"))
        } catch (e: Exception) {
            trySend(LLMResult.Error("请求出错: ${e.localizedMessage ?: "未知错误"}"))
        }
        close()
    }.flowOn(Dispatchers.IO)

    fun shutdown() {
        client.dispatcher.executorService.shutdown()
        client.connectionPool.evictAll()
    }
}
