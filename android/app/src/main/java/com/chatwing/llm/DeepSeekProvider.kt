package com.chatwing.llm

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * DeepSeek API 适配器
 * 默认接入 deepseek-chat 模型，中文表现优秀且价格低廉
 */
class DeepSeekProvider : LLMProvider {

    companion object {
        private const val DEFAULT_BASE_URL = "https://api.deepseek.com/v1"
        private const val DEFAULT_MODEL = "deepseek-chat"
    }

    override val name: String get() = "DeepSeek"
    override var apiKey: String = ""
    override val baseUrl: String get() = DEFAULT_BASE_URL
    override val modelName: String get() = DEFAULT_MODEL

    private val chatEndpoint: String get() = "$baseUrl/chat/completions"

    override suspend fun chat(
        messages: List<LLMProvider.ChatMessage>,
        temperature: Float,
        maxTokens: Int
    ): String = withContext(Dispatchers.IO) {
        val jsonBody = JSONObject().apply {
            put("model", modelName)
            put("temperature", temperature)
            put("max_tokens", maxTokens)
            put("messages", JSONArray().apply {
                messages.forEach { msg ->
                    put(JSONObject().apply {
                        put("role", msg.role)
                        put("content", msg.content)
                    })
                }
            })
        }

        val connection = URL(chatEndpoint).openConnection() as HttpURLConnection
        connection.apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Authorization", "Bearer $apiKey")
            doOutput = true
            connectTimeout = 30000
            readTimeout = 30000
        }

        OutputStreamWriter(connection.outputStream).use { writer ->
            writer.write(jsonBody.toString())
            writer.flush()
        }

        val responseCode = connection.responseCode
        if (responseCode != 200) {
            val error = connection.errorStream?.bufferedReader()?.readText() ?: "Unknown error"
            throw RuntimeException("DeepSeek API error $responseCode: $error")
        }

        val response = connection.inputStream.bufferedReader().readText()
        val json = JSONObject(response)
        json.getJSONArray("choices")
            .getJSONObject(0)
            .getJSONObject("message")
            .getString("content")
            .trim()
    }
}
