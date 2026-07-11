import 'package:flutter/foundation.dart';
import '../models/message.dart';
import '../services/llm_service.dart';

/// 聊天引擎状态管理
/// 管理三种模式的激活状态、样式切换、回复生成
class EngineState extends ChangeNotifier {
  EngineType _activeMode = EngineType.strategist;
  ChatStyle _currentStyle = ChatStyle.humorous;
  bool _isAutoPilotActive = false;
  bool _isLoading = false;
  List<String> _currentReplies = [];
  String _analysis = '';
  String _lastError = '';

  EngineType get activeMode => _activeMode;
  ChatStyle get currentStyle => _currentStyle;
  bool get isAutoPilotActive => _isAutoPilotActive;
  bool get isLoading => _isLoading;
  List<String> get currentReplies => _currentReplies;
  String get analysis => _analysis;
  String get lastError => _lastError;

  /// 切换模式
  void switchMode(EngineType mode) {
    _activeMode = mode;
    _currentReplies = [];
    _analysis = '';
    _lastError = '';
    notifyListeners();
  }

  /// 切换聊天风格（灵感模式）
  void switchStyle(ChatStyle style) {
    _currentStyle = style;
    notifyListeners();
  }

  /// 开启/关闭赛博替身
  void toggleAutoPilot() {
    _isAutoPilotActive = !_isAutoPilotActive;
    notifyListeners();
  }

  /// 生成回复
  Future<void> generateReply({
    required String lastMessage,
    String context = '',
    String systemPrompt = '',
  }) async {
    _isLoading = true;
    _lastError = '';
    notifyListeners();

    try {
      switch (_activeMode) {
        case EngineType.autoPilot:
          final reply = await _generateAutoPilot(lastMessage);
          _currentReplies = [reply];
          break;
        case EngineType.strategist:
          _currentReplies =
              await _generateStrategistReplies(lastMessage, context);
          break;
        case EngineType.inspiration:
          final reply = await _generateInspiration(lastMessage);
          _currentReplies = [reply];
          break;
      }
    } catch (e) {
      _lastError = e.toString();
      _currentReplies = [];
    }

    _isLoading = false;
    notifyListeners();
  }

  Future<String> _generateAutoPilot(String lastMessage) async {
    return LLMService.chat(
      systemPrompt: '''
你是一个AI僚机助手ChatWing，正在全托管模式下代替用户聊天。
回复要求：
- 极度自然口语化，带轻微手滑或错别字
- 情绪价值拉满，目标是撩妹/交友
- 每句话都要让对方有接话的欲望
- 长度20-60字
''',
      userPrompt: '对方最后一条消息：$lastMessage\n\n请生成回复：',
    );
  }

  Future<List<String>> _generateStrategistReplies(
      String lastMessage, String context) async {
    final response = await LLMService.chat(
      systemPrompt: '''
你是一个AI军师，分析对方心理并生成6条不同风格的回复候选。
分析对方心理（矜持/期待/测试/无聊/热情/回避等）。
风格要求：
1. 直球进攻：大胆直接
2. 幽默化解：用幽默化解尴尬
3. 深情共鸣：共情理解
4. 冷读推拉：先冷淡后拉回
5. 开启新话题：自然过渡
6. 调皮调侃：略带挑衅

输出格式：
分析：<心理分析>
1. [直球进攻] <内容>
2. [幽默化解] <内容>
...
''',
      userPrompt: '对方消息：$lastMessage\n上下文：$context',
      maxTokens: 1536,
    );

    // 解析回复
    final lines = response.split('\n');
    _analysis = lines
            .firstWhere((l) => l.startsWith('分析'),
                orElse: () => '未识别')
            .replaceFirst('分析：', '')
            .trim() ??
        '';

    final replyRegex = RegExp(r'^\d+\.\s*\[.+?\]\s*(.+)$');
    final replies = lines
        .map((l) => replyRegex.firstMatch(l)?.group(1)?.trim())
        .whereType<String>()
        .toList();

    return replies.isNotEmpty ? replies : [response];
  }

  Future<String> _generateInspiration(String lastMessage) async {
    final stylePrompts = {
      ChatStyle.humorous: '风趣幽默，像段子手',
      ChatStyle.warm: '温暖体贴，像冬日暖阳',
      ChatStyle.deep: '感性深情，有文艺气息',
      ChatStyle.intellectual: '知性有品位，展示学识',
      ChatStyle.dominant: '主导感，略带强势',
    };

    return LLMService.chat(
      systemPrompt: '''
你是一个AI文案生成器，根据用户粘贴的内容生成回复。
风格：${stylePrompts[_currentStyle] ?? '自然口语'}
规则：只输出回复文本，15-50字，有回勾。
''',
      userPrompt: '对方发的消息：$lastMessage',
      maxTokens: 512,
    );
  }

  void clearReplies() {
    _currentReplies = [];
    _analysis = '';
    notifyListeners();
  }
}
