package com.chatwing.llm

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * ???? API ???
 * ?????????????????
 */
class TongyiQwenProvider : LLMProvider {

    companion object {
        private const val DEFAULT_BASE_URL = "https://dashscope.aliyuncs.com/api/v1"
        private const val DEFAULT_MODEL = "qwen-turbo"
    }

    override val name: String get() = "????"
    override var apiKey: String = ""
    override val baseUrl: String get() = DEFAULT_BASE_URL
    override val modelName: String get() = DEFAULT_MODEL

    private val chatEndpoint: String get() = "$baseUrl/services/aigc/text-generation/generation"

    override suspend fun chat(
        messages: List<LLMProvider.ChatMessage>,
        temperature: Float,
        maxTokens: Int
    ): String = withContext(Dispatchers.IO) {
        val jsonBody = JSONObject().apply {
            put("model", modelName)
            put("input", JSONObject().apply {
                put("messages", JSONArray().apply {
                    messages.forEach { msg ->
                        put(JSONObject().apply {
                            put("role", msg.role)
                            put("content", msg.content)
                        })
                    }
                })
            })
            put("parameters", JSONObject().apply {
                put("temperature", temperature)
                put("max_tokens", maxTokens)
                put("result_format", "message")
            })
        }

        val connection = URL(chatEndpoint).openConnection() as HttpURLConnection
        connection.apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Authorization", "Bearer $apiKey")
            doOutput = true
            connectTimeout = 30000
            readTimeout = 60000
        }

        OutputStreamWriter(connection.outputStream).use { it.write(jsonBody.toString()); it.flush() }

        val code = connection.responseCode
        if (code != 200) throw RuntimeException("Qwen API error $code: ${connection.errorStream?.bufferedReader()?.readText()}")

        val json = JSONObject(connection.inputStream.bufferedReader().readText())
        json.getJSONObject("output").getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content").trim()
    }
}

