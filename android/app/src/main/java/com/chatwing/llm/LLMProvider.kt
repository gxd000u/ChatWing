package com.chatwing.llm

/**
 * 大模型供应商接口
 * 所有国产大模型适配器需实现此接口
 * 默认接入 DeepSeek，预留接口可切换至智谱、通义千问
 */
interface LLMProvider {
    /** 供应商名称 */
    val name: String
    /** 要加密存储的 API Key */
    var apiKey: String
    /** API 基础地址 */
    val baseUrl: String
    /** 模型名称 */
    val modelName: String

    /**
     * 发送聊天请求
     * @param messages 消息列表（role + content）
     * @param temperature 生成温度
     * @param maxTokens 最大token数
     * @return 模型回复文本
     */
    suspend fun chat(
        messages: List<ChatMessage>,
        temperature: Float = 0.8f,
        maxTokens: Int = 1024
    ): String

    data class ChatMessage(
        val role: String,  // "system", "user", "assistant"
        val content: String
    )
}
