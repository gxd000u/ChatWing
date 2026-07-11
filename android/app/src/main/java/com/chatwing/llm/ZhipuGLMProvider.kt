package com.chatwing.llm

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * 智谱 GLM API 适配器
 * 备用模型，擅长中文理解和创意生成
 */
class ZhipuGLMProvider : LLMProvider {

    companion object {
        private const val DEFAULT_BASE_URL = "https://open.bigmodel.cn/api/paas/v4"
        private const val DEFAULT_MODEL = "glm-4-flash"
    }

    override val name: String get() = "智谱GLM"
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

        OutputStreamWriter(connection.outputStream).use { it.write(jsonBody.toString()); it.flush() }

        val code = connection.responseCode
        if (code != 200) throw RuntimeException("GLM API error $code: ${connection.errorStream?.bufferedReader()?.readText()}")

        val json = JSONObject(connection.inputStream.bufferedReader().readText())
        json.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content").trim()
    }
}
