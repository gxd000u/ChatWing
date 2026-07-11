package com.chatwing.engine

import com.chatwing.db.entity.MemoryEntity
import com.chatwing.llm.LLMClient

/**
 * 模式一：全托管「赛博替身」
 * 自动监听新消息，全自动生成并发送回复
 * 支持强制终结、主动出击、发表情包等干预按钮
 */
class AutoPilotEngine : BaseChatEngine() {

    companion object {
        private const val TAG = "AutoPilotEngine"
    }

    /** 是否处于托管状态 */
    var isActive = false; private set
    /** 自动回复间隔（毫秒），模拟人类打字速度 */
    var replyDelayMs: Long = 2000L

    private val interventionQueue = mutableListOf<Intervention>()

    fun activate() { isActive = true }
    fun deactivate() { isActive = false; interventionQueue.clear() }

    /** 添加干预指令 */
    fun addIntervention(type: Intervention) { interventionQueue.add(type) }

    override suspend fun loadMemory(memories: List<MemoryEntity>) {
        // 从记忆中恢复关系进展摘要
        memories.filter { it.type == MemoryEntity.TYPE_SUMMARY }
            .sortedByDescending { it.timestamp }
            .firstOrNull()?.let { relationshipSummary = it.content }
    }

    override suspend fun generateReply(lastMessage: String, context: String): ReplyResult {
        if (!isActive) return ReplyResult(emptyList(), "引擎未激活", EngineType.AUTO_PILOT)

        // 检查是否有待处理的干预指令
        val intervention = interventionQueue.removeFirstOrNull()
        val interventionInstruction = when (intervention) {
            Intervention.FORCE_END -> "\n[用户干预] 请用自然的方式终结当前话题，回复要简短礼貌。"
            Intervention.PROACTIVE -> "\n[用户干预] 请主动出击，发起一个新话题或邀约，风格大胆有趣。"
            Intervention.SEND_STICKER -> "\n[用户干预] 请生成一段适合配合表情包的文字（短句），活泼俏皮。"
            null -> ""
        }

        val systemPrompt = buildString {
            appendLine(buildSystemPrompt())
            appendLine()
            appendLine("【当前模式：全托管·赛博替身】")
            appendLine("- 你完全代替用户进行聊天，无需用户确认")
            appendLine("- 回复要极度自然，带轻微手滑/错别字/口语化")
            appendLine("- 目标是撩妹/交友，情绪价值拉满")
            appendLine("- 每句话都要让对方有接话的欲望")
            if (interventionInstruction.isNotBlank()) appendLine(interventionInstruction)
            appendLine()
            appendLine("【输出格式】只输出回复文本，不要加引号、前缀或说明。")
        }

        val userPrompt = buildString {
            appendLine(buildContextPrompt())
            appendLine()
            appendLine("对方最后一条消息：$lastMessage")
            appendLine()
            appendLine("请生成1条回复：")
        }

        val reply = LLMClient.chat(systemPrompt, userPrompt, maxTokens = maxTokens)

        return ReplyResult(
            replies = listOf(reply),
            analysis = "全托管模式自动生成",
            engineType = EngineType.AUTO_PILOT
        )
    }

    enum class Intervention {
        FORCE_END,      // 强制终结话题
        PROACTIVE,      // 主动出击
        SEND_STICKER    // 发一个表情包
    }
}
