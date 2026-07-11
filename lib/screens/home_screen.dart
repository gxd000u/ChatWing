import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../models/message.dart';
import '../providers/engine_state.dart';
import '../providers/contact_state.dart';
import 'contact_list_screen.dart';
import 'profile_editor_screen.dart';
import 'settings_screen.dart';
import '../widgets/reply_card.dart';

/// 主页面 - 融合三种模式的控制面板
class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  final _messageController = TextEditingController();
  final _contextController = TextEditingController();
  int _selectedIndex = 0;

  final _pages = <Widget>[
    const _ChatPanel(),
    const ContactListScreen(),
    const ProfileEditorScreen(),
    const SettingsScreen(),
  ];

  @override
  void dispose() {
    _messageController.dispose();
    _contextController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: _pages[_selectedIndex],
      bottomNavigationBar: NavigationBar(
        selectedIndex: _selectedIndex,
        onDestinationSelected: (i) => setState(() => _selectedIndex = i),
        destinations: const [
          NavigationDestination(icon: Icon(Icons.chat_bubble), label: '聊天'),
          NavigationDestination(icon: Icon(Icons.people), label: '联系人'),
          NavigationDestination(icon: Icon(Icons.person), label: '人物小传'),
          NavigationDestination(icon: Icon(Icons.settings), label: '设置'),
        ],
      ),
    );
  }
}

/// 聊天控制面板
class _ChatPanel extends StatefulWidget {
  const _ChatPanel();

  @override
  State<_ChatPanel> createState() => _ChatPanelState();
}

class _ChatPanelState extends State<_ChatPanel> {
  final _textController = TextEditingController();
  final _contextController = TextEditingController();

  @override
  void dispose() {
    _textController.dispose();
    _contextController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final engineState = context.watch<EngineState>();
    final contactState = context.watch<ContactState>();

    return Scaffold(
      appBar: AppBar(
        title: Text(
            contactState.selectedContact?.nickname ?? 'ChatWing AI 僚机'),
        actions: [
          // 悬浮窗控制
          IconButton(
              icon: const Icon(Icons.circle_outlined),
              tooltip: '悬浮球',
              onPressed: () {}),
          // 无障碍服务
          IconButton(
              icon: Icon(
                  engineState.isAutoPilotActive
                      ? Icons.brightness_high
                      : Icons.accessibility_new,
                  color: engineState.isAutoPilotActive
                      ? Colors.green
                      : null),
              tooltip: '无障碍服务',
              onPressed: () {}),
        ],
      ),
      body: Column(
        children: [
          // 模式切换
          _ModeSelector(
            currentMode: engineState.activeMode,
            onChanged: (mode) => engineState.switchMode(mode),
          ),

          // 赛博替身控制栏
          if (engineState.activeMode == EngineType.autoPilot)
            _AutoPilotControls(
              isActive: engineState.isAutoPilotActive,
              onToggle: () => engineState.toggleAutoPilot(),
            ),

          // 风格选择器（灵感模式）
          if (engineState.activeMode == EngineType.inspiration)
            _StyleSelector(
              currentStyle: engineState.currentStyle,
              onChanged: (style) => engineState.switchStyle(style),
            ),

          // 输入区域
          Padding(
            padding: const EdgeInsets.all(12),
            child: Column(
              children: [
                TextField(
                  controller: _textController,
                  decoration: InputDecoration(
                    hintText: engineState.activeMode == EngineType.inspiration
                        ? '粘贴对方发来的消息...'
                        : '输入对方最后一条消息...',
                    border: const OutlineInputBorder(),
                    suffixIcon: engineState.activeMode ==
                            EngineType.inspiration
                        ? IconButton(
                            icon: const Icon(Icons.paste),
                            tooltip: '粘贴即回',
                            onPressed: () async {},
                          )
                        : null,
                  ),
                  maxLines: 3,
                  minLines: 1,
                ),
                const SizedBox(height: 8),
                TextField(
                  controller: _contextController,
                  decoration: const InputDecoration(
                    hintText: '额外上下文（可选）',
                    border: OutlineInputBorder(),
                    isDense: true,
                  ),
                  maxLines: 2,
                  minLines: 1,
                ),
                const SizedBox(height: 12),
                SizedBox(
                  width: double.infinity,
                  child: FilledButton.icon(
                    onPressed: engineState.isLoading
                        ? null
                        : () => _generateReply(engineState),
                    icon: engineState.isLoading
                        ? const SizedBox(
                            width: 16,
                            height: 16,
                            child: CircularProgressIndicator(strokeWidth: 2))
                        : Icon(engineState.activeMode == EngineType.strategist
                            ? Icons.psychology
                            : Icons.auto_awesome),
                    label: Text(
                      engineState.activeMode == EngineType.autoPilot
                          ? '全托管模式'
                          : engineState.activeMode == EngineType.strategist
                              ? '生成回复候选'
                              : '粘贴即回',
                    ),
                  ),
                ),
              ],
            ),
          ),

          // 心理分析（策略军师模式）
          if (engineState.analysis.isNotEmpty)
            Container(
              width: double.infinity,
              margin: const EdgeInsets.symmetric(horizontal: 12),
              padding: const EdgeInsets.all(12),
              decoration: BoxDecoration(
                color: Theme.of(context)
                    .colorScheme
                    .tertiaryContainer,
                borderRadius: BorderRadius.circular(12),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text('心理分析',
                      style: Theme.of(context).textTheme.labelLarge),
                  const SizedBox(height: 4),
                  Text(engineState.analysis),
                ],
              ),
            ),

          // 回复列表
          if (engineState.currentReplies.isNotEmpty)
            Expanded(
              child: ListView.builder(
                padding: const EdgeInsets.all(12),
                itemCount: engineState.currentReplies.length,
                itemBuilder: (_, i) => ReplyCard(
                  reply: engineState.currentReplies[i],
                  index: i,
                  onTap: (text) => _textController.text = text,
                  onRefresh: () => _generateReply(engineState),
                ),
              ),
            ),

          // 错误提示
          if (engineState.lastError.isNotEmpty)
            Padding(
              padding: const EdgeInsets.all(12),
              child: Text(engineState.lastError,
                  style: TextStyle(color: Theme.of(context).colorScheme.error)),
            ),
        ],
      ),
    );
  }

  Future<void> _generateReply(EngineState engineState) async {
    if (_textController.text.isEmpty) return;
    await engineState.generateReply(
      lastMessage: _textController.text,
      context: _contextController.text,
    );
  }
}

