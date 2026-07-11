package com.chatwing.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 联系人实体
 * 每个联系人有独立的记忆库
 */
@Entity(tableName = "contacts")
data class ContactEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** 微信/App 内的昵称 */
    val nickname: String,

    /** 用户手动设置的备注 */
    var remark: String = "",

    /** 头像 Base64 编码（小图，不存文件） */
    var avatarBase64: String = "",

    /** 联系人来源包名（com.tencent.mm 等） */
    val sourcePackage: String = "",

    /** 主页简介文本（OCR/无障碍抓取） */
    var profileBio: String = "",

    /** 主页分析的 JSON 结果（破冰话题、兴趣图谱） */
    var profileAnalysis: String = "",

    /** 最近K轮对话的缓存（JSON 数组） */
    var recentChatCache: String = "",

    /** 最后活跃时间戳 */
    @ColumnInfo(name = "last_active_at")
    var lastActiveAt: Long = System.currentTimeMillis(),

    /** 创建时间 */
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
