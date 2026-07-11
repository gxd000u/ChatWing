package com.chatwing.engine

import com.chatwing.db.entity.ContactEntity
import com.chatwing.db.entity.MemoryEntity

/**
 * 聊天引擎基类
 * 定义三种引擎共用的Prompt拼接策略和接口规范
 */
abstract class BaseChatEngine {

    /** 当前对话的联系人 */
    protected var currentContact: ContactEntity? = null
    /** 用户人物小传（全局基础Prompt） */
    protected var userProfile: UserProfile = UserProfile()
    /** 最近的K轮对话 */
    protected val recentMessages = mutableListOf<ChatMessage>()
    /** 关系进展摘要记忆 */
    protected var relationshipSummary: String = ""

    /** 单次生成的最大token数 */
    protected open val maxTokens: Int = 1024
    /** 滑动窗口保留的最近轮数 */
    protected open val maxRecentRounds: Int = 10

    // ── 公开方法 ─────────────────────────────────────────

    /** 设置当前联系人 */
    fun setContact(contact: ContactEntity) { currentContact = contact }

    /** 更新用户人物小传 */
    fun setUserProfile(profile: UserProfile) { userProfile = profile }

    /** 添加一条对话记录 */
    fun addMessage(msg: ChatMessage) {
        recentMessages.add(msg)
        if (recentMessages.size > maxRecentRounds * 2) {
            // 触发滑动窗口压缩：将老旧对话压缩为摘要
            compressHistory()
        }
    }

    /** 设置已经压缩的历史摘要 */
    fun setRelationshipSummary(summary: String) { relationshipSummary = summary }

    /** 加载历史记忆 */
    abstract suspend fun loadMemory(memories: List<MemoryEntity>)

    /** 生成回复（由子类实现） */
    abstract suspend fun generateReply(lastMessage: String, context: String = ""): ReplyResult

    // ── Prompt 构建 ──────────────────────────────────────

    /**
     * 构建基础 System Prompt（所有人共享的部分）
     */
    protected open fun buildSystemPrompt(): String = buildString {
        appendLine("你是一个AI僚机助手ChatWing，正在帮助用户进行社交聊天。")
        appendLine()
        appendLine("【用户人物小传】")
        appendLine("年龄：${userProfile.age}")
        appendLine("星座：${userProfile.zodiac}")
        appendLine("MBTI：${userProfile.mbti}")
        appendLine("职业：${userProfile.occupation}")
        appendLine("兴趣爱好：${userProfile.hobbies}")
        appendLine("感情观：${userProfile.loveView}")
        appendLine("经典口癖：${userProfile.verbalTics}")
        appendLine("额外设定：${userProfile.extraInfo}")
        appendLine()
        appendLine("【通用规则】")
        appendLine("1. 回复必须自然口语化，无AI味，适当加入手滑、错别字或口语化表达")
        appendLine("2. 回复要有情绪价值，目标明确（撩妹/交友/破冰）")
        appendLine("3. 回复长度控制在20-80字之间，不要长篇大论")
        appendLine("4. 禁止出现'作为AI'、'作为助手'等暴露AI身份的内容")
        appendLine("5. 使用中文，偶尔夹杂英文单词增加真实感")
    }

    /**
     * 构建上下文 Prompt（包含联系人信息和对话历史）
     */
    protected open fun buildContextPrompt(): String = buildString {
        currentContact?.let { contact ->
            appendLine("【对方信息】")
            appendLine("昵称：${contact.nickname}")
            appendLine("备注：${contact.remark}")
            appendLine("主页分析：${contact.profileAnalysis}")
            appendLine()
        }
        if (relationshipSummary.isNotBlank()) {
            appendLine("【关系进展摘要】")
            appendLine(relationshipSummary)
            appendLine()
        }
        appendLine("【最近对话（最新在前）】")
        recentMessages.takeLast(maxRecentRounds).reversed().forEach { msg ->
            val sender = if (msg.isFromMe) "我" else "对方"
            appendLine("$sender: ${msg.content}")
        }
    }

    /**
     * 滑动窗口压缩：将最早的一半对话转移为摘要
     */
    private fun compressHistory() {
        val half = recentMessages.size / 2
        val toSummarize = recentMessages.take(half)
        recentMessages.removeAll(toSummarize)
        relationshipSummary = "$relationshipSummary\n[历史摘要] 前${half}轮对话已压缩：" +
                toSummarize.joinToString(" | ") { "${if (it.isFromMe) "我" else "对方"}：${it.content.substring(0, minOf(30, it.content.length))}..." }
    }

    // ── 数据类 ───────────────────────────────────────────

    data class UserProfile(
        var age: String = "",
        var zodiac: String = "",
        var mbti: String = "",
        var occupation: String = "",
        var income: String = "",
        var hobbies: String = "",
        var loveView: String = "",
        var verbalTics: String = "",
        var extraInfo: String = ""
    )

    data class ChatMessage(
        val content: String,
        val isFromMe: Boolean,
        val timestamp: Long = System.currentTimeMillis()
    )

    data class ReplyResult(
        val replies: List<String>,
        val analysis: String = "",
        val engineType: EngineType
    )

    enum class EngineType { AUTO_PILOT, STRATEGIST, INSPIRATION }
}
