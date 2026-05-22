package com.agentforandroid.data.remote

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
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
            put("model", request.model)
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
                trySend(LLMResult.Error(
                    when (response.code) {
                        401 -> "API Key 无效，请检查设置"
                        404 -> "模型 ID 不匹配，请检查配置"
                        429 -> "请求过于频繁，请稍后重试"
                        in 500..599 -> "模型服务异常，请稍后重试"
                        else -> "请求失败 (${response.code})"
                    }
                ))
                close()
                return@callbackFlow
            }

            response.body?.source()?.let { source ->
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
                                trySend(LLMResult.Chunk(delta))
                            }
                        } catch (_: Exception) { /* skip bad chunks */ }
                    }
                }
            }
            trySend(LLMResult.Done)
        } catch (e: IOException) {
            trySend(LLMResult.Error("网络连接失败: ${e.localizedMessage}"))
        }
        close()
    }

    fun shutdown() {
        client.dispatcher.executorService.shutdown()
        client.connectionPool.evictAll()
    }
}
