package com.chatwing.engine

import com.chatwing.db.entity.MemoryEntity
import com.chatwing.llm.LLMClient

/**
 * 模式三：纯手动「灵感文案」
 * 用户粘贴对方发来的内容，点击"粘贴即回"快速生成回复
 * 支持手动切换聊天风格（风趣幽默/暖男/深情/知性/霸道）
 */
class InspirationEngine : BaseChatEngine() {

    companion object {
        private const val TAG = "InspirationEngine"
    }

    /** 当前选中的聊天风格 */
    var currentStyle: ChatStyle = ChatStyle.HUMOROUS

    override suspend fun loadMemory(memories: List<MemoryEntity>) {
        // 灵感模式不依赖记忆
    }

    override suspend fun generateReply(lastMessage: String, context: String): ReplyResult {
        val systemPrompt = buildString {
            appendLine("你是一个AI僚机文案生成器，根据用户粘贴的对方消息，快速生成一条高质量回复。")
            appendLine()
            appendLine("【当前风格：${currentStyle.label}】")
            appendLine(currentStyle.prompt)
            appendLine()
            appendLine("【规则】")
            appendLine("1. 只输出回复文本，不加任何说明")
            appendLine("2. 回复要自然，像真人说的话")
            appendLine("3. 长度控制在15-60字之间")
            appendLine("4. 要有回勾（让对方想继续聊）")
        }

        val userPrompt = "对方发的消息：$lastMessage\n\n请根据【${currentStyle.label}】风格生成回复："

        val reply = LLMClient.chat(systemPrompt, userPrompt, maxTokens = 512)

        return ReplyResult(
            replies = listOf(reply),
            analysis = "灵感文案·${currentStyle.label}",
            engineType = EngineType.INSPIRATION
        )
    }

    /** 切换聊天风格 */
    fun switchStyle(style: ChatStyle) { currentStyle = style }

    enum class ChatStyle(val label: String, val prompt: String) {
        HUMOROUS("风趣幽默", "回复要幽默风趣，用段子手的感觉，让对方笑出来。"),
        WARM("暖男", "回复要温暖体贴，关心对方的感受，像冬日暖阳。"),
        DEEP("深情", "回复要感性深情，有文艺气息，打动人心。"),
        INTELLECTUAL("知性", "回复要有文化底蕴，展示学识和品位，不轻浮。"),
        DOMINANT("霸道", "回复要有主导感，略带强势但不过分，让对方感觉被保护。");
    }
}
