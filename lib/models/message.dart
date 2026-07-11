/// 聊天消息数据模型
class ChatMessage {
  final String content;
  final bool isFromMe;
  final DateTime timestamp;
  final String? style;    // 灵感模式使用的风格标签
  final String? analysis; // 策略军师模式的心理分析

  ChatMessage({
    required this.content,
    required this.isFromMe,
    DateTime? timestamp,
    this.style,
    this.analysis,
  }) : timestamp = timestamp ?? DateTime.now();

  Map<String, dynamic> toMap() => {
        'content': content,
        'isFromMe': isFromMe ? 1 : 0,
        'timestamp': timestamp.millisecondsSinceEpoch,
        'style': style,
        'analysis': analysis,
      };

  factory ChatMessage.fromMap(Map<String, dynamic> map) => ChatMessage(
        content: map['content'] as String,
        isFromMe: (map['isFromMe'] as int) == 1,
        timestamp: map['timestamp'] != null
            ? DateTime.fromMillisecondsSinceEpoch(map['timestamp'] as int)
            : null,
        style: map['style'] as String?,
        analysis: map['analysis'] as String?,
      );
}

/// 生成回复的结果
class ReplyResult {
  final List<String> replies;
  final String analysis;
  final EngineType engineType;

  ReplyResult({
    required this.replies,
    this.analysis = '',
    this.engineType = EngineType.strategist,
  });
}

enum EngineType { autoPilot, strategist, inspiration }

enum ChatStyle {
  humorous('风趣幽默'),
  warm('暖男'),
  deep('深情'),
  intellectual('知性'),
  dominant('霸道');

  final String label;
  const ChatStyle(this.label);
}
