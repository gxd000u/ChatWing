package com.chatwing.llm

/**
 * 大模型客户端统一入口
 * 封装底层 Provider，提供便捷调用方法
 * 支持运行时切换 Provider
 */
object LLMClient {

    private var currentProvider: LLMProvider = DeepSeekProvider()

    /** Provider 类型枚举 */
    enum class ProviderType { DEEPSEEK, ZHIPU_GLM, TONGYI_QWEN }

    /** 当前使用的 Provider */
    fun getProvider(): LLMProvider = currentProvider

    /** 切换 Provider */
    fun switchProvider(type: ProviderType) {
        currentProvider = when (type) {
            ProviderType.DEEPSEEK -> DeepSeekProvider()
            ProviderType.ZHIPU_GLM -> ZhipuGLMProvider()
            ProviderType.TONGYI_QWEN -> TongyiQwenProvider()
        }
    }

    /** 更新 API Key（加密存储和读取由调用方负责） */
    fun setApiKey(key: String) { currentProvider.apiKey = key }

    /**
     * 简易聊天调用
     * @param systemPrompt 系统提示词
     * @param userPrompt 用户输入
     * @param temperature 生成温度 0.0-1.0
     * @param maxTokens 最大生成 token 数
     */
    suspend fun chat(
        systemPrompt: String,
        userPrompt: String,
        temperature: Float = 0.8f,
        maxTokens: Int = 1024
    ): String {
        val messages = listOf(
            LLMProvider.ChatMessage("system", systemPrompt),
            LLMProvider.ChatMessage("user", userPrompt)
        )
        return currentProvider.chat(messages, temperature, maxTokens)
    }

    /**
     * 带历史的多轮聊天
     */
    suspend fun chatWithHistory(
        systemPrompt: String,
        history: List<Pair<String, String>>,  // (user, assistant) pairs
        userPrompt: String,
        temperature: Float = 0.8f,
        maxTokens: Int = 1024
    ): String {
        val messages = mutableListOf(LLMProvider.ChatMessage("system", systemPrompt))
        history.forEach { (user, assistant) ->
            messages.add(LLMProvider.ChatMessage("user", user))
            messages.add(LLMProvider.ChatMessage("assistant", assistant))
        }
        messages.add(LLMProvider.ChatMessage("user", userPrompt))
        return currentProvider.chat(messages, temperature, maxTokens)
    }
}
