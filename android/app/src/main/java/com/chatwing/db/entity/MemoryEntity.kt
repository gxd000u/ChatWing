package com.chatwing.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 记忆实体
 * 每个联系人有独立的记忆列表，用于存储对话摘要和关键信息
 * 采用"摘要+最近K轮"的滑动窗口架构
 */
@Entity(
    tableName = "memories",
    foreignKeys = [
        ForeignKey(
            entity = ContactEntity::class,
            parentColumns = ["id"],
            childColumns = ["contact_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("contact_id")]
)
data class MemoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** 关联的联系人 ID */
    @ColumnInfo(name = "contact_id")
    val contactId: Long,

    /** 记忆类型 */
    val type: String = TYPE_SUMMARY,

    /** 记忆内容（摘要文本或向量化后的 JSON） */
    val content: String,

    /** 记忆标签（用于分类检索） */
    val tags: String = "",

    /** 创建时间戳 */
    val timestamp: Long = System.currentTimeMillis(),

    /** 嵌入向量（Base64 编码的浮点数组，用于相似度检索） */
    val embedding: String = ""
) {
    companion object {
        const val TYPE_SUMMARY = "summary"         // 关系进展摘要
        const val TYPE_KEY_INFO = "key_info"        // 对方重要信息（生日、喜好等）
        const val TYPE_TOPIC = "topic"              // 话题记录
        const val TYPE_EMOTION = "emotion"          // 情绪状态记录
    }
}
