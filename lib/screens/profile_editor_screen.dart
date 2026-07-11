import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../providers/profile_state.dart';

/// 人物小传编辑页面
/// 让用户填写自己的人物设定，作为所有模式的 Prompt 基础
class ProfileEditorScreen extends StatefulWidget {
  const ProfileEditorScreen({super.key});

  @override
  State<ProfileEditorScreen> createState() => _ProfileEditorScreenState();
}

class _ProfileEditorScreenState extends State<ProfileEditorScreen> {
  final _controllers = <String, TextEditingController>{};
  bool _isSaving = false;

  final _fields = [
    ('age', '年龄', TextInputType.number),
    ('zodiac', '星座', TextInputType.text),
    ('mbti', 'MBTI 人格', TextInputType.text),
    ('occupation', '职业', TextInputType.text),
    ('income', '收入范围', TextInputType.text),
    ('hobbies', '兴趣爱好', TextInputType.multiline),
    ('loveView', '感情观', TextInputType.multiline),
    ('verbalTics', '经典口癖', TextInputType.text),
    ('extraInfo', '额外设定', TextInputType.multiline),
  ];

  @override
  void initState() {
    super.initState();
    final profile = context.read<ProfileState>().profile;
    for (final (key, _, _) in _fields) {
      _controllers[key] = TextEditingController();
    }
    _syncFromProfile(profile);
  }

  @override
  void dispose() {
    for (final c in _controllers.values) {
      c.dispose();
    }
    super.dispose();
  }

  void _syncFromProfile(profile) {
    setState(() {
      _controllers['age']?.text = profile.age;
      _controllers['zodiac']?.text = profile.zodiac;
      _controllers['mbti']?.text = profile.mbti;
      _controllers['occupation']?.text = profile.occupation;
      _controllers['income']?.text = profile.income;
      _controllers['hobbies']?.text = profile.hobbies;
      _controllers['loveView']?.text = profile.loveView;
      _controllers['verbalTics']?.text = profile.verbalTics;
      _controllers['extraInfo']?.text = profile.extraInfo;
    });
  }

  Future<void> _save() async {
    setState(() => _isSaving = true);
    final profileState = context.read<ProfileState>();
    for (final (key, _, _) in _fields) {
      profileState.updateField(key, _controllers[key]?.text ?? '');
    }
    await profileState.saveProfile(profileState.profile);
    setState(() => _isSaving = false);
    if (mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('人物小传已保存（加密存储）')),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    final profileState = context.watch<ProfileState>();

    return Scaffold(
      appBar: AppBar(
        title: const Text('人物小传设定'),
        actions: [
          TextButton.icon(
            onPressed: _isSaving ? null : _save,
            icon: _isSaving
                ? const SizedBox(
                    width: 16,
                    height: 16,
                    child: CircularProgressIndicator(strokeWidth: 2))
                : const Icon(Icons.save),
            label: const Text('保存'),
          ),
        ],
      ),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          Card(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Icon(Icons.person,
                          color: Theme.of(context).colorScheme.primary),
                      const SizedBox(width: 8),
                      Text('我的设定',
                          style: Theme.of(context).textTheme.titleMedium),
                    ],
                  ),
                  const SizedBox(height: 4),
                  Text(
                    '这些信息将作为所有聊天模式的基础 Prompt\n设置越详细，AI 回复越贴近你的真实人设',
                    style: Theme.of(context).textTheme.bodySmall?.copyWith(
                        color: Theme.of(context).colorScheme.onSurfaceVariant),
                  ),
                  if (profileState.isSaved)
                    Chip(
                      avatar: const Icon(Icons.check, size: 16),
                      label: const Text('已加密保存'),
                      color: WidgetStatePropertyAll(Theme.of(context)
                          .colorScheme
                          .primaryContainer),
                    ),
                ],
              ),
            ),
          ),
          const SizedBox(height: 16),
          ..._fields.map((field) {
            final (key, label, keyboardType) = field;
            return Padding(
              padding: const EdgeInsets.only(bottom: 12),
              child: TextField(
                controller: _controllers[key],
                keyboardType: keyboardType,
                maxLines: keyboardType == TextInputType.multiline ? 3 : 1,
                decoration: InputDecoration(
                  labelText: label,
                  border: const OutlineInputBorder(),
                ),
              ),
            );
          }),
          const SizedBox(height: 24),
          OutlinedButton.icon(
            onPressed: () async {
              final confirm = await showDialog<bool>(
                context: context,
                builder: (ctx) => AlertDialog(
                  title: const Text('重置'),
                  content: const Text('确定要清除所有人物设定吗？'),
                  actions: [
                    TextButton(
                        onPressed: () => Navigator.pop(ctx, false),
                        child: const Text('取消')),
                    TextButton(
                        onPressed: () => Navigator.pop(ctx, true),
                        child: const Text('确定')),
                  ],
                ),
              );
              if (confirm == true) {
                await context.read<ProfileState>().resetProfile();
                for (final c in _controllers.values) {
                  c.clear();
                }
              }
            },
            icon: const Icon(Icons.delete_sweep),
            label: const Text('重置全部设定'),
            style: OutlinedButton.styleFrom(
                foregroundColor: Theme.of(context).colorScheme.error),
          ),
        ],
      ),
    );
  }
}
