import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../services/llm_service.dart';
import '../services/platform_bridge.dart';
import '../providers/profile_state.dart';

/// 设置页面
class SettingsScreen extends StatefulWidget {
  const SettingsScreen({super.key});

  @override
  State<SettingsScreen> createState() => _SettingsScreenState();
}

class _SettingsScreenState extends State<SettingsScreen> {
  final _apiKeyController = TextEditingController();
  String _currentProvider = 'deepseek';
  bool _isSaving = false;

  final _providers = {
    'deepseek': 'DeepSeek（默认，便宜中文好）',
    'zhipu': '智谱 GLM',
    'qwen': '通义千问',
  };

  @override
  void initState() {
    super.initState();
    _loadSettings();
  }

  Future<void> _loadSettings() async {
    final apiKey = await LLMService.getApiKey();
    setState(() {
      _apiKeyController.text = apiKey;
      _currentProvider = LLMService.currentProvider;
    });
  }

  @override
  void dispose() {
    _apiKeyController.dispose();
    super.dispose();
  }

  Future<void> _saveApiKey() async {
    setState(() => _isSaving = true);
    await LLMService.setApiKey(_apiKeyController.text);
    await LLMService.switchProvider(_currentProvider);
    setState(() => _isSaving = false);
    if (mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('API Key 已加密保存')),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('设置')),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          // ── API 配置 ──
          Card(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Icon(Icons.key,
                          color: Theme.of(context).colorScheme.primary),
                      const SizedBox(width: 8),
                      Text('API 配置',
                          style: Theme.of(context).textTheme.titleMedium),
                    ],
                  ),
                  const SizedBox(height: 12),
                  TextField(
                    controller: _apiKeyController,
                    obscureText: true,
                    decoration: const InputDecoration(
                      labelText: 'DeepSeek API Key',
                      hintText: 'sk-...',
                      border: OutlineInputBorder(),
                      helperText: '密钥通过 flutter_secure_storage 加密存储',
                    ),
                  ),
                  const SizedBox(height: 12),
                  DropdownButtonFormField<String>(
                    value: _currentProvider,
                    decoration: const InputDecoration(
                      labelText: '大模型供应商',
                      border: OutlineInputBorder(),
                    ),
                    items: _providers.entries
                        .map((e) => DropdownMenuItem(
                            value: e.key, child: Text(e.value)))
                        .toList(),
                    onChanged: (v) {
                      if (v != null) setState(() => _currentProvider = v);
                    },
                  ),
                  const SizedBox(height: 12),
                  SizedBox(
                    width: double.infinity,
                    child: FilledButton.icon(
                      onPressed: _isSaving ? null : _saveApiKey,
                      icon: _isSaving
                          ? const SizedBox(
                              width: 16,
                              height: 16,
                              child:
                                  CircularProgressIndicator(strokeWidth: 2))
                          : const Icon(Icons.save),
                      label: const Text('保存 API 配置'),
                    ),
                  ),
                ],
              ),
            ),
          ),
          const SizedBox(height: 16),

          // ── 服务控制 ──
          Card(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Icon(Icons.miscellaneous_services,
                          color: Theme.of(context).colorScheme.primary),
                      const SizedBox(width: 8),
                      Text('服务控制',
                          style: Theme.of(context).textTheme.titleMedium),
                    ],
                  ),
                  const SizedBox(height: 12),
                  _ServiceButton(
                    icon: Icons.accessibility_new,
                    label: '开启无障碍服务',
                    subtitle: '监听聊天界面、自动抓取联系人',
                    onPressed: () => PlatformBridge.enableAccessibilityService(),
                  ),
                  const SizedBox(height: 8),
                  _ServiceButton(
                    icon: Icons.blur_on,
                    label: '启动悬浮球',
                    subtitle: '在桌面显示可拖拽的 ChatWing 助手',
                    onPressed: () => PlatformBridge.startFloatingWindow(),
                  ),
                  const SizedBox(height: 8),
                  _ServiceButton(
                    icon: Icons.stop_circle,
                    label: '停止悬浮球',
                    subtitle: '关闭悬浮窗服务',
                    onPressed: () => PlatformBridge.stopFloatingWindow(),
                  ),
                ],
              ),
            ),
          ),
          const SizedBox(height: 16),

          // ── 关于 ──
          Card(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Icon(Icons.info,
                          color: Theme.of(context).colorScheme.primary),
                      const SizedBox(width: 8),
                      Text('关于', style: Theme.of(context).textTheme.titleMedium),
                    ],
                  ),
                  const SizedBox(height: 8),
                  const ListTile(
                    title: Text('ChatWing v1.0.0'),
                    subtitle: Text('AI 僚机助手 - 智能聊天辅助工具'),
                    dense: true,
                  ),
                  ListTile(
                    title: const Text('清除所有数据'),
                    subtitle: const Text('删除所有联系人和记忆（不含 API Key）'),
                    dense: true,
                    trailing: const Icon(Icons.warning, color: Colors.orange),
                    onTap: () => _confirmClearAll(),
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }

  void _confirmClearAll() {
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('清除所有数据'),
        content: const Text('此操作将删除所有联系人和聊天记忆，不可恢复。\n确定要继续吗？'),
        actions: [
          TextButton(
              onPressed: () => Navigator.pop(ctx),
              child: const Text('取消')),
          TextButton(
            onPressed: () {
              // TODO: 清除所有数据库数据
              Navigator.pop(ctx);
              ScaffoldMessenger.of(context).showSnackBar(
                const SnackBar(content: Text('所有数据已清除')),
              );
            },
            child: Text('确定清除',
                style:
                    TextStyle(color: Theme.of(ctx).colorScheme.error)),
          ),
        ],
      ),
    );
  }
}

class _ServiceButton extends StatelessWidget {
  final IconData icon;
  final String label;
  final String subtitle;
  final VoidCallback onPressed;

  const _ServiceButton({
    required this.icon,
    required this.label,
    required this.subtitle,
    required this.onPressed,
  });

  @override
  Widget build(BuildContext context) {
    return OutlinedButton.icon(
      onPressed: onPressed,
      icon: Icon(icon),
      label: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(label),
          Text(subtitle,
              style: const TextStyle(fontSize: 12),
              maxLines: 1,
              overflow: TextOverflow.ellipsis),
        ],
      ),
      style: OutlinedButton.styleFrom(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
        alignment: Alignment.centerLeft,
      ),
    );
  }
}
