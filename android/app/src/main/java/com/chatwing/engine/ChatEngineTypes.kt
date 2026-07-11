package com.chatwing.engine

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
    val engineType: ChatEngineTypes.EngineType = ChatEngineTypes.EngineType.AUTO_PILOT
)

object ChatEngineTypes {
    enum class EngineType { AUTO_PILOT, STRATEGIST, INSPIRATION }
}