/// 模式切换器
class _ModeSelector extends StatelessWidget {
  final EngineType currentMode;
  final ValueChanged<EngineType> onChanged;

  const _ModeSelector(
      {required this.currentMode, required this.onChanged});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
      child: SegmentedButton<EngineType>(
        segments: const [
          ButtonSegment(
              value: EngineType.autoPilot,
              icon: Icon(Icons.auto_mode),
              label: Text('替身')),
          ButtonSegment(
              value: EngineType.strategist,
              icon: Icon(Icons.psychology),
              label: Text('军师')),
          ButtonSegment(
              value: EngineType.inspiration,
              icon: Icon(Icons.lightbulb),
              label: Text('灵感')),
        ],
        selected: {currentMode},
        onSelectionChanged: (selected) => onChanged(selected.first),
      ),
    );
  }
}

/// 赛博替身控制栏
class _AutoPilotControls extends StatelessWidget {
  final bool isActive;
  final VoidCallback onToggle;

  const _AutoPilotControls(
      {required this.isActive, required this.onToggle});

  @override
  Widget build(BuildContext context) {
    return Card(
      margin: const EdgeInsets.symmetric(horizontal: 12),
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
        child: Row(
          children: [
            Switch(value: isActive, onChanged: (_) => onToggle()),
            Text(isActive ? '托管中' : '已暂停',
                style: Theme.of(context).textTheme.labelLarge),
            const Spacer(),
            ActionChip(
                label: const Text('终结'),
                onPressed: isActive ? () {} : null),
            const SizedBox(width: 4),
            ActionChip(
                label: const Text('出击'),
                onPressed: isActive ? () {} : null),
            const SizedBox(width: 4),
            ActionChip(
                label: const Text('表情包'),
                onPressed: isActive ? () {} : null),
          ],
        ),
      ),
    );
  }
}

/// 风格选择器（灵感模式）
class _StyleSelector extends StatelessWidget {
  final ChatStyle currentStyle;
  final ValueChanged<ChatStyle> onChanged;

  const _StyleSelector(
      {required this.currentStyle, required this.onChanged});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 12),
      child: SingleChildScrollView(
        scrollDirection: Axis.horizontal,
        child: Row(
          children: ChatStyle.values.map((style) {
            final selected = style == currentStyle;
            return Padding(
              padding: const EdgeInsets.only(right: 6),
              child: FilterChip(
                label: Text(style.label),
                selected: selected,
                onSelected: (_) => onChanged(style),
              ),
            );
          }).toList(),
        ),
      ),
    );
  }
}
