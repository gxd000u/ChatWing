package com.chatwing.engine

import com.chatwing.db.entity.MemoryEntity
import com.chatwing.llm.LLMClient

/**
 * 模式二：半自动「策略军师」
 * 用户收到消息后点击"生成回复"，并行生成5-6条不同风格候选
 * 分析对方心理状态，用户手动选择后复制到输入框
 */
class StrategistEngine : BaseChatEngine() {

    companion object {
        private const val TAG = "StrategistEngine"
    }

    override val maxTokens: Int get() = 1536

    override suspend fun loadMemory(memories: List<MemoryEntity>) {
        memories.filter { it.type == MemoryEntity.TYPE_SUMMARY }
            .sortedByDescending { it.timestamp }
            .firstOrNull()?.let { relationshipSummary = it.content }
    }

    override suspend fun generateReply(lastMessage: String, context: String): ReplyResult {
        val systemPrompt = buildString {
            appendLine(buildSystemPrompt())
            appendLine()
            appendLine("【当前模式：半自动·策略军师】")
            appendLine("你的任务：")
            appendLine("1. 分析对方最后一条消息的潜在心理状态（矜持、期待、测试、无聊、热情、回避等）")
            appendLine("2. 根据分析生成6条风格迥异的回复候选")
            appendLine()
            appendLine("【六种风格要求】")
            appendLine("1. 直球进攻：大胆直接，表达好感或邀约")
            appendLine("2. 幽默化解：用幽默感化解可能的尴尬")
            appendLine("3. 深情共鸣：共情对方的感受，表达理解")
            appendLine("4. 冷读推拉：先冷淡后拉回，制造好奇心")
            appendLine("5. 开启新话题：自然过渡到一个新的有趣话题")
            appendLine("6. 调皮调侃：略带挑衅的玩笑，测试反应")
            appendLine()
            appendLine("【输出格式】")
            appendLine("分析：<心理分析结果>")
            appendLine("1. [直球进攻] <回复内容>")
            appendLine("2. [幽默化解] <回复内容>")
            appendLine("3. [深情共鸣] <回复内容>")
            appendLine("4. [冷读推拉] <回复内容>")
            appendLine("5. [开启新话题] <回复内容>")
            appendLine("6. [调皮调侃] <回复内容>")
        }

        val userPrompt = buildString {
            appendLine(buildContextPrompt())
            appendLine()
            appendLine("对方最后一条消息：$lastMessage")
            appendLine("额外上下文：$context")
        }

        val response = LLMClient.chat(systemPrompt, userPrompt, maxTokens = maxTokens)

        // 解析响应
        val lines = response.split("\n")
        val analysis = lines.firstOrNull { it.startsWith("分析") }?.removePrefix("分析：")?.trim() ?: "未识别"
        val replies = lines.filter { it.matches(Regex("^\\d+\\.\\s*\\[.+\\]\\s*.+")) }
            .map { it.replace(Regex("^\\d+\\.\\s*\\[.+\\]\\s*"), "").trim() }

        return ReplyResult(
            replies = replies.ifEmpty { listOf(response) },
            analysis = analysis,
            engineType = EngineType.STRATEGIST
        )
    }
}
